package de.fhms.bde.weatherPull;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

class ReduceClass extends Reducer<Text, Text, Text, Text> {
	private Text result = new Text();

	public void reduce(Text key, Iterator<Text> values, Context context)
			throws IOException, InterruptedException {
		String translations = "";

		while (values.hasNext()) {
			translations = values.next().toString();

		}
		result.set(translations);
		context.write(key, result);
	}
}
