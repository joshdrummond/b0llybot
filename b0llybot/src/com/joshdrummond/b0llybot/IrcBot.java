package com.joshdrummond.b0llybot;

import java.io.FileInputStream;
import java.util.Properties;
import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.events.IRCEvent;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.MessageEvent;
import jerklib.listeners.IRCEventListener;


public class IrcBot implements IRCEventListener {

	private Properties props = null;
	
    public IrcBot(Properties props)
    {
    	this.props = props;
        ConnectionManager manager = new ConnectionManager(new Profile(props.getProperty("name"), props.getProperty("nick")));
        manager.requestConnection(props.getProperty("hostname"),  Integer.valueOf(props.getProperty("port"))).addIRCEventListener(this);
    }

    public static void main(String args[])
    {
    	try
    	{
        	Properties props = new Properties();
    		props.load(new FileInputStream("b0llybot.properties"));
            new IrcBot(props);
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
    @Override
    public void receiveEvent(IRCEvent e) {
        if (e.getType() == IRCEvent.Type.CONNECT_COMPLETE)
        {
            e.getSession().join(props.getProperty("channel"));
        }
        else if (e.getType() == IRCEvent.Type.JOIN_COMPLETE)
        {
            ((JoinCompleteEvent)e).getChannel().say("i'm a cranky bot, deal with it");
        }
        else if (e.getType() == IRCEvent.Type.CHANNEL_MESSAGE)
        {
            MessageEvent event = (MessageEvent)e;
            System.out.println(e.getRawEventData());
            if (e.getRawEventData().contains("owly"))
            {
                event.getChannel().say("wut? shutup");
            }
            else if (e.getRawEventData().startsWith(".weather"))
            {
                event.getChannel().say(getCurrentWeather(e.getRawEventData()));
            }
        }
    }
    
    private String getCurrentWeather(String msg)
    {
        return "";
    }
}

