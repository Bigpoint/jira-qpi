/**
 * Package holding all classes concerned about DB data.
 */
package net.bigpoint.jira.plugins.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author jschweizer Mapper class for connection to caching db.
 *
 */
public class KpiCacheDBMapper {

	/**
	 * If db connection is OK, caching is enabled, else not. Default: enabled.
	 */
	private boolean m_cacheEnabled = true;

	/**
	 * Database connection to caching DB.
	 */
	private Connection con;

	private final File configFile = new File("/home/jira/cfg/app.conf");

	private String[] configStrings;

	/**
	 * Default constructor that sets up the default connection.
	 */
	public KpiCacheDBMapper() {

		configStrings = // Add here String array containing in that order: DB host, name, user, password

		this.initDB();

	}

	/**
	 * Inititalizes a new db connection if there's is none. Configs are read from app.conf @see{lookupConfigs} This is
	 * done by calling php because integrating an external config file in Maven is not a better solution due to
	 * maintenance of config data.
	 */
	private void initDB() {

		try {
			if((con == null || con.isClosed()) && configStrings != null && configStrings.length >= 4 ) {

				String host = configStrings[0];
				String name = configStrings[1];
				String user = configStrings[2];
				String pass = configStrings[3];
				KpiDataProvider.LOGGER.debug("Trying to establish DB connection");
				Class.forName("com.mysql.jdbc.Driver");
				con = DriverManager.getConnection("jdbc:mysql://" + host + "/" + name, user, pass);
			}
		} catch(SQLException se) {
			m_cacheEnabled = false;
			KpiDataProvider.LOGGER.error("SQL Connection problem: " + se.getMessage());
		} catch(Exception e) {
			m_cacheEnabled = false;
			KpiDataProvider.LOGGER.error("Exception while connecting to database: " + e.getMessage());
		}

	}

	/**
	 * Retrieves the cached value from the dedicated jira_kpi table
	 *
	 * @param id
	 *            the project id
	 * @param end
	 *            the timestamp the value is requested for
	 * @return the kpi value, retrieved from the DB, -1, if no value could be found
	 */
	protected double getCachedValue(long id, Timestamp end) {
		initDB();
		if(m_cacheEnabled) {
			double value = -1;
			try {
				String sql = "Select kpiValue From cachedKpiNumbers Where projectId=" + id + " AND timeForKpi= '" + end + "';";
				PreparedStatement stmt = con.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery();
				while(rs.next()) {
					value = rs.getDouble("kpiValue");
				}
				rs.close();
			} catch(SQLException e) {
				System.out.println("SQL exception : " + e.getMessage());
				KpiDataProvider.LOGGER.error("SQL Exception retrieving cached value: " + e.getMessage());
				return -1;
			} catch(Exception e) {
				KpiDataProvider.LOGGER.error("Exception retrieving cached value: " + e.getMessage());
				return -1;
			}

			return value;
		} else {
			return -1;
		}
	}

	/**
	 * Caches the kpi value of one specific project at one specific time.
	 *
	 * @author jschweizer
	 * @param id
	 *            the project id
	 * @param end
	 *            the timestamp, the value needs to be calculated for
	 * @param value
	 *            the kpi value
	 */
	protected void cacheValue(long id, Timestamp end, double value) {
		initDB();
		if(m_cacheEnabled) {
			String sql = "INSERT INTO `jira_kpi`.`cachedKpiNumbers` " + "(`id` ,`timeForKpi` ,`projectId` ,`kpiValue`)"
					+ " VALUES (NULL , '" + end + "', '" + id + "', '" + value + "');";
			try {
				PreparedStatement stmt = con.prepareStatement(sql);
				stmt.executeUpdate();
			} catch(SQLException e) {
				KpiDataProvider.LOGGER.error("SQL Exception while caching value: " + e.getMessage());
			} catch(Exception e) {
				KpiDataProvider.LOGGER.error("Exception while caching value: " + e.getMessage());
			}
		}

	}

}
