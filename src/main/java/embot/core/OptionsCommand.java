package embot.core;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
/*Options command is a helper class which details information about commands which take extra options as inputs
  Includes:
    *options Mono string array containing each of the options
* */
public abstract class OptionsCommand extends Command{
    private Mono<String[]> options;

    public OptionsCommand(){
        super();
        options = null;
    }
    public OptionsCommand(String commandString){
        super(commandString);
        options = null;
    }
    //By default given a command string and an event Options Commands build options by splitting the trimmed command
    //options is set to a null Mono if there are no options included
    //Not used at the moment but could be used for future commands
    public OptionsCommand(String commandString,MessageCreateEvent event){
        super(commandString,event);
        options = trimCommand().defaultIfEmpty("").map(optionsString->{
            if(optionsString.equals(""))
                return new String[0];
            return optionsString.split(" ");});
    }
    public OptionsCommand(String commandString,MessageCreateEvent event, Mono<String[]> options){
        super(commandString,event);
        this.options = options;
    }

    public abstract Mono<Void> run();

    //simple get method
    public Mono<String[]> getOptions(){return  options;}

    //Method which handles malformed options errors.
    //TODO possibly make this an actual error and also maybe do the checking in this abstract class since it's pretty universal
    public Mono<Message> malformedOptionsError(MessageCreateEvent event, String errorString){
        return event.getMessage().getChannel().flatMap(channel->channel.createMessage(errorString).then(channel.createMessage("See " + CommandHandler.getPrefix() + "help for further instructions")));
    }

}
