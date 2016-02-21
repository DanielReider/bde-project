package de.fhmuenster.bde.twitchChatPull;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;

public class HiveClient {
	private static String driverName = "org.apache.hive.jdbc.HiveDriver";
	private static Statement stmt;
	private static String tableName = "channel";

	public HiveClient() {
		try {
			Class.forName(driverName);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		Connection con;
		try {
			con = DriverManager.getConnection("jdbc:hive2://localhost:10000/default", "", "");
			stmt = con.createStatement();
			stmt.execute("create table if not exists " + tableName + " (chan string)");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeChannel(String chan) {
		String sql = "insert into " + tableName + " values('" + chan + "')";
		try {
			stmt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeChannel(String chan) {
		String sql = "INSERT OVERWRITE TABLE " + tableName + " SELECT * FROM " + tableName + " WHERE chan NOT IN ( '"
				+ chan + "')";
		try {
			stmt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Boolean channelExists(String chan) {
		String sql = "select chan from " + tableName + " where chan = '" + chan + "'";
		try {
			ResultSet res = stmt.executeQuery(sql);
			if (res != null && res.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return false;

	}

}