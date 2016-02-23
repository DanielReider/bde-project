package de.bde.master.bean;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Remote;
import javax.ejb.Schedule;
import javax.ejb.Singleton;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

import de.bde.master.common.LabeledDocument;

@Singleton
@Remote
public class SparkModelSingleton {
	SparkConf sparkConf = null;
	JavaSparkContext jsc = null;
	SQLContext sqlContext = null;
	PipelineModel pipeModel = null;
	String modelPath = "hdfs://10.60.64.45:8020/data/analysis/model";

	@PostConstruct
	public void init() {
		sparkConf = new SparkConf().setAppName("RunJavaMLModel");
		sparkConf.setMaster("local");
		jsc = new JavaSparkContext(sparkConf);
		sqlContext = new org.apache.spark.sql.SQLContext(jsc);
		pipeModel = PipelineModel.load(modelPath);
	}

	@Lock(LockType.READ)
	public DataFrame getPrediction(String features) {
		DataFrame data = sqlContext.createDataFrame(Arrays.asList(new LabeledDocument(0L, features, 1.0)),
				LabeledDocument.class);

		pipeModel.transform(data);

		return pipeModel.transform(data);
	}

	@Lock(LockType.WRITE)
	@Schedule(hour = "1")
	public void reloadModel() {
		System.out.println("Reloading model");
		pipeModel = PipelineModel.load(modelPath);
	}

}
