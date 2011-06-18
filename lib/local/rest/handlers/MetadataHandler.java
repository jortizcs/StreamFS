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

import local.db.*;
import local.metadata.*;
import local.metadata.context.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import net.sf.json.*;
import com.sun.net.httpserver.*;
import java.io.*;

public class MetadataHandler extends Filter implements HttpHandler {

	private static Logger logger = Logger.getLogger(MetadataHandler.class.getPackage().getName());

	//This addresses the HttpContext switch bug
	//For every call to create a context in the httpServer after the root, the HttpContext object changes, and the filter
	//is no longer used in the new object.
	protected HttpContext thisContext = null;

	protected static String URI = null;

	private static final int META = 0;
	private static final int CONTEXT = 1;
	private static final int OBJECT = 2;
	private static final int LOGIC = 3;

	public MetadataHandler(String uri){
			URI = uri;
	}

	public String description(){
		return "MetadataHandler " + URI + " filter";
	}

	public void doFilter(HttpExchange exchange, Filter.Chain chain) throws IOException {
		logger.info("doFilter");
		boolean paramsOk = false;
		if((paramsOk = parseParams(exchange)) && chain==null)
			this.handle(exchange);
		else if (!paramsOk) 
			sendResponse(exchange, 404, null);
		else
			chain.doFilter(exchange);
	}

	protected boolean parseParams(HttpExchange exchange) {
		logger.info("parseParams");
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
		logger.info("filterCheck");
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

	private int classifyResourceType(String uri){
		StringTokenizer tokenizer = new StringTokenizer(uri, "?");
		String u = tokenizer.nextToken();
		if(u.endsWith("metadata")) return META;
		else if(u.endsWith("metadata/context")) return CONTEXT;
		else if (u.endsWith("metadata/object")) return OBJECT;
		else if(u.endsWith("metadata/logic")) return LOGIC;
		else return -1;
	}

	public void handle(HttpExchange exchange) throws IOException{
		logger.info("handle");
		//check if the filter was hit up
		filterCheck(exchange);
		classifyResourceType(exchange.getRequestURI().toString());

		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			handleGetRequest(exchange);			
		} 
		else if (requestMethod.equalsIgnoreCase("PUT")){
			handlePutRequest(exchange);
		}
		else if (requestMethod.equalsIgnoreCase("POST") ){
			handlePostRequest(exchange);
		}
		else if (requestMethod.equalsIgnoreCase("DELETE")){
			handleDeleteRequest(exchange);
		}

		exchange.close();
	}

	private void handleGetRequest(HttpExchange exchange) {

		logger.finer("GET Request received");
		int resourceClass = classifyResourceType(exchange.getRequestURI().toString());
		switch(resourceClass){	
			case META:
					handleMetaGet(exchange);
					break;
			case CONTEXT:
					handleContextGet(exchange);
					break;
			case LOGIC:
					handleLogicGet(exchange);
					break;
			case OBJECT:
					handleObjectGet(exchange);
					break;
			default:
					JSONObject unknown = new JSONObject();
					JSONArray errors = new JSONArray();
					errors.add("Unknown resource");
					unknown.put("errors", errors);
					sendResponse(exchange, 200, unknown.toString());
					break;
			
		}
	}

	private void handleMetaGet(HttpExchange exchange){
		String id = (String)exchange.getAttribute("pubid");
		logger.fine("exchange.id=" + id);
		if(id != null && !(new StringTokenizer(exchange.getRequestURI().toString(), "&").countTokens()>1)) {
			processIdFetch(exchange, id);
		} else {
			JSONObject r = new JSONObject();
			JSONArray errors = new JSONArray();

			String errorMsg = "\"pubid\" URL parameter must be set";
			errors.add(errorMsg);
			logger.info(errorMsg);

			r.put("operation", "get_metadata");
			r.put("status", "fail");
			r.put("errors", errors);

			sendResponse(exchange, 200, r.toString());
		}

		exchange.setAttribute("pubid", null);
	}

	private boolean hasParams(HttpExchange exchange){
		StringTokenizer t = new StringTokenizer(exchange.getRequestURI().toString(), "?");
		if(t.countTokens()>1)
			return true;
		return false;
	}

	private void handleContextGet(HttpExchange exchange){
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		logger.info("handleContextGet");
		if(hasParams(exchange)){
			String id = (String) exchange.getAttribute("id");
			String getImage= (String) exchange.getAttribute("getimage");

			//handle invalid requests
			if(id==null) {
				errors.add("Must specify a context id");
				response.put("errors", errors.toString());
				sendResponse(exchange, 200, response.toString());
			} else if(id.length()<8){
				errors.add("Invalid Id");
				response.put("errors", errors.toString());
				sendResponse(exchange, 200, response.toString());
			}

			//handle request base on id type
			if(id.length()>=8){
				JSONObject cmap = ((MySqlDriver)DBAbstractionLayer.database).getContextGraph(id.substring(0,8));
				cmap.remove("name");
				if(cmap != null && !cmap.toString().equals("{}")) {
					//node or edge
					if(id.length()>8 && id.charAt(8) == 'n'){
						JSONArray nodes = cmap.getJSONArray("graph_nodes");
						int i=0;
						while(i<nodes.size()){
							JSONObject currentNode = (JSONObject) nodes.get(i);
							if(currentNode.getString("cnid").equals(id)){
								sendResponse(exchange, 200, currentNode.toString());
								return;
							}
							i+=1;
						}
							
					} else if(id.length()>8 && id.charAt(8) == 'e'){
						JSONArray edges = cmap.getJSONArray("graph_edges");
						int i=0;
						while(i<edges.size()){
							JSONObject currentEdge = (JSONObject) edges.get(i);
							if(currentEdge.getString("ceid").equals(id)){
								sendResponse(exchange, 200, currentEdge.toString());
								return;
							}
							i+=1;
						}
					} else if(id.length()==8) {
						if (getImage != null && getImage.equalsIgnoreCase("true")){
							String dotOutput = dotConversion(cmap, "http://localhost/is4/metadata/context/dot");
							String cid = cmap.getString("cid");
							String iurl = "http://localhost/is4/metadata/context/dot/ContextMap_" + cid + ".svg";
							sendDot(cid, dotOutput, "jortiz@jortiz81.homelinux.com", "/var/www/is4/metadata/context/dot");
							cmap.put("imageUrl", iurl);
						}
						sendResponse(exchange, 200, cmap.toString());
					}
				} else {
					errors.add("Could not find context information for id=" + id);
					response.put("errors", errors);
					sendResponse(exchange, 200, response.toString());
				}
			}
		} else{
			errors.add("Id must be provided");
			response.put("errors", errors);
			sendResponse(exchange, 200, response.toString());
		}
	}

	private void handleLogicGet(HttpExchange exchange){
	}

	private void handleObjectGet(HttpExchange exchange){
	}

	private void handlePutRequest(HttpExchange exchange){
		if(classifyResourceType(exchange.getRequestURI().toString()) == META){
			String requestType = (String) exchange.getAttribute("type");
			if(requestType != null && requestType.equalsIgnoreCase("context_graph")) {
				JSONArray errors = new JSONArray();
				//validate the input type
				JSONObject metadata = getJSONRequestBody(exchange);
				if(!ContextMngr.getInstance().addNewContextMap(metadata, errors)){
					JSONObject response = new JSONObject();
					response.put("errors", errors);
					sendResponse(exchange, 200, response.toString());
				}
			}
		} else {  //put only on /is4/metadata
			sendResponse(exchange, 400, null);
		}
	}

	private void handlePostRequest(HttpExchange exchange){
	}

	private void handleDeleteRequest(HttpExchange exchange){
	}

	private void processIdFetch(HttpExchange exchange, String id) {
		JSONObject r = new JSONObject();
		if(id.equals("*")){
			JSONObject all = new JSONObject();
			JSONArray activeIds = MetadataMngr.getInstance().getAllBoundIds();
			for(int i=0; i<activeIds.size(); i++){
				String thisId = (String) activeIds.get(i);
				JSONObject thisMeta = DBAbstractionLayer.database.getMetadata(thisId);
				all.put(thisId, thisMeta);
			}
			sendResponse(exchange, 200, all.toString());
		} else{
			try {
				if(r==null)
					logger.warning("r is NULL");
				if(id==null)
					logger.warning("id is NULL");
				//check valud UUID
				UUID u = UUID.fromString(id);
				JSONObject metadata = DBAbstractionLayer.database.getMetadata(id);
				r.put("id", id);
				r.put("metadata", metadata);
				sendResponse(exchange, 200, r.toString());
			} 
			catch(IllegalArgumentException e) {
				
				JSONArray errors = new JSONArray();

				String errorMsg = "Invalid UUID format for pubid: " + id;
				errors.add(errorMsg);
				logger.info(errorMsg);

				r.put("operation", "get_metadata");
				r.put("status", "fail");
				r.put("errors", errors);

				sendResponse(exchange, 200, r.toString());
			}
		}
	}

	private JSONObject getJSONRequestBody(HttpExchange exchange) {
		try{
			BufferedReader is = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
			String line="";
			StringBuffer bodyBuf = new StringBuffer();
			while((line=is.readLine())!=null){
				bodyBuf.append(line);
			}

			return (JSONObject) JSONSerializer.toJSON(bodyBuf.toString());
		} catch (JSONException e){
			logger.log(Level.WARNING, "Request Error: Invalid JSON format", e);
			JSONObject r = new JSONObject();
			JSONArray a = new JSONArray();
			a.add("Request Error: Invalid JSON format");
			r.put("operation", "add_metadata");
			r.put("status", "fail");
			r.put("errors", a);
			sendResponse(exchange, 200, r.toString());
		} catch (IOException ioe){
			logger.log(Level.WARNING, "Could not get body", ioe);
		}
		return new JSONObject();
	}

	public String dotConversion(JSONObject cmap, String url){
		try {
			StringBuffer dotBuf = new StringBuffer().append("digraph ContextMap_").append(cmap.getString("cid")).append("{");
			Hashtable<String, JSONObject> nodeLookupTable = new Hashtable<String, JSONObject>();
			JSONArray nodes = cmap.getJSONArray("graph_nodes");
			dotBuf.append("graph [URL=\"").
					append("http://localhost:8080/is4/metadata/context?id=").append(cmap.getString("cid")).append("\"];");
			for(int i=0; i<nodes.size(); i++){
				JSONObject thisNode = (JSONObject)nodes.get(i);
				String thisCnid = ((JSONObject)nodes.get(i)).getString("cnid");
				nodeLookupTable.put(thisCnid, thisNode);
				dotBuf.append("\"").append(thisNode.getString("label")).append("\" [URL=\"").
					append("http://localhost:8080/is4/metadata/context?id=").append(thisNode.getString("cnid")).append("\"];");
			}

			JSONArray edges = cmap.getJSONArray("graph_edges");
			for(int i=0; i<edges.size(); i++){
				JSONObject thisEdge = (JSONObject) edges.get(i);
				String sourceCnid = thisEdge.getString("sourceNode");
				String destCnid = thisEdge.getString("destinationNode");
				String sourceName = nodeLookupTable.get(sourceCnid).getString("label");
				String destName =  nodeLookupTable.get(destCnid).getString("label");
				String dotEdge =  "\"" + sourceName + "\"->\"" + destName + "\";";
				dotBuf.append(dotEdge);
			}
			dotBuf.append("}");
			return dotBuf.toString();
		} catch(Exception e){
			logger.log(Level.WARNING, "Error during conversion of graph to DOT", e);
			return "";
		}
	}

	public void sendDot(String cid, String dotOutput, String host, String uri){
		String filename = "ContextMap_" + cid + ".dot";
		String filename2 = "ContextMap_" + cid + ".svg";
		try {
			String makePngCommand = "dot -Tsvg " + filename + " -o ContextMap_" + cid + ".svg";
			//String command  = "scp " + filename2 + " " + host + ":" + uri;
			String command = "cp " + filename2 + " " + "/var/www/is4/metadata/context/dot/";
			String command2 = "cp " + filename + " " + "/var/www/is4/metadata/context/dot/";
			System.out.println(command);
			String deleteCmd1 = "rm -f " + filename2;
			String deleteCmd2 = "rm -f " + filename;

			File dotFile = new File(filename);
			FileOutputStream dotFileOstream = new FileOutputStream(dotFile);
			dotFileOstream.write(dotOutput.getBytes());
			dotFileOstream.close();

			System.out.println(makePngCommand);
			Process p = Runtime.getRuntime().exec(makePngCommand);
			System.out.println(command);
			p = Runtime.getRuntime().exec(command);
			p = Runtime.getRuntime().exec(command2);
			p = Runtime.getRuntime().exec(deleteCmd1);
			p = Runtime.getRuntime().exec(deleteCmd2);
		}catch(Exception e){
			logger.log(Level.WARNING, "Error sending dot file", e);
		}
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
}
