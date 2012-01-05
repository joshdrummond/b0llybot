package com.joshdrummond.b0llybot;

import java.io.FileInputStream;
import java.util.Properties;


public class RunIrcBot {
    
    public static void main(String[] args)
    {
        try
        {
            Properties props = new Properties();
            props.load(new FileInputStream("b0llybot.properties"));
            IrcBot bot = new IrcBot(props);
            bot.setVerbose(true);
            bot.connect(props.getProperty("hostname"), Integer.valueOf(props.getProperty("port")));
            bot.joinChannel(props.getProperty("channel"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
