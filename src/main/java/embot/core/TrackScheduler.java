package embot.core;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

//Simple class which handles loading audio tracks into lavaplayer
//TODO maybe figure out how to queue things
public class TrackScheduler implements AudioLoadResultHandler {
    private final AudioPlayer player;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        Logger.log("Track " + audioTrack.getInfo().title + " loaded");
        player.playTrack(audioTrack);
    }
    //TODO play all songs in a playlist? Seems a bit spammy though.
    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        AudioTrack audioTrack = audioPlaylist.getSelectedTrack();
        Logger.log("Playlist " + audioPlaylist.getName() + " loaded.\n Playing track " + audioTrack.getInfo().title + " loaded");
        player.playTrack(audioTrack);
    }

    @Override
    public void noMatches() {
        Logger.log("No matches found");
    }

    //TODO handle the load failing better perhaps
    @Override
    public void loadFailed(FriendlyException e) {
        e.printStackTrace();
    }
}
