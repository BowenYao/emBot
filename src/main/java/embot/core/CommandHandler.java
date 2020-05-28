package embot.core;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

//General class which handles all the CommandMethod methods which are executed in the Main class
public class CommandHandler {
    //The commands HashMaps maps command strings to their corresponding methods
    private static final Map<String, CommandMethod> commands = new HashMap<>();
    //just storing the command prefix
    private static final String prefix = "em:";

    //Static method initializes the commands HashMap. I'm pretty proud of this on it should make adding future commands easy
    static {
        /*Simply putting commands in manually.
        This is for commands that can not be toggled or otherwise restricted by admins and the like
            For now this is the help command and admin commands as well as my personal kill switch*/
        commands.put("help", CommandHandler::helpCommand);
        commands.put("toggle", CommandHandler::toggleCommand);
        commands.put("assign prayer team", CommandHandler::assignPTeamCommand);
        commands.put("kill", CommandHandler::killCommand);

        /*The following SUPER long code essentially does this:
        It gets all the commands listed in a list and then puts in a method which checks if the command is toggled and if the user has permission to use it
        If it does it then runs the command. All this is super expandable. All one needs to do to add new commands is add the command class in the list of Commands
        */
        for (Class<? extends Command> commandClass : Command.getCommandClasses()) {
            try {
                //gets the commandString retrieved from the command class by getting the getCommandString() method and invoking it with a default instance of the command
                //This means all commands scheduled with this process must have a PUBLIC default constructor which properly supplies the command string
                commands.put(commandClass.getMethod("getCommandString").invoke(commandClass.getConstructor().newInstance()).toString(),
                        //the following is the method which is put in as the value in the map which consumes a MessageCreateEvent
                        event -> {
                            try {
                                Command command = commandClass.getConstructor(MessageCreateEvent.class).newInstance(event);
                                //Gets the constructor for the class which takes a message create event and creates a new instance of it
                                return command.checkToggled()
                                        //Gets the command toggled state
                                        .flatMap(toggled -> {
                                            //maps the toggle state to a Mono<Void>
                                            if (toggled) {
                                                //If toggle is true the command is returned normally
                                                return event.getGuild()
                                                        //gets the guild associated with the event
                                                        .map(guild -> Main.getEmBot().getServer(guild))
                                                        //maps the guild to a Server instance
                                                        .flatMap(server -> server.memberAllowed(commandClass, event.getMessage().getAuthorAsMember()))
                                                        //maps the server to a boolean representing whether or not that member is allowed to use the command
                                                        .flatMap(allowed -> {
                                                            if (allowed)
                                                                //If they are allowed run the command
                                                                return command.run();
                                                            //Otherwise return a message informing them they can not use the command
                                                            return event.getMessage().getChannel()
                                                                    .flatMap(channel -> channel.createMessage("You do not have permission to use this command")).then();
                                                        });
                                            }
                                            //If toggled off a message informing the user about the command is sent instead
                                            return event.getMessage().getChannel()
                                                    .flatMap(channel -> channel.createMessage("The " + command.getCommandString() + " command is toggled off")).then();
                                        });
                            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                                /*This catch catches several errors which might occur while attempting to get methods and constructors related to the classes
                                Assuming the Command classes are defined properly this error should NOT occur.
                                Again all classes need to have a PUBLIC default constructor if they are to be put in Command.commandClasses()
                                */
                                e.printStackTrace();
                                Logger.log("Fatal error in command setup");
                                System.exit(1);
                            }
                            return null;
                        });
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                 /*This catch also catches several errors which might occur while attempting to get methods and constructors related to the classes
                                Assuming the Command classes are defined properly this error should NOT occur.
                                Again all classes need to have a PUBLIC default constructor if they are to be put in Command.commandClasses()
                                */
                e.printStackTrace();
                Logger.log("Fatal error in command setup");
                System.exit(1);
            }
        }
    }

    //All command methods defined below are NOT toggleable or otherwise hampered by admins.
    private static Mono<Void> helpCommand(MessageCreateEvent event) {
        //Handles the help command. Simply sends a help message
        return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(
                /*TODO put help text somewhere else. Maybe as an image or file so char limit doesn't stop me?
                    alternatively create different help commands for each command and keep this simple*/
                "Hello! My name is emBot.\n" +
                        "I am a Discord bot created with the purpose of helping moderate the Discord server of the Epic Movement at Penn State.\n\nAll commands are prefaced with " + prefix + "\n" +
                        "-----------------------------------------------------\n" +
                        prefix + "help -> prints this help text :thumbsup:\n" +
                        prefix + "play [url|pause|stop|title] -> handles different functions related to playing music\n" +
                        "\tplay [url] -> plays the song associated with the given url. Seems to work with soundCloud and YouTube links mainly\n" +
                        "\tplay pause -> pauses the current playing song\n" +
                        "\tplay stop -> stops the current playing song and disconnects emBot from the voice channel\n" +
                        "\tplay title -> displays the title and author of the current playing song\n" +
                        "-----------------------------------------------------\n\n" +
                        "DM Commands\n" +
                        "\tdm commands can only be used in a direct message to me emBot! Don't worry I won't tell anyone :shushing_face:\n" +
                        "\tdm commands DO NOT have the " + prefix + " prefix before them\n" +
                        "-----------------------------------------------------\n" +
                        "pray -> this command will create a private voice channel that only you and an assigned group of people can see.\n" +
                        "\tIt is designed to be as anonymous as possible\n" +
                        "\tThis is intended for people who feel like they need prayer or advice from someone but can't turn to someone specific\n" +
                        "-----------------------------------------------------\n\n" +
                        "Admin Commands (ooooh scary)\n" +
                        "\tadmin commands can only be used by admins\n" +
                        "\tadmin commands ARE prefaced by " + prefix + "\n" +
                        "-----------------------------------------------------\n" +
                        "toggle [command] -> Toggles a command on or off. Toggled commands cannot be used. Even by admins!\n" +
                        "\tcertain commands like help and other admin commands can not be toggled\n" +
                        "assign prayer team [roleID|role name|'clear'] -> assigns a role as the prayer team\n" +
                        "\tassign prayer team clear -> clears the current prayer team role\n" +
                        "-----------------------------------------------------\n\n" +
                        "Please talk to Bowen if you have any further questions!"
        ).then());

    }
    //Runs toggle command
    private static Mono<Void> toggleCommand(MessageCreateEvent event) {
        return new ToggleCommand(event).run();
    }
    //Run assign prayer team command
    private static Mono<Void> assignPTeamCommand(MessageCreateEvent event) {
        return new AssignPrayerTeamCommand(event).run();
    }
    //runs kill command
    private static Mono<Void> killCommand(MessageCreateEvent event) {
        //Kill button registered to me just in case
        //Disconnects the bot and stops the program
        return Mono.justOrEmpty(event.getMember())
                .flatMap(member -> {
                    String id = member.getId().asString();
                    if (id.equals("140958636234113025")) {
                        return ReadyDisconnectHandler.disconnect();
                    }
                    return Mono.empty();
                });
    }
    //Test command for personal use
    private static Mono<Void> testCommand(MessageCreateEvent event) {
        return event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("Hey! " + Snowflake.of("-1").asString())).then();
    }

    //Simple get methods
    protected static Map<String, CommandMethod> getCommands() {
        return commands;
    }

    protected static String getPrefix() {
        return prefix;
    }

    protected static boolean commandFilter(String content, String commandString) {
        //This function returns true if content is a command of the type specified by command string
        String lower = content.toLowerCase();
        if (lower.startsWith(prefix)) {
            lower = lower.replace(prefix, "");
            return lower.matches("^(?i)( ?" + commandString + ")( .*)?");
            /*This regex specifically matches the command string at the start of the string preceded by 0 or 1 spaces
             * ^(?i)( ?command_string)( .*)?
             * ^ forces it to be at the beginning of the string
             * (?i) Causes the rest of the pattern to be case insensitive
             * The [ ?] matches a whitespace 0 or 1 times.
             *( .*)? Allows the command to be followed by a space and then anything else 0 or 1 times with
             *This forces the command to have whitespace between the command and any options while allowing for commands with nothing after them
             * */
        }
        return false;
    }
}
