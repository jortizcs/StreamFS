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
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.sf.json.*;

import com.sun.net.httpserver.*;

import is4.*;
import is4.exceptions.*;
import local.html.tags.*;
import local.json.validator.*;
import local.db.*;
import local.metadata.MetadataMngr;

public class JoinHandler extends Filter implements HttpHandler {
	private Vector<String> errorVec = new Vector<String>();
	private Registrar registrar = Registrar.registrarInstance();
	protected static Logger logger = Logger.getLogger(JoinHandler.class.getPackage().getName());

	//This addresses the HttpContext switch bug
	//For every call to create a context in the httpServer after the root, the HttpContext object changes, and the filter
	//is no longer used in the new object.
	protected HttpContext thisContext = null;

	protected static String URI = null;

	public JoinHandler(String uri){
		URI = uri;
	}

	public void doFilter(HttpExchange exchange, Filter.Chain chain) throws IOException {
		logger.info("here");
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

	private boolean filterCheck(HttpExchange exchange){
		logger.info("here");
		try {
			//This addresses the HttpContext switch bug in the library
			//The filter must be called BEFORE the handler
			if (exchange.getHttpContext() != thisContext && exchange.getHttpContext().getFilters().size()==0) {
				thisContext = exchange.getHttpContext();
				thisContext.getFilters().add(this);
				this.doFilter(exchange, null);
				return true;
			}
		} catch (IOException e){
			logger.log(Level.WARNING, "Could not carry out the Filter operation", e);
			return false;
		}
		return false;
	}

	public String description(){
		return "JoinHandler " + URI + " filter";
	}

	public void handle(HttpExchange exchange) throws IOException{
		logger.info("handler:exchange handler: " + exchange.getLocalAddress().getHostName() + ":" + exchange.getLocalAddress().getPort() + "->" + 
						exchange.getRemoteAddress());
		//check if the filter was hit up
		if(filterCheck(exchange))
			return;
		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			// TODO:  page should be fetched and served from a file on the local host.
		
			OutputStream responseBody = exchange.getResponseBody();
			HTMLSimpleTags response =  new HTMLSimpleTags();
			response.setTitle("Join Resource");

			//compose body for response
			String body = "Join resource<br>";
			String joinSchemaUrl="http://www.eecs.berkeley.edu/~jortiz/";
			joinSchemaUrl = joinSchemaUrl + "gridos/site/schemas/protocols/join_schema.json";
			body = body + "Schema: <a href=\"" + joinSchemaUrl + "\">Join Schema</a><br>";
			body = body + "Instructions: <a href=\"http://www.eecs.berkeley.edu/~jortiz/gridos/site\">";
			body = body + "here</a><br>";
	
			//construct response and send	
			System.out.println("Join: GET heard something and responded");
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/html");
			exchange.sendResponseHeaders(200, 0);
			responseBody.write(response.toString().getBytes());
			responseBody.close();
			return;
				
		  } else if (requestMethod.equalsIgnoreCase("POST") ||
				  requestMethod.equalsIgnoreCase("PUT")){
			OutputStream responseBody = exchange.getResponseBody();
		
			//Print out the request body
			BufferedReader is = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
			String line="";
			StringBuffer bodyBuf = new StringBuffer();
			while((line=is.readLine())!=null){
				bodyBuf.append(line).append(" ");
			}
		
			//Parse the json request
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			JSONObject jsonObj= getJSON(bodyBuf.toString());
			String regId=processJoin(exchange, jsonObj);
			if(jsonObj!=null) {
				if(regId!=null && !regId.equals("0")){
					logger.info("Join Success");
					sendJoinSuccessReply(exchange,regId);
				} else{
					logger.info("Join Fail");
					sendJoinErrorReply(exchange,"0",200);
				}
				return;
			}
			else {
				//error parsing json request
				exchange.sendResponseHeaders(400, 0);

				//response should be a JSON object that describes the error
				responseBody.write((new String("JSON Syntex Error")).getBytes());
			}

			//responseBody.close();
			
		  } else {
			//System.out.println("heard something");
			logger.warning("JoinHandler:  Heard invalid request type");
		  }
	}

	public JSONObject getJSON(String jsonPiece) {
		try {
			JSONObject jsonObj = (JSONObject) JSONSerializer.toJSON(jsonPiece);
			return jsonObj;
		}
		catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	private String easyReg(){
		String regId = null;
		try {
			Random r = new Random();
			String deviceName = "smap_" + r.nextInt(323234);
			regId=registrar.registerDevice(deviceName);
			logger.info("Registering device with name " + deviceName + "; registration identifier=" + regId);
		} catch(Exception e){
			String thisError = "";
			if(e instanceof NameRegisteredException){
				thisError = "Name already registered; try a new device name";
				errorVec.addElement(thisError);
				//System.out.println(thisError);
				logger.warning(thisError);

			}
			else if (e instanceof NoMoreRegistrantsException){
				thisError="Maximum Joins reached; try again later";
				errorVec.addElement(thisError);
				logger.warning(thisError);
			}
			else
				logger.log(Level.WARNING, "Join processing error", e);
			return regId;

		}
		return regId;
	}

	private String processJoin(HttpExchange exchange, JSONObject joinReq) {
		logger.info("processing join");
		String newPubParam = (String) exchange.getAttribute("new");
		if(newPubParam != null && newPubParam.equalsIgnoreCase("true")){
			return easyReg();
		} else {
			try {
				String regId=null;
				//get the associated streams
				JSONObject objectStream = joinReq.getJSONObject("object_stream");
				JSONObject contextStream = joinReq.getJSONObject("context_stream");
				JSONObject logicStream = joinReq.getJSONObject("logic_stream");

				//fetch join schema and validate join request (joinReq)
				if(objectStream ==null || contextStream == null || logicStream ==null)
					return regId;

				//JSONSchemaValidator validator = new JSONSchemaValidator();
				//JSONObject joinSchema = JSONSchemaValidator.fetchJSONObj("http://www.eecs.berkeley.edu/~jortiz/gridos/site/schemas/protocols/join_schema.json");

				//validate and insert streams into repository
				try {
					String deviceName = objectStream.getString("device_name");
					regId=registrar.registerDevice(deviceName);
					logger.info("Registering device with name " + deviceName + "; registration identifier=" + regId);
					//if(validator.validate(joinReq, joinSchema) && regId !=null && !regId.equals("0")) {
					if(regId !=null && !regId.equals("0") && !regId.equals("")) {
						System.out.println("device_name: " + deviceName);

						//store in database
						Is4Database dbLayer = (Is4Database) new DBAbstractionLayer();
						
						//timestamp before entering
						objectStream.put("PubId", regId);
						contextStream.put("PubId", regId);
						logicStream.put("PubId", regId);

						Date date = new Date();
						long timestamp = date.getTime()/1000;

						objectStream.put("timestamp", timestamp);
						contextStream.put("timestamp", timestamp);
						logicStream.put("timestamp", timestamp);

						//place into database
						dbLayer.putEntry(objectStream);
						dbLayer.putEntry(contextStream);
						dbLayer.putEntry(logicStream);
						logger.log(Level.FINER, "Adding object stream:\n", objectStream);
						logger.log(Level.FINER, "Adding context stream:\n", contextStream);
						logger.log(Level.FINER, "Adding logic stream:\n", logicStream);

						//create metadata binding
						JSONObject metadata = new JSONObject();
						metadata.put("object_stream", objectStream);
						metadata.put("context_stream", contextStream);
						metadata.put("logic_stream", logicStream);
						MetadataMngr.getInstance().bind(regId, metadata);

						return regId;
					}
				} catch(Exception e){
					String thisError = "";
					if(e instanceof NameRegisteredException){
						thisError = "Name already registered; try a new device name";
						errorVec.addElement(thisError);
						//System.out.println(thisError);
						logger.warning(thisError);

					}
					else if (e instanceof NoMoreRegistrantsException){
						thisError="Maximum Joins reached; try again later";
						errorVec.addElement(thisError);
						logger.warning(thisError);
					}
					else
						logger.log(Level.WARNING, "Join processing error", e);
					return regId;

				}

				return regId;


			} catch (Exception e){
				logger.log(Level.WARNING, "Join processing error 2", e);
				return null;
			}
		}
	}

	private boolean processObjectStream(JSONObject objStream) {
		return true;
	}

	private boolean processConextStream(JSONObject contextStream) {
		return true;
	}

	private boolean processLogicStream(JSONObject logicStream){
		return true;
	}

	private void sendJoinSuccessReply(HttpExchange exchange, String joinId){
		logger.info("sendJoinSuccessReply");

		try {
			OutputStream responseBody = exchange.getResponseBody();

			//compose body for response
			JSONObject sendSuccessDoc = new JSONObject();
			sendSuccessDoc.put("operation", "join");
			sendSuccessDoc.put("status", "success");
			sendSuccessDoc.put("ident",joinId);
			String body = sendSuccessDoc.toString();

			//construct response and send	
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(202, 0);	
			responseBody.write(body.getBytes());
			responseBody.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private void sendJoinErrorReply(HttpExchange exchange, String joinId, int errorCode){
		logger.info("sendJoinErrorReply");
		try {
			OutputStream responseBody = exchange.getResponseBody();

			//compose body for response
			JSONObject sendErrorDoc = new JSONObject();
			JSONArray errorArray = new JSONArray();
			
			sendErrorDoc.put("operation", "join");
			sendErrorDoc.put("status", "fail");
			sendErrorDoc.put("ident",joinId);

			//add all the errors to the response message
			for (int i=0; i<errorVec.size(); i++){
				errorArray.add(i, (String)errorVec.elementAt(i));
			}

			sendErrorDoc.put("errors", errorArray);
			errorVec.clear();
			String body = sendErrorDoc.toString();


			//construct response and send	
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			
			if(errorCode>0) 
				exchange.sendResponseHeaders(errorCode, 0);	
			else
				exchange.sendResponseHeaders(200, 0);

			responseBody.write(body.getBytes());
			responseBody.close();
		} catch (Exception e){
			logger.log(Level.WARNING, "Error sending join error reply", e);
		}
	}
	
	protected void sendResponse(HttpExchange exchange, int errorCode, String response){
		logger.info("sendResponse");
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
}
