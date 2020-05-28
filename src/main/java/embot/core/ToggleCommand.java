package embot.core;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;

//Simple admin single option command which toggles the ability to use a certain command
public class ToggleCommand extends SingleOptionCommand {
    private static final String commandString = "toggle";

    //default constructor
    public ToggleCommand() {
        //There's actually no need to include the command string or even this constructor for that matter
        // since toggle isn't one of the commands automatically handled in CommandHandler but it's fine
        super(commandString);
    }

    public ToggleCommand(MessageCreateEvent event) {
        super(commandString, event);
    }

    @Override
    public Mono<Void> run() {
        return checkAdmin().switchIfEmpty(getOptions()
                //Checks admin and if admin gets the options
                .flatMap(options -> {
                    //maps options to a message based off the option string
                    //Goes through all the commands in Command.commandClasses until it finds the relevant command and toggles it
                    for (Class<? extends Command> command : getCommandClasses()) {
                        try {
                            if (command.getMethod("getCommandString").invoke(command.getConstructor().newInstance()).toString().equals(options[0])) {
                                //Checks if the command string is equal to the options string
                                //NOTE it doesn't work if options is exactly right. Figured I'd be case sensitive here but I might change this
                                return getEvent().getGuild()
                                        //gets guild of the event
                                        .map(guild -> Main.getEmBot().getServer(guild))
                                        //maps guild to associated server
                                        .map(server -> server.toggleCommand(command))
                                        //toggles the command and maps server to the result
                                        .flatMap(toggle -> {
                                            //Maps toggle result to a string based off of whether or not the command was toggle on or off
                                            String out = "off";
                                            if (toggle)
                                                out = "on";
                                            String finalOut = out;
                                            Logger.log(options[0] + " toggled " + finalOut);
                                            return getEvent().getMessage().getChannel()
                                                    .flatMap(channel -> channel.createMessage(options[0] + " command toggled " + finalOut));
                                        });
                            }
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
                            e.printStackTrace();
                            //This shouldn't be possible as a failure would have occured earlier I think
                            // it is again due to an error with how a command class was structured if this error occurs
                        }
                    }
                    return getEvent().getMessage().getChannel()
                            .flatMap(channel -> channel.createMessage(options[0] + " is not a toggleable command"));
                    //returns error message if none of the commands match
                })).then();
    }
}
