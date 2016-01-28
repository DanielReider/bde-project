package de.fhms.bde.weatherPull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.json.JSONObject;

class MapClass extends MapReduceBase implements Mapper<Text, Text, Text, Text> {

	private Text place = new Text();
	private Text weatherInfo = new Text();
	
	public void map(Text key, Text value, OutputCollector<Text, Text> output,
			Reporter reporter) throws IOException {

		Enumeration<NetworkInterface> e = NetworkInterface
				.getNetworkInterfaces();
		
		while (e.hasMoreElements()) {
			NetworkInterface n = (NetworkInterface) e.nextElement();
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				if (i.getHostAddress().toString().equals("10.60.64.45")) {
					System.setProperty("http.proxyHost", "10.60.17.102");
					System.setProperty("http.proxyPort", "8080");
					System.setProperty("https.proxyHost", "10.60.17.102");
					System.setProperty("https.proxyPort", "8080");
				}
			}
		}
		
		String place = value.toString();
		String concatURL = "http://api.openweathermap.org/data/2.5/weather?q="+ place +",DE&appid=58c42f4cf0f253c2f5ae3f28adc0505c";
		URL url = new URL(concatURL);
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

		double tempInCelsius = KelvinToCelsius(tempInKelvin);
		this.place.set(place);
		weatherInfo.set(weather + " " + windSpeed + " " + tempInCelsius);
		System.out.println(weatherInfo);
		output.collect(this.place, weatherInfo);
	}

	private static double KelvinToCelsius(double tempInKelvin) {
		return tempInKelvin - 273;
	}
}

