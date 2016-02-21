package org.machineLearning;

import java.io.IOException;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.NaiveBayes;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

public class Analysis {
	public static void main(String[] args) {
		createModel();
	}

	public static void createModel() {
		SparkConf sparkConf = new SparkConf().setAppName("JavaRandomForestRegression");
		sparkConf.setMaster("local");
		JavaSparkContext jsc = new JavaSparkContext(sparkConf);
		SQLContext sqlContext = new org.apache.spark.sql.SQLContext(jsc);
		// Load and parse the data file.
		String datapath = "hdfs://quickstart.cloudera:8020/data/analysis/input/";
		JavaRDD<String> traindata = jsc.textFile(datapath);

		JavaRDD<LabeledDocument> traindataframerdd = traindata.map(new Function<String, LabeledDocument>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public LabeledDocument call(String row) throws Exception {
				return splitStringtoDoubles(row);
			}

			private LabeledDocument splitStringtoDoubles(String s) {
				String[] splitVals = s.split(",");
				System.out.println(splitVals[0] + "::"+ splitVals[1]);
				return new LabeledDocument(Long.parseLong(splitVals[0]),splitVals[1], Double.parseDouble(splitVals[2]));
			}
		});


		DataFrame traindf = sqlContext.createDataFrame(traindataframerdd, LabeledDocument.class);
		
		// Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
		Tokenizer tokenizer = new Tokenizer()
		  .setInputCol("text")
		  .setOutputCol("words");
		HashingTF hashingTF = new HashingTF()
		  .setNumFeatures(1000)
		  .setInputCol(tokenizer.getOutputCol())
		  .setOutputCol("features");
		
		StringIndexer si = new StringIndexer()
				.setInputCol("target")
				.setOutputCol("label");
		
		/*
		RandomForestClassifier rf = new RandomForestClassifier()
				.setMaxBins(16)
				.setMaxDepth(4)
				.setSeed(12345)
				.setFeatureSubsetStrategy("auto")
				.setNumTrees(500)
				.setImpurity("gini");
				*/
		NaiveBayes nb = new NaiveBayes();
		
		Pipeline pipeline = new Pipeline()
		  .setStages(new PipelineStage[] {tokenizer, hashingTF, si, nb});

		// Fit the pipeline to training documents.
		PipelineModel model = pipeline.fit(traindf);
		
		try {
			model.write().overwrite().save("hdfs://quickstart.cloudera:8020/data/analysis/model");
			System.out.println("Model generated");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
