package com.rsrini.stickycache.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class DBUtil {

	static Logger logger = Logger.getLogger(DBUtil.class);
	private static Connection conn = null;
	
	public DBUtil() {}

	public static void registerDB(String iurl, String iuser, String ipass) {

		try {
			if(conn == null) {
				Class.forName("org.h2.Driver");
				
				conn = DriverManager.getConnection(iurl, iuser, ipass);
				conn.setAutoCommit(true);
				
				logger.info(" Db Connection established successfully to " + iurl
						+ " " + iuser + " " + ipass);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ClassNot found : org.h2.Driver");
		} catch (SQLException e) {
			throw new RuntimeException("exection occured",e);
		}
	}
	
	public static Connection getConnection() {
		return conn;
	}
	
}
