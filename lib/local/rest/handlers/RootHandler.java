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

import net.sf.json.*;
import com.sun.net.httpserver.*;

import javax.naming.InvalidNameException;
import local.rest.resources.*;
import local.db.*;
import local.rest.*;

public class RootHandler extends Resource{
	private static Logger logger= Logger.getLogger(RootHandler.class.getPackage().getName());

	public RootHandler(String path) throws Exception, InvalidNameException{
		//super("/is4");
		super(path);
	}

	public void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		try {
			logger.fine("GETTING RESOURCES: " + URI);
			JSONObject response = new JSONObject();
			JSONArray subResourceNames = ((MySqlDriver)(DBAbstractionLayer.database)).rrGetChildren(URI);
			logger.fine(subResourceNames.toString());
			response.put("status", "success");
			response.put("children",subResourceNames);
			response.put("uptime", ((new Date()).getTime()/1000) - RESTServer.start_time);
			response.put("uptime_units", "seconds");
			response.put("activeResources", database.rrGetAllPaths().size());
			sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
			return;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error while responding to GET request",e);
			sendResponse(exchange, 200, null, internalCall, internalResp);
		} finally {
			try {
				if(exchange !=null){
					exchange.getRequestBody().close();
					exchange.getResponseBody().close();
					exchange.close();
				} 
			} catch(Exception e){
				logger.log(Level.WARNING, "Trouble closing exchange in RootHandler", e);
			}
		}
	}

	/*public void handle(HttpExchange exchange) throws IOException{
		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			//System.out.println("Root: GET heard something and responded");
			logger.info("GET request received");
			
		  } else if (requestMethod.equalsIgnoreCase("POST")){
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0);

			OutputStream responseBody = exchange.getResponseBody();
			Headers requestHeaders = exchange.getRequestHeaders();
			Set<String> keySet = requestHeaders.keySet();
			Iterator<String> iter = keySet.iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				List values = requestHeaders.get(key);
				String s = key + " = " + values.toString() + "\n";
				responseBody.write(s.getBytes());
			}

			//Print out the request body
			BufferedReader is = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
			String line="";
			StringBuffer bodyBuf = new StringBuffer();
			while((line=is.readLine())!=null){
				System.out.println(line);
				bodyBuf.append(line).append(" ");
			}
			JSONObject jsonObj= getJSON(bodyBuf.toString());
			//if(jsonObj!=null) {
			//	System.out.println("Root: JSON parsed");
			//}

			responseBody.close();
			
		  } else {
			//System.out.println("heard something");
			logger.info("Heard something, not a GET or POST");
		  }
	}

	public JSONObject getJSON(String jsonPiece) {
		try {
			JSONObject jsonObj = (JSONObject) JSONSerializer.toJSON(jsonPiece);
			return jsonObj;
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Error parsing json piece", e);
			return null;
		}
	}*/

}
