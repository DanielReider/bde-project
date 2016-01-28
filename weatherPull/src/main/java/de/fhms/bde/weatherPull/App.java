package de.fhms.bde.weatherPull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.codehaus.jettison.json.JSONObject;


/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	double roundOff = Math.round(10.444444 * 100.0) / 100.0;
        System.out.println( "Hello World!: " + roundOff );
        weatherTest();
    }
    
    public static void weatherTest(){
    	String concatURL = "http://api.openweathermap.org/data/2.5/weather?q="+ "Muenchen" +",DE&appid=58c42f4cf0f253c2f5ae3f28adc0505c";
		
    	URL url;
		try {
			url = new URL(concatURL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		// optional default is GET
		con.setRequestMethod("GET");
		// add request header
		con.setRequestProperty("User-Agent", "Mozilla/5.0");

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		JSONObject obj = new JSONObject(response.toString());
		String weather = obj.getJSONArray("weather").getJSONObject(0)
				.get("main").toString();
		double tempInKelvin = obj.getJSONObject("main").getDouble("temp");
		String windSpeed = obj.getJSONObject("wind").getString("speed");


		System.out.println(weather + "," + windSpeed + "," + (tempInKelvin-273));
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    }
}
