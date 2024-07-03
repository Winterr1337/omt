package com.winter.omt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {

	public static HikariDataSource ds;

	static boolean octetDatabaseExists;

	public Database(String hostname, String username, String password, String dbName) throws SQLException {
		octetDatabaseExists = false;

		HikariConfig config = new HikariConfig();
		String jdbcUrl = "jdbc:mysql://" + hostname;
		config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);
		config.setMaximumPoolSize(6);
		ds = new HikariDataSource(config);

		Connection conn = ds.getConnection();

		ResultSet resultSet = conn.getMetaData().getCatalogs();
		while (resultSet.next()) {

			String databaseName = resultSet.getString(1);
			if (databaseName.equals(dbName)) {
				System.out.println("Found existing MySQL Database " + dbName);
				octetDatabaseExists = true;

				break;
			}
		}

		if (!octetDatabaseExists) {
			Statement stmt = conn.createStatement();
			stmt.execute("CREATE DATABASE IF NOT EXISTS " + dbName);
			stmt.close();
		}

		resultSet.close();
		conn.close();
		ds.close();
		jdbcUrl += ("/" + dbName);
		config.setJdbcUrl(jdbcUrl);
		ds = new HikariDataSource(config);

	}

	public static boolean octetDatabaseExists() {
		return octetDatabaseExists;
	}

	public static Connection getConnection() throws SQLException {
		return ds.getConnection();
	}

	public static boolean isConnected() {

		return ds != null && !ds.isClosed();
	}

	public static void close() {

		ds.close();

	}

}
