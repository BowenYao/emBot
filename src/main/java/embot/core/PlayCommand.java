package embot.core;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

public class PlayCommand extends SingleOptionCommand {
    private static final String commandString = "play";

    public PlayCommand() {
        super(commandString);
    }

    public PlayCommand(MessageCreateEvent event) {
        super(commandString, event);
    }

    @Override
    public Mono<Void> run() {
        return getOptions().flatMap(option -> {
            if (option.length > 0) {
                switch (option[0].toLowerCase()) {
                    case "stop":
                        //Stops the current track
                        Main.getEmBot().getPlayer().stopTrack();
                        Main.getEmBot().getCurrentVoiceConnection().disconnect();
                        Main.getEmBot().setCurrentVoiceConnection(null);
                        Main.getEmBot().setConnectedVoiceChannel(null);
                        Main.getEmBot().getPlayer().setPaused(false);
                        return Mono.empty();
                    case "pause":
                        //Sets the player to pause if it is playing
                        if (Main.getEmBot().getPlayer().getPlayingTrack() != null) {
                            Main.getEmBot().getPlayer().setPaused(!Main.getEmBot().getPlayer().isPaused());
                            return Mono.empty();
                        }
                        //Otherwise inform user emBot is not playing
                        return getEvent().getMessage().getChannel()
                                .flatMap(channel->channel.createMessage("emBot is not playing any music right now!"));
                    case "title":
                        //Displays the author and title if music is playing and otherwise inform user emBot is not playing
                        String outString = "emBot is not playing any music right now!";
                        AudioTrack playing = Main.getEmBot().getPlayer().getPlayingTrack();
                        if (playing != null) {
                            outString = playing.getInfo().title + " by " + playing.getInfo().author;
                        }
                        String finalOutString = outString;
                        return getEvent().getMessage().getChannel()
                                .flatMap(channel -> channel.createMessage(finalOutString));
                    default:
                        //returns a Mono<Void> which connects emBot to the user's voice channel and plays requested music if found
                        return Mono.justOrEmpty(getEvent().getMember())
                                .flatMap(Member::getVoiceState)
                                .flatMap(VoiceState::getChannel)
                                .flatMap(channel -> {
                                    VoiceChannel connectedChannel = Main.getEmBot().getConnectedVoiceChannel();
                                    //If statement skips channel joining process and returns an empty mono if emBot is already connected to the requesting voice channel
                                    if (connectedChannel != null && connectedChannel.equals(channel)) {
                                        return Mono.empty();
                                    }
                                    Main.getEmBot().setConnectedVoiceChannel(channel);
                                    Mono<VoiceConnection> voiceConnectionMono = channel.join(spec -> spec.setProvider(Main.getEmBot().getAudioProvider()));
                                    return voiceConnectionMono.doOnNext(Main.getEmBot()::setCurrentVoiceConnection);
                                })
                                /*Tries to load the given audio track
                                 * Must be done with doFirst() since I believe the voice connection mono doesn't necessarily terminate
                                 * until the voice connection disconnects.
                                 * FIXME this might cause unforeseen errors where the track is not loaded at the right time
                                 *  I can't think of any cases atm but keep an eye out for better alternatives to doFirst()*/
                                .doFirst(() -> {
                                            try {
                                                Main.getEmBot().getManager().loadItem(option[0], Main.getEmBot().getTrackScheduler()).get();
                                            } catch (InterruptedException | ExecutionException e) {
                                                //Logs and informs the user if an unforeseen error occurs while loading track since loadItem() returns a Future
                                                Logger.log("Error while loading audio track");
                                                e.printStackTrace();
                                                getEvent().getMessage().getChannel().flatMap(channel -> channel.createMessage("Oops! Looks like there was an error loading the audio track. Please try again"));
                                            }
                                        }
                                );
                }
            } else {
                //Handles scenario where no options are provided
                //Logs malformed play command and informs user that command was incorrectly input
                Logger.log("Malformed play command");
                return malformedOptionsError(getEvent(), "The " + commandString + " command must be followed by a song url.");
            }
        }).then();
    }
}
