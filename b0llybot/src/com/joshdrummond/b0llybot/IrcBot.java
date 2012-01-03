package com.joshdrummond.b0llybot;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    List<String> quotes = null;
    
    public IrcBot(Properties props)
    {
        this.props = props;
        System.out.println("connecting to "+props.getProperty("hostname"));
        ConnectionManager manager = new ConnectionManager(new Profile(props.getProperty("name"), props.getProperty("nick")));
        Session session = manager.requestConnection(props.getProperty("hostname"), Integer.valueOf(props.getProperty("port")));
        loadReplies();
        loadQuotes();
        addEventHandlers(session);
    }

    private void addEventHandlers(Session session)
    {
    	System.out.println("adding event handlers");
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
                    loadQuotes();
                }
//                else if (message.contains("owly is dead to me"))
//                {
//                    event.getChannel().say(props.getProperty("introduction"));
//                    e.getSession().close("bye");
//                    System.exit(0);
//                }
                else if (message.contains(".weather"))
                {
                	String[] s = message.split("weather");
                    event.getChannel().say(getCurrentWeather(s[1].trim()));
                }
                else if (message.contains(".quote"))
                {
                	int num = (new Random()).nextInt(quotes.size()) + 1;
                    event.getChannel().say("Quote #"+num+": "+quotes.get(num-1));
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
            reader.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private void loadQuotes()
    {
        System.out.println("loading quotes");
        quotes = new ArrayList<String>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader("quotes.dat"));
            String line = reader.readLine();
            while (line != null)
            {
            	line = line.trim();
            	if (line != "") quotes.add(line);
                line = reader.readLine();
            }
            reader.close();
        }
        catch (Exception e)
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
    
    private String getCurrentWeather(String location)
    {
    	String weather = "";
    	System.out.println("getting weather for "+location);
    	try
    	{
	    	HttpClient hc = new DefaultHttpClient();
	    	HttpGet httpget = new HttpGet("http://www.google.com/ig/api?hl=en&weather="+location.replaceAll(" ", "%20"));
	    	HttpResponse response = hc.execute(httpget);
	    	DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    	Document dom = db.parse(response.getEntity().getContent());
	    	String city = (((Element)dom.getElementsByTagName("forecast_information").item(0)).getElementsByTagName("city")).item(0).getAttributes().getNamedItem("data").getNodeValue();
	    	String tempF = (((Element)dom.getElementsByTagName("current_conditions").item(0)).getElementsByTagName("temp_f")).item(0).getAttributes().getNamedItem("data").getNodeValue();
	    	String tempC = (((Element)dom.getElementsByTagName("current_conditions").item(0)).getElementsByTagName("temp_c")).item(0).getAttributes().getNamedItem("data").getNodeValue();
	    	String condition = (((Element)dom.getElementsByTagName("current_conditions").item(0)).getElementsByTagName("condition")).item(0).getAttributes().getNamedItem("data").getNodeValue();
	    	String humidity = (((Element)dom.getElementsByTagName("current_conditions").item(0)).getElementsByTagName("humidity")).item(0).getAttributes().getNamedItem("data").getNodeValue();
	    	String wind = (((Element)dom.getElementsByTagName("current_conditions").item(0)).getElementsByTagName("wind_condition")).item(0).getAttributes().getNamedItem("data").getNodeValue();
	    	weather = "Current conditions for "+city+" are "+condition+ " "+tempF+"F / "+tempC+"C "+humidity+" "+wind;
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
        return weather;
    }
}

