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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.net.*;
import java.util.*;
import java.sql.*;

import is4.*;
import local.db.*;
import local.json.validator.*;
import net.sf.json.*;
import com.sun.net.httpserver.*;



public class SmapSourceHandler extends Filter implements HttpHandler {
	protected static Logger logger = Logger.getLogger(SmapSourceHandler.class.getPackage().getName());

	//This addresses the HttpContext switch bug
	//For every call to create a context in the httpServer after the root, the HttpContext object changes, and the filter
	//is no longer used in the new object.
	protected HttpContext thisContext = null;
	protected static String URI = null;

	//maps the deployment name to an Hashtable that maps sMAP device name to IS4 publisher identifier
	//Example: acme302-->067e6162-3b6f-4ae2-a171-2470b63dff00
	//	Usually acme devices are named with an integer(i.e. 302), this process appends
	//	the model in front of the id.
	public Hashtable<String, Hashtable<String, String>> allDeployments = new Hashtable<String, Hashtable<String,String>>();

	public static String is4URL = "http://smote.cs.berkeley.edu:8080";

	public SmapSourceHandler(String uri){
		URI = uri;
		if (System.getenv("IS4_HOSTNAME") != null && System.getenv("IS4_PORT") !=null)
			is4URL = "http://" + System.getenv("IS4_HOSTNAME") + ":" + System.getenv("IS4_PORT");
	}

	public String description(){
		return "SmapSourceHandler filter";
	}

	public synchronized void doFilter(HttpExchange exchange, Filter.Chain chain) throws IOException {
		logger.fine("doFilter invoked");
		boolean paramsOk = false;
		if((paramsOk = parseParams(exchange)) && chain==null)
			this.handle(exchange);
		else if (!paramsOk) 
			sendResponse(exchange, 404, null);
		else
			chain.doFilter(exchange);
	}

	protected synchronized boolean parseParams(HttpExchange exchange) {
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

	public synchronized void handle(HttpExchange exchange) throws IOException{
		java.util.Date javaDate = new java.util.Date();
		long ttime = javaDate.getTime();
		Timestamp tstamp  = new Timestamp(ttime);
		logger.fine("putEntry: localtime=" + ttime);
		String tstampStr = tstamp.toString();
		/*logger.warning(tstampStr+ ": handle() called; "+ exchange.getRequestURI().toString()
				+ "\nexchange: " + exchange + "\nthis: " + this);*/

		String deploymentName = null;
		String sensepoint = null;
		String schema = null;
		String templateAttr = null;
		String formatStr = null;

		//This addresses the HttpContext switch bug in the library
		//The filter must be called BEFORE the handler
		if (exchange.getHttpContext() != thisContext && exchange.getHttpContext().getFilters().size()==0) {
			this.parseParams(exchange);
			
			/*deploymentName = (String)exchange.getAttribute("deployment");
			sensepoint = (String)exchange.getAttribute("sensepoint");
			schema = (String)exchange.getAttribute("schema");
			templateAttr = (String)exchange.getAttribute("template");
			formatStr = (String)exchange.getAttribute("format");*/

			thisContext = exchange.getHttpContext();
			thisContext.getFilters().add(this);
		}

		try{
			String requestMethod = exchange.getRequestMethod();
			logger.info("Heard request Method= " + requestMethod);
			if (requestMethod.equalsIgnoreCase("POST") ||
					requestMethod.equalsIgnoreCase("PUT")) {

				//Print out the request body
				BufferedReader is = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
				String line="";
				StringBuffer bodyBuf = new StringBuffer();
				while((line=is.readLine())!=null)
					bodyBuf.append(line).append(" ");
			
				//handle it
				JSONObject report = (JSONObject) JSONSerializer.toJSON(bodyBuf.toString());

				deploymentName = (String)exchange.getAttribute("deployment");
				sensepoint = (String)exchange.getAttribute("sensepoint");
				schema = (String)exchange.getAttribute("schema");
				templateAttr = (String)exchange.getAttribute("template");
				formatStr = (String)exchange.getAttribute("format");

				marshallIs4Push(exchange, report, deploymentName, sensepoint, schema, templateAttr, formatStr);

			}
		} catch (JSONException e){
			e.printStackTrace();
			System.out.println("Response is NOT valid JSON");
		}
	}

	protected synchronized void sendResponse(HttpExchange exchange, int errorCode, String response){
		try{
			logger.info("Sending Response");
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "application/json");
			exchange.sendResponseHeaders(errorCode, 0);

			OutputStream responseBody = exchange.getResponseBody();
			if(response!=null)
				responseBody.write(response.getBytes());
			responseBody.close();
			exchange.close();
		}catch(Exception e){
			logger.log(Level.WARNING, "Exception thrown while sending response",e);
			exchange.close();
		}
	}

	public synchronized void marshallIs4Push(HttpExchange exchange, JSONObject report,
					String deploymentName, String sensepoint, String schema, String templateAddr,
					String smapResource){
		logger.info("marshalIs4Push: handling incoming report");
		/*String deploymentName = null;
		String sensepoint = null;
		String schema = null;
		String templateAddr = null;
		String smapResource = null;*/

		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		/*if(exchange.getAttribute("deployment") != null 
			&& exchange.getAttribute("sensepoint")!=null && exchange.getAttribute("schema")!=null
			&& exchange.getAttribute("format") != null) 
		{

			deploymentName =(String)exchange.getAttribute("deployment");
			sensepoint=(String)exchange.getAttribute("sensepoint");
			schema=(String)exchange.getAttribute("schema");
			templateAddr = (String)exchange.getAttribute("template");
			smapResource = (String) exchange.getAttribute("format");*/
		if(deploymentName != null && sensepoint != null && schema != null && templateAddr !=null
				&& smapResource != null){

			if(smapResource == null || !smapResource.endsWith("/formatting") || (new StringTokenizer(smapResource, "*")).countTokens() != 2){
				errors.add("Invalid or missing 'format' parameter");
				errors.add("Url format schema: http://[smap_resource]/data/*/<channel>/formatting");
				response.put("errors", errors);
				sendResponse(exchange, 200, response.toString());
				return;
			}

			if(sensepoint.equalsIgnoreCase("all") && ((schema.equalsIgnoreCase("report"))|| schema.equalsIgnoreCase("reading"))) {
				handleBulkReports(deploymentName,report,smapResource, templateAddr);
				sendResponse(exchange, 200, null);
			} else if(!sensepoint.equalsIgnoreCase("all") && (schema.equalsIgnoreCase("report") || schema.equalsIgnoreCase("reading"))) {
				handleSingleReport(deploymentName,sensepoint,report,smapResource,templateAddr);
				sendResponse(exchange, 200, null);
			} else {
				errors.add("Unrecognized request: " + exchange.getRequestURI().toString());
				response.put("errors", errors);
				sendResponse(exchange, 200, response.toString());
			}
		}else {
			StringBuffer errorBuf = new StringBuffer();
			errorBuf.append("marshallIs4Push:  Could not handle incoming smap report; ");
			errorBuf.append("\n\t\tAll URL parameters must be included: deployment=[name], sensepoint=[name|all], schema=[smap_schema], format=[smap_format_url]");
			errorBuf.append("\n\t\tValid smap schema labels:");
			errorBuf.append(" report, reading");
			//logger.warning(errorBuf.toString());
			errors.add(errorBuf.toString());
			response.put("errors", errors);
			sendResponse(exchange, 200, response.toString());
		}
	}

	private String cleanFormatResource(String formatStr, String sensepoint){
		String newFormatStr = null;
		if(formatStr != null && !formatStr.equals("") && formatStr.endsWith("/formatting")) {
			StringTokenizer formatTokens = new StringTokenizer(formatStr, "*");
			if(formatTokens.countTokens() > 1)
				newFormatStr = formatTokens.nextToken() + sensepoint + formatTokens.nextToken();
		}
		return newFormatStr;
	}

	private void storeFormatData(String formatUrl, String pubId){/*, String m1, String m2){
		System.out.println(m1);
		System.out.println(m2);*/
		if(formatUrl != null){
			try {
				JSONObject formatObject = JSONSchemaValidator.fetchJSONObj(formatUrl);
				if(formatObject != null){
					formatObject.put("name", "formatting_entry");
					formatObject.put("PubId", pubId);
					formatObject.put("formatUrl", formatUrl);
					DBAbstractionLayer.database.putEntry(formatObject);
				}
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
			}
		}
	}

	private void handleBulkReports(String deploymentName, JSONObject report, String resource, String templateAddr){
		Hashtable<String, String> nameToPubIdTable = null;
		if(allDeployments.containsKey(deploymentName)) {
			nameToPubIdTable = allDeployments.get(deploymentName);
		} else {
			nameToPubIdTable = new Hashtable<String, String>();
			allDeployments.put(deploymentName, nameToPubIdTable);
			/*if(pubid!=null && MySqlDriver.checkPubIdIsRegistered(pubid) && !nameToPubIdTable.containsKey(deploymentName+'_'+spid)){
			}*/
		}

		String pubid = null;
		Vector<String> sensepoints = new Vector<String>(report.keySet());
		for(int i=0; i<sensepoints.size(); i++) {

			String spid = sensepoints.get(i);
			JSONObject thisDataObj = report.optJSONObject(spid);
			
			boolean validObj = (thisDataObj != null && !thisDataObj.isNullObject());
			//String message = "spid: " + spid + "\nreport: " + report.toString();
			//message += "\nAssociate_object: " + thisDataObj + "valid? " + validObj + "\n";
			//logger.warning(message);
			
			String formatUrl = cleanFormatResource(resource, spid);
			String dataResource = resource.replace("*", spid);
			dataResource = resource.replace("/formatting", "");
			
			if(nameToPubIdTable.containsKey(deploymentName+'_'+spid)){
				pubid =  nameToPubIdTable.get(deploymentName+'_'+spid);
				if(validObj) 
					sendToIs4(pubid, thisDataObj);
			} else if((pubid=sendJoin(deploymentName+'_'+spid, dataResource,templateAddr))==null || pubid.equals("") || pubid.equals("0")){
				logger.warning("Could not add deployment " + deploymentName + ", sensepoint " + spid + " as a publisher");
			} else{
				nameToPubIdTable.put(deploymentName+'_'+spid, pubid);
				if(validObj)
					sendToIs4(pubid, thisDataObj);
			}

			if(validObj && pubid!=null){
				//String m1 = "######### Fetched " + spid + " from report jsonobject:\nreport\n\n" + report.toString();
				//String m2 = "######### ValidObject? " + validObj;
				String message = "spid: " + spid + "\nreport: " + report.toString();
				message += "\nAssociate_object: " + thisDataObj + "\nvalid? " + validObj + "\n";
				message += "deployment=" + deploymentName + "\nresource=" + resource;
				message += "\ntemplateAddr=" + templateAddr;
				//logger.warning(message);

				storeFormatData(formatUrl, pubid);//, m1, m2);
			}	
		}
	}

	private void handleSingleReport(String deploymentName, String spid, JSONObject report, String resource, String templateAddr){
		Hashtable<String, String> nameToPubIdTable = null;
		if(allDeployments.containsKey(deploymentName)) {
			nameToPubIdTable = allDeployments.get(deploymentName);
		} else {
			nameToPubIdTable = new Hashtable<String, String>();
			allDeployments.put(deploymentName, nameToPubIdTable);
		}
		
		String pubid = null;
		resource = resource.replace("/formatting", "");

		if(nameToPubIdTable.containsKey(deploymentName+'_'+spid)){
			pubid =  nameToPubIdTable.get(deploymentName+'_'+spid);
			sendToIs4(pubid, report);
		} else if((pubid=sendJoin(deploymentName+'_'+spid, resource, templateAddr))==null || pubid.equals("") || pubid.equals("0")){
			logger.warning("Could not add deployment " + deploymentName + ", sensepoint " + spid + " as a publisher");
		} else{
			nameToPubIdTable.put(deploymentName+'_'+spid, pubid);
			sendToIs4(pubid, report);
		}

		storeFormatData(resource, pubid);
	}

	public String sendJoin(String devName, String resource, String templateAddr){
		String regId = null;
		if((regId = ((MySqlDriver)DBAbstractionLayer.database).getPid(devName)) != null)
			return regId;
		try
		{
			JSONObject joinReq = new JSONObject();
			if(templateAddr != null) {
				if(!templateAddr.startsWith("http://"))
					templateAddr = "http://" + templateAddr;
				joinReq = JSONSchemaValidator.fetchJSONObj(templateAddr); if(joinReq == null){
					logger.warning("Invalid schema url: " + templateAddr);
					return null;
				}
			} else {
				joinReq = JSONSchemaValidator.fetchJSONObj("http://jortiz81.homelinux.com/is4/schemas/acme_join_request.json");
			}
			JSONObject objectStream = joinReq.getJSONObject("object_stream");
			objectStream.put("device_name", devName);
			joinReq.put("object_stream", objectStream);

			if(resource != null){
				JSONObject logicStream = joinReq.getJSONObject("logic_stream");
				logicStream.put("resource", resource);
				joinReq.put("logic_stream", logicStream);
			}

			String requestStr = joinReq.toString();

			URL yahoo = new URL(is4URL + "/is4/pub/join");
			URLConnection yc = yahoo.openConnection();
			yc.setRequestProperty("Content-Type", "application/json");
			yc.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(yc.getOutputStream());
			wr.write(requestStr);
			wr.flush();

			//get response from IS4
			BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			String inputLine;
			String wholeDoc = "";
			while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
				wholeDoc += inputLine;
			}

			JSONObject is4Resp = (JSONObject) JSONSerializer.toJSON(wholeDoc);
			if(is4Resp.getString("operation").equalsIgnoreCase("join") && is4Resp.getString("status").equalsIgnoreCase("success")){
				regId = is4Resp.getString("ident");
				UUID regIdUUID = UUID.fromString(regId);
			}
			in.close();
		} catch(Exception e){
			e.printStackTrace();
		}

		return regId;
	}

	public void sendToIs4(String pubid, JSONObject dataObj){
		try {
			if(dataObj != null && !dataObj.isNullObject()) {
				URL pubRsrc = new URL(is4URL + "/is4/pub?schema=reading");

				dataObj.put("PubId", pubid);

				URLConnection urlConn = pubRsrc.openConnection();
				urlConn.setRequestProperty("Content-Type", "application/json");
				urlConn.setDoOutput(true);
				OutputStreamWriter wr = new OutputStreamWriter(urlConn.getOutputStream());
				wr.write(dataObj.toString());
				wr.flush();
				
				BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}
