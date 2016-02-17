package de.fhmuenster.bde.mr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Calendar;
import java.util.Enumeration;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.codehaus.jettison.json.JSONObject;



public class TwitchPull extends Configured implements Tool{
	
	
	public int run(String[] arg0) throws Exception{
		Job job = Job.getInstance();
		job.setJobName("twitchMetaDataPull");
		job.setJarByClass(this.getClass());
		
		Configuration conf = job.getConfiguration();
		conf.set("mapreduce.output.textoutputformat.separator", ";");
		
		String twitchURL = "https://api.twitch.tv/kraken/streams?limit=#limit#&offset=#offset#";
				
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

		try {
			Path linksPath = new Path(
					"hdfs://quickstart.cloudera:8020/data/twitch/streammetadata/input/links.txt");
			FileSystem fs = FileSystem.get(new Configuration());
			fs.delete(linksPath, true);
			BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
					fs.create(linksPath, true)));

			URL url = new URL(twitchURL.replace("#limit#", "1").replace(
					"#offset#", "0"));
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			JSONObject obj = new JSONObject(response.toString());

			int total = obj.getInt("_total");
			int offset = 0;
			while (total > offset) {
				br.write(twitchURL.replace("#limit#", "100").replace(
						"#offset#", "" + offset));
				br.newLine();
				total -= 100;
				offset += 100;
			}

			br.close();

			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);

			
			job.setMapperClass(MapClass.class);

			job.setReducerClass(ReduceClass.class);

			// conf.setWorkingDirectory(new Path("/temp"));

			FileInputFormat.addInputPath(job, linksPath);
			Calendar cal = Calendar.getInstance();
			FileOutputFormat.setOutputPath(
					job,
					new Path("/data/twitch/streammetadata/processing/"
							+ cal.get(Calendar.YEAR) + "/"
							+ (cal.get(Calendar.MONTH) + 1)  + "/"
							+ cal.get(Calendar.DAY_OF_MONTH) + "/"
							+ cal.get(Calendar.HOUR_OF_DAY) + "/"
							+ cal.get(Calendar.MINUTE)));

			
			return job.waitForCompletion(true) ? 0: 1;
			
		} catch (Exception ex) {
			ex.printStackTrace();
			return 0;
		}

	}

	public static void main(String[] args) {
		TwitchPull twitchPull = new TwitchPull();
		try {
			twitchPull.run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
