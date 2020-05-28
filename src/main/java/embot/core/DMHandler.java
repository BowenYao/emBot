package embot.core;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//General class which handles DM commands. Very similar to CommandHandler
public class DMHandler {

    //hashmap maps dm command strings to their respective methods
    private static Map<String, CommandMethod> dmCommands = new HashMap<>();

    //note there is no prefix for dm commands

    static {
        //Similar to in CommandHandler puts all the command strings and their methods in the hash map
        dmCommands.put("pray", DMHandler::prayDM);
    }

    //test dm command for personal use
    public static Mono<Void> testDM(MessageCreateEvent event) {
        return null;
    }

    /*currently the only dm command. Finds all servers emBot and the requesting user shares and attempts to create a private voice channel
     that only they and a user defined 'prayer team' can see. Also pings this team in a separate generated prayer team channel.
    TODO pray command currently hamfistedly goes through all servers a user shares with emBot.
     It might be nice to add a feature for users to specify which server they want. Lotta work tho*/
    public static Mono<Void> prayDM(MessageCreateEvent event) {
        return event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("The pray command will create a private voice channel for you to enter in EVERY server possible for you.\n" +
                        "After you are done with each voice channel please delete it. Thank you!"))
                //Method starts by sending some basic instructions to the user on what the pray command does
                .then(Mono.justOrEmpty(event.getMessage().getAuthor())
                        //gets the author of the message
                        .flatMapMany(author -> Main.getEmBot().getUserGuilds(author)
                                //Maps the author to all the guilds they are connected to
                                .flatMap(guild -> {
                                    //maps the guild to messages based off of the Server instance associated with the guild
                                    Server server = Main.getEmBot().getServer(guild);
                                    //Gets the Server associated with the guild
                                    if (server == null) {
                                        //This should not occur in normal runtime but if for some reason emBot does not have the current server saved an error message will be sent to the user for that guild
                                        return event.getMessage().getChannel()
                                                .flatMap(channel -> channel.createMessage("There was an error loading information for server " + guild.getName()));
                                    }
                                    String prayerTeamRole = server.getPrayerTeamRole();
                                    //Gets the prayer team role ID Snowflake
                                    if (prayerTeamRole.equals("empty")) {
                                        //If the prayer team role is not defined an error message is sent to the user for this server
                                        return event.getMessage().getChannel()
                                                .flatMap(channel -> channel.createMessage("A prayer voice channel could not be created for the " + guild.getName() + " server since a prayer team has not been assigned to that server"));
                                    }
                                    //If prayer team role and channel are both defined then the command proceeds as normal
                                    return guild.getRoleById(Snowflake.of(prayerTeamRole))
                                            //gets the role by the role ID of the server
                                            .flatMap(role -> prayerPermissionOverwrites(event, guild, role)
                                                    //maps it to a set of permission overwrites defined by the prayerPermissionOverwrites method
                                                    .flatMap(overwrites -> guild.createVoiceChannel(spec -> spec.setReason("Prayer Request").setName(author.getUsername() + "'s prayer room").setPermissionOverwrites(overwrites)))
                                                    //Creates the voice channel with the above specifications
                                                    .doOnSuccess(voiceChannel -> guild.getChannelById(Snowflake.of(server.getPrayerTeamChannel()))
                                                    //Gets the prayer team channel after the voice channel has been created
                                                            .flatMap(channel -> ((TextChannel) channel).createMessage(role.getMention() + ". Prayer request from " + author.getUsername() + ". " + voiceChannel.getName() + " created.")))).then(event.getMessage().getChannel()
                                                            //Sends a message to the prayer team channel alerting the prayer team
                                                    .flatMap(channel -> channel.createMessage("A prayer room created in " + guild.getName())));
                                                    //Sends a message afterwards the requesting user informing them the prayer room voice channel has been created
                                })).then());
    }

    public static Mono<Set<PermissionOverwrite>> prayerPermissionOverwrites(MessageCreateEvent event, Guild guild, Role prayerTeamRole) {
        //Simply returns a Mono containing the set of permission overwrites necessary for creating the voice channel
        return guild.getEveryoneRole().flatMap(everyoneRole ->
                Mono.justOrEmpty(event.getMessage().getAuthor())
                        .flatMap(author -> author.asMember(guild.getId()).map(member -> {
                            PermissionOverwrite prayerTeamOverwrite = PermissionOverwrite.forRole(prayerTeamRole.getId(), PermissionSet.of(Permission.CONNECT, Permission.VIEW_CHANNEL), PermissionSet.of(Permission.MANAGE_CHANNELS));
                            //Permits the prayer team to view and connect to the voice channel but not to delete it
                            PermissionOverwrite everyoneOverwrite = PermissionOverwrite.forRole(everyoneRole.getId(), PermissionSet.none(), PermissionSet.of(Permission.CONNECT, Permission.VIEW_CHANNEL));
                            //Forbids the everyone role from any permissions so it is as anonymous as possible
                            PermissionOverwrite authorOverwrite = PermissionOverwrite.forMember(member.getId(), PermissionSet.of(Permission.CONNECT, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNELS), PermissionSet.none());
                            //Allows author to see, view, and manage the prayer voice channel so they can delete after they're done with it
                            Set<PermissionOverwrite> overwrites = new HashSet<>();
                            overwrites.add(everyoneOverwrite);
                            overwrites.add(prayerTeamOverwrite);
                            overwrites.add(authorOverwrite);
                            return overwrites;
                        })));
    }
    //Simple get command
    public static Map<String, CommandMethod> getDmCommands() {
        return dmCommands;
    }
}
