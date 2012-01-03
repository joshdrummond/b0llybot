package com.joshdrummond.b0llybot;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.IRCEvent.Type;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.MessageEvent;
import jerklib.tasks.TaskImpl;


public class IrcBot
{
    private Properties props = null;
    Map<String, Reply> replies = null;
    
    public IrcBot(Properties props)
    {
        this.props = props;
        System.out.println("connecting to "+props.getProperty("hostname"));
        ConnectionManager manager = new ConnectionManager(new Profile(props.getProperty("name"), props.getProperty("nick")));
        Session session = manager.requestConnection(props.getProperty("hostname"), Integer.valueOf(props.getProperty("port")));
        loadReplies();
        addEventHandlers(session);
    }

    private void addEventHandlers(Session session)
    {
        session.onEvent(new TaskImpl("join_channel") {
            @Override
            public void receiveEvent(IRCEvent e) {
                System.out.println("joining "+props.getProperty("channel"));
                e.getSession().join(props.getProperty("channel"));
            }
        }, Type.CONNECT_COMPLETE);
        session.onEvent(new TaskImpl("introduce") {
            @Override
            public void receiveEvent(IRCEvent e) {
                ((JoinCompleteEvent)e).getChannel().say(props.getProperty("introduction"));
            }
        }, Type.JOIN_COMPLETE);
        session.onEvent(new TaskImpl("handle_message") {
            @Override
            public void receiveEvent(IRCEvent e) {
                MessageEvent event = (MessageEvent)e;
                String message = e.getRawEventData().toLowerCase();
                System.out.println(message);
                if (message.contains(".reload"))
                {
                    loadReplies();
                }
//                else if (message.contains("owly is dead to me"))
//                {
//                    event.getChannel().say(props.getProperty("introduction"));
//                    e.getSession().close("bye");
//                    System.exit(0);
//                }
                else if (message.contains(".weather"))
                {
                    event.getChannel().say(getCurrentWeather(message));
                }
                else
                {
                    for (String key : replies.keySet())
                    {
                        if (key.equals("*") || message.contains(key))
                        {
                            Reply reply = replies.get(key);
                            if ((new Random()).nextInt(100) < reply.getChance())
                            {
                                String[] answers = reply.getAnswers();
                                int i = (new Random().nextInt(answers.length));
                                event.getChannel().say(answers[i]);
                            }
                        }
                    }
                }
            }
        }, Type.CHANNEL_MESSAGE);
    }

    private void loadReplies()
    {
        System.out.println("loading replies");
        replies = new HashMap<String, Reply>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader("replies.dat"));
            String line = reader.readLine();
            while (line != null)
            {
//                System.out.println(line);
                StringTokenizer st = new StringTokenizer(line,"|");
                String trigger = st.nextToken();
                int chance = Integer.parseInt(st.nextToken());
                List<String> answers = new ArrayList<String>();
                while (st.hasMoreTokens())
                {
                    answers.add(st.nextToken());
                }
                replies.put(trigger, new Reply(chance, (String[])answers.toArray(new String[answers.size()])));
                line = reader.readLine();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
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
    
    private String getCurrentWeather(String msg)
    {
        return "";
    }
}

