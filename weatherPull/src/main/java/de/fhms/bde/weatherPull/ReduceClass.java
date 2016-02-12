package de.fhms.bde.weatherPull;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

class ReduceClass extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
	private Text result = new Text();

	public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter)
			throws IOException {
		String translations = "";

		while (values.hasNext()) {
			translations = values.next().toString();

		}
		result.set(translations);
		output.collect(key, result);
	}
}
