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
package local.rest.handlers;

import java.lang.*;
import java.util.*;
import java.io.*;

import is4.*;
import local.db.*;
import net.sf.json.*;
import com.sun.net.httpserver.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import local.db.*;

/**
 *  Handles the subscription control resource.  Through this handler a subscriber
 *  can choose which streams to subscribe to and which to unsubscribe to.
 *
 */
public class SubControlHandler implements HttpHandler{
	protected static Logger logger = Logger.getLogger(SubControlHandler.class.getPackage().getName());
	protected static MySqlDriver database = (MySqlDriver) DBAbstractionLayer.database;

	public void SubControlHandler(){}

	public void handle(HttpExchange exchange) throws IOException{
		/*Vector<String> errorVec = new Vector<String>();
		String requestMethod = exchange.getRequestMethod();

		if(requestMethod.equalsIgnoreCase("GET")){
		} else if(requestMethod.equalsIgnoreCase("PUT") || requestMethod.equalsIgnoreCase("POST")){

			//get the json request
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));

			String line="";
			String doc= "";
			while((line=inputReader.readLine())!=null)
				doc += line;

			try{
				JSONObject requestObj = (JSONObject) JSONSerializer.toJSON(doc);

				//check request type and subscriber id
				SubMngr subMngr = SubMngr.getSubMngrInstance();
				String subId = requestObj.getString("SubId");

				//System.out.println("check 1: " + requestObj.getString("name").equalsIgnoreCase("sub_control"));
				//System.out.println("check 2: " + subId.TYPE.toString() + "val=" + subId.longValue());
				//System.out.println("check 3: " + subMngr.isSubscriber(subId.longValue()));

				if(requestObj.getString("name").equalsIgnoreCase("sub_control") && 
						(subId = requestObj.getString("SubId"))!=null && subMngr.isSubscriber(subId)){

					//Subscription request -- pull out streams to subscribe to
					JSONArray subReq = requestObj.getJSONArray("StreamSubIds");

					//Subscription cancellation -- pull out streams to cancel subscription for
					JSONArray subCancel = requestObj.getJSONArray("StreamCancelIds");

					for(int i=0; i<subReq.size(); i++){
						logger.info("Adding stream: " + subReq.get(i));
						String thisPid = (String) subReq.get(i);
						subMngr.subscribeToStream(thisPid, subId);
						database.insertNewSubEntry(UUID.fromString(subId), UUID.fromString(thisPid));
					}

					for(int j=0; j<subCancel.size(); j++){
						logger.info("Removing stream: " + subCancel.get(j));
						String thisPid = (String) subCancel.get(j);
						subMngr.unsubscribeFromStream(thisPid, subId);
						database.removeSubEntry(UUID.fromString(subId), UUID.fromString(thisPid));
					}

					sendResp(exchange, "sub_control", errorVec);
					errorVec.clear();
				}else{
					errorVec.addElement("Invalid Request format or invalid subscriber ID");
					sendResp(exchange, "sub_control", errorVec);
					errorVec.clear();
				}


			}catch(Exception e){
				if(e instanceof JSONException){
					errorVec.addElement("JSON Malformed, StreamSubIds or StreamCancelIds not found or not arrays");
					sendResp(exchange, "sub_control", errorVec);
					errorVec.clear();
				}
			}
		}*/
	}

	public void sendResp(HttpExchange exchange, String op, Vector<String> errorVec){
		try{
			JSONObject respObj = new JSONObject();
			respObj.put("operation", op);

			//success or fail?
			if(errorVec.size()>0) {
				respObj.put("status","fail");
				JSONArray errors = new JSONArray();
				errors.addAll((Collection<String>) errorVec);
				respObj.put("errors", errors);
			}
			else
				respObj.put("status","success");

			//send the message
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, 0);
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(respObj.toString().getBytes());
			responseBody.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
