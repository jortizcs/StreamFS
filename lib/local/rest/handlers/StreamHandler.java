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

public class StreamHandler implements HttpHandler {
	private static Logger logger = Logger.getLogger(StreamHandler.class.getPackage().getName());

	public StreamHandler (){}

	public void handle(HttpExchange exchange) throws IOException{
		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			//System.out.println("Heard GET: " + exchange.getRemoteAddress().toString());
			//System.out.println(exchange.getRequestHeaders().keySet());
			logger.info("GET: " + exchange.getRemoteAddress().toString());
			logger.finer(exchange.getRequestHeaders().keySet().toString());
			
			Iterator<String> it= exchange.getRequestHeaders().keySet().iterator();
			while(it.hasNext()){
				String val = it.next();
				//System.out.println(val + exchange.getRequestHeaders().getFirst(val));
				logger.finer(val + exchange.getRequestHeaders().getFirst(val));
			}
			
			try{Thread.sleep(1000*3);} catch(Exception e){e.printStackTrace();}
			
			//compose body for response
			JSONObject dataObject = new JSONObject();
			Random random = new Random();
			int faketemp=60 + Math.abs(random.nextInt(26));
			dataObject.put("data",new Integer(faketemp));
			String body = dataObject.toString();
			System.out.println("Sending: " + body.toString());
	
			//construct response and send
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, 0);
			
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(body.getBytes());
			responseBody.close();
				
		  } else if (requestMethod.equalsIgnoreCase("POST") ||
				  requestMethod.equalsIgnoreCase("PUT")){
			  	System.out.println("Cannot handle a POST or PUT");
			  	Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0);
		  }

	}
}
