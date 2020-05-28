package embot.core;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.PrivateChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class Main {
    static EMBot emBot;

    public static void main(String[] args) {
        //TODO Even though EMBot is primarily for one server only. Probably should add in something to handle being added to a new server
        //Create discord client
        DiscordClient client = DiscordClient.create(secret.getDiscordToken());
        emBot = new EMBot(client);
        //EMBot is a class I made which handles some extra information
        //It basically handles the current instance of the program and needs to be instantiated at the beginning
        client.getEventDispatcher().on(ReadyEvent.class).subscribe(ReadyDisconnectHandler::readyUp);
        //Assigns subscriber to perform certain initialization tasks when ready

        //FIXME the take(Duration.ofSeconds(10) is supposed to keep each Mono from running for longer than 10 seconds but I've yet to observe a timeout happen. Not sure if it's working or not
        //Assign commands to their respective command functions
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent())
                        //maps each message create event to a mono that gets the message content or nothing
                        .flatMap(content -> Flux.fromIterable(CommandHandler.getCommands().entrySet())
                                //maps message content mono to the commands detailed in CommandHandler
                                .filter(command -> CommandHandler.commandFilter(content, command.getKey()))
                                //filters to only commands that exist
                                .flatMap(command -> command.getValue().execute(event)).next()).take(Duration.ofSeconds(10))).subscribe();
                                //maps to the command method execution.

        //maps DM commands to their functions
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> event.getMessage().getChannel()
                        //maps each message create event to the channel
                        .filter(channel -> channel instanceof PrivateChannel)
                        //filters to just private channels (dm channels)
                        .flatMap(dm -> Mono.justOrEmpty(event.getMessage().getContent())
                                .flatMap(content -> Flux.fromIterable(DMHandler.getDmCommands().entrySet())
                                        //maps message content to commands detailed in DMHandler
                                        .filter(dmCommand -> content.toLowerCase().startsWith(dmCommand.getKey()))
                                        //filters to only commands that exist
                                        .flatMap(dmCommand -> dmCommand.getValue().execute(event)).next())).take(Duration.ofSeconds(10))).subscribe();

        //Simply clears both the prayer team channel and role when the prayer team announcement channel is deleted
        client.getEventDispatcher().on(TextChannelDeleteEvent.class)
                .map(TextChannelDeleteEvent::getChannel)
                //maps channel delete events to their channel
                .flatMap(channel -> channel.getGuild()
                        //maps channels to their guild
                        .map(guild -> emBot.getServer(guild))
                        //gets the Server class associated with the guild
                        .filter(server -> server.getPrayerTeamChannel().equals(channel.getId().asString())).doOnSuccess(server -> {
                            //Filters the server found to just servers with the channel deleted as the prayer team channel
                            //On success we set the prayer team channel and role to their default empty values
                            server.setPrayerTeamChannel("empty");
                            server.setPrayerTeamRole("empty");
                        }))
                .subscribe();

        //logs in the client
        client.login().block();
//TODO block bots from triggering embot perhaps? It could be fun so I'm leaving as is. Nothing I've written should cause disastrous loops yet hopefully
    }
    //Static class to return the current EMBot instance in other classes
    public static EMBot getEmBot() {
        return emBot;
    }

}
