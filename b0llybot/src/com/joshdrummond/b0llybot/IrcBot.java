package com.joshdrummond.b0llybot;

import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.events.IRCEvent;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.MessageEvent;
import jerklib.listeners.IRCEventListener;

public class IrcBot implements IRCEventListener {

    public IrcBot(String name, String nick, String hostname, int port)
    {
        ConnectionManager manager = new ConnectionManager(new Profile(name, nick));
        manager.requestConnection(hostname,  port).addIRCEventListener(this);
    }

    public static void main(String args[])
    {
        new IrcBot("owly bot", "owly", "irc.colosolutions.net", 6667);
    }
    
    @Override
    public void receiveEvent(IRCEvent e) {
        if (e.getType() == IRCEvent.Type.CONNECT_COMPLETE)
        {
            e.getSession().join("#smashing_pumpkins");
        }
        else if (e.getType() == IRCEvent.Type.JOIN_COMPLETE)
        {
            ((JoinCompleteEvent)e).getChannel().say("i'm a cranky bot, deal with it");
        }
        else if (e.getType() == IRCEvent.Type.CHANNEL_MESSAGE)
        {
            MessageEvent event = (MessageEvent)e;
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

