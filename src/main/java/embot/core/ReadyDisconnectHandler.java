package embot.core;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//Class to handle readying up and disconnecting
public class ReadyDisconnectHandler {
    public static void readyUp(ReadyEvent event) {
        event.getClient().updatePresence(Presence.online(Activity.playing("Type em: help for help"))).subscribe();
        //Updates presence

        //Below is very similar to EMBot.saveData() except it is loading the data instead
        File persistentInfo = new File("ServerInits.ser");
        boolean defaultNecessary = false;
        Set<ReadyEvent.Guild> guilds = event.getGuilds();
        if (persistentInfo.exists()) {
            //Load the servers arraylist if the persistent info exists
            Logger.log("Loading persistent data");
            try {
                FileInputStream fileInputStream = new FileInputStream(persistentInfo);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                ArrayList<Server> servers = (ArrayList<Server>) objectInputStream.readObject();
                if(servers.size()<guilds.size()){
                    defaultNecessary = true;
                }else
                    Main.getEmBot().updateServers(servers);
                objectInputStream.close();
                fileInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                //If there is an error getting the persistent data simply start over from scratch
                e.printStackTrace();
                Logger.log("Error getting persistent data. Restarting with default");
                defaultNecessary = true;
            }
        }else{
            //Otherwise create new save data from scratch
            Logger.log("Creating new persistent data file");
            try {
                persistentInfo.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Logger.log("Issue with creating persistent data file");
            }
            defaultNecessary = true;
        }
        if(defaultNecessary){
            //If it is necessary to create a default the bot creates a new arraylist of Server instances based off of connecting guilds
            ArrayList<Server> servers = new ArrayList<>();
            for(ReadyEvent.Guild guild:guilds){
                servers.add(new Server(guild.getId()));
            }
            Main.getEmBot().setServers(servers);
        }

        //The following code creates a timer which saves new data every hour currently
        /* FIXME Not sure if this is the offending code but sometimes it seems like commands break when run while data is being saved?
        *   KEEP AN EYE OUT but it seems like switching to ScheduledThreadPoolExecutor fixed the issue
        * Possible fix might be making the update threads daemons? */
        ScheduledThreadPoolExecutor updateScheduler = new ScheduledThreadPoolExecutor(1);
        updateScheduler.scheduleAtFixedRate(()->Main.getEmBot().saveData(),0,1, TimeUnit.HOURS);

        /*TODO initialize emBot.
            1.Get connected voice channels if any? (not sure how necessary)
        */
        Logger.log("Bot ready");
    }

//Disconnect can not be done off of a disconnect event since it takes too long and the bot loses its resources to fast to finish all this stuff
    public static Mono<Void> disconnect() {
        Logger.log("Disconnecting...");
        VoiceConnection voiceConnection = Main.getEmBot().getCurrentVoiceConnection();
        if (voiceConnection != null) {
            voiceConnection.disconnect();
            Main.getEmBot().setCurrentVoiceConnection(null);
            Main.getEmBot().setConnectedVoiceChannel(null);
        }
        Main.getEmBot().saveData();
        return Main.getEmBot().getClient().logout().doOnSuccess(thing->System.exit(0));
    }
}
