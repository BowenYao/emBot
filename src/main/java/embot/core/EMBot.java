package embot.core;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Flux;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

//Class handles information that needs to be stored during the runtime of the bot as instance variables
public class EMBot {
    //The following variables all have to do with the audio player
    private final LavaPlayerAudioProvider audioProvider;
    private final AudioPlayerManager manager;
    private final AudioPlayer player;
    private final TrackScheduler trackScheduler;

    //The discord client for the bot
    private final DiscordClient client;

    //Stores the current connect voice channel and its associated voice connection. Useful for pausing and stopping music and the like
    private VoiceChannel connectedVoiceChannel = null;
    private VoiceConnection currentVoiceConnection = null;
    private ArrayList<Server> servers = new ArrayList<>();

    EMBot(DiscordClient client) {
        manager = new DefaultAudioPlayerManager();
        manager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(manager);
        player = manager.createPlayer();
        player.setVolume(20);
        audioProvider = new LavaPlayerAudioProvider(player);
        trackScheduler = new TrackScheduler(player);
        this.client = client;
    }

    //Simple get methods
    public LavaPlayerAudioProvider getAudioProvider() {
        return audioProvider;
    }

    public AudioPlayerManager getManager() {
        return manager;
    }

    public TrackScheduler getTrackScheduler() {
        return trackScheduler;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public DiscordClient getClient() {
        return client;
    }

    public VoiceChannel getConnectedVoiceChannel() {
        return connectedVoiceChannel;
    }

    public VoiceConnection getCurrentVoiceConnection() {
        return currentVoiceConnection;
    }

    public ArrayList<Server> getServers() {
        return servers;
    }

    //Slightly more complicated get method which searches servers for the Server associated with a given guild
    public Server getServer(Guild guild) {
        for (Server server : servers) {
            if (guild.getId().asString().equals(server.getGuildId())) {
                return server;
            }
        }
        return null;
    }

    //Get method which returns a flux of guilds connected to a given user
    public Flux<Guild> getUserGuilds(User user) {
        return client.getGuilds()
                //Gets guilds connected to our Discord Client
                .flatMap(guild -> guild.getMemberById(user.getId())
                        //Maps guilds to the Member associated with our user. Should be null if the user is not a member of hte guild
                        .filter(Objects::nonNull)
                        //filters the members based off of whether or not they exist
                        .map(member -> guild));
        //maps the members back to the guild
    }

    //Simple set methods
    public void setConnectedVoiceChannel(VoiceChannel voiceChannel) {
        connectedVoiceChannel = voiceChannel;
    }

    public void setCurrentVoiceConnection(VoiceConnection voiceConnection) {
        currentVoiceConnection = voiceConnection;
    }

    public void setServers(ArrayList<Server> servers) {
        this.servers = servers;
    }

    //Saves persistent data to the ServerInits.ser file
    public void saveData() {
        Logger.log("Saving persistent data");
        File file = new File("ServerInits.ser");
        File backup = new File("ServerInitsbackup.ser");
        //File for both the data and a backup
        if (file.exists()) {
            Logger.log("Backing up previous data");
            try {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                //Tries to copy existing data to a backup
            } catch (IOException e) {
                Logger.log("Backup failed. Proceeding without backup");
            }
        }
        FileOutputStream persistentInfo;
        boolean backupNecessary = false;
        try {
            Logger.log("Writing persistent data to file");
            //TODO maybe does something with the result of create new file for more garbage protection
            file.createNewFile();
            //Creates a new file which overwrites the old one I believe so it can be written into
            persistentInfo = new FileOutputStream("ServerInits.ser");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(persistentInfo);
            objectOutputStream.writeObject(servers);
            //Writes the object to the file. Note this means the Server class MUST IMPLEMENT Serializable as well as all its variables
            objectOutputStream.close();
            persistentInfo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Logger.log("ServerInits.ser file not found. Data not stored.");
            backupNecessary = true;
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log("Error saving persistent data");
            backupNecessary = true;
        }
        //If an error is caught in the saving process the data is restored to the last save using the backup file from before

        //TODO maybe do something with the result of delete() for more garbage protection
        if (backupNecessary && backup.exists()) {
            Logger.log("Giving up on current data.\n  Attempting to restore backup...");
            try {
                Files.copy(backup.toPath(), file.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                backup.delete();
                //Copies the backup back to the original file and deletes the backup copy
            } catch (IOException e) {
                e.printStackTrace();
                Logger.log("Restoring to backup failed. Unclear what is stored in ServerInits.ser");
            }
        } else
            backup.delete();
        Logger.log("Finished saving data");
    }

    /*Method which updates the servers ArrayList based off of the persistent data servers
    This is designed to make the program able to add new commands without having to delete server preferences
    FIXME Currently changing Server in any way causes the saving feature fail and have to restart
     I can't think of a way to fix this however. I have no idea how to make serialization update resilient
     Maybe I'd just have to create a feature to update the code but I also don't know how to do that either*/
    public void updateServers(ArrayList<Server> servers) {
        for (Server server : servers) {
            //Iterates through each saved Server and gets their commandPermissions and commandToggle
            Map<Class<? extends Command>, Set<Role>> commandPermissions = server.getCommandPermissions();
            Map<Class<? extends Command>, Boolean> commandToggle = server.getCommandToggle();
            for (Class<? extends Command> commandClass : Command.getCommandClasses()) {
                server.getCommandPermissions().put(commandClass, commandPermissions.getOrDefault(commandClass, new HashSet<>()));
                server.getCommandToggle().put(commandClass, commandToggle.getOrDefault(commandClass, true));
                /*The above overwrites the commandPermissions and commandToggle associated with a specific command with the saved version.
                    It also creates a default if the saved data does not have a command that is in the current version. Possibly due to code updates.
                */
            }
            this.servers.remove(server);
            this.servers.add(server);
            //Since the server equals method only checks for the guildID remove removes the old server so we can add in the new one back in
        }

    }
}
