package com.ks.utils;

import java.sql.Connection;
import java.sql.DriverManager;

public class JDBCUtil {

	//static Connection conn = null;

	public static Connection getConnectionByJDBC() {
		Connection conn = null;
		if(conn == null){
			try{
				Class.forName(Constant.MYSQL_DRIVER);
				conn = DriverManager.getConnection(Constant.MYSQL_URL,Constant.MYSQL_USERNAME,Constant.MYSQL_PASSWORD);
				conn.setAutoCommit(false);
			}catch(Exception e) {
				System.out.println("连接建立失败");
				e.printStackTrace();
			}
		}
		return conn;
	}
	
	
	public static void main(String[] args){
		System.out.println(getConnectionByJDBC());
	}
	
	
}
