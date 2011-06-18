/*
 * "Copyright (c) 2010-11 The Regents of the University  of California. 
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Author:  Jorge Ortiz (jortiz@cs.berkeley.edu)
 * IS4 release version 1.0
 */

/**
 * Connects to a CouchDB database.  Extends the DBAbstractionLayer abstract class.
 */
package local.db;

import local.rest.*;
import local.rest.resources.*;
import local.rest.resources.util.*;

import net.sf.json.*;
import snaq.db.*;
import java.sql.*;

import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.lang.StringBuffer;
import javax.sql.rowset.serial.*;

public class MySqlDriver implements Is4Database {

	private static transient final Logger logger = Logger.getLogger(MySqlDriver.class.getPackage().getName());

	private static String HOST = null;
	private static int PORT = -1;
	private static String LOGIN = null;
	private static String PW = null;

	private static String dbName = "jortiz";

	protected static ConnectionPool pool = null;
	private static int openConns =0;

	private static Hashtable<String, String> validSchemas = null;

	public MySqlDriver(String host, int port) {
		super();
		HOST = host; PORT = port; LOGIN = null; PW = null;
		try {
			if(pool == null){
				String url = "jdbc:mysql://localhost/" + dbName;
				Driver driver = (Driver)Class.forName ("com.mysql.jdbc.Driver").newInstance ();
				DriverManager.registerDriver(driver);
				pool = new ConnectionPool("local", 5, 100, 200, 10L, url, LOGIN, PW);
			} else {
				logger.info("Pool already created");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public MySqlDriver(String host, int port, String login, String pw, String dbname){
		super();
		HOST = host; PORT = port; LOGIN = login; PW = pw; dbName = dbname;
		logger.info("host: " + HOST + " port:" + PORT);

		try {
			if(pool == null){
				String url = "jdbc:mysql://localhost/" + dbName;
				Driver driver = (Driver)Class.forName ("com.mysql.jdbc.Driver").newInstance ();
				DriverManager.registerDriver(driver);
				pool = new ConnectionPool("local", 5, 10, 20, 0, url, LOGIN, PW);
			} else {
				logger.info("Pool already created");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public static void main(String[] args){
		MySqlDriver  driver = new MySqlDriver("localhost", 3306, "root", "410soda", "jortiz");
	}

	private Connection openConnLocal(){
		Connection conn = null;
		try {
			/*String url = "jdbc:mysql://localhost/" + dbName;
			Class.forName ("com.mysql.jdbc.Driver").newInstance ();
			conn = DriverManager.getConnection (url, LOGIN, PW);*/
			
			/*JDCConnectionDriver driver = new JDCConnectionDriver("com.mysql.jdbc.Driver", url, LOGIN, PW);
			conn = driver.connect(url, null);*/
		
			conn =  pool.getConnection(1000);
			logger.info ("Database connection established");
			
			if(conn != null) {
				openConns += 1;
				logger.info("Open: conn_count=" + openConns);
			}
		} catch (Exception e){
			logger.log(Level.SEVERE, "Cannot connect to database server", e);
		}

		return conn;
	}


	private Connection openConn() {
		Connection conn = null;
		try {
			/*String url = "jdbc:mysql://" + HOST + "/" + dbName;
			Class.forName ("com.mysql.jdbc.Driver").newInstance ();
			conn = DriverManager.getConnection (url, LOGIN, PW);*/

			/*JDCConnectionDriver driver = new JDCConnectionDriver("com.mysql.jdbc.Driver", url, LOGIN, PW);
			conn = driver.connect(url, null);*/

			conn =  pool.getConnection(1000);
			logger.finer("Free_count: " + pool.getFreeCount());
			logger.info ("Database connection established");

			if(conn != null) {
				openConns += 1;
				logger.info("Open: conn_count=" + openConns);
			}
		} catch (Exception e){
			logger.log(Level.SEVERE, "Cannot connect to database server", e);
		}

		return conn;
	}

	private void closeConn(Connection conn){
		try {
			if (conn != null && !conn.isClosed()){
				conn.close ();
				logger.finer("Free_count_close: " + pool.getFreeCount());
				logger.info("Database connection terminated");

				openConns -= 1;
				logger.info("Close: conn_count=" + openConns);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error while closing database connection", e);
		}
	}

	public JSONObject getEntry(String name) {
		return null;
	}

	public void putEntry(JSONObject entry) {
		Connection conn=null;
		try {
			if (entry == null) {
				logger.info("Entry is null");
				return;
			}

			//get current timestamp
			java.util.Date javaDate = new java.util.Date();
			long ttime = javaDate.getTime();
			Timestamp tstamp  = new Timestamp(ttime);
			logger.fine("putEntry: localtime=" + ttime);
			//java.sql.Date sqlDate = new java.sql.Date(ttime);
			//String tstamp = sqlDate.toString();
			String tstampStr = tstamp.toString();
			logger.fine("putEntry: sqltime=" + tstampStr);
			
			conn = openConn();
			
			/*String namePropVal = entry.getString("name");
			if(namePropVal.equalsIgnoreCase("object_stream")){
				putInObjectStream(conn, entry, tstampStr);
			} else if(namePropVal.equalsIgnoreCase("context_stream")){
				putInContextStream(conn, entry, tstampStr);
			} else if(namePropVal.equalsIgnoreCase("context_node_schema")){
				closeConn(conn);
				conn = openConnLocal();
				putInContextNode(conn, entry, tstampStr);
			}else if(namePropVal.equalsIgnoreCase("logic_stream")){
				putInLogicStream(conn, entry, tstampStr);
			} else if(namePropVal.equalsIgnoreCase("device_entry")){
				putInDevice(conn, entry);
			} else if(namePropVal.equalsIgnoreCase("location_entry")){
				putInLocation(conn, entry);
			} else if(namePropVal.equalsIgnoreCase("meta_entry")){
				putInMeta(conn, entry);
			} else if(namePropVal.equalsIgnoreCase("meter_reading_entry")){
				putInMeterReading(conn, entry, tstampStr);
			} else if(namePropVal.equalsIgnoreCase("profile_entry")){
				putInProfile(conn, entry);
			} else if(namePropVal.equalsIgnoreCase("resource_listing_entry")){
				putInResourceListing(conn, entry);
			} else if(namePropVal.equalsIgnoreCase("unit_labels_entry")){
				putInUnitLabels(conn, entry);
			} else if(namePropVal.equalsIgnoreCase("formatting_entry")){
				putInFormatting(conn, entry, tstampStr);
			} else if(namePropVal.equalsIgnoreCase("parameter_entry")){
				putInParameter(conn, entry);
			}else if(namePropVal.equalsIgnoreCase("context_graph")){
				closeConn(conn);
				conn = openConnLocal();
				putInContextGraphs(conn, entry, tstampStr);
			}*/

		} catch (Exception exception){
			logger.log(Level.WARNING, "MySqlDriver: Error in putEntry()", exception);
		} finally {
			closeConn(conn);
		}
	}

	public static boolean isValidSchema(String schemaName){
		/*if(validSchemas == null)
			setupValidSchemas();

		if(schemaName != null && validSchemas.get(schemaName) != null)*/
			return true;
		//return false;
	}

	private void putInContextGraphs(Connection conn, JSONObject data, String timestamp){
		try {
			//handle mandatory fields
			String cid = data.getString("cid");
	
			Statement s = conn.createStatement ();
			int count;
			StringBuffer queryBuf = new StringBuffer().append("INSERT INTO `contextGraphs` (`id`, `graph`, `timestamp`");
			StringBuffer queryBufVals = new StringBuffer().append(") VALUES ('" + cid + "', '" + data.toString() + "', '" + timestamp + "')");
			queryBuf.append(queryBufVals.toString());

			count = s.executeUpdate(queryBuf.toString());
			s.close();
			logger.info(count + " rows were inserted into 'contextGraphs'");
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error while storing device information from publisher", e);
		}
	}

	private void putInContextNode(Connection conn, JSONObject data, String timestamp){
		try {
			String cid = data.getString("cid");
			String ctype  = data.optString("type");

			Statement s = conn.createStatement ();
			int count;
			StringBuffer queryBuf = new StringBuffer().append("INSERT INTO `contextNode` (`cid`, `type`, `timestamp`, `body`) VALUES ");
			StringBuffer queryBufVals = new StringBuffer().append("('" + cid + "', '" + ctype + "', '" + timestamp + "', '" +  data.toString() + "')");
			queryBuf.append(queryBufVals.toString());
			count = s.executeUpdate(queryBuf.toString());
			s.close();

			logger.info(count + " rows were inserted into 'context_node'");
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error while storing device information from publisher", e);
		}
	}

	/************************************************************************/
	public java.util.Date getPublisherCreated(String smapUrl){
		Connection conn = null;
		java.util.Date created = null;
		try {
			//decompose the smap url
			logger.info("SMAP_URL: http://" + smapUrl);
			URL smapurl = new URL("http://" + smapUrl);
			String smapServer = smapurl.getHost();
			String smapUri = smapurl.getPath();
			int smapPort = smapurl.getPort();
			smapPort = (smapPort<0)?80:smapPort;

			conn = openConnLocal();
			Statement s = conn.createStatement ();
			int count;
			String query = "SELECT created from publishers where `smap_server`= \"" + smapServer +
									"\" and `smap_port`= " + smapPort + " and `smap_uri`=\"" + 
									smapUri + "\"";
			logger.info("QUERY: " + query);
			ResultSet rs = s.executeQuery (query);
			while(rs.next()) {
				Timestamp createdTS = rs.getTimestamp("created");
				if(createdTS != null)
					created = (java.util.Date) createdTS;
			}
			s.close();
			closeConn(conn);
			return created;
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
			if(conn != null)
				closeConn(conn);
			return created;
		} finally {
			closeConn(conn);
		}
	}

	public void removePublisher(String smapUrl){
		Connection conn = null;
		try {
			//decompose the smap url
			URL smapurl = new URL(smapUrl);
			String smapServer = smapurl.getHost();
			String smapUri = smapurl.getPath();
			int smapPort = smapurl.getPort();
			smapPort = (smapPort<0)?80:smapPort;

			conn = openConnLocal();
			Statement s = conn.createStatement ();
			s.executeUpdate (
				"DELETE from `publishers` where `smap_server`=" + smapServer +
				" `smap_port`= " + smapPort +  "`smap_uri`=" + smapUri);
			s.close();
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
			if(conn != null)
				closeConn(conn);
		} finally {
			closeConn(conn);
		}
	}

	public void removePublisher(UUID pubid){
		Connection conn = null;
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			s.executeUpdate ("DELETE from `publishers` where `pubid`=\"" + pubid.toString() + "\"");
			s.close();
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
			if(conn != null)
				closeConn(conn);
		} finally {
			closeConn(conn);
		}
	}
	
	/*public isLastSmapBulkReportStreamPub(UUID pubid){
		boolean isLast = false;
		Connection conn = null;
		try {
			String query = "SELECT `smap_report_id` from `publishers` where `pubid`=?";
			String query2 = "SELECT count(`id`) as numpubs from `publisher` where `smap_report_id`=?";
			conn = openConnLocal();
			PreparedStatement ps1 = conn.prepareStatement(query);
			ps1.setString(1, pubid.toString());
			ResultSet rs1 = ps1.executeQuery();
			if(rs1.next()){
				String id = rs1.getString("smap_report_id");
				if(id != null){
					PreparedStatement ps2 = conn.prepareStatement(query2);
					ps2.setString(1, id);
					ResultSet rs2 = ps2.executeQuery();
					int count =0;
					if(rs2.next()){
						int numpubs = rs.getInt("numpubs");
						if(numpubs>0)
							isLast = true;
					}
				} else {
					logger.warning("Could not find " + pubid.toString() + " in publishers table");
				}
			}
			
		} catch(Exception e) {
			logger.log(Level.WARNING, "", e);
		}

		closeConn(conn);
		logger.info("LAST_BULK_STREAM? (pubid="+ pubid+", smap_report_id="+id+"); return " + isLast);
		return isLast;
	}*/

	public boolean isSmapBulkReportEntry(UUID pubid){
		boolean isSmapBulkReportEntry = false;
		Connection conn = null;
		String id=null;
		try {
			String query = "SELECT `smap_report_id` from `publishers` where `pubid`=?";
			conn = openConnLocal();
			PreparedStatement ps1 = conn.prepareStatement(query);
			ps1.setString(1, pubid.toString());
			ResultSet rs1 = ps1.executeQuery();
			if(rs1.next()){
				id = rs1.getString("smap_report_id");
				logger.info("(pubid="+ pubid+", smap_report_id="+id+")");
				if(id != null){
					String query2 = "SELECT `id` from `bulk_reports` where `smap_report_id`=?";
					PreparedStatement ps2 = conn.prepareStatement(query2);
					ps2.setString(1, id);
					ResultSet rs2 = ps2.executeQuery();
					int count =0;
					if(rs2.next()){
						isSmapBulkReportEntry = true;
					}
				} else {
					logger.warning("Could not find " + pubid.toString() + " in publishers table");
				}
			}
			
		} catch(Exception e) {
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		logger.info("(pubid="+ pubid+", smap_report_id="+id+"); return " + isSmapBulkReportEntry);
		return isSmapBulkReportEntry;
	}

	public void removeRestResource(String resourcePath){
		Connection conn = null;
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			int count = s.executeUpdate (
				"DELETE from `rest_resources` where `path`=\"" + resourcePath + "\"");
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}
	}

	public int publisherCount(String reportId) {
		Connection conn = null;
		int count =0;
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery (
				"SELECT count(*) as numpubs FROM `publishers` WHERE `smap_report_id`=\"" + reportId + "\"");
			if(rs.next()){
				count = rs.getInt("numpubs");
			}
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return count;
	}
	
	public int publisherCount() {
		Connection conn = null;
		int count =0;
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery (
				"SELECT count(*) as numpubs FROM `publishers`");
			if(rs.next()){
				count = rs.getInt("numpubs");
			}
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}
		return count;
	}
	
	/**
	 *  Returns a JSONArray with all unique SMAP report ids.
	 */
	public JSONArray getUniqueSmapReportIds(){
		Connection conn = null;
		JSONArray smapReportIds = new JSONArray();
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery (
				"SELECT distinct(`smap_report_id`) FROM `publishers`");
			while(rs.next())
				smapReportIds.add(rs.getString("smap_report_id"));
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return smapReportIds;
	}
	
	/**
	 *  Returns a JSONArray with all the publisher ids associated with this SMAP report id.
	 */
	public JSONArray getAssocSmapReportPubIds(String reportId){
		Connection conn = null;
		JSONArray brPubids = new JSONArray();
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery (
				"SELECT `pubid` FROM `publishers` where `smap_report_id`=\"" + reportId + "\"");
			if(rs.next())
				brPubids.add(rs.getString("pubid"));
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return brPubids;
	}
	
	public String getBulkReportSmapUrl(String bulkReportId){
		Connection conn = null;
		StringBuffer bulkSmapUrlBuf = new StringBuffer();
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery (
				"SELECT `smap_server`,`smap_port`,`smap_uri` FROM `bulk_reports` WHERE `smap_report_id`=\"" 
					+ bulkReportId + "\"");
			if(rs.next()){
				bulkSmapUrlBuf.append("http://").append(rs.getString("smap_server")).
					append(":").append(rs.getInt("smap_port")).append(rs.getString("smap_uri"));
			}
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}finally {
			closeConn(conn);
		}

		return bulkSmapUrlBuf.toString();
	}
	
	public int deleteBulkReportById(String bulkReportId){
		Connection conn = null;
		int count=0;
		StringBuffer bulkSmapUrlBuf = new StringBuffer();
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			count = s.executeUpdate ("DELETE FROM `bulk_reports` WHERE `smap_report_id`=\"" + bulkReportId + "\"");
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}finally {
			closeConn(conn);
		}

		return count;
	}
	
	public int updateBulkReportId(String oldReportId, String newReportId){
		Connection conn = null;
		int count =0;
		try {
			conn = openConnLocal();
			String query = "UPDATE `bulk_reports` set `smap_report_id`= ? WHERE `smap_report_id`= ?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, newReportId);
			ps.setString(2, oldReportId);
			count = ps.executeUpdate();
			ps.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return count;
	}
	

	public boolean isBulkReport(String bulkReportId){
		Connection conn = null;
		boolean isBulkReport = false;
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery (
				"SELECT id FROM `bulk_reports` WHERE `smap_report_id`=\"" + bulkReportId + "\"");
			if(rs.next())
				isBulkReport = true;
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return isBulkReport;
	}

	public void inputBulkReportsEntry(String smapUrl, String reportId, String muxReportEx){
		Connection conn = null;
		try{
			if(smapUrl != null && reportId != null){
				URL smapurl = new URL(smapUrl);
				String smapServer = smapurl.getHost();
				int smapPort = (smapurl.getPort()<0)?80:smapurl.getPort();
				String smapUri = smapurl.getPath();

				conn = openConnLocal();
				String query = "INSERT INTO `bulk_reports` (`smap_report_id`,`smap_server`,`smap_port`, `smap_uri`, `muxReportEx`) " +
						"values (?, ?, ?, ?, ?)";
				logger.info("QUERY: " + query);
				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1, reportId);
				ps.setString(2, smapServer);
				ps.setInt(3, smapPort);
				ps.setString(4, smapUri);
				ps.setString(5, muxReportEx);
				logger.info("BOUND QUERY: " + ps.toString());
				int count = ps.executeUpdate();
				ps.close();
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

	}

	public int updatePublisherEntry(String oldReportId, String reportId){
		Connection conn = null;
		int count=0;
		try{
			if(oldReportId != null && reportId != null){
				conn = openConnLocal();
				String query = "UPDATE `publishers` set `smap_report_id`= ? WHERE `smap_report_id`=?";
				logger.info("QUERY: " + query);
				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1, reportId);
				ps.setString(2, oldReportId);
				count = ps.executeUpdate();
				logger.info("Update ROWS: " + count);
				ps.close();
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return count;
	}
	
	public String getSmapReportPath (String smap_report_id){
		String reportUri = null;	
		Connection conn = null;
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			int count;
			ResultSet rs = s.executeQuery (
				"SELECT `report_uri` FROM `publishers` WHERE `smap_report_id`= \"" + smap_report_id + "\"");
			if(rs.next())
				reportUri = rs.getString("report_uri");
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return reportUri;
	}
	
	
	public JSONObject getInactiveSmapPubs(){
		JSONObject res = new JSONObject();
		Connection conn = null;
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			int count;
			ResultSet rs = s.executeQuery (
				"SELECT id, smap_server, smap_port, smap_uri FROM `publishers` WHERE smap_report_id IS NULL");
			while(rs.next()) {
				int id = rs.getInt("id");
				String smapServer = rs.getString("smap_server");
				int smapPort = rs.getInt("smap_port");
				String smapUri = rs.getString("smap_uri");
				String url = null;
				if(smapPort != 80)
					url = smapServer + ":" + smapPort + smapUri;
				else 
					url = smapServer + smapUri;
				res.put(id, url);
			}
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return res;
	}

	public String getPath(int rrTableId){
		Connection conn = null;
		try {
			String path  =null;
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			int count;
			ResultSet rs = s.executeQuery (
				"SELECT path FROM `rest_resources` WHERE id=" + rrTableId);
			while(rs.next())
				path = rs.getString("path");
			s.close();
			closeConn(conn);
			return path;
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return null;
	}

	public int publishersEntryId(String url){
		Connection conn = null;
		int id = -1;
		try {
			URL smapUrl = new URL(url);
			String host = smapUrl.getHost();
			int port = smapUrl.getPort();
			String uri = smapUrl.getPath();
			conn = openConnLocal();
			Statement s = conn.createStatement();
			int count;
			ResultSet rs = s.executeQuery(
					"SELECT id from `publishers` where `smap_server`= " + host + " and `smap_port`=" +
						port + " `smap_uri`= " + uri
					);
			if(rs.next())
				id = rs.getInt("id");	
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			closeConn(conn);
		}

		return id;
	}

	public void addPublisher(UUID pubid, String alias, String smapUrl, String reportUri, String reportId){
		Connection conn = null;
		try {
			logger.info("Adding new publisher: " +
					"(" + pubid.toString() + ", " + smapUrl + ", " + reportUri + ")");
			String query = null;
			if(smapUrl != null && reportUri != null && reportId !=null){
				//decompose the smap url
				URL smapurl = new URL(smapUrl);
				String smapServer = smapurl.getHost();
				String smapUri = smapurl.getPath();
				int smapPort = smapurl.getPort();
				smapPort = (smapPort<0)?80:smapPort;
				query = "INSERT INTO `publishers` (`pubid`, `alias`, `smap_server`, " + 
						"`smap_port`, `smap_uri`, `report_uri`, `smap_report_id`) VALUES "
					+ "(\"" + pubid.toString()
					+ "\",\"" + alias
					+ "\",\"" + smapServer
					+ "\",\"" + smapPort
					+ "\",\"" + smapUri
					+ "\",\"" + reportUri
					+ "\",\"" + reportId
					+ "\")";
			} else if(reportUri != null) {
				query = "INSERT INTO `publishers` (`pubid`, `report_uri`" + 
						") VALUES "
					+ "(\"" + pubid.toString()
					+ "\",\"" + reportUri
					+ "\")";
			}

			if (query != null) {
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				int count;
				count = s.executeUpdate (query);
				s.close();
				logger.info(count + " rows were inserted into 'publishers'");
			} else {
				logger.warning("Null smapUrl OR reportUri");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while storing publisher id", e);
		} finally {
			closeConn(conn);
		}	
	}

	public void addBulkPublisher(UUID pubid, String alias, String smapUrl, String is4RRPath, String bulkReportId){
		Connection conn = null;
		try {
			logger.info("Adding new Bulk publisher: " +
					"(" + pubid.toString() + ",  " + is4RRPath + ", " + bulkReportId + ")");
			logger.info("SmapURL: " + smapUrl);
			URL smapURLObj = null;
			try{
				if(smapUrl!=null)
					smapURLObj = new URL(smapUrl);
			} catch(Exception e){
				logger.info("No smap url provided for bulk report");
			}

			String query = null;
			if(smapURLObj != null){
				String smapServer = smapURLObj.getHost();
				String smapUri = smapURLObj.getPath();
				int smapPort = smapURLObj.getPort();
				smapPort = (smapPort<0)?80:smapPort;
				if(pubid != null && is4RRPath != null && bulkReportId != null) {
					query = "INSERT INTO `publishers` (`pubid`, `alias`, `smap_server`, " + 
							"`smap_port`, `smap_uri`, `report_uri`, `smap_report_id`) VALUES "
						+ "(\"" + pubid.toString()
						+ "\",\"" + alias
						+ "\",\"" + smapServer
						+ "\",\"" + smapPort
						+ "\",\"" + smapUri
						+ "\",\"" + is4RRPath
						+ "\",\"" + bulkReportId
						+ "\")";
				}
			} else {
				if(pubid != null && is4RRPath != null && bulkReportId != null) {
					query = "INSERT INTO `publishers` (`pubid`, `alias`, `report_uri`, `smap_report_id`" + 
							") VALUES "
						+ "(\"" + pubid.toString()
						+ "\",\"" + alias
						+ "\",\"" + is4RRPath
						+ "\",\"" + bulkReportId
						+ "\")";
				}
			}

			if (query != null) {
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				int count;
				count = s.executeUpdate (query);
				s.close();
				logger.info(count + " rows were inserted into 'publishers'");
			} else {
				logger.warning("Null smapUrl OR reportUri");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while storing bulk publisher id", e);
		} finally {
			closeConn(conn);
		}
	}


	/**
	 * If there is a smapUrl in the databases, return the associated publisher id.  If
	 * there is no entry for this smapUrl, return null;
	 */
	public UUID isBulkPublisher(String is4Uri){
		Connection conn = null;
		UUID pubidUUID = null;

		try{
			String query = "SELECT `pubid` FROM `publishers` WHERE `smap_server`=NULL and `smap_port`=NULL and `smap_uri`=NULL and `report_uri`=\"" +
					is4Uri + "\"";
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			logger.info("QUERY: " + query);
			ResultSet rs = s.executeQuery (query); 
			while(rs.next()) {
				String pubidStr = rs.getString("pubid");
				pubidUUID = UUID.fromString(pubidStr);
			}
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return pubidUUID;
	}

	/**
	 * If there is a smapUrl in the databases, return the associated publisher id.  If
	 * there is no entry for this smapUrl, return null;
	 */
	public UUID isPublisher(String uriUrl, boolean smap){
		Connection conn = null;
		UUID pubidUUID = null;

		try{
			String query = null;
			if(smap){
				//decompose the smap url
				URL smapurl = new URL(uriUrl);
				String smapServer = smapurl.getHost();
				String smapUri = smapurl.getPath();
				int smapPort = smapurl.getPort();
				smapPort = (smapPort<0)?80:smapPort;
				query = "SELECT `pubid` FROM `publishers` WHERE `smap_server`=\"" + smapServer +
									"\" and `smap_port`=" + smapPort + " and `smap_uri`=\"" 
									+ smapUri + "\"";
			}  else {
				query = "SELECT `pubid` FROM `publishers` WHERE `report_uri`=\"" + uriUrl + "\"";
			}

			conn = openConnLocal();
			Statement s = conn.createStatement ();
			logger.info("QUERY: " + query);
			ResultSet rs = s.executeQuery (query); 
			while(rs.next()) {
				String pubidStr = rs.getString("pubid");
				pubidUUID = UUID.fromString(pubidStr);
			}
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return pubidUUID;
	}

	public UUID isRRPublisher2(String restResource){
		Connection conn = null;
		UUID pubidUUID = null;

		try{
			conn = openConnLocal();

			Statement s = conn.createStatement ();
			String pubid_query = "select pubid from publishers where report_uri=? or report_uri=?";
			PreparedStatement ps1 = conn.prepareStatement(pubid_query);
			
			ps1.setString(1, restResource);
			String restResource2 = null;
			if(restResource.endsWith("/"))
				restResource2 = restResource.substring(0, restResource.length()-1);
			else
				restResource2 = restResource + "/";
			ps1.setString(2, restResource2);
			logger.info("Checking for " + restResource + " and " + restResource2);
			
			ResultSet rs1 = ps1.executeQuery();
			String pubUUIDStr = null;
			if(rs1.next())
				pubUUIDStr = rs1.getString("pubid");
			ps1.close();

			if(pubUUIDStr != null)
				pubidUUID = UUID.fromString(pubUUIDStr);	
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return pubidUUID;
	}

	public String getRRType(String restResource){
		Connection conn = null;
		String rrTypeStr = null;

		try{
			conn = openConnLocal();

			Statement s = conn.createStatement ();
			String rrtype_query = "select type from rest_resources where path=? or path=?";
			PreparedStatement ps1 = conn.prepareStatement(rrtype_query);
			
			ps1.setString(1, restResource);
			String restResource2 = null;
			if(restResource.endsWith("/"))
				restResource2 = restResource.substring(0, restResource.length()-1);
			else
				restResource2 = restResource + "/";
			ps1.setString(2, restResource2);
			
			ResultSet rs1 = ps1.executeQuery();
			
			if(rs1.next())
				rrTypeStr = rs1.getString("type");
			ps1.close();

		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return rrTypeStr;
	}

	public void setRRType(String path, String type){
		Connection conn = null;
		try{
			conn = openConnLocal();

			Statement s = conn.createStatement ();
			String query = "UPDATE `rest_resources` set `type`= ? WHERE path= ? OR path=?";
			PreparedStatement ps = conn.prepareStatement(query);
			
			ps.setString(1, type);
			ps.setString(2, path);
			String path2 = null;
			if(path.endsWith("/"))
				path2 = path.substring(0, path.length()-1);
			else
				path2 = path + "/";
			ps.setString(3, path2);
			
			int count = ps.executeUpdate();
			logger.info("Updated " + count + " rows in rest_resources");
			
			ps.close();

		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
	}

	/**
	 * If there is a rest resource in the databases, return the associated publisher id.  If
	 * there is no entry for this rest resource, return null;
	 */
	public UUID isRRPublisher(String restResource){
		Connection conn = null;
		UUID pubidUUID = null;

		try{
			conn = openConnLocal();

			Statement s = conn.createStatement ();
			String rridt_query = "select id from rest_resources where path=? or path=?";//rs1
			String pubidt_query = "select pubtable_id from devices where rrid=?";//rs1.getInt("id"), rs2
			String pubid_query = "select pubid from publishers where id=?";//rs2.getInt("pubtable_id"),rs3.getString("pubid")

			PreparedStatement ps1 = conn.prepareStatement(rridt_query);
			
			ps1.setString(1, restResource);
			String restResource2 = null;
			if(restResource.endsWith("/"))
				restResource2 = restResource.substring(0, restResource.length()-1);
			else
				restResource2 = restResource + "/";
			ps1.setString(2, restResource2);
			logger.info("Checking for " + restResource + " and " + restResource2);
			
			ResultSet rs1 = ps1.executeQuery();
			int rrTableid = -1;
			if(rs1.next())
				rrTableid = rs1.getInt("id");
			ps1.close();

			if(rrTableid >0){
				PreparedStatement ps2 = conn.prepareStatement(pubidt_query);
				ps2.setInt(1, rrTableid);
				ResultSet rs2 = ps2.executeQuery();
				int pubTableid = -1;
				if(rs2.next())
					pubTableid = rs2.getInt("pubtable_id");
				ps2.close();

				if(pubTableid >0){
					PreparedStatement ps3 = conn.prepareStatement(pubid_query);
					ps3.setInt(1, pubTableid);
					ResultSet rs3 = ps3.executeQuery();
					if(rs3.next())
						pubidUUID = UUID.fromString(rs3.getString("pubid"));
					ps3.close();
				}
			}

		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return pubidUUID;
	}

	public String getIs4RRPath(UUID pubid){
		Connection conn = null;
		String smapReportId = null;
		try{
			if(pubid != null){
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				String query = "SELECT `report_uri` FROM `publishers` WHERE `pubid`=\"" 
								+ pubid.toString() + "\"";
				logger.info("QUERY: " + query);
				ResultSet rs = s.executeQuery (query);
				while(rs.next()) 
					smapReportId = rs.getString("report_uri");
				s.close();
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
		return smapReportId;
	}

	public String getSmapReportId(UUID pubid){
		Connection conn = null;
		String smapReportId = null;
		try{
			if(pubid != null){
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				String query = "SELECT `smap_report_id` FROM `publishers` WHERE `pubid`=\"" 
								+ pubid.toString() + "\"";
				logger.info("QUERY: " + query);
				ResultSet rs = s.executeQuery (query);
				while(rs.next()) 
					smapReportId = rs.getString("smap_report_id");
				s.close();
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return smapReportId;
	}

	public String getSmapUrl(UUID pubid){
		Connection conn = null;
		String smapUrlValid = null;
		try{
			if(pubid != null){
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				String query = "SELECT `smap_server`, `smap_port`, `smap_uri` FROM `publishers` WHERE `pubid`=\"" 
								+ pubid.toString() + "\"";
				logger.info("QUERY: " + query);
				ResultSet rs = s.executeQuery (query);
				if(rs.next()){ 
					String smapServerStr = rs.getString("smap_server");
					int smapPortInt = rs.getInt("smap_port");
					String smapUriStr = rs.getString("smap_uri");

					//make sure the url is valid (should be)
					String smapUrlStrTest = "http://" + smapServerStr + ":" + smapPortInt + smapUriStr;
					URL smapUrl = new URL(smapUrlStrTest);
					smapUrlValid = smapUrl.toString();
				}
				s.close();
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return smapUrlValid;
	}

	public String getAlias(UUID pubid){
		Connection conn = null;
		String alias = null;
		try{
			if(pubid != null){
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				String query = "SELECT `alias` FROM `publishers` WHERE `pubid`=\"" 
								+ pubid.toString() + "\"";
				logger.info("QUERY: " + query);
				ResultSet rs = s.executeQuery (query);
				while(rs.next()) 
					alias = rs.getString("alias");
				s.close();
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return alias;
	}

	/**
	 *  If there is a smap url associated with this publisher id return it.  Otherwise return
	 *  null.
	 */
	public String isSmapPublisher(UUID pubid){
		Connection conn = null;
		String smapUrl = null;
		try{
			if(pubid != null){
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				String query = "SELECT `smap_server`, `smap_port`, `smap_uri` FROM `publishers` WHERE `pubid`=\"" 
								+ pubid.toString() + "\"";
				logger.info("QUERY: " + query);
				ResultSet rs = s.executeQuery (query);
				while(rs.next()) 
					smapUrl = "http://" + rs.getString("smap_server") + ":" + rs.getInt("smap_port") + 
						rs.getString("smap_uri");
				s.close();
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return smapUrl;
	}
	
	/**
	 *  If there is publisher id return true.  Otherwise return false.
	 */
	public boolean isPublisher(UUID pubid){
		Connection conn = null;
		boolean exists = false;	
		try{
			if(pubid != null){
				conn = openConnLocal();
				String query = "SELECT `pubid` FROM `publishers` WHERE `pubid`=?"; 
				logger.info("QUERY: " + query);
				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1,pubid.toString());
				logger.info("QUERY VAL: " + pubid.toString());
				ResultSet rs = ps.executeQuery ();
				if(rs.next()) 
					exists = true;
				ps.close();
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return exists;
	}

	/**
	 *  Returns the entire publishers table add attribute value pair in a json object.
	 *  The attributes are the publisher ids and the values the smap urls.
	 */
	public JSONObject getPublishersTable(){
		Connection conn = null;
		JSONObject pubTable = new JSONObject();
		try{
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery ("SELECT pubid, smap_server, smap_port, smap_uri FROM `publishers`");

			while(rs.next()) {
				String smapUrl = rs.getString("smap_server") + rs.getInt("smap_port") +
									rs.getString("smap_uri");
				pubTable.put(rs.getString("pubid"), smapUrl);
			}
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return pubTable;
	}

	private int getPubTableId(UUID pubid){
		Connection conn = null;
		int id = -1;
		try {
			if(pubid != null) {
				conn = openConnLocal();
				Statement s = conn.createStatement();
				String query ="SELECT `id` from `publishers` where pubid=\"" + pubid.toString() + "\""; 
				logger.info("QUERY: " + query);
				ResultSet rs = s.executeQuery(query);
				while(rs.next())
					id=rs.getInt("id");
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return id;
	}

	public int getRRTableId(String path){
		Connection conn = null;
		int id=-1;
		String path2;
		if(path.endsWith("/"))
			path2 = path.substring(0, path.length()-1);
		else
			path2 = path + "/";

		try {
			conn = openConnLocal();
			Statement s = conn.createStatement();
			String query = "SELECT `id` from `rest_resources` where `path`=\"" + path + "\" or `path`=\"" + path2 + "\"";
			ResultSet rs = s.executeQuery(query);
			logger.info("query: " + query);
					
			if(rs.next()){
				id =rs.getInt("id");
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return id;
	}

	public boolean deviceEntryExists(String device_name, String restResource){
		
		Connection conn = null;
		try{
			conn = openConnLocal();

			Statement s = conn.createStatement ();
			String q1 = "SELECT `id` FROM `rest_resources` WHERE `path`=? OR `path`=?";
			String q2 = "SELECT `id` FROM `devices` WHERE `device_name`=? AND `rrid`=?";

			PreparedStatement ps1 = conn.prepareStatement(q1);
			ps1.setString(1, restResource);
			String restResource2 = null;
			if(restResource.endsWith("/"))
				restResource2 = restResource.substring(0, restResource.length()-1);
			else
				restResource2 = restResource + "/";
			ps1.setString(2, restResource2);
			ResultSet rs1 = ps1.executeQuery();

			int rrTableid = -1;
			if(rs1.next())
				rrTableid = rs1.getInt("id");
			ps1.close();

			if(rrTableid >0){
				PreparedStatement ps2 = conn.prepareStatement(q2);
				ps2.setString(1, device_name);
				ps2.setInt(2, rrTableid);
				ResultSet rs2 = ps2.executeQuery();
				int devicesTableId = -1;
				if(rs2.next())
					devicesTableId = rs2.getInt("id");
				ps2.close();
				closeConn(conn);

				if(devicesTableId >0)
					return true;
				return false;
			}
			closeConn(conn);

		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return false;
	}


	public void addDeviceEntry(String device_name, String resource_uri, UUID pubid){
		Connection conn = null;
		try {
			int pubtable_id = -1;
			if(pubid != null)
				pubtable_id= this.getPubTableId(pubid);
			int rrid = this.getRRTableId(resource_uri);
			logger.info(resource_uri + " PTID: " + pubtable_id + " RRID: " + rrid + " pubid=" + pubid);

			conn = openConnLocal();
			if(pubtable_id >=0 && rrid>=0){
				try {
					Statement s = conn.createStatement ();
					int count;
					count = s.executeUpdate (
						"INSERT INTO `devices` (`device_name`, `pubtable_id`, `rrid`) VALUES "
							+ "(\"" 
							+ device_name
							+ "\",\"" + pubtable_id
							+ "\",\"" + rrid
							+ "\")");
					s.close();
					logger.info(count + " rows were inserted into 'devices'");
				} catch (Exception e){
					logger.log(Level.WARNING, "Error while storing publisher id", e);
				}
			} else if(pubtable_id < 0 && pubid == null && rrid>0){
				try {
					Statement s = conn.createStatement ();
					int count;
					count = s.executeUpdate (
						"INSERT INTO `devices` (`device_name`, `rrid`) VALUES "
							+ "(\"" 
							+ device_name
							+ "\",\"" + rrid
							+ "\")");
					s.close();
					logger.info(count + " rows were inserted into 'devices'");
				} catch (Exception e){
					logger.log(Level.WARNING, "Error while storing publisher id", e);
				}
			} else {
				if(pubtable_id <0 && pubid != null)
					logger.warning("Could not find " + pubid.toString() + " entry in publishers table");
				if(rrid <0)
					logger.warning("Could not find " + resource_uri + " entry in rest_resources table");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "",e);
		}
		finally {
			closeConn(conn);
		}
	}

	public void removeDeviceEntry(String rrPath){
		Connection conn = null;
		try{
			int rrid = getRRTableId(rrPath);
			if(rrid>=0){
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				s.executeUpdate ("DELETE from `devices` where `rrid`=" + rrid);
				s.close();
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
	}

	public String getPathWithId(int id){
		Connection conn = null;
		String path = null;
		try {
			conn = openConnLocal();
			Statement s = conn.createStatement();
			String query = "SELECT path from `rest_resources` where `id`=" + id;
			logger.info("QUERY: " + query);
			ResultSet rs = s.executeQuery(query);
			if(rs.next())
				path = rs.getString("path");
		} catch (Exception e) {
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return path;
	}

	
	/************************************************************************/
	public String rrGetPropertiesStr(String path){
		Connection conn = null;
		try{
			if(path != null) {
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				ResultSet rs = s.executeQuery ( "SELECT `properties` FROM `rest_resources` WHERE `path`=\"" + path + "\"");
				while(rs.next()){
					Blob propsBlob = rs.getBlob("properties");
					String propsStr = null;
					if(propsBlob != null){
						propsStr = new String (propsBlob.getBytes(1L, (int)propsBlob.length()));
					}
					s.close();
					closeConn(conn);
					return propsStr;
				}
				return null;
			}

		} catch (Exception e){
			if(conn != null)
				closeConn(conn);
			logger.log(Level.WARNING, "", e);
			return null;
		}
		finally {
				closeConn(conn);
		}
		return null;
	}

	public JSONObject rrGetProperties(String path){
		Connection conn = null;
		try{
			if(path != null) {
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				ResultSet rs = s.executeQuery ( "SELECT `properties` FROM `rest_resources` WHERE `path`=\"" + path + "\"");
				while(rs.next()){
					Blob propsBlob = rs.getBlob("properties");
					JSONObject props = new JSONObject();
					if(propsBlob != null){
						String propsStr = new String (propsBlob.getBytes(1L, (int)propsBlob.length()));
						props = (JSONObject) JSONSerializer.toJSON(propsStr);
					}
					s.close();
					closeConn(conn);
					return props;
				}
				return null;
			}

		} catch (Exception e){
			if(conn != null)
				closeConn(conn);
			logger.log(Level.WARNING, "", e);
			return null;
		}

		finally {
			closeConn(conn);
		}
		
		return null;
	}

	public void rrPutProperties(String path, JSONObject props){
		Connection conn = null;
		try{
			if(path != null && props != null && rrPathExists(path)){
				conn = openConnLocal();
				//Statement s = conn.createStatement ();
				PreparedStatement ps = conn.prepareStatement("UPDATE `rest_resources` set `properties`= ? WHERE path= ? or path=?");
				String withoutSlash=null;
				String withSlash=null;
				if(!path.endsWith("/")){
					withSlash = path + "/";
					withoutSlash = path;
				} else {
					withSlash = path;
					withoutSlash = path.substring(0,path.length()-1);
				}
				SerialBlob thisBlob = new SerialBlob(props.toString().getBytes());
				ps.setBlob(1, thisBlob);
				ps.setString(2, withoutSlash);
				ps.setString(3, withSlash);
				//String properties = props.toString().replaceAll("\"", "\\\"");
				//String query = "UPDATE `rest_resources` set `properties`= \"" + properties + "\" WHERE path=\"" + path + "\"";
				//logger.info("Executing: " + query);
				//int count = ps.executeUpdate (query);
				int count = ps.executeUpdate ();
				ps.close();
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
	}

	public void rrPutProperties(String path, String propsStr){
		Connection conn = null;
		try{
			if(path != null && propsStr != null && rrPathExists(path)){
				conn = openConnLocal();
				//Statement s = conn.createStatement ();
				PreparedStatement ps = conn.prepareStatement("UPDATE `rest_resources` set `properties`= ? WHERE path= ? or path=?");
				String withoutSlash=null;
				String withSlash=null;
				if(!path.endsWith("/")){
					withSlash = path + "/";
					withoutSlash = path;
				} else {
					withSlash = path;
					withoutSlash = path.substring(0,path.length()-1);
				}
				SerialBlob thisBlob = new SerialBlob(propsStr.getBytes());
				ps.setBlob(1, thisBlob);
				ps.setString(2, withoutSlash);
				ps.setString(3, withSlash);
				//String properties = props.toString().replaceAll("\"", "\\\"");
				//String query = "UPDATE `rest_resources` set `properties`= \"" + properties + "\" WHERE path=\"" + path + "\"";
				//logger.info("Executing: " + query);
				//int count = ps.executeUpdate (query);
				int count = ps.executeUpdate ();
				ps.close();
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
	}

	public void rrPutPath(String path){
		Connection conn = null;
		try {
			if(path!=null && !this.rrPathExists(path)){
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				int count = s.executeUpdate ("INSERT INTO `rest_resources`(`path`) VALUES(\"" + path + "\")");
				s.close();
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
	}

	public boolean rrPathExists(String path) {
		Connection conn = null;
		try{
			if(path != null) {
				conn = openConnLocal();
				PreparedStatement ps= conn.prepareStatement ( "SELECT * FROM `rest_resources` WHERE `path`=? or `path`=?");
				String withoutSlash=null;
				String withSlash=null;
				if(!path.endsWith("/")){
					withSlash = path + "/";
					withoutSlash = path;
				} else {
					withSlash = path;
					withoutSlash = path.substring(0,path.length()-1);
				}
				ps.setString(1, withSlash);
				ps.setString(2, withoutSlash);
				ResultSet rs = ps.executeQuery();
				if(rs.next()){
					closeConn(conn);
					return true;
				}
			}

		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return false;
	}

	public JSONArray rrGetChildren(String path){
		Connection conn = null;
		try{
			JSONArray children = new JSONArray();
			Hashtable<String, String> childrenAdded = new Hashtable<String, String>();
			if(path != null) {
				conn = openConnLocal();
				Statement s = conn.createStatement ();
				String query = "SELECT * FROM `rest_resources` WHERE `path` like \"" + path + "%\"";
				logger.info("QUERY: " + query);
				ResultSet rs = s.executeQuery (query);
				while(rs.next()){
					String thisPath = rs.getString("path");

					//parse out the path's immediate children
					thisPath = thisPath.replace(path, "");
					StringTokenizer pathTokenizer = new StringTokenizer(thisPath, "/");
					
					if(!thisPath.equalsIgnoreCase(path) && pathTokenizer.countTokens()>0){
						String nToken = pathTokenizer.nextToken();
						logger.fine("Checking:\n\tbase: " + path + "\n\ttoken:" +  nToken);
						logger.info("Checking path: \n\t" + path + nToken + "\n\tpath: " +
								path + nToken + "/");
						
						if(!childrenAdded.containsKey(nToken) && (rrPathExists(path + nToken + "/") || rrPathExists(path + nToken))) {
							children.add(nToken);
							childrenAdded.put(nToken, "");
						}
					}
				}
				s.close();
				closeConn(conn);
				return children;
			}

		} catch (Exception e){
			if(conn != null);
				closeConn(conn);
			logger.log(Level.WARNING, "", e);
			return null;
		}
		finally {
			closeConn(conn);
		}
		return null;
	}

	public JSONArray rrGetAllPaths(){
		JSONArray allpaths = new JSONArray();
		Connection conn = null;
		try{
			conn = openConnLocal();
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery ( "SELECT path FROM `rest_resources`");
			while(rs.next())
				allpaths.add(rs.getString("path"));
			s.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return allpaths;
	}

	public JSONArray getAllHardLinks(){
		JSONArray hardlinkUris = new JSONArray();
		Connection conn = openConn();
		try{
			String query = "SELECT `path` FROM `rest_resources` WHERE `type`!=\"symlinks\"";
			PreparedStatement ps = conn.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				hardlinkUris.add(rs.getString("path"));

		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
		
		return hardlinkUris;

	}

	/**********************************************************************************************/

	public boolean checkPubIdIsRegistered(String id){

		Connection conn = openConn();
		try {
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery ( "SELECT * FROM `object_stream` WHERE `id`=\"" + id + "\"");
			while(rs.next()){
				closeConn(conn);
				return true;
			}
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while checking if pubid " + id + " exists", e);
		}
		finally {
			closeConn(conn);
		}
		return false;
	}

	/**********************************************************************************************/
	
	public JSONObject getContextGraph(String contextGraphId){

		Connection conn = openConnLocal();
		JSONObject os =  new JSONObject();
		try {
			
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery ( "SELECT * FROM `contextGraphs` WHERE `id`=\"" + contextGraphId + "\"");
			while(rs.next()){
				Blob mapBlob = rs.getBlob("graph");
				os = (JSONObject) JSONSerializer.toJSON(new String(mapBlob.getBytes(1L, (int)mapBlob.length())));
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while fetching context graph", e);
		}
		finally {
			closeConn(conn);
		}
		return os;
	}

	public JSONArray getPubIdsFromSnodes(JSONArray snodes){
		JSONArray pubids = new JSONArray();

		Connection conn = openConn();
		try{			
			//build query
			StringBuffer queryBuf = new StringBuffer().append("select `pubid` from `publishers` where `report_uri` in ");
			StringBuffer setlist = new StringBuffer().append("(");
			StringBuffer starLike = new StringBuffer();
			JSONArray nonStarred = new JSONArray();
			JSONArray starredList = new JSONArray();
			for(int i=0; i<snodes.size(); i++){
				String thisPath = snodes.getString(i);
				if(thisPath.contains("*")){
					starredList.add(thisPath);
				} else {
					nonStarred.add(thisPath);
				}
			}
			for (int i=0; i<nonStarred.size(); i++){
				//adjust all snode urls to include those endings with / and without a /
				String thisPath = nonStarred.getString(i);
				String thisPath2 = null;
				if(thisPath.endsWith("/")){
					thisPath2 = thisPath.substring(0,thisPath.length()-1);
				} else {
					thisPath2 = thisPath + "/";
				}
				
				setlist.append("\"").append(thisPath).append("\", \"").append(thisPath2).append("\"");
				if(i<nonStarred.size()-1)
					setlist.append(", ");
			}
			if(nonStarred.size()==0)
				setlist.append("\"place_holder\"");
			setlist.append(")");
			for(int i=0; i<starredList.size(); i++){
				//adjust the like portion of the query for path with the * wildcard
				String thisPath = starredList.getString(i);
				thisPath = thisPath.replace("*", "%");
				starLike.append(" or `report_uri` like \"").append(thisPath).append("\"");
			}
			queryBuf.append(setlist);
			queryBuf.append(starLike);

			String query = queryBuf.toString();
			logger.info("QUERY: " + query);

			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery (query);
			while(rs.next())
				pubids.add(rs.getString("pubid"));
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return pubids;
	}

	public boolean wildcardPubMatch(String wildcardPath){
		boolean exists=false;
		Connection conn  = openConn();
		try{
			wildcardPath = wildcardPath.replace("*", "%");
			String query = "SELECT `report_uri` FROM `publishers` WHERE `report_uri` like ? limit 1";

			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, wildcardPath);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				exists=true;

		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		return exists;
	}

	/*** Family of functions to fetch/set data from the subscriptions table ***/

	public void insertNewSubEntry(UUID subid, String alias, String uri, String destUrl, String destUri, UUID src_pubid, String wildcardPath){

		Connection conn  = openConn();
		try{
			if(src_pubid!=null && isPublisher(src_pubid) ==false){
				logger.warning("Unknown publisher " + src_pubid.toString() + "\nCannot add subscriber");
				return;
			}
			
			if(destUrl !=null && destUrl.startsWith("http://")){
				destUri = null;
			} else if(destUrl == null && destUri == null){
				logger.finer("RETURNING");
				return;
			}

			Resource r = null;
			if(
				(destUrl!=null && destUri==null) || 
				(destUrl==null && destUri!=null && ((r=RESTServer.getResource(destUri)) != null)) 
				&& r.TYPE==ResourceUtils.MODEL_GENERIC_PUBLISHER_RSRC){
					
				String query = "INSERT INTO `subscriptions` (`subid`, `alias`, `uri`, `dest_url`, `dest_uri`,`src_pubid`, `wildcardPath`) ";
				query = query + "values (?, ?, ?, ?, ?, ?, ?)";

				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1, subid.toString());
				ps.setString(2, alias);
				ps.setString(3, uri);
				ps.setString(4, destUrl);
				ps.setString(5, destUri);
				if(src_pubid !=null)
					ps.setString(6, src_pubid.toString());
				else
					ps.setString(6, "wc");
				ps.setString(7, wildcardPath);
				int count = ps.executeUpdate();
				logger.info("InsertQuery: " + query);
				logger.info("InsertValues: ("+ subid.toString() + "," + alias + "," + uri + "," + destUrl + "," + destUri +
						"," + src_pubid + "," + wildcardPath);
				logger.info("INSERT_COUNT: " + count);
			} else {
				logger.finer("\n\tDestUrl: " + destUrl + "\n\tDestUri: " + destUri +
						"\n\tresource: " + r + "\n\tr.TYPE=" + r.TYPE);
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
	}
	
	public void partialUpdateSubEntry(UUID subid, String alias, String uri, UUID src_pubid, String wildcardPath){

		Connection conn  = openConn();
		try{
			if(src_pubid != null && isPublisher(src_pubid) ==false){
				logger.warning("Unknown publisher " + src_pubid.toString() + "\nCannot add subscriber");
				return;
			}

			String query = "UPDATE `subscriptions` set `alias`=?, `uri`=?, `src_pubid`=?, `wildcardPath`=? where `subid`=?";

			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, alias);
			ps.setString(2, uri);
			if(src_pubid !=null)
				ps.setString(3, src_pubid.toString());
			else 
				ps.setString(3, "wc");
			ps.setString(4, wildcardPath);
			ps.setString(5, subid.toString());
			int count= ps.executeUpdate();


			if(src_pubid !=null)
				logger.info("Query: " + query + "\n\talias=" + alias + "\n\turi:" + uri + 
					"\n\tsrc_pubid=" + src_pubid + "\n\twildcardPath=" + wildcardPath + "\n\tsubid=" + subid);
			else 
				logger.info("Query: " + query + "\n\talias=" + alias + "\n\turi:" + uri + 
					"\n\tsrc_pubid=wc" + "\n\twildcardPath=" + wildcardPath + "\n\tsubid=" + subid);

			logger.info("PartialUpdate_COUNT=" + count);
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
	}
	
	public String getSubscriptionId(UUID pubid, String wildcardPath, String target){
		Connection conn = openConn();
		String subid = null;
		try {
			String query = "SELECT `subid` FROM `subscriptions` where (`src_pubid`=? OR `wildcardPath`=?) AND " +
								"(`dest_url`=? OR `dest_url`=? OR `dest_uri`=? OR `dest_uri`=?)";
			String withSlash = null;
			String withoutSlash = null;
			if(!target.endsWith("/")){
				withSlash = target + "/";
				withoutSlash = target;
			} else {
				withSlash = target;
				withoutSlash = target.substring(0,target.length()-1);
			}

			String pubidStr = null;
			if(pubid != null)
				pubidStr = pubid.toString();

			logger.info("QUERY: " + query +  "\n\t1:" + pubidStr + "\n\t2:" + wildcardPath + 
							 "\n\t3:" + withSlash  + "\n\t4:" + withoutSlash 
							+ "\n\t5:" + withSlash  + "\n\t6:" + withoutSlash);
			PreparedStatement ps = conn.prepareStatement(query);
			if(pubid !=null)
				ps.setString(1, pubid.toString());
			else
				ps.setString(1, "wc");
			ps.setString(2, wildcardPath);
			ps.setString(3, withSlash);
			ps.setString(4, withoutSlash);
			ps.setString(5, withSlash);
			ps.setString(6, withoutSlash);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				subid = rs.getString("subid");
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}
		finally {
			closeConn(conn);
		}
		return subid;
	}
	
	public String getSubcriptionSourceStream(UUID subid){
		Connection conn = openConn();
		String pid = null;
		try {
			String query = "SELECT `src_pubid` FROM `subscriptions` where `subid`=?";
			logger.info("QUERY: " + query +  "\n\t1:" + subid.toString());
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				pid = rs.getString("src_pubid");
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}
		finally {
			closeConn(conn);
		}
		return pid;
	}

	public void removeSubEntry(UUID subid, UUID src_pubid, String wildcardPath){

		Connection conn  = openConn();
		try{

			if(src_pubid!=null && isPublisher(src_pubid) == false){
				logger.warning("Unknown publisher " + src_pubid.toString() + "\nCannot add subscriber");
				closeConn(conn);
				return;
			}

			String query=null;
			if(src_pubid !=null){
				query = "DELETE FROM `subscriptions` where `subid`=? AND `src_pubid`=?";

				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1, subid.toString());
				ps.setString(2, src_pubid.toString());
				ps.executeUpdate();
			} else if(src_pubid==null && wildcardPath!=null){
				query = "DELETE FROM `subscriptions` where `subid`=? AND `wildcardPath`=?";
				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1, subid.toString());
				ps.setString(2, wildcardPath);
				ps.executeUpdate();
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
	}

	public void removeSubEntry(UUID subid){

		Connection conn  = openConn();
		try{
			String query = "DELETE FROM `subscriptions` where `subid`=?";

			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ps.executeUpdate();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}
		closeConn(conn);
	}

	public void removeSubEntries(UUID subid, JSONArray streams){
		try{
			for(int i=0; i<streams.size(); i++){
				UUID thisPubId = null;
				try{ thisPubId = UUID.fromString(streams.getString(i)); } 
				catch(Exception e){ logger.log(Level.WARNING, "",e); }

				if(thisPubId != null)
					removeSubEntry(subid, thisPubId, null);
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}
	}
	
	public void removeSubByPubId(UUID pubid){
		Connection conn  = openConn();
		try{
			String query = "DELETE FROM `subscriptions` where `src_pubid`=?";

			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, pubid.toString());
			ps.executeUpdate();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}
		finally {
			closeConn(conn);
		}
	}

	public void removeSubByWildcardPath(String wildcardPath){
		Connection conn  = openConn();
		try{
			String query = "DELETE FROM `subscriptions` where `wildcardPath`=?";

			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, wildcardPath);
			ps.executeUpdate();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}
		finally {
			closeConn(conn);
		}
	}

	public String isSubscription(UUID subid){
		String subUri = null;
		Connection conn  = openConn();
		try{
			String query  = "SELECT `uri` from `subscriptions` where `subid`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				subUri = rs.getString("uri");
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		//returns the associated uri or null if this subid is not a subscriber
		return subUri;
	}

	public String getSubDestUrlStr(UUID subid){
		String destUrl = null;
		Connection conn  = openConn();
		try{
			String query = "SELECT `dest_url` from `subscriptions` where `subid`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				destUrl = rs.getString("dest_url");
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		//returns the destination URL or null if this subid is not a subscriber
		return destUrl;
	}
	
	public String getSubDestUriStr(UUID subid){
		String destUrl = null;
		Connection conn  = openConn();
		try{
			String query = "SELECT `dest_uri` from `subscriptions` where `subid`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				destUrl = rs.getString("dest_uri");
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		//returns the destination URL or null if this subid is not a subscriber
		return destUrl;
	}

	public JSONArray getAllAssociatedSubUris(String destUri){
		JSONArray a =new JSONArray();
		Connection conn  = openConn();
		try{
			String query = "SELECT `uri` from `subscriptions` where `dest_uri`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, destUri);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				a.add(rs.getString("uri"));


		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		return a;
	}

	public int getSubCountToModelPub(String modelPubUri){
		int numSubids = 0;
		Connection conn  = openConn();
		try{
			String query = "SELECT count(*) as c from `subscriptions` where `dest_uri`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, modelPubUri);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				numSubids = rs.getInt("c");

		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		//returns the destination URL or null if this subid is not a subscriber
		return numSubids;
	}

	public JSONArray getModelPubliserIds(String modelUri){
		JSONArray pubids = new JSONArray();
		Connection conn = openConn();
		try{
			String query =  "select `pubid` from `publishers` where `report_uri` like ?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1,modelUri + "%");
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				pubids.add(rs.getString("pubid"));
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
		return pubids;
	}


	public JSONArray getAssocStreamIds(UUID subid){
		JSONArray srcPubIds = new JSONArray();
		Connection conn  = openConn();
		try{
			String query = "SELECT `src_pubid`, `wildcardPath` from `subscriptions` where `subid`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String spidStr = rs.getString("src_pubid");
				String wildcardPath = rs.getString("wildcardPath");
				if(spidStr.equalsIgnoreCase("wc")){
					JSONArray matchingPubs = getMatchingStreamIds(wildcardPath);
					srcPubIds.addAll(matchingPubs);
				} else {
					srcPubIds.add(rs.getString("src_pubid"));
				}
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		//returns a list of pubids routing their data to the assocaited destination url or proxy for this subscription
		return srcPubIds;
	}

	public JSONArray getMatchingStreamIds(String wildcardPath){
		JSONArray srcPubIds = new JSONArray();
		Connection conn  = openConn();
		try{
			wildcardPath = wildcardPath.replace("*", "%");
			String query = "SELECT `pubid` from `publishers` where `report_uri` like ?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, wildcardPath);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				srcPubIds.add(rs.getString("pubid"));
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		//returns a list of pubids routing their data to the assocaited destination url or proxy for this subscription
		return srcPubIds;
	}

	public JSONArray getAssocStreamUris(UUID subid){

		Connection conn  = openConn();
		JSONArray srcPubUris = new JSONArray();
		try{
			JSONArray srcPubIds = getAssocStreamIds(subid);
			int numPubs = srcPubIds.size();
			if(numPubs>0){
				String query = "SELECT `report_uri` from `publishers` where `pubid` IN (";
				for(int i=0; i<numPubs; i++){
					query = query + "?";
					if(i != numPubs-1)
						query = query + ", ";
				}
				query = query + ")";

				PreparedStatement ps = conn.prepareStatement(query);
				for(int i=1; i<=numPubs; i++)
					ps.setString(i, srcPubIds.getString(i-1));
				ResultSet rs = ps.executeQuery();
				while(rs.next())
					srcPubUris.add(rs.getString("report_uri"));
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		//translates all the pubids to uris that are pubishing data to a destination url or proxy
		return srcPubUris;
	}

	public UUID getSubId(String uri){
		UUID subid = null;
		Connection conn  = openConn();
		try{
			String query = "SELECT `subid` from `subscriptions` where `uri`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, uri);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				subid = UUID.fromString(rs.getString("subid"));
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		//return the associated uri
		return subid;
	}

	public JSONArray getAllSubIds(){

		Connection conn  = openConn();
		JSONArray allsubids = new JSONArray();
		try{
			String query = "SELECT `subid` from `subscriptions`";
			PreparedStatement ps = conn.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				allsubids.add(rs.getString("subid"));
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}
		finally {
			closeConn(conn);
		}
		return allsubids;
	}

	public JSONArray getAllSubUris(){
		Connection conn  = openConn();
		JSONArray allsuburis = new JSONArray();
		try{
			String query = "SELECT `uri` from `subscriptions`";
			PreparedStatement ps = conn.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				allsuburis.add(rs.getString("uri"));
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		return allsuburis;
	}
	
	public String getSubUriBySubId(UUID subid){
		Connection conn  = openConn();
		JSONArray allsuburis = new JSONArray();
		try{
			String query = "SELECT `uri` from `subscriptions` where subid=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				return rs.getString("uri");
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		return null;
	}
	
	public JSONArray getSubIdsByPubId(UUID pubid){
		Connection conn  = openConn();
		JSONArray allsubids = new JSONArray();
		try{
			String query = "SELECT `subid` from `subscriptions` where `src_pubid`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, pubid.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				allsubids.add(rs.getString("subid"));

			//now check if any of the wildcard subscriptions match the path of the publisher with
			//the given pubid
			query = "SELECT `subid`, `wildcardPath` from `subscriptions` where `src_pubid`=?";
			ps = conn.prepareStatement(query);
			ps.setString(1, "wc");
			rs = ps.executeQuery();
			while(rs.next()){
				String subidStr = rs.getString("subid");
				String wcpath = rs.getString("wildcardPath");
				JSONArray pubidsArray = getMatchingStreamIds(wcpath);
				if(pubidsArray.contains(pubid.toString()))
					allsubids.add(subidStr);
			}

			//finally, check if there are any subscriptions to this publisher through symbolic links
			//get the uri associated with this publisher
			query = "select `report_uri` form `publishers` where pubid=?";

			//get any symbolic link uris that point to this publisher

			//for each symbolic link, see if there are any subscriptions to the symlink and add it to the return set
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		return allsubids;
	}

	public String getSubSourcePubId(UUID subid){
		Connection conn = openConn();
		String sourcePubid = null;
		try {
			String query = "SELECT `src_pubid` FROM `subscriptions` where `subid`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				sourcePubid = rs.getString("src_pubid");
		} catch(Exception e){
			
		}
		finally {
			closeConn(conn);
		}
		return sourcePubid;
	}

	public String getSubSourceWildcardPath(UUID subid){
		Connection conn = openConn();
		String sourcePubid = null;
		try {
			String query = "SELECT `wildcardPath` FROM `subscriptions` where `subid`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, subid.toString());
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				sourcePubid = rs.getString("wildcardPath");
		} catch(Exception e){
			
		}
		finally {
			closeConn(conn);
		}
		return sourcePubid;
	}

	/** Structural Queries **/
	public JSONArray resolveStarredUri(String uri, String type){
		JSONArray uris = new JSONArray();
		String queryUri = uri;

		Connection conn  = openConn();
		try {
			if(uri.contains("*")){
				boolean typeset = false;
				uri = uri.replaceAll("\\*", "\\%");
				logger.info("New uri: " + uri);
				String query = "SELECT `path` from `rest_resources` where `path` like ?";
				if(type != null && type.length()>0){
					query = query + " AND type=?";
					typeset = true;
				}

				//make sure we recursively search down subtree rooted at the symlink
				if(typeset && !type.equalsIgnoreCase("symlink") && uri.endsWith("%") )
					query = query + " or type=\"symlink\"";

				logger.fine("QUERY: " + query);
				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1, uri);
				if(typeset)
					ps.setString(2, type);
				ResultSet rs = ps.executeQuery();
				while(rs.next())
					uris.add(rs.getString("path"));
				
				//see if there's any symlinks that match, add them, remove duplicates, return
				uris.addAll(symlinkMatch(queryUri));
				//symlinkMatch(queryUri);
				JSONObject ht= new JSONObject();
				for(int i=0; i<uris.size(); i++)
					ht.put((String)uris.get(i), "");
				uris.clear();
				Iterator keys = ht.keys();
				StringBuffer debugBuf = new StringBuffer();
				while(keys.hasNext()){
					String thisKey = (String)keys.next();
					uris.add(thisKey);
					debugBuf.append(thisKey + ", ");
				}
				logger.fine("Resolution: " + queryUri + " resolved to " + debugBuf.toString());
			} else {
				uris.add(uri);
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return uris;
	}

	private JSONArray symlinkMatch(String uri){
		logger.fine("uri param: " + uri);
		JSONArray paths = new JSONArray();
		JSONArray matches = new JSONArray();
		Connection conn = openConn();
		try{
			if(uri.contains("*") ){//&& !uri.endsWith("*")){
				String query = "SELECT `path` from `rest_resources` where `type`=\"symlink\"";
				logger.fine("QUERY: " + query);
				PreparedStatement ps = conn.prepareStatement(query);
				ResultSet rs = ps.executeQuery();
				StringBuffer debugBuf = new StringBuffer();
				while(rs.next()){
					paths.add(rs.getString("path"));
					debugBuf.append(rs.getString("path") + ", ");
				}
				logger.fine("QSTRING: " + uri + "\nS_PATHS: " + debugBuf.toString());

				for(int i=0; i<paths.size(); i++){
					String thisPath = (String) paths.get(i);

					String queryStr = uri;
					logger.fine("Tokenizing : " + uri + " by * delimiter");
					StringTokenizer tokenizer = new StringTokenizer(queryStr, "*");
					Vector<String> tokenVec = new Vector<String>();
					while(tokenizer.hasMoreTokens()){
						tokenVec.addElement(tokenizer.nextToken());
						logger.fine("Element added: " + (String) tokenVec.get(tokenVec.size()-1));
					}

					//0. remove anything after the last element in this path name in the uri from the query string
					logger.fine("Tokenizing : " + thisPath + " by / delimiter");
					StringTokenizer pathEltsTokenizer = new StringTokenizer(thisPath, "/");
					Vector<String> pathEltsVec = new Vector<String>();
					while(pathEltsTokenizer.hasMoreTokens())
						pathEltsVec.addElement(pathEltsTokenizer.nextToken());
					String lastElt="";
					if(pathEltsVec.size()>0)
						lastElt = (String) pathEltsVec.get(pathEltsVec.size()-1);
					logger.fine("LastElt: " + lastElt);
				
					String lastQueryToken = (String) tokenVec.get(tokenVec.size()-1);
					if(lastQueryToken.contains(lastElt)){	
						lastQueryToken = lastQueryToken.substring(0, lastQueryToken.indexOf(lastElt)+lastElt.length());
						tokenVec.remove(tokenVec.size()-1);
						tokenVec.addElement(lastQueryToken);

						//DEBUG///
						StringBuffer buf = new StringBuffer();
						for(int c=0; c<tokenVec.size(); c++)
							buf.append((String)tokenVec.get(c) + ", ");
						logger.fine("tokenVec: " + buf.toString());
						//DEBUG///

						//1.  check that this path has all tokens
						Vector<Integer> tokenPosVec = new Vector<Integer>();
						boolean hasAllTokens = true;
						int j=0;
						while (j<tokenVec.size() && hasAllTokens){
							String thisToken = tokenVec.get(j);
							if(thisPath.contains(thisToken)){
								tokenPosVec.add(new Integer(thisPath.indexOf(thisToken)));
								logger.fine("TOKEN_SEARCH, found " + thisToken + " at " + thisPath.indexOf(thisToken) +
										" in " + thisPath);
							} else {
								hasAllTokens = false;
								logger.fine("TOKEN_SEARCH, NOT found " + thisToken + " in " + thisPath);
							}
							j+=1;
						}

						//2.  Check that all tokens are in correct order
						if(hasAllTokens && tokenVec.size()>0){
							boolean inOrder = true;
							int k=1;
							int lastVal=((Integer)tokenPosVec.get(0)).intValue();
							if(tokenPosVec.size()>1){
								while(k<tokenPosVec.size() && inOrder){
									if(tokenPosVec.get(k) < lastVal)
										inOrder=false;
									k+=1;
								}
							}

							logger.fine("TOKEN_CHECK, order ok? " + inOrder);

							//3. save the uri if it passes
							if(inOrder)
								matches.add(thisPath);

							logger.info(thisPath + " matches " +  uri  + " query");
						}
					}
				}
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
		return matches;
	}

	/*************************/
	/** Symlink Queries **/
	/*************************/

	public void insertNewSymlinkEntry(String uri, String uriOrIs4Url){

		Connection conn  = openConn();
		try{
			String query = "INSERT INTO `symlinks` (`symlink_uri`, `links_to`) values (?, ?)";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, uri);
			ps.setString(2, uriOrIs4Url);
			ps.executeUpdate();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
	}

	public void removeSymlinkEntry(String uri){

		Connection conn  = openConn();
		try{
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			String query = "DELETE FROM `symlinks` where `symlink_uri`=? or `symlink_uri`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, uri);
			ps.setString(2, uri2);
			ps.executeUpdate();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
	}

	public boolean isSymlink(String uri){
		boolean isSymlink = false;

		Connection conn  = openConn();
		try{
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			String query = "SELECT `symlink_uri` FROM `symlinks` where `symlink_uri`=? or `symlink_uri`=?";
			logger.info("Checking if " + uri + " or " + uri2 + " is a symlink");
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, uri);
			ps.setString(2, uri2);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				isSymlink = true;
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		return isSymlink;
	}

	public String getSymlinkAlias(String uri){
		String alias = "";

		Connection conn  = openConn();
		try{
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			String query = "SELECT `links_to` FROM `symlinks` where `symlink_uri`=? or `symlink_uri`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, uri);
			ps.setString(2, uri2);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				alias = alias.concat(rs.getString("links_to"));
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		finally {
			closeConn(conn);
		}
		return alias;
	}

	public JSONArray getAllSymlinks(){
		JSONArray symlinkUris = new JSONArray();
		Connection conn = openConn();
		try{
			String query = "SELECT `symlink_uri` FROM `symlinks`";
			PreparedStatement ps = conn.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				symlinkUris.add(rs.getString("symlink_uri"));

		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
		
		return symlinkUris;
	}

	public JSONArray getAllExternalLinks(){
		JSONArray linksToUrls = new JSONArray();
		Connection conn = openConn();
		try{
			String query = "SELECT `links_to` FROM `symlinks` where `links_to` like \"http://%\"" ;
			PreparedStatement ps = conn.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				linksToUrls.add(rs.getString("links_to"));

		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
		
		return linksToUrls;
	}

	/****************************/
	/** Data timestamp queries **/
	/****************************/

	public void updateLastRecvTs(String uri, long timestamp){
		Connection conn = null;
		try{
			String query = "UPDATE `publishers` set `last_recv_time`=? where `report_uri`=? or `report_uri`=?";
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			conn = openConn();
			PreparedStatement ps = conn.prepareStatement(query);
			logger.info("QUERY: " + query + " 1:" +timestamp+ " 2:" + uri + " 3:" + uri2);
			ps.setLong(1, timestamp);
			ps.setString(2, uri);
			ps.setString(3, uri2);
			ps.executeUpdate();
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
	}

	public long getLastRecvTs(String uri){
		Connection conn = null;
		long ts = 0;
		try{
			String query = "SELECT `last_recv_time` from `publishers` where `report_uri`=? or `report_uri`=?";
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			conn = openConn();
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, uri);
			ps.setString(2, uri2);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				ts = rs.getLong("last_recv_time");
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}

		finally {
			closeConn(conn);
		}
		return ts;
	}

	/************************/
	/** Properties queries **/
	/************************/

	public void updateLastPropsTs(String uri, long timestamp){
		Connection conn = null;
		try{
			String query = "UPDATE `rest_resources` set `last_props_update_time`=? where `path`=? or `path`=?";
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			conn = openConn();
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setLong(1, timestamp);
			ps.setString(2, uri);
			ps.setString(3, uri2);
			ps.executeUpdate();
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
	}

	public long getLastPropsTs(String uri){
		long ts = 0;
		Connection conn = null;
		try {
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			conn = openConn();
			String query = "SELECT `last_props_update_time` FROM `rest_resources` where `path`=? or `path`=?";
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, uri);
			ps.setString(2, uri2);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				ts = rs.getLong("last_props_update_time");

		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		
		finally {
			closeConn(conn);
		}
		return ts;
	}


	/*******************************/
	/** Properties buffer queries **/
	/*******************************/

	/*
	public boolean hasPropertiesBuffered(String uri){
		boolean hasProps = false;
		try{
			String query = "SELECT `id` from `last_props_buffer` where `uri`=?";
			logger.info("QUERY: " + query);
			Connection conn = openConn();
			PreparedStatement ps = conn.preparedStatement(query);
			ps.setString(1,uri);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				hasProps = true;
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return hasProps;
	}

	public void insertPropertiesIntoBuffer(String uri, JSONObject props){
		try{
			String query = "INSERT into `last_props_buffer` (`uri`, `properties`) values (?, ?)";
			logger.info("QUERY: " + query);
			Connection conn = openConn();
			PreparedStatement ps = conn.preparedStatement(query);
			ps.setString(1,uri);
			SerialBlob thisBlob = new SerialBlob(props.toString().getBytes());
			ps.setBlob(2, thisBlob);
			ps.executeUpdate();
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public void updatePropertiesInBuffer(String uri, JSONObject props){
		try{
			String query = "UPDATE `last_props_buffer` set `properties`=? where `uri`=?";
			logger.info("QUERY: " + query);
			Connection conn = openConn();
			PreparedStatement ps = conn.preparedStatement(query);
			SerialBlob thisBlob = new SerialBlob(props.toString().getBytes());
			ps.setBlob(1, thisBlob);
			ps.setString(2,uri);
			ps.executeUpdate();
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}*/

	/*************************/
	/** Data buffer Queries **/
	/*************************/
	/*
	public boolean hasDataBuffered(String uri){
		boolean hasData = false;
		try{
			String query = "SELECT `id` from `last_data_buffer` where `uri`=?";
			logger.info("QUERY: " + query);
			Connection conn = openConn();
			PreparedStatement ps = conn.preparedStatement(query);
			ps.setString(1,uri);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				hasData = true;
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return hasData;
	}

	public void insertIntoDataBuffer(String uri, JSONObject data){
		try{
			String query = "INSERT into `last_data_buffer` (`uri`, `data`) values (?, ?)";
			logger.info("QUERY: " + query);
			Connection conn = openConn();
			PreparedStatement ps = conn.preparedStatement(query);
			ps.setString(1,uri);
			SerialBlob thisBlob = new SerialBlob(data.toString().getBytes());
			ps.setBlob(2, thisBlob);
			ps.executeUpdate();
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public void updateDataBuffer(String uri, JSONObject data){
		try{
			String query = "UPDATE `last_data_buffer` set `data`=? where `uri`=?";
			logger.info("QUERY: " + query);
			Connection conn = openConn();
			PreparedStatement ps = conn.preparedStatement(query);
			SerialBlob thisBlob = new SerialBlob(data.toString().getBytes());
			ps.setBlob(1, thisBlob);
			ps.setString(2,uri);
			ps.executeUpdate();
			closeConn(conn);
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}*/
	
	
		/************************/
		/** Model queries **/
		/************************/

		public void updateLastModelTs(String uri, long timestamp){
			Connection conn = null;
			try{
				String query = "UPDATE `rest_resources` set `last_model_update_time`=? where `path`=? or `path`=?";
				String uri2 = null;
				if(uri.endsWith("/"))
					uri2 = uri.substring(0, uri.length()-1);
				else
					uri2 = uri + "/";
				conn = openConn();
				PreparedStatement ps = conn.prepareStatement(query);
				ps.setLong(1, timestamp);
				ps.setString(2, uri);
				ps.setString(3, uri2);
				ps.executeUpdate();
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
			}
			closeConn(conn);
		}

		public long getLastModelTs(String uri){
			long ts = 0;
			Connection conn = null;
			try {
				String uri2 = null;
				if(uri.endsWith("/"))
					uri2 = uri.substring(0, uri.length()-1);
				else
					uri2 = uri + "/";
				conn = openConn();
				String query = "SELECT `last_model_update_time` FROM `rest_resources` where `path`=? or `path`=?";
				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1, uri);
				ps.setString(2, uri2);
				ResultSet rs = ps.executeQuery();
				if(rs.next())
					ts = rs.getLong("last_model_update_time");

			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
			}

			finally {
				closeConn(conn);
			}
			return ts;
		}
	


	/************************************************************************************/

	public String getPid(String deviceName) {
		String pid  = null;
		Connection conn = openConn();
		try {
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery ( "SELECT id FROM `object_stream` WHERE `device_name`=\"" + deviceName + "\"");
			if(rs.next())
				pid = rs.getString("id");
			
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while fetching regId", e);
		}
		finally {
			closeConn(conn);
		}
		return pid;
	}

	public boolean inFormatting(String pid) {

		Connection conn = openConn();
		boolean found = false;
		try{
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery ( "SELECT * FROM `Formatting` WHERE `id`=\"" + pid + "\"");
			String name =null;
			if(rs.next())
				found=true;
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
		finally {
			closeConn(conn);
		}
		return found;
	}

	public String getName(String pid){

		Connection conn = openConn();
		String name =null;
		try {
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery ( "SELECT device_name FROM `object_stream` WHERE `id`=\"" + pid + "\"");
			if(rs.next())
				name = rs.getString("device_name");
		
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while fetching pud id", e);
		}
		finally {
			closeConn(conn);
		}
		return name;
	}

	public JSONObject getMetadata(String id) {

		JSONObject meta = new JSONObject();
		JSONObject os = new JSONObject();
		JSONObject cs = new JSONObject();
		JSONObject ls = new JSONObject();

		Connection conn = openConn();
		try {
			
			Statement s = conn.createStatement ();
			ResultSet rs = s.executeQuery ( "SELECT * FROM `object_stream` WHERE `id`=\"" + id + "\"");
			while(rs.next()){
				os.put("id", id);
				os.put("device_name", rs.getString("device_name"));
				os.put("make", rs.getString("make"));
				os.put("model", rs.getString("model"));
				os.put("desc", rs.getString("desc"));
				os.put("address", rs.getString("address"));
				os.put("sensors", rs.getString("sensors"));
				os.put("timestamp", rs.getString("timestamp"));
			}

			s = conn.createStatement ();
			rs = s.executeQuery ( "SELECT * FROM `context_stream` WHERE `id`=\"" + id + "\"");
			while(rs.next()){
				cs.put("id", id);
				cs.put("context_desc", rs.getString("context_desc"));
				cs.put("timestamp", rs.getString("timestamp"));
			}

			s = conn.createStatement ();
			rs = s.executeQuery ( "SELECT * FROM `logic_stream` WHERE `id`=\"" + id + "\"");
			while (rs.next()){
				ls.put("id", id);
				ls.put("functions", rs.getString("functions"));
				ls.put("dataschema", rs.getString("dataschema"));
				ls.put("timestamp", rs.getString("timestamp"));
			}


			meta.put("object_stream", os);
			meta.put("context_stream", cs);
			meta.put("logic_stream", ls);

			return meta;
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while fetching metadata", e);
		}

		finally {
			closeConn(conn);
		}
		return new JSONObject();
	}

	/* There are different types of queries that you can run so this might/should expand*/
	public JSONObject query(String query) {
		return null;
	}

	public boolean createView(/*query or rows or something*/) {
		return false;
	}

	//CouchDBDriver -- private methods
	private boolean createObjectStreamRepos(){
		/*if(dbSession != null && (objectStreamDB=dbSession.createDatabase(OSTREAM_REPOS)) == null)
			return false;*/
		return true;
	}

	private boolean createContextStreamRepos(){
		/*if(dbSession != null && (contextStreamDB=dbSession.createDatabase(CSTREAM_REPOS)) == null)
			return false;*/
		return true;
	}

	private boolean createLogicStreamRepos(){
		/*if(dbSession != null && (logicStreamDB=dbSession.createDatabase(LSTREAM_REPOS)) == null)
			return false;*/
		return true;
	}

	private boolean createDataStreamRepos(){
		/*if(dbSession != null && (dataStreamDB=dbSession.createDatabase(DSTREAM_REPOS)) == null)
			return false;*/
		return true;
	}
}
