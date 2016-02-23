package de.fhms.bde.weatherPull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.json.JSONObject;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

class MapClass extends Mapper<LongWritable, Text, Text, Text> {

	private Text place = new Text();
	private Text writtenData = new Text();
	private String weatherInfo;
	private Boolean success = false;
	private int trys = 0;
	private StringBuffer response = null;

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		String place = value.toString();
		while (success == false && trys < 3) {
			System.out.println("test");
			try {
				Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();

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
							System.out.println("setting proxy");
						}
					}
				}
				System.out.println(place);
				String concatURL = "http://api.openweathermap.org/data/2.5/weather?q=" + place
						+ ",DE&appid=58c42f4cf0f253c2f5ae3f28adc0505c";
				URL url = new URL(concatURL);
				System.out.println(concatURL);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				// optional default is GET
				con.setRequestMethod("GET");
				// add request header
				con.setRequestProperty("User-Agent", "Mozilla/5.0");

				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("catch! next try:" + trys);
				trys++;
			}
		}
		if (response != null) {
			try {
				JSONObject obj = new JSONObject(response.toString());
				String weather = obj.getJSONArray("weather").getJSONObject(0).get("main").toString();
				double tempInKelvin = obj.getJSONObject("main").getDouble("temp");
				String windSpeed = obj.getJSONObject("wind").get("speed").toString();
				
				double tempInCelsius = KelvinToCelsius(tempInKelvin);
				this.place.set(place);
				weatherInfo = weather + "," + windSpeed + "," + tempInCelsius;
				System.out.println(weatherInfo);
				Calendar now = Calendar.getInstance();
				String generalData = new SimpleDateFormat("yyyyMMddHH").format(now.getTime());
				
				writtenData.set(generalData + "," + weatherInfo);
				
				dataToHBase(generalData, this.place.toString(),weatherInfo);
				
				context.write(this.place, writtenData);
			} catch (Exception e) {

			}
			success = false;
			weatherInfo = "";
		}
	}

	private static double KelvinToCelsius(double tempInKelvin) {
		tempInKelvin -= 273;
		return (Math.round(tempInKelvin * 100.0)) / 100.0;
	}
	
	private static void dataToHBase(String generalData, String plz, String content) throws IOException{
		// Instantiating Configuration class
	      Configuration config = HBaseConfiguration.create();

	      // Instantiating HTable class
	      HTable hTable = new HTable(config, "weather");

	      // Instantiating Put class
	      // accepts a row name.
	      Put p = new Put(Bytes.toBytes(generalData)); 
	   // adding values using add() method
	      // accepts column family name, qualifier/row name ,value
	      p.add(Bytes.toBytes("weather"),
	      Bytes.toBytes(plz),Bytes.toBytes(content));
	      
	      // Saving the put Instance to the HTable.
	      hTable.put(p);
	      System.out.println("data inserted");
	      
	      // closing HTable
	      hTable.close();
	}
}