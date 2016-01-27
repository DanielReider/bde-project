package de.fhms.bde.weatherPull;

import java.util.Calendar;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;

public class WeatherPull {
	
		public void run() throws Exception {
			Calendar now = Calendar.getInstance();

			JobConf conf = new JobConf(WeatherPull.class);
			String inputPath = "hdfs://quickstart.cloudera:8020/data/weather/input/places.csv";
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("hdfs://quickstart.cloudera:8020/data/weather/processing/");
			stringBuilder.append(now.get(Calendar.YEAR));
			stringBuilder.append("/");
			stringBuilder.append(now.get(Calendar.MONTH));
			stringBuilder.append("/");
			stringBuilder.append(now.get(Calendar.DAY_OF_MONTH));
			stringBuilder.append("/");
			stringBuilder.append(now.get(Calendar.HOUR));
			stringBuilder.append("/");
			stringBuilder.append(now.get(Calendar.MINUTE));
			String outputPath = stringBuilder.toString();
			conf.setJobName("weatherPull");

			// the keys are words (strings)
			conf.setOutputKeyClass(Text.class);
			// the values are counts (ints)
			conf.setOutputValueClass(IntWritable.class);

			conf.setMapperClass(MapClass.class);
			conf.setReducerClass(ReduceClass.class);

			FileInputFormat.addInputPath(conf, new Path(inputPath));
			FileOutputFormat.setOutputPath(conf, new Path(outputPath));

			JobClient.runJob(conf);
		}

		public static void main(String[] args) {
			WeatherPull wp = new WeatherPull();
			try {
				wp.run();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

