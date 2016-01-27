package de.fhmuenster.bde.mr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.json.JSONArray;
import org.json.JSONObject;

class MapClass extends MapReduceBase implements
		Mapper<LongWritable, Text, Text, Text> {
	private Text resKey = new Text();
	private Text word = new Text();

	public void map(LongWritable key, Text value,
			OutputCollector<Text, Text> output, Reporter reporter)
			throws IOException {
		String line = value.toString();
		String separator = "\t";
		Boolean success = false; 
		int trys = 0;
		StringBuffer response = null;

		while (success == false && trys < 3) {
			try {
				Enumeration<NetworkInterface> e = NetworkInterface
						.getNetworkInterfaces();
				while (e.hasMoreElements()) {
					NetworkInterface n = (NetworkInterface) e.nextElement();
					Enumeration<InetAddress> ee = n.getInetAddresses();
					while (ee.hasMoreElements()) {
						InetAddress i = (InetAddress) ee.nextElement();
						if (i.getHostAddress().toString().equals("10.60.64.45")) {
							System.out.println("Setting proxy");
							System.setProperty("http.proxyHost", "10.60.17.102");
							System.setProperty("http.proxyPort", "8080");
							System.setProperty("https.proxyHost", "10.60.17.102");
							System.setProperty("https.proxyPort", "8080");
						}
					}
				}
				URL url = new URL(line);
				HttpURLConnection con = (HttpURLConnection) url
						.openConnection();

				// optional default is GET
				con.setRequestMethod("GET");

				// add request header
				con.setRequestProperty("User-Agent", "Mozilla/5.0");

				BufferedReader in = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				String inputLine;
				response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				success = true;
			} catch (Exception e) {
				System.out.println("catch! next try:"+trys);
				trys++;
			}
		}
		if (response != null) {
			JSONObject obj = new JSONObject(response.toString());

			JSONArray arr = obj.getJSONArray("streams");
			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			String currentDate = formatter.format(new Date());
			for (int i = 0; i < arr.length(); i++) {
				String streamId = arr.getJSONObject(i).getBigInteger("_id")
						.toString();
				String viewers = arr.getJSONObject(i).getBigInteger("viewers")
						.toString();
				JSONObject channel = arr.getJSONObject(i).getJSONObject(
						"channel");
				if (channel != null) {
					String game = channel.get("game").toString();
					String name = channel.get("name").toString();
					String status = channel.get("status").toString();
					String mature = channel.get("mature").toString();
					Object updated = channel.get("updated_at");
					Double fps = arr.getJSONObject(i).getDouble("average_fps");

					if (mature == null)
						mature = "false";
					if (game != null && !game.equals("null") && name != null
							&& !name.equals("null") && status != null
							&& !status.equals("null")) {
						resKey.set(streamId);

						word.set(viewers + separator + game + separator + name
								+ separator + status + separator + currentDate
								+ separator + mature + separator + updated
								+ separator + fps);
						output.collect(resKey, word);
						System.out.println("writing");
					}
				}
			}
		}
	}
}