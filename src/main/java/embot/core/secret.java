package embot.core;


import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.HashMap;

public class secret {
    //Class for accessing secret info. Right now it's just the discord bot token. Could expand for other apis and the like
    private static final HashMap<String, String> secrets = new HashMap<>(1);

    public static String getDiscordToken() {
        return secrets.get("discord_token");
    }

    static {
        File secret = new File("secret.json");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(secret);
            JsonReader jsonReader = new JsonReader(new InputStreamReader(fis));
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                secrets.put(name, jsonReader.nextString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
