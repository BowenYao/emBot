package embot.core;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Permission;
import embot.games.CannibalCommand;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;

/*Command is a helper class for commands which might require more than a simple method to execute
 * It is also to help build a structure to minimize rewriting shared code
 * As of now Command simply stores the command string and the event which spawned the command
 * Any other features are detailed in extending classes
 * TODO perhaps implement feature to check which roles are allowed to access given command
 * */
public abstract class Command {
    private static final ArrayList<Class<? extends Command>> commandClasses = new ArrayList<>(Arrays.asList(PlayCommand.class, CannibalCommand.class));
    private final String commandString;
    private final MessageCreateEvent event;

    //abstract method run is called to execute the command and therefore must be overridden in each subclass
    public abstract Mono<Void> run();

    //default constructor
    public Command() {
        commandString = "";
        event = null;
    }
    //Constructor used to make default command classes which are actually run
    public Command(String commandString) {
        this.commandString = commandString;
        event = null;
    }

    //simple constructor
    public Command(String commandString, MessageCreateEvent event) {
        this.commandString = commandString;
        this.event = event;
    }

    //simple get methods
    public String getCommandString() {
        return commandString;
    }

    public MessageCreateEvent getEvent() {
        return event;
    }

    public static ArrayList<Class<? extends Command>> getCommandClasses() {
        return commandClasses;
    }

    //Method that returns an empty Mono if the calling member is an admin. Otherwise sends a message informing user they need to be an admin to use the command
    public Mono<Message> checkAdmin() {
        return Mono.justOrEmpty(getEvent().getMember())
                //Gets Optional Member as a Mono. I believe this means admin commands do not run in DMs
                .flatMap(Member::getBasePermissions)
                //maps member to their base permissions
                .map(permissions -> permissions.contains(Permission.ADMINISTRATOR))
                //maps permissions to a boolean value based off whether or not they include the admin permission
                .flatMap(admin -> {
                    //If permissions don't include admin return error message
                    if (!admin)
                        return getEvent().getMessage().getChannel()
                                .flatMap(channel -> channel.createMessage("You need admin permissions to use the " + commandString + " command"));
                    //Otherwise return empty mono
                    return Mono.empty();
                });
    }

    //Function which checks whether or not a specific command has been toggled for a specific server
    public Mono<Boolean> checkToggled() {
        return getEvent().getGuild()
                //Gets guild
                .map(guild -> Main.getEmBot().getServer(guild))
                //maps guild to server
                .map(server ->  server.getToggle(this.getClass()));
                //maps server to their toggle info for the calling command class
    }

    //Command which takes returns a mono containing the string that spawned this command with the command trimmed off
    //Calls the static trimCommand
    public Mono<String> trimCommand() {
        assert event != null;
        //This assertion should be safe since commands need to be triggered by an event
        return trimCommand(commandString, event);
    }


    //Statically trims the command off of a given command event
    public static Mono<String> trimCommand(String commandString, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getContent())
                .map(content -> content.substring(content.toLowerCase().indexOf(commandString) + commandString.length()).trim());
        //Trims command by mapping the string to a substring starting after the command string
    }
}
