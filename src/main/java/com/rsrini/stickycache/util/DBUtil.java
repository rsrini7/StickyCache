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
				
				/*Server server = Server.createTcpServer("-tcp","-web","-trace","-webAllowOthers", "-tcpAllowOthers").start();
				logger.info("H2 Server started... "+server.getURL()+" port : "+server.getPort()+" Status : "+server.getStatus());*/
				
				/*Console c = new Console();
				c.runTool("-web", "-webPort", "9000", "-tool","-tcp","-tcpPort","8000","-browser");*/
				
				conn = DriverManager.getConnection(iurl, iuser, ipass);
				conn.setAutoCommit(true);
				
				logger.info(" Db Connection established successfully to " + iurl
						+ " " + iuser + " " + ipass);
				
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ClassNot found : org.h2.Driver");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("exection occured",e);
		}
	}
	
	public static Connection getConnection() {
		return conn;
	}
	
}
