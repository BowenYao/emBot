package embot.core;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import discord4j.voice.AudioProvider;

import java.nio.ByteBuffer;

//A class to handle LavaPlayer stuff. I didn't really write any of this stuff refer to https://github.com/Discord4J/Discord4J/wiki/Music-Bot-Tutorial
public class LavaPlayerAudioProvider extends AudioProvider {
    private final AudioPlayer player;
    private final MutableAudioFrame frame = new MutableAudioFrame();
    LavaPlayerAudioProvider(final AudioPlayer player){
        super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
        frame.setBuffer(getBuffer());
        this.player = player;
    }
    public AudioPlayer getPlayer(){return player;}

    @Override
    public boolean provide() {
        final boolean provided = player.provide(frame);
        if(provided)
            getBuffer().flip();
        return provided;
    }
}
