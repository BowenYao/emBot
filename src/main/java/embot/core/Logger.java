package embot.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.GregorianCalendar;


//Class made to handle logging
//TODO implement a more legitimate logging than just printing and saving the file
public class Logger {
    public static void log(String logString) {
        //Logger prepends the current date to every log
        logString = new GregorianCalendar().getTime().toString() + ": " + logString;
        //Simply prints out the log
        System.out.println(logString);
        //Saves log to log file
        File file = new File("log.txt");
        try {
            //Creates log file if one doesn't exist
            if (!file.exists())
                file.createNewFile();
            FileWriter fw = new FileWriter(file, true);
            fw.write(logString + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            /*TODO handle something for the logger to do if writing to the log file fails
            *  Also possibly add something in to clear out older entries of the log file it'll probably get really long as is*/
        }
    }
}
