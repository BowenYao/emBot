package embot.core;

import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.*;

//Server class is essentially a wrapper for the Discord4J Guild class which adds a few variables and methods
//An arraylist of Server instances is what is saved in the persistent data so Server MUST be Serializable
public class Server implements Serializable {
    //Stores all the allowed roles for each command on a server.
    //TODO currently nothing is done with the commandPermissions as there is no way to assign them something to do later
    private Map<Class<? extends Command>, Set<Role>> commandPermissions = new HashMap<>();
    //Stores all the toggle states for each command on a server
    private Map<Class<? extends Command>, Boolean> commandToggle = new HashMap<>();

    private String guildId;
    //TODO probably should change the default value for these strings
    private String prayerTeamRole = "empty";
    private String prayerTeamChannel = "empty";

    //default constructor
    public Server() {
        guildId = "";
        for (Class<? extends Command> commandClass : Command.getCommandClasses()) {
            commandPermissions.put(commandClass, new HashSet<>());
            commandToggle.put(commandClass, true);
        }
    }

    //Creates a new Server based off an existing guild. Not exactly used in this form since we load Servers from saved data almost exclusive
    //Could be useful still especially if we handle being invited to new guilds
    public Server(Guild guild) {
        guildId = guild.getId().asString();
        for (Class<? extends Command> commandClass : Command.getCommandClasses()) {
            commandPermissions.put(commandClass, new HashSet<>());
            commandToggle.put(commandClass, true);
        }
    }

    //Same as above but with a guildId rather than the guild itself. Useful since sometimes it's hard to get the guild outside of a Mono
    public Server(Snowflake guildId) {
        this.guildId = guildId.asString();
        for (Class<? extends Command> commandClass : Command.getCommandClasses()) {
            commandPermissions.put(commandClass, new HashSet<>());
            commandToggle.put(commandClass, true);
        }
    }

    //Add and remove role used to update the commandPermissions map but this hasn't been used yet
    public void addRole(Class<? extends Command> commandClass, Role role) {
        commandPermissions.get(commandClass).add(role);
    }

    public void removeRole(Class<? extends Command> commandClass, Role role) {
        commandPermissions.get(commandClass).remove(role);
    }

    //Toggles a command on or off
    public boolean toggleCommand(Class<? extends Command> commandClass) {
        boolean toggle = !commandToggle.get(commandClass);
        commandToggle.put(commandClass, toggle);
        return toggle;
    }

    //Checks if a role is allowed to use a command might be useless because of memberAllowed but we'll see
    public boolean roleAllowed(Class<? extends Command> commandClass, Role role) {
        //The default state, with no roles specifically allowed is always true
        if (commandPermissions.get(commandClass).isEmpty())
            return true;
        return commandPermissions.get(commandClass).contains(role);
    }

    //Checks if a specific member is allowed to use a command
    public Mono<Boolean> memberAllowed(Class<? extends Command> commandClass, Mono<Member> member) {
        if (commandPermissions.get(commandClass).isEmpty())
            //Again default is true if nothing is in the commandPermissions set for a command
            return Mono.just(true);
        return member.flatMap(mem -> mem.getRoles().collectList()
                //maps a member to the list of their roles
                .map(roles -> {
                    //maps the list of the roles to a boolean value based off of whether or not
                    // the list has a role contained in the corresponding set in commandPermissions
                    for (Role role : roles) {
                        if (commandPermissions.get(commandClass).contains(role)) {
                            return true;
                        }
                    }
                    return false;
                }));
        //Member allowed is lazy in that it only checks if at least one role is allowed not if a role is not specifically placed inside.
        //This means one has to properly remove roles from commands or people or they will still be able to use it
    }

    //Simple get commands
    public String getGuildId() {
        return guildId;
    }

    public boolean getToggle(Class<? extends Command> commandClass) {
        return commandToggle.get(commandClass);
    }

    public Map<Class<? extends Command>, Boolean> getCommandToggle() {
        return commandToggle;
    }

    public Map<Class<? extends Command>, Set<Role>> getCommandPermissions() {
        return commandPermissions;
    }

    public String getPrayerTeamRole() throws IllegalArgumentException {
        return prayerTeamRole;
    }

    public String getPrayerTeamChannel() throws IllegalArgumentException {
        return prayerTeamChannel;
    }

    //Simple set commands
    public void setPrayerTeamChannel(String prayerTeamChannel) {
        this.prayerTeamChannel = prayerTeamChannel;
    }

    public void setPrayerTeamRole(String prayerTeamRole) {
        this.prayerTeamRole = prayerTeamRole;
    }

    //This method is pretty complex as it not only sets the prayer team role but also creates a new channel for that role if it doesn't already exist and modifies the permissions for the role and channel
    //I am not going to protect against prayer room existing while a switch occurs. That can be handled by users
    public Mono<Void> setPrayerTeamRole(Role role) throws IllegalArgumentException {
        if (role.getGuildId().asString().equals(guildId)) {
            String oldPrayerTeamRole = prayerTeamRole;
            prayerTeamRole = role.getId().asString();
            //saves the old prayer team role for later use

            //The following section creates the overwrites for the prayer team role, everyone role, and author
            PermissionOverwrite prayerTeamOverwrite = PermissionOverwrite.forRole(role.getId(), PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES), PermissionSet.none());
            //Note the next two are Mono<PermissionOverwrite> instances
            Mono<PermissionOverwrite> everyoneOverwrite = role.getGuild()
                    .flatMap(Guild::getEveryoneRole)
                    .map(everyone -> PermissionOverwrite.forRole(everyone.getId(), PermissionSet.none(), PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES)));
            Mono<PermissionOverwrite> botOverwrite = Main.getEmBot().getClient().getSelf()
                    .map(User::getId)
                    .map(snowflake -> PermissionOverwrite.forMember(snowflake, PermissionSet.all(), PermissionSet.none()));
            //Creates a flux of all the overwrites
            Flux<PermissionOverwrite> overwrites = Mono.just(prayerTeamOverwrite).concatWith(botOverwrite).concatWith(everyoneOverwrite);

            if (prayerTeamChannel.equals("empty")) {
                //If a prayer team channel doesn't already exist create it
                Set<PermissionOverwrite> permissionOverwrites = new HashSet<>();
                //Returns a Mono which creates the channel and then sets it for this Server instance
                return Main.getEmBot().getClient().getGuildById(Snowflake.of(guildId))
                        //Get associated guild
                        .flatMap(guild -> overwrites.collectList()
                                //Get the overwrites as a list
                                .flatMap(overwriteList -> {
                                    permissionOverwrites.addAll(overwriteList);
                                    //adds all the overwrites from before to the set
                                    //creates the channel with the following specifications and then sets the prayer team channel for this server
                                    return guild.createTextChannel(spec -> spec.setName("Prayer Team Announcement Channel").setPermissionOverwrites(permissionOverwrites))
                                            .doOnSuccess(channel -> {
                                                prayerTeamChannel = channel.getId().asString();
                                            });
                                })).then();
            }
            //Updates the permission for the channel if it is already existing
            return role.getGuild()
                    .flatMap(guild -> guild.getChannelById(Snowflake.of(prayerTeamChannel))
                            //gets associated guild
                            .flatMap(channel -> {
                                Mono<Void> out = channel.addRoleOverwrite(role.getId(), prayerTeamOverwrite);
                                //Overwrites the new prayer team role permissions
                                if (oldPrayerTeamRole.equals("empty"))
                                    //Stops here if the old team role wasn't defined
                                    return out;
                                //Otherwise get the old team role and add overwrites for that role
                                Snowflake oldRole = Snowflake.of(oldPrayerTeamRole);
                                PermissionOverwrite oldPrayerOverwrite = PermissionOverwrite.forRole(oldRole, PermissionSet.none(), PermissionSet.all());
                                //create the channel with the overwrites
                                return out.then(channel.addRoleOverwrite(oldRole, oldPrayerOverwrite));
                            }));
        } else
            throw new IllegalArgumentException("Role not from this server");
    }
    //Clears a prayer team role.
    public Mono<Void> clearPrayerTeamRole() {
        if (prayerTeamRole.equals("empty"))
            return Mono.empty();
        //Doesn't do anything if it's already clear
        else {
            //Otherwise update the values and also update the permissions for the prayer team channel.
            String oldRole = prayerTeamRole;
            prayerTeamRole = "empty";
            return Main.getEmBot().getClient().getGuildById(Snowflake.of(guildId))
                    .flatMap(guild -> guild.getChannelById(Snowflake.of(prayerTeamChannel))
                            .flatMap(channel -> channel.addRoleOverwrite(Snowflake.of(oldRole), PermissionOverwrite.forRole(Snowflake.of(oldRole), PermissionSet.none(), PermissionSet.all()))));
        }
    }

    //Finds a role in a server based off its name
    public Mono<List<Role>> findRole(String roleName) {
        return Main.getEmBot().getClient().getGuildById(Snowflake.of(guildId))
                //gets the guild associated with this Server
                .flatMapMany(Guild::getRoles)
                //Maps the guild to a flux of all the roles
                .filter(role -> role.getName().equals(roleName)).collectList();
                //filters the roles to just the ones which have the given role name and collects the flux into a list
    }

    //equals method I wrote to make Servers able to be removed from arraylists easier
    public boolean equals(Object server) {
        if (server instanceof Server) {
            return guildId.equals(((Server) server).guildId);
        }
        return false;
    }

    //Simple to string method I wrote real quick to help with debugging
    public String toString() {
        return "Guild ID: " + guildId + "\n" +
                "Prayer Channel ID: " + prayerTeamChannel + "\n" +
                "Prayer Team Role ID: " + prayerTeamRole;
    }
}
