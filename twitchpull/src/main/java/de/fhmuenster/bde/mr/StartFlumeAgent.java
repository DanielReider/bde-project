package de.fhmuenster.bde.mr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class StartFlumeAgent {

	private static String DESTPATH = "/home/cloudera/chat/config/";

	public static Properties loadProperties(String filename) {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(filename);

			// load a properties file
			prop.load(input);

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

			// save properties to project root folder
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

	public StartFlumeAgent(String chan) {
		String[] strChan = { chan };
		main(strChan);
	}

	public static void main(String[] args) {
		try {
			String chan = args[0];
			System.out.println("Prepare config file for " + chan);
			File f = new File(DESTPATH + chan + ".config");
			if (!f.exists() || f.isDirectory()) {
				writeProperties(loadProperties("/home/cloudera/bde-project/twitchChatPull/config/flume.properties"),
						chan);
				System.out.println("Config file was created successfully");
			} else {
				System.out.println("Config file already exists");
			}

			HiveClient hc = new HiveClient();
			if (!hc.channelExists(chan)) {
				Process p;
				p = Runtime.getRuntime()
						.exec("/usr/lib/flume-ng/bin/flume-ng agent -n a1 -c conf -f " + DESTPATH + chan + ".config");
				System.out.println("Agent started successfully");
				hc.writeChannel(chan);
				System.out.println("Added Agent to List");
				try {
					p.waitFor();
					hc.removeChannel(chan);
					System.out.println("Removed Agent from List");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println(e.getMessage());
					hc.removeChannel(chan);
				}
			} else {
				System.out.println("Agent already started");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
