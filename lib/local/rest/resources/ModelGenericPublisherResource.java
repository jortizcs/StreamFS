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
 * IS4 release version 2.0
 */
package local.rest.resources;

import is4.*;
import local.db.*;
import local.rest.*;
import local.rest.resources.util.*;
import local.metadata.context.*;
import com.mongodb.*;

import net.sf.json.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*;

/**
 *  Resource object for a device.
 */
public class ModelGenericPublisherResource extends GenericPublisherResource {
	protected static transient Logger logger = Logger.getLogger(ModelGenericPublisherResource.class.getPackage().getName());
	
	private ModelResource parent = null;
	private boolean materialize = false;
	
	public ModelGenericPublisherResource(String uri, UUID pubId, ModelResource parentModel,
					boolean createview) throws Exception, InvalidNameException{
		super(uri, pubId);
		if(parentModel != null)
			parent = parentModel;
		else
			throw new Exception("parentModel is null!");
		materialize = createview;

		TYPE = ResourceUtils.MODEL_GENERIC_PUBLISHER_RSRC;
	}

	public synchronized void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		if(materialize){
			if(exchange.getAttribute("query") != null &&
				((String) exchange.getAttribute("query")).equalsIgnoreCase("true")){
				if(!internalCall)
					exchange.setAttribute("query", "false");
				query(exchange, null, internalCall, internalResp);
				return;
			}

			logger.info("GET " + this.URI);
			JSONObject response = new JSONObject();
			try {
				JSONObject properties = database.rrGetProperties(this.URI);
				if(properties == null){
					properties = new JSONObject();
				}
				properties.put("type", "properties");
				response.put("status", "success");
				response.put("ParentModel", parent.URI);

				UUID assocPubid = database.isRRPublisher2(this.URI);
				if(assocPubid != null){
					response.put("pubid", assocPubid.toString());
					logger.info("POPULATING");
					properties.put("PubId", assocPubid.toString());
					properties.put("URI", this.URI);
					String dataStr = "URI: " + this.URI + "\n\nPubId: " + assocPubid.toString();
					properties.put("data", dataStr);

					//get last few values received
					//db.is4_main_coll.find().sort({timestamp:1}).limit(5);
					JSONObject queryJSON = new JSONObject();
					queryJSON.put("PubId", publisherId.toString());
					queryJSON.put("timestamp", new Long(last_data_ts));
					JSONObject sortByJSON = new JSONObject();
					sortByJSON.put("timestamp",1);
					MongoDBDriver mongoDriver = new MongoDBDriver();
					JSONObject lastValuesReceived = mongoDriver.queryWithLimit(
										queryJSON.toString(), 
										sortByJSON.toString(), 
										headCount);
					properties.put("head", lastValuesReceived.toString());
				}
				response.put("properties", properties);
				sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
				return;
			} catch (Exception e){
				logger.log(Level.WARNING, "", e);
			}
			sendResponse(exchange, 200, null, internalCall, internalResp);
		} else {
			JSONObject resp = new JSONObject();
			resp.put("status", "success");
			resp.put("type", ResourceUtils.translateType(TYPE));
			resp.put("pubid", publisherId.toString());
			resp.put("active_view", false);
			sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
		}
	}

	public synchronized void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		post(exchange, data, internalCall, internalResp);
	}

	public synchronized void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
		try{
			JSONObject request = (JSONObject) JSONSerializer.toJSON(data);
			String op = request.optString("operation");
			if(op.equalsIgnoreCase("materialize")){
				boolean val = request.optBoolean("createview", materialize);
				materialize = val;
				resp.put("status", "success");
				resp.put("createview", materialize);
			}else {
				errors.add("Unknown request");
				resp.put("status", "fail");
				resp.put("errors",errors);
			}
			
		}catch(Exception e){
			logger.log(Level.WARNING, "",e);
			
			if(e instanceof JSONException)
				errors.add("Invalid JSON request");
			resp.put("status", "fail");
			resp.put("errors",errors);
		}
		sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
	}
	
	public void saveData(JSONObject data){
		logger.info(URI + " saving data: " + data.toString());
		if(materialize){
			super.handleIncomingData(data, true);
		} else {
			//Forward to subscribers
			JSONObject dataCopy = (JSONObject)JSONSerializer.toJSON(data);
			dataCopy.put("pubid", publisherId.toString());
			dataCopy.put("is4_uri", this.URI.toString());
			SubMngr submngr = SubMngr.getSubMngrInstance();
			logger.info(URI + "; SubMngr Copy: " + dataCopy.toString());
			submngr.dataReceived(dataCopy);
		}
	}

	public void delete(HttpExchange exchange,boolean internalCall, JSONObject internalResp){
		super.delete(exchange, internalCall, internalResp);
		
		//kill the associated thread (remove associated pipe)
		SubMngr submngr = SubMngr.getSubMngrInstance();
		submngr.signalModelThreadKill(this);

		//delete all subscriptions where the associate model thread was the publisher
		JSONArray subs = database.getAllAssociatedSubUris(URI);
		for(int i=0; i<subs.size(); i++){
			Resource r = RESTServer.getResource((String)subs.get(i));
			if(r instanceof SubscriptionResource){
				//remove the subscription
				database.removeSubEntry(((SubscriptionResource)r).getSubId());

				//delete rest_resource entry
				database.removeRestResource(r.getURI());

				//remove the resource from the rest server
				RESTServer.removeResource(r);

				//remove from internal graph
				this.metadataGraph.removeNode(r.getURI());
			}
		}
	}

}
