package embot.core;

public class secret {
    //Class for accessing secret info. Right now it's just the discord bot token. Could expand for other apis and the like
    private static final String discordToken = "your token here";
    public static String getDiscordToken(){return discordToken;}
}
