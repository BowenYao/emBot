package embot.core;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

//Class which extends the options command class for commands which only have a single option
//For now this isn't that necessary but perhaps I'll have enough commands in the future where this is worth it
public abstract class SingleOptionCommand extends OptionsCommand {
    public abstract Mono<Void> run();

    public SingleOptionCommand() {
        super();
    }

    public SingleOptionCommand(String commandString) {
        super(commandString);
    }

    //This class only changes really changes the one constructor by passing the entire trimCommand string as the only option
    public SingleOptionCommand(String commandString, MessageCreateEvent event) {
        super(commandString, event, trimCommand(commandString, event).map(option -> {
            if (option.equals(""))
                return new String[0];
            return new String[]{option};
        }));
    }

}
