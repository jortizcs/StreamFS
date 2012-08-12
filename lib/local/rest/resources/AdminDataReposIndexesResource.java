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
 * StreamFS release version 2.0
 */

package local.rest.resources;

import is4.*;
import local.db.*;
import local.rest.smap.*;

import net.sf.json.*;
import java.net.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import javax.naming.InvalidNameException;

import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;

public class AdminDataReposIndexesResource extends Resource {
	private static transient final Logger logger = Logger.getLogger(AdminDataReposIndexesResource.class.getPackage().getName());
	private static final String dbname = MongoDBDriver.getDBName();
	private static final String collName = MongoDBDriver.getDataCollName();

	public AdminDataReposIndexesResource() throws Exception, InvalidNameException{
		super("/admin/data/indexes/");
	}

	public void get(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		try {
			resp.put("status", "success");
			resp.put("indexes", mongoDriver.getIndexInfo(collName));
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
	}

	public void put(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
		try {
			JSONObject dataObj = (JSONObject) JSONSerializer.toJSON(data);
			addRemoveIndexes(m_request,dataObj);
			resp.put("status", "success");
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			if(e instanceof JSONException)
				errors.add("Invalid JSON posted");
			resp.put("status","fail");
			resp.put("errors", errors);
		}
		sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
	}

	public void post(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		put(m_request, m_response, path, data, internalCall, internalResp);
	}

	public void delete(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
		sendResponse(m_request, m_response, 504, null, internalCall, internalResp);
	}

	protected void addRemoveIndexes(Request m_request, JSONObject dataObj){
        Query query = m_request.getQuery();
		Iterator keys = query.keySet().iterator();
		JSONObject addIndices = new JSONObject();

		while(keys.hasNext()){
			String thisKey = (String) keys.next();
			logger.fine("Keys found!; thisKey=" + thisKey);
			if(thisKey.startsWith("idx_")){
				String str = "idx_";
				String queryKey = thisKey.substring(thisKey.indexOf(str)+str.length(), thisKey.length());
				String queryValue = (String)query.get(thisKey);
				logger.info("Query Value: " + queryValue);
				if(isNumber(queryValue)){
					int val = Integer.parseInt(queryValue);
					switch(val){
						case -1:
							addIndices.put(queryKey, -1);
							break;
						case 0:
							mongoDriver.removeIndex(collName, queryKey);
							break;
						case 1:
							addIndices.put(queryKey, 1);
							break;
						default:
							break;
					}
				}
			}
		}

		//use the posted data to populate the index objects
		keys = dataObj.keys();
		while(keys.hasNext()){
			String queryKey = (String) keys.next();
			String queryValue = (String)query.get(queryKey);
			logger.info("Query Value: " + queryValue);
			if(isNumber(queryValue)){
				int val = Integer.parseInt(queryValue);
				switch(val){
					case -1:
						addIndices.put(queryKey, -1);
						break;
					case 0:
						mongoDriver.removeIndex(collName, queryKey);
						break;
					case 1:
						addIndices.put(queryKey, 1);
						break;
					default:
						break;
				}
			}
		}

		if(!addIndices.toString().equals("{}")){
			mongoDriver.setIndexes(collName, addIndices);
		}
	}
}
