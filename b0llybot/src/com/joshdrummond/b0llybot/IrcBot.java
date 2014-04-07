package com.joshdrummond.b0llybot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;


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
    private Map<String, String> hostnames = null;
    
    public IrcBot(Properties props)
    {
    	this.hostnames = new HashMap<String,String>();
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
        if (message.contains(".finger"))
        {
        	String[] s = message.split("finger");
        	if (s.length > 1)
        	{
        		sendMessage(channel, finger(s[1].trim()));
        	}
        }
        else if (message.contains(".reload"))
        {
            reload();
        }
        else if (message.contains(".earthquake"))
        {
        	String[] s = message.trim().split(" ");
        	int numEarthquakes = 1;
        	String location = "";
        	if (s.length > 1)
        	{
           		location = s[1].trim();
           		if (isNumeric(location))
           		{
           			location = "";
           			numEarthquakes = safeDefaultInt(s[1].trim(), 1);
           		}
           		else if (s.length > 2)
        		{
        			numEarthquakes = safeDefaultInt(s[2].trim(), 1);
        		}
        	}
        	List<String> earthquakes = getAllEarthquakes(numEarthquakes, location);
        	for (String earthquake : earthquakes)
        	{
        		sendMessage(channel, earthquake);
        	}
        }
        else if (message.contains(".bigearthquake"))
        {
        	String[] s = message.trim().split(" ");
        	int numEarthquakes = 1;
        	String location = "";
        	if (s.length > 1)
        	{
           		location = s[1].trim();
           		if (isNumeric(location))
           		{
           			location = "";
           			numEarthquakes = safeDefaultInt(s[1].trim(), 1);
           		}
           		else if (s.length > 2)
        		{
        			numEarthquakes = safeDefaultInt(s[2].trim(), 1);
        		}
        	}
        	List<String> earthquakes = getBigEarthquakes(numEarthquakes, location);
        	for (String earthquake : earthquakes)
        	{
        		sendMessage(channel, earthquake);
        	}
        }
        else if (message.contains(".news"))
        {
        	String[] s = message.trim().split(" ");
        	int numNews = 1;
        	if (s.length > 1)
        	{
        		numNews = safeDefaultInt(s[1].trim(), 1);
        	}
        	List<String> news = getNews(numNews);
        	for (String newsItem : news)
        	{
        		sendMessage(channel, newsItem);
        	}
        }
        else if (message.contains(".wzf "))
        {
        	String[] s = message.split("wzf");
        	if (s.length > 1)
        	{
        		sendMessage(channel, getWeatherForecast(s[1].trim()));
        	}
        }
        else if (message.contains(".wzfd "))
        {
        	String[] s = message.split("wzfd");
        	if (s.length > 1)
        	{
            	List<String> weather = getWeatherForecastDetail(s[1].trim());
            	for (String weatherItem : weather)
            	{
            		sendMessage(channel, weatherItem);
            	}
        	}
        }
        else if (message.contains(".weather ") || message.contains(".wz "))
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
    
    private int safeDefaultInt(String s, int i)
    {
    	System.out.println("s="+s);
    	int safeInt = i;
    	if (null != s)
    	{
    		try
    		{
    			safeInt = Integer.parseInt(s);
    		}
    		catch (NumberFormatException e)
    		{
    		}
    	}
    	return safeInt;
    }
    
    private boolean isNumeric(String s)
    {
    	boolean isNumeric = true;
		try
		{
			Integer.parseInt(s);
		}
		catch (Exception e)
		{
			isNumeric = false;
		}
    	return isNumeric;
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
    
    private String finger(String user)
    {
    	System.out.println("finger "+user);
    	this.sendCTCPCommand(user, "FINGER");
    	return "";
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
    	//API- http://developer.weatherbug.com/docs/read/WeatherBug_API_JSON
    	int numDays = 5;
        String weather = "";
        location = location.replaceAll(" ", "%20");
        location = location.replaceAll(",", "%20");
        System.out.println("getting weather forecast for "+location);
        try
        {
            String weatherApiKey = props.getProperty("weather_api_key");
            //String jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetLocation.ashx?zip="+location+"&api_key="+weatherApiKey);
            String url = "http://i.wxbug.net/REST/Direct/GetLocationSearch.ashx?ss="+location+"&api_key="+weatherApiKey;
            String jsonString = httpGet(url);
            System.out.println(url);
            JSONObject json = (JSONObject)JSONSerializer.toJSON(jsonString);
            String city = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("city");
            String state = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("state");
            String cityCode = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("cityCode");
            String country = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("country");
            String zipcode = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("zipCode");
            
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
            	weather += ((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("title") + " - ";
            	
            	String high = ((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("high");
            	if (!high.equals("null"))
            	{
            		weather += "High: "+high + json.getString("temperatureUnits") + ", ";
            	}
            	weather += "Low: " +
            		((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("low") + json.getString("temperatureUnits");
            }
            //String temp = json.getString("temperature") + json.getString("temperatureUnits") + " ("+getCelcius(json.getString("temperature"))+"C)";
        }
        catch (Exception e)
        {
        	System.out.println("Error: "+e.getMessage());
            e.printStackTrace(System.out);
            weather = "error";
        }
        return weather;
    }
    
    private List<String> getWeatherForecastDetail(String location)
    {
    	//API- http://developer.weatherbug.com/docs/read/WeatherBug_API_JSON
    	int numDays = 3;
        List<String> weather = new ArrayList<String>();
        location = location.replaceAll(" ", "%20");
        location = location.replaceAll(",", "%20");
        System.out.println("getting weather forecast detail for "+location);
        try
        {
            String weatherApiKey = props.getProperty("weather_api_key");
            //String jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetLocation.ashx?zip="+location+"&api_key="+weatherApiKey);
            String url = "http://i.wxbug.net/REST/Direct/GetLocationSearch.ashx?ss="+location+"&api_key="+weatherApiKey;
            String jsonString = httpGet(url);
            System.out.println(url);
            JSONObject json = (JSONObject)JSONSerializer.toJSON(jsonString);
            String city = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("city");
            String state = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("state");
            String cityCode = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("cityCode");
            String country = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("country");
            String zipcode = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("zipCode");
            
            if (!cityCode.equals("null"))
            {
            	url = "http://i.wxbug.net/REST/Direct/GetForecast.ashx?city="+cityCode+"&nf="+numDays+"&ih=0&ht=t&ht=i&l=en&c=US&api_key="+weatherApiKey;
            }
            else
            {
            	url = "http://i.wxbug.net/REST/Direct/GetForecast.ashx?zip="+zipcode+"&nf="+numDays+"&ih=0&ht=t&ht=i&l=en&c=US&api_key="+weatherApiKey;
            }
            System.out.println(url);
        	jsonString = httpGet(url);
            json = (JSONObject)JSONSerializer.toJSON(jsonString);
            weather.add("Detailed Forecast conditions for "+city+", "+state+", "+country+" --- ");
            for (int i = 0; i < numDays; i++)
            {
            	//if (i > 0) weather += "; ";
            	String title = ((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("title") + " - ";
            	String day = ((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("dayPred");
            	if (!day.equals("null"))
            	{
            		title += day + " ";
            	}
            	String night = ((JSONObject)(json.getJSONArray("forecastList")).get(i)).getString("nightPred");
            	if (!night.equals("null"))
            	{
            		if (!day.equals("null")) title += "Night- ";
            		title += night;
            	}
            	weather.add(title);
            }
        }
        catch (Exception e)
        {
        	System.out.println("Error: "+e.getMessage());
            e.printStackTrace();
            weather.add("error");
        }
        return weather;
    }
    
    private String getCurrentWeather(String location)
    {
    	//API- http://developer.weatherbug.com/docs/read/WeatherBug_API_JSON
        String weather = "";
        location = location.replaceAll(" ", "%20");
        location = location.replaceAll(",", "%20");
        System.out.println("getting current weather for "+location);
        try
        {
            String weatherApiKey = props.getProperty("weather_api_key");
            //String jsonString = httpGet("http://i.wxbug.net/REST/Direct/GetLocation.ashx?zip="+location+"&api_key="+weatherApiKey);
            String url = "http://i.wxbug.net/REST/Direct/GetLocationSearch.ashx?ss="+location+"&api_key="+weatherApiKey;
            String jsonString = httpGet(url);
            System.out.println(url);
            JSONObject json = (JSONObject)JSONSerializer.toJSON(jsonString);
            String city = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("city");
            String state = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("state");
            String cityCode = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("cityCode");
            String country = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("country");
            String zipcode = ((JSONObject)(json.getJSONArray("cityList")).get(0)).getString("zipCode");
            
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
            weather = "error";
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
    
    private List<String> getEarthquakes(int numEarthquakes, String location, String url)
    {
    	//API- http://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php
        System.out.println("getting last " + numEarthquakes + " earthquakes for "+location);
        List<String> earthquakes = new ArrayList<String>();
        try
        {
            String jsonString = httpGet(url);
            System.out.println(url);
            JSONArray json = ((JSONObject)JSONSerializer.toJSON(jsonString)).getJSONArray("features");
            int earthquakeCount = 0;
            if (json != null)
            {
            	for (int i = 0; (i < json.size()) && (earthquakeCount < numEarthquakes); i++)
            	{
            		JSONObject props = ((JSONObject)json.get(i)).getJSONObject("properties");
            		String time = (new Date(props.getLong("time"))).toString();
            		String title = props.getString("title");
            		String usgsUrl = props.getString("url");
            		if (location.equals("") || title.toLowerCase().contains(location.toLowerCase()))
            		{
            			earthquakes.add(time+": "+title+" ("+usgsUrl+")");
            			earthquakeCount++;
            		}
            	}
            }
        }
        catch (Exception e)
        {
        	System.out.println("Error: "+e.getMessage());
            e.printStackTrace();
        }
        return earthquakes;
    }
    
    private List<String> getBigEarthquakes(int numEarthquakes, String location)
    {
    	return getEarthquakes(numEarthquakes, location, "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_month.geojson");
    }

    private List<String> getAllEarthquakes(int numEarthquakes, String location)
    {
    	return getEarthquakes(numEarthquakes, location, "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.geojson");
    }

    private List<String> getNews(int numNews)
    {
        System.out.println("getting last " + numNews + " news");
        List<String> news = new ArrayList<String>();
        String url = "http://ajax.googleapis.com/ajax/services/feed/load?v=1.0&num=8&q=http%3A%2F%2Fnews.google.com%2Fnews%3Foutput%3Drss";
        //String url = "http://ajax.googleapis.com/ajax/services/feed/load?v=1.0&num=8&q=http%3A%2F%2Fnews.google.com%2Fnews%2Ffeeds%3Foutput%3Drss";
        try
        {
            String jsonString = httpGet(url);
            System.out.println(url);
            JSONArray json = ((JSONObject)((JSONObject)((JSONObject)JSONSerializer.toJSON(jsonString)).getJSONObject("responseData")).getJSONObject("feed")).getJSONArray("entries");
            int newsCount = 0;
            if (json != null)
            {
            	for (int i = 0; (i < json.size()) && (newsCount < numNews); i++)
            	{
            		String title = ((JSONObject)json.get(i)).getString("title");
            		String publishedDate = ((JSONObject)json.get(i)).getString("publishedDate");
            		news.add(publishedDate + " : "+title);
            		newsCount++;
            	}
            }
        }
        catch (Exception e)
        {
        	System.out.println("Error: "+e.getMessage());
            e.printStackTrace();
        }
        return news;
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
    
    @Override
    protected void handleLine(String line) {
        // Check for server pings.
        if (line.startsWith("PING ")) {
            // Respond to the ping and return immediately.
            this.onServerPing(line.substring(5));
            return;
        }

        String sourceNick = "";
        String sourceLogin = "";
        String sourceHostname = "";

        StringTokenizer tokenizer = new StringTokenizer(line);
        String senderInfo = tokenizer.nextToken();
        String command = tokenizer.nextToken();
        String target = null;

        int exclamation = senderInfo.indexOf("!");
        int at = senderInfo.indexOf("@");
        if (senderInfo.startsWith(":")) {
            if (exclamation > 0 && at > 0 && exclamation < at) {
                sourceNick = senderInfo.substring(1, exclamation);
                sourceLogin = senderInfo.substring(exclamation + 1, at);
                sourceHostname = senderInfo.substring(at + 1);
            }
            else {
                
                if (tokenizer.hasMoreTokens()) {
                    String token = command;

                    int code = -1;
                    try {
                        code = Integer.parseInt(token);
                    }
                    catch (NumberFormatException e) {
                        // Keep the existing value.
                    }
                    
                    if (code != -1) {
                        String errorStr = token;
                        String response = line.substring(line.indexOf(errorStr, senderInfo.length()) + 4, line.length());
                        this.processServerResponse(code, response);
                        // Return from the method.
                        return;
                    }
                    else {
                        // This is not a server response.
                        // It must be a nick without login and hostname.
                        // (or maybe a NOTICE or suchlike from the server)
                        sourceNick = senderInfo;
                        target = token;
                    }
                }
                else {
                    // We don't know what this line means.
                    this.onUnknown(line);
                    // Return from the method;
                    return;
                }
                
            }
        }
        
        command = command.toUpperCase();
        if (sourceNick.startsWith(":")) {
            sourceNick = sourceNick.substring(1);
        }
        if (target == null) {
            target = tokenizer.nextToken();
        }
        if (target.startsWith(":")) {
            target = target.substring(1);
        }

        // Check for CTCP requests.
        if (command.equals("PRIVMSG") && line.indexOf(":\u0001") > 0 && line.endsWith("\u0001")) {
        }
        else if (command.equals("JOIN")) {
            hostnames.put(sourceNick,sourceHostname);
        }
        else if (command.equals("PART")) {
        }
        else if (command.equals("NICK")) {
            hostnames.put(target,sourceHostname);
        }
    	super.handleLine(line);
    }
    
    private final void processServerResponse(int code, String response) {
        
        if (code == RPL_LIST) {
            // This is a bit of information about a channel.
            int firstSpace = response.indexOf(' ');
            int secondSpace = response.indexOf(' ', firstSpace + 1);
            int thirdSpace = response.indexOf(' ', secondSpace + 1);
            int colon = response.indexOf(':');
            String channel = response.substring(firstSpace + 1, secondSpace);
            int userCount = 0;
            try {
                userCount = Integer.parseInt(response.substring(secondSpace + 1, thirdSpace));
            }
            catch (NumberFormatException e) {
                // Stick with the value of zero.
            }
            String topic = response.substring(colon + 1);
            this.onChannelInfo(channel, userCount, topic);
        }
        else if (code == RPL_TOPIC) {
            // This is topic information about a channel we've just joined.
        }
        else if (code == RPL_TOPICINFO) {

        }
        else if (code == RPL_NAMREPLY) {
            // This is a list of nicks in a channel that we've just joined.
            int channelEndIndex = response.indexOf(" :");
            String channel = response.substring(response.lastIndexOf(' ', channelEndIndex - 1) + 1, channelEndIndex);
            System.out.println("SERVERSTRING="+response);
            StringTokenizer tokenizer = new StringTokenizer(response.substring(response.indexOf(" :") + 2));
            while (tokenizer.hasMoreTokens()) {
                String nick = tokenizer.nextToken();
                String prefix = "";
                if (nick.startsWith("@")) {
                    // User is an operator in this channel.
                    prefix = "@";
                }
                else if (nick.startsWith("+")) {
                    // User is voiced in this channel.
                    prefix = "+";
                }
                else if (nick.startsWith(".")) {
                    // Some wibbly status I've never seen before...
                    prefix = ".";
                }
                nick = nick.substring(prefix.length());
                hostnames.put(nick, "");
            }
        }
        else if (code == RPL_ENDOFNAMES) {
            // This is the end of a NAMES list, so we know that we've got
            // the full list of users in the channel that we just joined. 
        }
        
        this.onServerResponse(code, response);
    }
}

