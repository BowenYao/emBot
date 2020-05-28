package embot.core;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

//Class which handles the assign prayer team command.
//It is a single option admin command
public class AssignPrayerTeamCommand extends SingleOptionCommand {
    private static String commandString = "assign prayer team";

    //Default constructors nothing special
    public AssignPrayerTeamCommand() {
        super(commandString);
    }

    public AssignPrayerTeamCommand(MessageCreateEvent event) {
        super(commandString, event);
    }

    @Override
    public Mono<Void> run() {
        Logger.log("Prayer Team Assignment Command Triggered");
        //Checks for admin permission. If admin found the checkAdmin() mono returns empty and rest of the command is triggered
        return checkAdmin().switchIfEmpty(getOptions()
                //gets the options for this command
                .flatMap(options -> {
                    //Maps the option to a function that returns different messages based off of the option
                    if (options.length > 0) {
                        //Checks if an option is included
                        //As a single option command options should either be of length 0 or 1
                        try {
                            //Tries to parse the single option as a long
                            Long.parseLong(options[0]);
                            //If this passes returns a Mono which takes the long as a Snowflake
                            /*FIXME I think there might be an error which occurs if the role is not a role on the server.
                             *  Need to do some testing on that*/
                            return getEvent().getGuild()
                                    //Gets guild associated with the event
                                    .flatMap(guild -> guild.getRoleById(Snowflake.of(options[0]))
                                            //maps guilds to the role given
                                            .flatMap(role -> Main.getEmBot().getServer(guild).setPrayerTeamRole(role).then(getEvent().getMessage().getChannel()
                                                    .flatMap(channel -> channel.createMessage("Prayer Team Role set as " + role.getName())))));
                            //Sets the prayer team role and then sends a message confirming the role has been set.
                        } catch (NumberFormatException e) {
                            //If a NumberFormatException is thrown the bot checks for the word clear and then role
                            if (options[0].toLowerCase().equals("clear")) {
                                //Checks for clear command
                                //Returns a a Mono which clears the prayer team role and then sends a message confirming that the role has been cleared
                                return getEvent().getGuild()
                                        .map(Main.getEmBot()::getServer)
                                        .flatMap(Server::clearPrayerTeamRole)
                                        .then(getEvent().getMessage().getChannel()
                                                .flatMap(channel -> channel.createMessage("Prayer Team Cleared")));
                            } else {
                                //returns a Mono which tries to match the given option[0] with a role in the guild
                                return getEvent().getGuild()
                                        //gets the guild associated with the event
                                        .map(Main.getEmBot()::getServer)
                                        //maps the guild to a Server instance
                                        .flatMap(server -> server.findRole(options[0]))
                                        //maps server to a list of roles on the server matching the role name
                                        .flatMap(roles -> {
                                            Mono<Message> out = Mono.empty();
                                            if (roles.size() <= 0)
                                                //If there are no roles matching returns an error message
                                                return getEvent().getMessage().getChannel()
                                                        .flatMap(channel -> channel.createMessage("There is no role by the name of " + options[0] + " on this server"));
                                            else if (roles.size() > 1)
                                                //If there are multiple roles matching uses the first one found and returns a message mentioning this fact
                                                out = getEvent().getMessage().getChannel()
                                                        .flatMap(channel -> channel.createMessage("There are multiple roles by the name of " + options[0] + ". Picking the first one"));
                                            //Sets the role and then sends a confirmation message
                                            return out.then(getEvent().getGuild()
                                                    .flatMap(guild -> Main.getEmBot().getServer(guild).setPrayerTeamRole(roles.get(0)).then(getEvent().getMessage().getChannel()
                                                            .flatMap(channel -> channel.createMessage("Prayer Team Role set as " + roles.get(0).getName())))));
                                        });
                            }
                        }
                    } else {
                        //If there are no options then the command options are malformed
                        //TODO might package this in the OptionsCommand class as it should be universally true for all OptionsCommand instances
                        Logger.log("Malformed Prayer Team Assignment Command");
                        return malformedOptionsError(getEvent(), "The " + commandString + " command must be followed by a role ID or name");
                    }
                })).then();
    }
}
