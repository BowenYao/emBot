package embot.core;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;
//A simple method interface which is how commands are fired.
//See Main.java for more details
public interface CommandMethod {
    Mono<Void> execute(MessageCreateEvent event);
}
