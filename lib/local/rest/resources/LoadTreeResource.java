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
package local.rest.resources;

import local.db.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*;
import net.sf.json.*;
import java.util.*;

public class LoadTreeResource extends Resource{
	protected static transient Logger logger = Logger.getLogger(LoadTreeResource.class.getPackage().getName());

	//this resource type
	public int thisType = -1;

	//types
	public static final int PANEL_ELEMENT 			= 0;
	public static final int CIRCUIT_ELEMENT 		= 1;
	public static final int CIRCUIT_BREAKER_ELEMENT 	= 2;
	public static final int LOAD_ELEMENT 			= 3;
	public static final int BUS_BAR_ELEMENT 		= 4;
	public static final int TRANSFORMER_ELEMENT		= 5;

	public LoadTreeResource(String path, int type) throws Exception, InvalidNameException {
		super(path);

		//set type
		switch(type){
			case PANEL_ELEMENT:break;
			case CIRCUIT_ELEMENT:break;
			case CIRCUIT_BREAKER_ELEMENT:break;
			case LOAD_ELEMENT:break;
			case BUS_BAR_ELEMENT:break;
			case TRANSFORMER_ELEMENT:break;
			default:
				throw new Exception("Invalid Load Tree type");
		}
		thisType = type;
	}

	public void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		if(URI.endsWith("properties/")){
			JSONObject props = ((MySqlDriver) DBAbstractionLayer.database).rrGetProperties(URI);
			logger.info("PROPS: " + props.toString());
			JSONObject resp = new JSONObject();
			resp.put("status", "success");
			resp.put("properties", props);
			sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
		} else{
			super.get(exchange, internalCall, internalResp);
		}
	}

	/**
	 * Overwrite the properties associated with this resource;
	 */
	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		logger.info("Handling PUT for resource: " + URI);
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
		boolean jsonOk = false;
		try {
			JSONObject properties = (JSONObject) JSONSerializer.toJSON(data);
			jsonOk = true;

			//check with status schema here
			if(!properties.getString("type").equalsIgnoreCase("properties")) {
				errors.add("Type " + properties.getString("type") + " not recognized");	
			}

			if(errors.size()==0){
				resp.put("status", "success");
			} else{
				resp.put("status", "fail");
				resp.put("errors", errors);
			}

			//((MySqlDriver) DBAbstractionLayer.database).rrPutProperties(URI, properties);
			updateProperties(properties);
			logger.info("RESPONSE: " + resp.toString());

			sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
			return;
		} catch(Exception e){
			if(e instanceof JSONException && !jsonOk)
				errors.add("Invalid json");

			if(e instanceof JSONException && jsonOk)
				errors.add("\"type=properties\" missing");

			resp.put("errors", errors);
			logger.info("E_RESPONSE: " + resp.toString());
			sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
		}
	}


	/**
	 *  Update the properties associated with this resource;
	 */
	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		if(exchange.getAttribute("query") != null){
			super.query(exchange, data, internalCall, internalResp);
		} else {
			JSONObject resp = new JSONObject();
			JSONArray errors = new JSONArray();
			boolean jsonOk = false;
			try {
				JSONObject properties = (JSONObject) JSONSerializer.toJSON(data);
				jsonOk = true;

				//check with status schema here
				if(!properties.getString("type").equalsIgnoreCase("properties")) {
					errors.add("Type " + properties.getString("type") + " not recognized");	
				}

				if(errors.size()==0){
					resp.put("status", "success");
				} else{
					resp.put("status", "fail");
					resp.put("errors", errors);
				}

				JSONObject storedProps = ((MySqlDriver) DBAbstractionLayer.database).rrGetProperties(URI);
				if(storedProps != null) {
					boolean dirty = false;
					Set<String> updatePropKeys = properties.keySet();
					Iterator<String> updatePropKeysIter = updatePropKeys.iterator();
					while(updatePropKeysIter.hasNext()){
						String thisKey = updatePropKeysIter.next();
						if(storedProps.containsKey(thisKey)){
							storedProps.put(thisKey, properties.getString(thisKey));
							dirty = true;
						}
					}

					//update the properties associated with this resource
					if(dirty)
						//((MySqlDriver) DBAbstractionLayer.database).rrPutProperties(URI, storedProps);
						updateProperties(storedProps);
				}

				sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
				return;
			} catch(Exception e){
				if(e instanceof JSONException && !jsonOk)
					errors.add("Invalid json");

				if(e instanceof JSONException && jsonOk)
					errors.add("\"type properties\" missing");

				resp.put("errors", errors);
				sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
			}
		}
	}

	/**
	 * Delete this resource;
	 */
	//public void delete(HttpExchange exchange) {
	//}
}
