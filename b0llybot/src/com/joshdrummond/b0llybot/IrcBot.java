package com.joshdrummond.b0llybot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jibble.pircbot.PircBot;


public class IrcBot
    extends PircBot
{
    private Properties props = null;
    private Map<String, Reply> replies = null;
    private List<String> quotes = null;
    private List<String> opips = null;
    private static final String FILE_QUOTES = "quotes.dat";
    private static final String FILE_REPLIES = "replies.dat";
    private static final String FILE_OPIPS = "opips.dat";
    
    
    public IrcBot(Properties props)
    {
        this.props = props;
        this.setName(props.getProperty("nick"));
        this.setAutoNickChange(true);
        this.setLogin(getName());
        reload();
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message)
    {
    	String originalMessage = message;
        message = message.toLowerCase();
        if (message.contains(".reload"))
        {
            reload();
        }
        else if (message.contains(".wzf"))
        {
        	String[] s = message.split("wzf");
        	if (s.length > 1)
        	{
        		sendMessage(channel, getWeatherForecast(s[1].trim()));
        	}
        }
        else if (message.contains(".weather") || message.contains(".wz"))
        {
            String[] s = message.split("weather");
            if (s.length < 2)
            {
            	s = message.split("wz");
            }
            
            if (s.length > 1)
            {
                sendMessage(channel, getCurrentWeather(s[1].trim()));
            }
        }
        else if (message.contains(".quote"))
        {
            int num = 0;
            try
            {
                String[] s = message.split("quote");
                if (s.length > 1)
                {
                    String snum = s[1].trim();
                    if (!"".equals(snum))
                    {
                        snum = snum.startsWith("#") ? snum.substring(1) : snum;
                        num = Integer.parseInt(snum);
                        if ((num < 1) || (num > quotes.size()))
                        {
                            num = 0;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                num = 0;
            }
            
            if (0 == num)
            {
                num = (new Random()).nextInt(quotes.size()) + 1;
            }
            sendMessage(channel, "Quote #"+num+": "+quotes.get(num-1));
        }
        else if (originalMessage.contains(".addquote"))
        {
            int num = 0;
            try
            {
                String[] s = originalMessage.split("addquote");
                if (s.length > 1)
                {
                    String quote = s[1].trim();
                    if (!"".equals(quote))
                    {
                        quote += " - added by "+sender;
                        BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_QUOTES, true));
                        writer.write(quote+System.getProperty("line.separator"));
                        writer.close();
                        quotes.add(quote);
                        num = quotes.size();
                        sendMessage(channel, "Quote #"+num+" Added: "+quotes.get(num-1));
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else if (message.contains(".countquote"))
        {
            sendMessage(channel, "There are "+quotes.size()+" Quotes in the quote file.");
        }
        else if (message.contains(".findquote"))
        {
            try
            {
                String[] s = message.split("findquote");
                if (s.length > 1)
                {
                    String quote = s[1].trim().toLowerCase();
                    if (!"".equals(quote))
                    {
                        int num = 0;
                        String found = "";
                        for (String q : quotes)
                        {
                            num++;
                            if (q.toLowerCase().contains(quote))
                            {
                                found += String.valueOf(num)+", ";
                            }
                        }
                        if (!"".equals(found))
                        {
                            sendMessage(channel, "Quote(s) with the specified text are: "+found.substring(0, found.length()-2));
                        }
                        else
                        {
                            sendMessage(channel, "Quote(s) not found.");
                        }
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
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
                        sendMessage(channel, answers[i]);
                        break;
                    }
                }
            }
        }
    }
    
    @Override
    protected void onJoin(String channel, String sender, String login, String hostname)
    {
        if (sender.equals(getNick()))
        {
            sendMessage(channel, props.getProperty("introduction"));
        }
        
        if (isOpIP(hostname))
        {
        	op(channel, sender);
        }
        
//        else
//        {
//            sendMessage(channel, props.getProperty("other_introduction")+" "+sender);
//        }
    }
    
//    @Override
//    protected void onPart(String channel, String sender, String login, String hostname)
//    {
//        sendMessage(channel, props.getProperty("goodbye")+" "+sender);
//    }
//
//    @Override
//    protected void onQuit(String channel, String sender, String login, String hostname)
//    {
//        sendMessage(channel, props.getProperty("goodbye")+" "+sender);
//    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        message = message.toLowerCase();
        if (message.contains(".reload"))
        {
            reload();
        }
    }
    
    private boolean isOpIP(String hostname)
    {
    	try
    	{
	    	String ipAddress = InetAddress.getByName(hostname).getHostAddress();
	    	for (String opip : opips)
	    	{
	    		if (ipAddress.equals(opip))
	    		{
	    			return true;
	    		}
	    	}
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    	return false;
    }
    
    private void loadReplies()
    {
        System.out.println("loading replies");
        replies = new HashMap<String, Reply>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_REPLIES));
            String line = reader.readLine();
            while (line != null)
            {
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
            BufferedReader reader = new BufferedReader(new FileReader(FILE_QUOTES));
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
    
    private void loadOpIps()
    {
        System.out.println("loading opips");
        opips = new ArrayList<String>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_OPIPS));
            String line = reader.readLine();
            while (line != null)
            {
                line = line.trim();
                if (line != "") opips.add(line);
                line = reader.readLine();
            }
            reader.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private String getWeatherForecast(String location)
    {
    	int numDays = 5;
        String weather = "";
        location = location.replaceAll(" ", "%20");
        location = location.replaceAll(",", "%20");
        System.out.println("getting weather for "+location);
        try
        {
            String weatherApiKey = props.getProperty("weather_api_key");
            String jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetLocation.ashx?zip="+location+"&api_key="+weatherApiKey);
            JSONObject json = (JSONObject)JSONSerializer.toJSON(jsonString);
            String city = ((JSONObject)json.get("location")).getString("city");
            String state = ((JSONObject)json.get("location")).getString("state");
            String cityCode = ((JSONObject)json.get("location")).getString("cityCode");
            String country = ((JSONObject)json.get("location")).getString("country");
            String zipcode = ((JSONObject)json.get("location")).getString("zipCode");
            
            if (!cityCode.equals("null"))
            {
            	jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetForecast.ashx?city="+cityCode+"&nf="+numDays+"&ih=0&ht=t&ht=i&l=en&c=US&api_key="+weatherApiKey);
            }
            else
            {
            	jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetForecast.ashx?zip="+zipcode+"&nf="+numDays+"&ih=0&ht=t&ht=i&l=en&c=US&api_key="+weatherApiKey);
            }
            json = (JSONObject)JSONSerializer.toJSON(jsonString);
            weather = "Forecast conditions for "+city+", "+state+", "+country+" --- ";
            for (int i = 0; i < numDays; i++)
            {
            	if (i > 0) weather += "; ";
            	weather += ((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("title") + " - High: "+
            		((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("high") + json.getString("temperatureUnits") + ", Low: " +
            		((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("low") + json.getString("temperatureUnits");
            }
            //String temp = json.getString("temperature") + json.getString("temperatureUnits") + " ("+getCelcius(json.getString("temperature"))+"C)";
        }
        catch (Exception e)
        {
        	System.out.println("Error: "+e.getMessage());
            e.printStackTrace();
        }
        return weather;
    }
    
    private String getCurrentWeather(String location)
    {
        String weather = "";
        location = location.replaceAll(" ", "%20");
        location = location.replaceAll(",", "%20");
        System.out.println("getting weather for "+location);
        try
        {
            String weatherApiKey = props.getProperty("weather_api_key");
            String jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetLocation.ashx?zip="+location+"&api_key="+weatherApiKey);
            JSONObject json = (JSONObject)JSONSerializer.toJSON(jsonString);
            String city = ((JSONObject)json.get("location")).getString("city");
            String state = ((JSONObject)json.get("location")).getString("state");
            String cityCode = ((JSONObject)json.get("location")).getString("cityCode");
            String country = ((JSONObject)json.get("location")).getString("country");
            String zipcode = ((JSONObject)json.get("location")).getString("zipCode");
            
            if (!cityCode.equals("null"))
            {
            	jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetObs.ashx?city="+cityCode+"&ic=1&api_key="+weatherApiKey);
            }
            else
            {
            	jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetObs.ashx?zip="+zipcode+"&ic=1&api_key="+weatherApiKey);
            }
            json = (JSONObject)JSONSerializer.toJSON(jsonString);
            String station = json.getString("stationName");
            String temp = json.getString("temperature") + json.getString("temperatureUnits") + " ("+getCelcius(json.getString("temperature"))+"C)";
//            String tempC = (((Element)dom.getElementsByTagName("current_conditions").item(0)).getElementsByTagName("temp_c")).item(0).getAttributes().getNamedItem("data").getNodeValue();
            String condition = json.getString("desc");
            String humidity = json.getString("humidity") + json.getString("humidityUnits");
            String wind = json.getString("windDirection") + " at " + json.getString("windSpeed") + " "+json.getString("windUnits");
//            String datetime = json.getString("dateTime");
//            Date date = new Date(Long.valueOf(datetime));
//            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd h:mm a z");

            weather = "Current conditions for "+city+", "+state+", "+country+" ("+station+") --- "+temp+ " / "+condition+ " / Humidity: "+humidity+" / Wind: "+wind;
        }
        catch (Exception e)
        {
        	System.out.println("Error: "+e.getMessage());
            e.printStackTrace();
        }
        return weather;
    }
    
    private String getCelcius(String f)
    {
    	DecimalFormat df = new DecimalFormat("0.#");
    	String c = "";
    	try
    	{
    		double dFar = Double.valueOf(f);
    		double dCel = (5.0/9.0)*(dFar-32.0);
    		c = df.format(dCel);
    	}
    	catch (Exception e)
    	{
    		c = "unknown";
    	}
    	return c;
    }
    
    private String httpGet(String URL)
    	throws Exception
    {
        HttpClient hc = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(URL);
//        System.out.println("httpGet: "+httpget.getURI());
        HttpResponse response = hc.execute(httpget);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder builder = new StringBuilder();
        String line = reader.readLine();
        while (line != null)
        {
        	builder.append(line).append("\n");
        	line = reader.readLine();
        }
//        System.out.println("httpResponse: "+builder.toString());
        return builder.toString();
    }
    
    private void reload()
    {
        loadReplies();
        loadQuotes();
        loadOpIps();
    }
}

