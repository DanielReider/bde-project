package de.fhmuenster.bde.twitchChatPull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;

public class StartFlumeAgent {

	private static String DESTPATH = "/home/cloudera/chat/config/";
	private static String SOURCEFILE = "/home/cloudera/bde-project/twitchChatPull/config/flume.properties";

	public static Properties createProperties() {
		Properties prop = new Properties();
		prop.put("a1.sources", "IRC");
		prop.put("a1.sinks", "HDFS");
		prop.put("a1.channels", "c1");

		prop.put("a1.sources.IRC.type", "de.fhmuenster.bde.twitchChatPull.IrcSource");
		prop.put("a1.sources.IRC.hostname", "irc.twitch.tv");
		prop.put("a1.sources.IRC.port", "6667");
		prop.put("a1.sources.IRC.nick ", "exclaw123");
		prop.put("a1.sources.IRC.chan", "");
		prop.put("a1.sources.IRC.password", "oauth:pjcft7qxw68ir00zfisr8xnwlpixtc");

		prop.put("a1.sinks.HDFS.type", "hdfs");
		prop.put("a1.sinks.HDFS.hdfs.path", "/data/twitch/chat/processing/");
		prop.put("a1.sinks.HDFS.hdfs.filePrefix", "");
		prop.put("a1.sinks.HDFS.hdfs.useLocalTimeStamp", "hdfs");
		prop.put("a1.sinks.HDFS.hdfs.rollInterval", "600");
		prop.put("a1.sinks.HDFS.hdfs.rollSize", "0");
		prop.put("a1.sinks.HDFS.hdfs.rollCount", "0");
		prop.put("a1.sinks.HDFS.hdfs.batchSize", "1000");
		prop.put("a1.sinks.HDFS.hdfs.writeFormat", "Text");
		prop.put("a1.sinks.HDFS.hdfs.callTimeout", "100000");
		prop.put("a1.sinks.HDFS.hdfs.inUsePrefix", "temp");

		prop.put("a1.channels.c1.type", "memory");
		prop.put("a1.channels.c1.capacity", "1500");
		prop.put("a1.channels.c1.transactionCapacity", "1100");

		prop.put("a1.sources.IRC.channels", "c1");
		prop.put("a1.sinks.HDFS.channel", "c1");

		return prop;
	}

	public static Properties loadProperties(String filename) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(filename);

			// load a properties file
			prop.load(input);
		} catch (FileNotFoundException ex) {
			prop = createProperties();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}

		}
		return prop;
	}

	public static void writeProperties(Properties prop, String chan) {
		OutputStream output = null;

		try {
			File theDir = new File(DESTPATH);

			// if the directory does not exist, create it
			if (!theDir.exists()) {
				boolean result = false;

				try {
					theDir.mkdirs();
					result = true;
				} catch (SecurityException se) {
					// handle it
				}
				if (result) {
					System.out.println("DIR created " + theDir);
				}
			}

			output = new FileOutputStream(DESTPATH + chan + ".properties");

			// set the properties value
			prop.setProperty("a1.sources.IRC.chan", chan);
			prop.setProperty("a1.sinks.HDFS.hdfs.filePrefix", "chatdata%Y%m%d_" + chan);

			prop.store(output, null);

		} catch (IOException io) {
			System.out.println(io.getMessage());
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}

		}
		// File (or directory) with old name
		File file = new File(DESTPATH + chan + ".properties");

		// File (or directory) with new name
		File file2 = new File(DESTPATH + chan + ".config");

		// Rename file (or directory)
		boolean success = file.renameTo(file2);

		if (!success) {
			// File was not successfully renamed
			System.out.println("Rename failed");
		}
	}

	private static void runAgent(String chan) {
		System.out.println("Prepare config file for " + chan);
		File f = new File(DESTPATH + chan + ".config");
		if (!f.exists() || f.isDirectory()) {
			writeProperties(loadProperties(SOURCEFILE), chan);
			System.out.println("Config file was created successfully");
		} else {
			System.out.println("Config file already exists");
		}

		try {
			Runtime.getRuntime()
					.exec("/usr/lib/flume-ng/bin/flume-ng agent -n a1 -c conf -f " + DESTPATH + chan + ".config &");
			System.out.println("Agent started successfully");
			System.out.println("Added Agent to List");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

	}

	public static Boolean agentExists(String chan) {
		try {
			FileSystem fs = FileSystem.get(new Configuration());
			FileStatus[] status = fs
					.listStatus(new Path("hdfs://quickstart.cloudera:8020/data/twitch/chat/processing"));
			Path[] paths = FileUtil.stat2Paths(status);
			for (Path path : paths) {
				System.out.println(path.toString());
				if (path.toString().matches(".*" + chan + ".*tmp$"))
					return true;
			}

		} catch (Exception e) {
			System.out.println("File not found");
		}
		return false;
	}

	public static void main(String[] args) {
		try {
			FileSystem fs = FileSystem.get(new Configuration());
			Calendar cal = Calendar.getInstance();
			Path inFile;
			int minute = cal.get(Calendar.MINUTE);
			do {
				inFile = new Path("hdfs://quickstart.cloudera:8020/data/twitch/streammetadata/processing/"
						+ cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/"
						+ cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.HOUR_OF_DAY) + "/" + minute + "/"
						+ "part-00000");
				minute = minute - 1;
				System.out.println(inFile.toString());
			} while ((!fs.exists(inFile) && minute >= 0));
			if (fs.exists(inFile)) {
				try {
					FSDataInputStream in = fs.open(inFile);
					String line;
					while ((line = in.readLine()) != null) {
						String[] dataArray = line.split("\t");
						if (dataArray.length >= 4) {
							if (agentExists(dataArray[3])) {
								System.out.println(args[0] + ": Agent already started");
							} else {
								runAgent(dataArray[3]);
							}

						}

					}
				} catch (IOException e) { // TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (IOException e) { // TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
