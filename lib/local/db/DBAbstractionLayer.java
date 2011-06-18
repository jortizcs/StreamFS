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
 * Provides an abstraction layer and interface for database connections.  The storage system
 * makes calls to classes that extend this interface, keeping the system agnostic to the type
 * of database the programmer has chosen.
 *
 */
package local.db;

import net.sf.json.*;

import java.lang.StringBuffer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.logging.Logger;
import java.util.logging.Level;

public class DBAbstractionLayer implements Is4Database {
	private static transient final Logger logger = Logger.getLogger(DBAbstractionLayer.class.getPackage().getName());

	//Configuration file
	private String dbConfigFile = "/project/eecs/tinyos/is4/lib/local/db/db_config/db_info.json";
	private String type = "";
	private String addr = "";
	private int port =-1;
	private String username = "";
	private String password = "";
	private String dbName = "";

	//Supported databases
	public static final int COUCHDB 	= 1;
	public static final int MONGODB 	= 2;
	public static final int MYSQL   	= 3;
	public static final int HADOOP 	= 4;

	private static final String MONGODB_STR = "MongoDB";
	private static final String MYSQL_STR = "MySQL";
	private static final String HADOOP_STR = "Hadoop";

	public static int DBTYPE = 0;

	//Database reference
	public static Is4Database database = null;
	
	public DBAbstractionLayer() {
		if (database == null){
			try{
				String home=null;
				if((home=System.getenv().get("IS4HOME")) != null)
					dbConfigFile = home + "/lib/local/db/db_config/db_info.json";
				logger.info("home: " + System.getenv().get("IS4HOME") + "; config: " + dbConfigFile);
				//home  = "/home/jortiz/Desktop/SMOTE_SVN/is4/trunk";
				//dbConfigFile = home + "/lib/local/db/db_config/db_info.json";
				File configFile = new File(dbConfigFile);
				FileReader cFileReader = new FileReader(configFile);
				BufferedReader bufReader = new BufferedReader(cFileReader);
			
				StringBuffer strBuf = new StringBuffer();
				String line = null;
				while((line=bufReader.readLine())!=null)
					strBuf.append(line).append(" ");
				JSONObject configJsonObj = (JSONObject)JSONSerializer.
									toJSON(strBuf.toString());
				cFileReader.close();
				bufReader.close();
				type = configJsonObj.getString("type");
				addr = configJsonObj.getString("address");
				port = configJsonObj.getInt("port");
				dbName = configJsonObj.getString("dbname");
				username = configJsonObj.optString("login");
				password = configJsonObj.optString("password");

				//Database driver setup
				int dbId = 0;
				if(type.equalsIgnoreCase(MONGODB_STR)) {
					dbId = MONGODB;
				}
				else if(type.equalsIgnoreCase(MYSQL_STR)){
					dbId = MYSQL;
				}
				else if(type.equalsIgnoreCase(HADOOP_STR)){
					dbId = HADOOP;
				}
				else {
					System.out.println("FATAL ERROR: Invalid Database type");
					System.exit(1);
				}

				setupDB(dbId);
			} catch(Exception e){
				e.printStackTrace();
				System.out.println("FATAL ERROR: Error instantiating DBAbstraction Layer");
				System.exit(1);
			}
		}
	}

	private void setupDB(int dbtype){
		boolean setupSuccess = false;
		switch(dbtype){
			case (MYSQL):
				setupMySql();
				setupSuccess = true;
				break;
		}

		if(setupSuccess) {
			DBTYPE = dbtype;
		} else {
			logger.severe("Could not set up the database");
			System.exit(1);
		}
	}

	private void setupMySql(){
		String parseStatus = "Addr: " + addr + "\nPort: " + port + "\nlogin: " + username + "\npw: " + password + " dbName: " + dbName;
		logger.info(parseStatus);
		if(!username.equalsIgnoreCase("")){
			database = (Is4Database) new MySqlDriver(addr, port, username, password, dbName);
		}
		else {
			database = (Is4Database) new MySqlDriver(addr, port);
		}
	}

	//Pass-through call to associated database driver
	public void putEntry(JSONObject entry) {
		database.putEntry(entry);
	}

	public JSONObject getEntry(String name) {
		return database.getEntry(name);
	}

	public JSONObject getMetadata(String id){
		return database.getMetadata(id);
		//return new JSONObject();
	}

	/* There are different types of queries that you can run so this might/should expand*/
	public JSONObject query(String q) {
		return database.query(q);
	}

	public boolean createView(/*query or rows or something*/) {
		//return database.createView();
		return true;
	}
}
