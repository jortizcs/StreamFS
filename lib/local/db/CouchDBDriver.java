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

import com.fourspaces.couchdb.*;
import com.fourspaces.couchdb.util.*;

import net.sf.json.*;

import java.util.*;

public class CouchDBDriver implements Is4Database {
	private Session dbSession = null;
	
	//stream repositories
	private Database objectStreamDB = null;
	private Database contextStreamDB = null;
	private Database logicStreamDB = null;
	private Database dataStreamDB = null;

	//repository names
	private static final String OSTREAM_REPOS = "ostream";
	private static final String CSTREAM_REPOS = "cstreams";
	private static final String LSTREAM_REPOS = "lstreams";
	private static final String DSTREAM_REPOS = "dstreams";

	//stream name label
	private static final String OSTREAM_NAME_PROPVAL = "object_stream";
	private static final String CSTREAM_NAME_PROPVAL = "context_stream";
	private static final String LSTREAM_NAME_PROPVAL = "logic_stream";
	private static final String DSTREAM_NAME_PROPVAL = "data_stream";


	public CouchDBDriver(String host, int port) {
		super();
		initDB(host, port, null, null);
		
	}

	public CouchDBDriver(String host, int port, String login, String pw){
		super();
		initDB(host, port, login, pw);
	}

	private void initDB(String host, int port, String login, String pw){
		if(login == null)
			dbSession = new Session(host, port, login, pw, false, false); //last two params are useAuth, secure
		else
			dbSession = new Session(host, port);


		/*Iterator dbNamesI = dbSession.getDatabaseNames().iterator();
		while (dbNamesI.hasNext()){
			System.out.println(dbNamesI.next());
		}*/

		objectStreamDB = dbSession.getDatabase(OSTREAM_REPOS);
		contextStreamDB = dbSession.getDatabase(CSTREAM_REPOS);
		logicStreamDB = dbSession.getDatabase(LSTREAM_REPOS);
		dataStreamDB = dbSession.getDatabase(DSTREAM_REPOS);

		//create the database if it does not exist
		boolean ok = true;
		Vector<String> missing = new Vector<String>();

		if(objectStreamDB == null){
			if(!(ok &= createObjectStreamRepos())){missing.add("object");}
		}
		if(contextStreamDB == null) {
			if(!(ok &= createContextStreamRepos())){missing.add("context");}
		}
		if(logicStreamDB == null){
			if(!(ok &= createLogicStreamRepos())){missing.add("logic");}
		}
		if(dataStreamDB == null){
			if(!(ok &= createDataStreamRepos())){missing.add("data");}
		}

		if(!ok){
			StringBuffer error = new StringBuffer();
			for(int i=0; i<missing.size(); i++){
				error.append(missing.get(i));
				if(i==0 || i<missing.size()-1 && missing.size()>1)
					error.append(", ");
			}
			
			System.out.println("Could not set up DB properly: " + error.toString());
			System.exit(1);
		}
	}

	//inherited methods to be overwritten
	public void putEntry(JSONObject entry) {
		try {
			if (entry == null) 
				System.out.println("Entry is null");

			String namePropVal = entry.getString("name");
			Document entryDoc = new Document(entry);
			if(entryDoc == null)
				System.out.println("Document is null");

			if(namePropVal.equals(OSTREAM_NAME_PROPVAL)){
				objectStreamDB.saveDocument(entryDoc);
			}
			else if(namePropVal.equals(CSTREAM_NAME_PROPVAL)) {
				contextStreamDB.saveDocument(entryDoc);
			}
			else if (namePropVal.equals(LSTREAM_NAME_PROPVAL)) {
				logicStreamDB.saveDocument(entryDoc);
			}
			else if(namePropVal.equals(DSTREAM_NAME_PROPVAL)) {
				dataStreamDB.saveDocument(entryDoc);
			}
		} catch (Exception exception){
			exception.printStackTrace();
			
			//possible entry document into data repository
			//dataStreamDB.saveDocument(entryDoc);
		}
	}

	public JSONObject getEntry(String name) {
		return null;
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
		if(dbSession != null && (objectStreamDB=dbSession.createDatabase(OSTREAM_REPOS)) == null)
			return false;
		return true;
	}

	private boolean createContextStreamRepos(){
		if(dbSession != null && (contextStreamDB=dbSession.createDatabase(CSTREAM_REPOS)) == null)
			return false;
		return true;
	}

	private boolean createLogicStreamRepos(){
		if(dbSession != null && (logicStreamDB=dbSession.createDatabase(LSTREAM_REPOS)) == null)
			return false;
		return true;
	}

	private boolean createDataStreamRepos(){
		if(dbSession != null && (dataStreamDB=dbSession.createDatabase(DSTREAM_REPOS)) == null)
			return false;
		return true;
	}

	public JSONObject getMetadata(String id){
		return new JSONObject();
	}
}
