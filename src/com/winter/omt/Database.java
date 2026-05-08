package com.winter.omt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {
	private static final String MYSQL_UNICODE_PARAMS =
			"?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci";
	private static final String MYSQL_UNICODE_DDL = " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

	public static HikariDataSource ds;
	private static String backend = "mysql";

	static boolean octetDatabaseExists;

	public Database(String hostname, String username, String password, String dbName) throws SQLException {
		backend = "mysql";
		octetDatabaseExists = false;

		HikariConfig config = new HikariConfig();
		String jdbcUrl = "jdbc:mysql://" + hostname + MYSQL_UNICODE_PARAMS;
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
			stmt.execute("CREATE DATABASE IF NOT EXISTS `" + dbName + "`" + MYSQL_UNICODE_DDL);
			stmt.close();
		} else {
			Statement stmt = conn.createStatement();
			stmt.execute("ALTER DATABASE `" + dbName + "`" + MYSQL_UNICODE_DDL);
			stmt.close();
		}

		resultSet.close();
		conn.close();
		ds.close();
		jdbcUrl = "jdbc:mysql://" + hostname + "/" + dbName + MYSQL_UNICODE_PARAMS;
		config.setJdbcUrl(jdbcUrl);
		ds = new HikariDataSource(config);

	}

	public Database(String sqlitePath) throws SQLException {
		backend = "sqlite";
		octetDatabaseExists = true; // Assume SQLite file exists or will be created

		HikariConfig config = new HikariConfig();
		String jdbcUrl = "jdbc:sqlite:" + sqlitePath;
		config.setDriverClassName("org.sqlite.JDBC");
		config.setJdbcUrl(jdbcUrl);
		config.setMaximumPoolSize(6);
		config.setConnectionInitSql("PRAGMA foreign_keys=ON");
		config.addDataSourceProperty("busy_timeout", "5000");
		ds = new HikariDataSource(config);



	}

	public static boolean isSQLite() {
		return "sqlite".equals(backend);
	}

	public static String sql(String mysql, String sqlite) {
		return isSQLite() ? sqlite : mysql;
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
