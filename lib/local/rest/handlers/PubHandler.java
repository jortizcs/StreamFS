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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import is4.*;
import local.rest.resources.*;
import local.db.*;
import local.metadata.*;
import net.sf.json.*;
import com.sun.net.httpserver.*;

import javax.naming.InvalidNameException;

//public class PubHandler extends Filter implements HttpHandler {
public class PubHandler extends Resource {
	protected static Logger logger = Logger.getLogger(PubHandler.class.getPackage().getName());

	//This addresses the HttpContext switch bug
	//For every call to create a context in the httpServer after the root, the HttpContext object changes, and the filter
	//is no longer used in the new object.
	//protected HttpContext thisContext = null;
	//protected static String URI = null;

	public PubHandler(String uri) throws Exception, InvalidNameException{
		super(uri);
		//URI = uri;
	}

	public String description(){
		return "PubHandler filter";		
	}

	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		logger.info("put");
		post(exchange, data, internalCall, internalResp);
	}

	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		logger.info("Handling post to " + URI);
			
		//check that the id is registered
		Registrar registrar = Registrar.registrarInstance();

		//Parse the json request
		JSONObject jsonObj = (JSONObject) JSONSerializer.toJSON(data);
		
		if(jsonObj!=null) {

			//if pubid provided as url paramter, override the pubid in the jsonobject request
			String pubidParam = (String) exchange.getAttribute("pubid");
			if(pubidParam != null){
				try {
					UUID u = UUID.fromString(pubidParam);
					exchange.setAttribute("pubid", null);
					jsonObj.put("PubId", pubidParam);
				} catch(IllegalArgumentException e){}
			}
			////////////////////////////////////////////////////////////////////////

			String regId = jsonObj.getString("PubId");
			boolean bound = true;
			boolean registered = true;
			if((registered=registrar.isRegisteredId(regId))){ //&& (bound=MetadataMngr.getInstance().isBound(regId))){
				logger.info("PubHandler: Valid PubId: " + regId);

				//add timestamp and store in database
				Date date = new Date();
				long timestamp = date.getTime()/1000;
				jsonObj.put("timestamp", timestamp);

				//update Metadata lease
				MetadataMngr.getInstance().touch(regId);

				//Forward to subscribers
				SubMngr submngr = SubMngr.getSubMngrInstance();
				submngr.dataReceived(jsonObj);

				//put in database
				Is4Database dbLayer = (Is4Database) new DBAbstractionLayer();
				if(DBAbstractionLayer.DBTYPE == DBAbstractionLayer.MYSQL && !tagEntry(exchange, jsonObj)){
					JSONArray errors = new JSONArray();
					errors.add("Unknown request data type");

					//sendPubFailReply(exchange, regId, errors);
					JSONObject sendFail = new JSONObject();
					sendFail.put("operation", "pub");
					sendFail.put("status", "failed");
					sendFail.put("ident",regId);
					sendFail.put("errors", errors);
					sendResponse(exchange, 202, sendFail.toString(), internalCall, internalResp);

					return;
				} else {
					dbLayer.putEntry(jsonObj);
				}

				//send ack success
				//sendPubSuccessReply(exchange,regId);
				JSONObject sendSuccessDoc = new JSONObject();
				sendSuccessDoc.put("operation", "pub");
				sendSuccessDoc.put("status", "success");
				sendSuccessDoc.put("ident",regId);
				sendResponse(exchange, 202, sendSuccessDoc.toString(), internalCall, internalResp);
				return;
			} else{
				//System.out.println("pub FAILED");
				JSONArray errors = new JSONArray();
				if(!registered) {
					String msg = "Publisher id " + regId + " is not registered; dropping data packet";
					logger.warning(msg);
					errors.add(msg);
				}
				if(!bound){
					String msg = "Publisher id " + regId + " is not bound; dropping data packet";
					logger.warning(msg);
					errors.add(msg);
				} 
				
				//sendPubFailReply(exchange,regId, errors);
				JSONObject sendFail = new JSONObject();
				sendFail.put("operation", "pub");
				sendFail.put("status", "failed");
				sendFail.put("ident",regId);
				sendFail.put("errors", errors);
				sendResponse(exchange, 202, sendFail.toString(), internalCall, internalResp);
			}
			//exchange.sendResponseHeaders(202, 0);
		}
		else {
			/*OutputStream responseBody = exchange.getResponseBody();
			exchange.sendResponseHeaders(400, 0);
			responseBody.write((new String("JSON Syntax Error")).getBytes());
			responseBody.close();*/
			sendResponse(exchange, 200, "JSON Syntac Error", internalCall, internalResp);
		}
	}
	//public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp);

	/*public void doFilter(HttpExchange exchange, Filter.Chain chain) throws IOException {
		boolean paramsOk = false;
		if((paramsOk = parseParams(exchange)) && chain==null)
			this.handle(exchange);
		else if (!paramsOk) 
			sendResponse(exchange, 404, null);
		else
			chain.doFilter(exchange);
	}

	

	protected boolean parseParams(HttpExchange exchange) {
		logger.info("Request URI: " + exchange.getRequestURI().toString());
		StringTokenizer tokenizer = new StringTokenizer(exchange.getRequestURI().toString(), "?");
		if(tokenizer != null && tokenizer.hasMoreTokens()){
			String thisResourcePath = tokenizer.nextToken();
			if(URI == null && !thisResourcePath.equals(URI) && !thisResourcePath.equals(URI + "/"))
				return false;
			if(tokenizer.countTokens()>0) {
				StringTokenizer paramStrTokenizer = new StringTokenizer(tokenizer.nextToken(), "&");
				if(paramStrTokenizer !=null && paramStrTokenizer.hasMoreTokens()){
					while (paramStrTokenizer.hasMoreTokens()){
						StringTokenizer paramPairsTokenizer = new StringTokenizer(paramStrTokenizer.nextToken(),"=");
						if(paramPairsTokenizer != null && paramPairsTokenizer.hasMoreTokens()){
							String attr = paramPairsTokenizer.nextToken();
							String val = paramPairsTokenizer.nextToken();
							exchange.setAttribute(attr, val);
							logger.info("Added (" + attr + ", " + val + ") pair to exchange session");
						}
					}
				}
			} else{
				logger.fine("Not enough tokens");
			}
		}
		return true;
	}

	public void handle(HttpExchange exchange) throws IOException{
		try {
			//This addresses the HttpContext switch bug in the library
			//The filter must be called BEFORE the handler
			if (exchange.getHttpContext() != thisContext && exchange.getHttpContext().getFilters().size()==0) {
				thisContext = exchange.getHttpContext();
				thisContext.getFilters().add(this);
				this.doFilter(exchange, null);
			}
		} catch (IOException e){
			logger.log(Level.WARNING, "Could not carry out the Filter operation", e);
		}

		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			//System.out.println("Pub: GET heard; Nothing to get from Pub resource");
			logger.finer("GET Request received");
		  } 
		else if (requestMethod.equalsIgnoreCase("POST") 
				  || requestMethod.equalsIgnoreCase("PUT")){
			logger.info("Handling post to " + URI);
			

			//check that the id is registered
			Registrar registrar = Registrar.registrarInstance();

			//Print out the request body
			BufferedReader is = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
			String line="";
			StringBuffer bodyBuf = new StringBuffer();
			while((line=is.readLine())!=null){
				System.out.println(line);
				bodyBuf.append(line).append(" ");
			}

			//Parse the json request
			JSONObject jsonObj = (JSONObject) JSONSerializer.toJSON(bodyBuf.toString());
			
			if(jsonObj!=null) {

				//if pubid provided as url paramter, override the pubid in the jsonobject request
				String pubidParam = (String) exchange.getAttribute("pubid");
				if(pubidParam != null){
					try {
						UUID u = UUID.fromString(pubidParam);
						exchange.setAttribute("pubid", null);
						jsonObj.put("PubId", pubidParam);
					} catch(IllegalArgumentException e){}
				}
				////////////////////////////////////////////////////////////////////////

				String regId = jsonObj.getString("PubId");
				boolean bound = true;
				boolean registered = true;
				if((registered=registrar.isRegisteredId(regId))){ //&& (bound=MetadataMngr.getInstance().isBound(regId))){
					

					logger.info("PubHandler: Valid PubId: " + regId);

					//add timestamp and store in database
					Date date = new Date();
					long timestamp = date.getTime()/1000;
					jsonObj.put("timestamp", timestamp);

					//update Metadata lease
					MetadataMngr.getInstance().touch(regId);

					//Forward to subscribers
					SubMngr submngr = SubMngr.getSubMngrInstance();
					submngr.dataReceived(jsonObj);

					//put in database
					Is4Database dbLayer = (Is4Database) new DBAbstractionLayer();
					if(DBAbstractionLayer.DBTYPE == DBAbstractionLayer.MYSQL && !tagEntry(exchange, jsonObj)){
						JSONArray errors = new JSONArray();
						errors.add("Unknown request data type");
						sendPubFailReply(exchange, regId, errors);
						return;
					} else {
						dbLayer.putEntry(jsonObj);
					}

					//send ack success
					sendPubSuccessReply(exchange,regId);
					return;
				} else{
					//System.out.println("pub FAILED");
					JSONArray errors = new JSONArray();
					if(!registered) {
						String msg = "Publisher id " + regId + " is not registered; dropping data packet";
						logger.warning(msg);
						errors.add(msg);
					}
					if(!bound){
						String msg = "Publisher id " + regId + " is not bound; dropping data packet";
						logger.warning(msg);
						errors.add(msg);
					} 
					
					sendPubFailReply(exchange,regId, errors);
				}
				//exchange.sendResponseHeaders(202, 0);
			}
			else {
				OutputStream responseBody = exchange.getResponseBody();
				//error parsing json request
				exchange.sendResponseHeaders(400, 0);
				//response should be a JSON object that describes the error
				responseBody.write((new String("JSON Syntax Error")).getBytes());
				responseBody.close();
			}

		  }
	}*/

	private boolean tagEntry(HttpExchange exchange, JSONObject jsonObj){
		logger.info("Tagging the entry");
		String schemaName = (String) exchange.getAttribute("schema");
		if(schemaName != null && MySqlDriver.isValidSchema(schemaName)){
			logger.info("Schema-name: " + schemaName);
			if(schemaName.equalsIgnoreCase("context")){
				jsonObj.put("name", "context_stream");
				return true;
			} else if(schemaName.equalsIgnoreCase("logic")){
				jsonObj.put("name", "logic_stream");
				return true;
			} else if(schemaName.equalsIgnoreCase("device")){
				jsonObj.put("name", "device_entry");
				return true;
			} else if(schemaName.equalsIgnoreCase("location")) {
				jsonObj.put("name", "location_entry");
				return true;
			} else if(schemaName.equalsIgnoreCase("meta")) {
				jsonObj.put("name", "meta_entry");
				return true;
			} else if(schemaName.equalsIgnoreCase("reading")) {
				jsonObj.put("name", "meter_reading_entry");
				return true;
			} else if(schemaName.equalsIgnoreCase("report")) {
				jsonObj.put("name", "meter_reading_entry");
				return true;
			}  else if(schemaName.equalsIgnoreCase("profile")){
				jsonObj.put("name", "profile_entry");
				return true;
			} else if(schemaName.equalsIgnoreCase("resource")){
				jsonObj.put("name", "resource_listing_entry");
				return true;
			} else if(schemaName.equalsIgnoreCase("unit")){
				jsonObj.put("name", "unit_labels_entry");
				return true;
			} else if(schemaName.equalsIgnoreCase("formatting")){
				jsonObj.put("name", "formatting_entry");
				return true;
			} else if(schemaName.equalsIgnoreCase("parameter")){
				jsonObj.put("name", "parameter_entry");
				return true;
			}
		} 
		return false;
	}

	protected void sendResponse(HttpExchange exchange, int errorCode, String response){
		try{
			logger.info("Sending Response");
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "application/json");
			exchange.sendResponseHeaders(errorCode, 0);

			OutputStream responseBody = exchange.getResponseBody();
			if(response!=null)
				responseBody.write(response.getBytes());
			responseBody.close();
		}catch(Exception e){
			logger.log(Level.WARNING, "Exception thrown while sending response",e);
		}
	}

	private void sendPubSuccessReply(HttpExchange exchange, String regId){
		logger.info("Sending publish success reply");
		try {
			OutputStream responseBody = exchange.getResponseBody();

			//compose body for response
			JSONObject sendSuccessDoc = new JSONObject();
			sendSuccessDoc.put("operation", "pub");
			sendSuccessDoc.put("status", "success");
			sendSuccessDoc.put("ident",regId);
			String body = sendSuccessDoc.toString();


			//construct response and send	
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(202, 0);	
			responseBody.write(body.getBytes());
			responseBody.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while sending publish-success reply", e);
		}
	}

	private void sendPubFailReply(HttpExchange exchange, String regId, JSONArray errors){
		logger.info("Sending publish fail reply");
		try {
			OutputStream responseBody = exchange.getResponseBody();

			//compose body for response
			JSONObject sendFail = new JSONObject();
			sendFail.put("operation", "pub");
			sendFail.put("status", "failed");
			sendFail.put("ident",regId);
			sendFail.put("errors", errors);
			String body = sendFail.toString();

			//construct response and send	
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(202, 0);	
			responseBody.write(body.getBytes());
			responseBody.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "Error while sending publish-fail reply", e);
		}
	}
}
