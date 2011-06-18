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

import local.db.*;
import local.rest.*;
import local.rest.resources.util.*;
import local.rest.interfaces.*;
import is4.*;

import net.sf.json.*;

import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*; 

public class ModelManagerResource extends Resource {
	
	protected static transient final Logger logger = Logger.getLogger(ModelManagerResource.class.getPackage().getName());
	
	//model instances root
	public static final String MODELS_ROOT = "/models/";
	
	public ModelManagerResource() throws Exception, InvalidNameException {
		super(MODELS_ROOT);
	}
	
	//public void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){}
	
	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		post(exchange, data, internalCall, internalResp);
	}
	
	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
		try{
			if(data != null){
				JSONObject dataObj = (JSONObject) JSONSerializer.toJSON(data);
				String op = dataObj.optString("operation");
				String name = dataObj.optString("name");
				if(op.equalsIgnoreCase("create_model")){
					
					if(!ResourceUtils.devNameIsUnique(MODELS_ROOT,name)){
						errors.add("There's already a model named " + name + "; try another name");
						resp.put("status","fail");
						resp.put("errors",errors);
						sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
						return;
					}
					
					JSONObject scriptObj = dataObj.optJSONObject("script");
				
					if(!scriptObj.containsKey("winsize") || !scriptObj.containsKey("func")){
						errors.add("script object must have winsize and func attributes");
						resp.put("status", "fail");
						resp.put("errors",errors);
						sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
						return;
					}
				
					int winsize = scriptObj.optInt("winsize", 0);
					String scriptStr = scriptObj.optString("func");
					boolean materialize = scriptObj.optBoolean("materialize", false);
					
					String cleanScript = scriptObj.toString().trim().replaceAll("\t", " ").replaceAll("\n", " ");
					cleanScript = cleanScript.trim().replace("\\t", " ").replaceAll("\"", "");
					logger.info("CleanScript:" + cleanScript);
				
					ModelResource newModelResource = new ModelResource(MODELS_ROOT + name + "/", cleanScript, materialize);
					RESTServer.addResource(newModelResource);
					sendResponse(exchange, 201, null, internalCall, internalResp);
				}
				
				else {
					errors.add("Unknown operation");
					resp.put("status", "fail");
					resp.put("errors",errors);
					sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
				}
			}
		} catch(Exception e){
			if(e instanceof JSONException){
				errors.add("Invalid JSON");
			} else {
				errors.add(e.getMessage());
			}
			resp.put("status","fail");
			resp.put("errors",errors);
			sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
			
		}
	}
	
	public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 400, null, internalCall, internalResp);
	}
}
