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
import local.metadata.context.*;

import net.sf.json.*;

import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*;

public class ContextResource extends Resource{

	protected static transient final Logger logger = Logger.getLogger(ContextResource.class.getPackage().getName());

	public ContextResource(String path) throws Exception, InvalidNameException {
		super(path);	
	}

	public synchronized void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		if(exchange.getAttribute("query") != null && 
			((String)exchange.getAttribute("query")).equalsIgnoreCase("true")){
			exchange.setAttribute("query", "false");
			super.query(exchange, data, internalCall, internalResp);
		} else {
			JSONObject response = new JSONObject();
			JSONArray errors = new JSONArray();
			try {
				JSONObject cmap = (JSONObject)JSONSerializer.toJSON(data);
				if(ContextMngr.getInstance().addNewContextMap(cmap, errors)) {
					response.put("status", "success");
				} else {
					response.put("status", "fail");
					response.put("errors", errors);
				}
				sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
			} catch (Exception e){
				if(e instanceof JSONException)
					errors.add("Invalid JSON");
				response.put("status", "fail");
				response.put("errors", errors);
				logger.log(Level.WARNING, "", e);
				sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
			}
		}
	}

	public synchronized void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 200, null, internalCall, internalResp);
	}
}
