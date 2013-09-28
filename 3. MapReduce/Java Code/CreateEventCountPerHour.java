package com.hadoop.github;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 
 * Count number of create events per hour
 *
 */
public class CreateEventCountPerHour {

	/**
	 * 
	 * @author thirumala kiran
	 *
	 */
	public static class Map extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, IntWritable> {

		private final JSONParser parser = new JSONParser();
		private Text word = new Text();

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {

			JSONObject jsonObj = null;

			String[] strs = value.toString().split("(?<=\\})(?=\\{)");

			for (int i = 0; i < strs.length; i++) {

				try {
					jsonObj = (JSONObject) parser.parse(strs[i]);
				} catch (ParseException e) {

					e.printStackTrace();
					return;

				}

				Object eventType = jsonObj.get("type");
				if (eventType != null) {
					
					if (eventType.toString().equals("CreateEvent")) {
						
						Object created_at = jsonObj.get("created_at");
						
						if (created_at != null ) {
							
							word.set(created_at.toString().substring(0, 13));
							output.collect(word, new IntWritable(1));
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @author Thirumala Kiran
	 *
	 */
	public static class Reduce extends MapReduceBase implements
			Reducer<Text, IntWritable, Text, IntWritable> {

		@Override
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
					
			int sum = 0;
			while (values.hasNext()) {
				sum += values.next().get();
			}

			output.collect(key, new IntWritable(sum));
			
		}
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		JobConf conf = new JobConf(CreateEventCountPerHour.class);
		conf.setJobName("CreateEventCountPerHour");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		conf.setMapperClass(Map.class);
		conf.setReducerClass(Reduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		JobClient.runJob(conf);

	}
}
