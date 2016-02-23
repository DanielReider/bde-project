package de.bde.master.bean;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Session Bean implementation class TwitchPrediction
 */
@Stateless
@Remote
@Path("prediction")
public class TwitchPrediction {

	@EJB
	SparkModelSingleton sparkModel;

	@GET
	public String getPrediction(@QueryParam("name") final String name, @QueryParam("game") final String game,
			@QueryParam("duration") final Double duration, @QueryParam("daytime") final String daytime,
			@QueryParam("fps") final Double fps, @QueryParam("day") final Timestamp day) {
		
		String features = name + " " + game + " " + duration + " " + daytime + " " + fps + getWeatherData(new String[]{"",""}, day, daytime);
		
		DataFrame predictions = sparkModel.getPrediction(features);
		String prediction = "";
		for (Row r : predictions.select("id", "text", "probability", "prediction").collect()) {
			prediction = r.get(3).toString();
		}

		return prediction;
	}

	private String getWeatherData(String[] places, Timestamp date, String timeClass) {
		int success = 0;
		String weatherInfo = "";
		int trys = 0;
		StringBuffer[] responses = new StringBuffer[places.length];

		while (success < places.length && trys < 3) {
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
						}
					}
				}

				for (int i = 0; i < places.length; i++) {
					String concatURL = "http://api.openweathermap.org/data/2.5/forecast?q=" + places[i]
							+ ",de&mode=json&appid=44db6a862fba0b067b1930da0d769e98";
					URL url = new URL(concatURL);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					// optional default is GET
					con.setRequestMethod("GET");
					// add request header
					con.setRequestProperty("User-Agent", "Mozilla/5.0");

					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String inputLine;
					responses[i] = new StringBuffer();
					while ((inputLine = in.readLine()) != null) {
						responses[i].append(inputLine);
					}
					in.close();
					success++;
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("catch! next try:" + trys);
				trys++;
			}
		}

		if (responses != null) {
			String weather = "";

			for (StringBuffer response : responses) {
				try {
					JSONObject obj = new JSONObject(response.toString());
					JSONArray wda = obj.getJSONArray("list");
					Double tempInKelvin = null;

					Double windSpeed = null;
					try {

						String predictionDate = formatTimestamp(date, timeClass);

						for (int i = 0; i < wda.length(); i++) {
							if (wda.getJSONObject(i).get("dt_txt").toString().equals(predictionDate)) {
								windSpeed = Double.parseDouble(
										wda.getJSONObject(i).getJSONObject("wind").get("speed").toString());
								tempInKelvin = Double
										.parseDouble(wda.getJSONObject(i).getJSONObject("main").get("temp").toString());
								weather = wda.getJSONObject(i).getJSONArray("weather").getJSONObject(0).get("main")
										.toString();
								break;
							}
						}

						double tempInCelsius = KelvinToCelsius(tempInKelvin);
						weatherInfo = weatherInfo + weather + " " + windSpeed + " " + tempInCelsius + " ";
					} catch (Exception e) {

					}
				} catch (Exception e) {

				}
			}
		}

		return removeLastChar(weatherInfo);
	}

	private String formatTimestamp(Timestamp timestamp, String timeClass) {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(timestamp);
		String hour;
		switch (timeClass) {
		case "Morgens":
			hour = "06:00:00";
			break;
		case "Mittags":
			hour = "12:00:00";
			break;
		case "Nachmittags":
			hour = "15:00:00";
			break;
		case "Abends":
			hour = "18:00:00";
			break;
		case "Nachts":
			hour = "21:00:00";
			break;
		default:
			hour = "12:00:00";
		}
		return date + " " + hour;
	}

	private static double KelvinToCelsius(double tempInKelvin) {
		tempInKelvin -= 273;
		return (Math.round(tempInKelvin * 100.0)) / 100.0;
	}

	private static String removeLastChar(String str) {
		if(str.equals(""))
			return str;
		else
			return str.substring(0, str.length() - 1);
	}

}
