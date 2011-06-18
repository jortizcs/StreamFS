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
 * IS4 release version 1.1
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

public class AllNodesResource extends Resource{

	protected static transient final Logger logger = Logger.getLogger(AllNodesResource.class.getPackage().getName());

	public AllNodesResource(String path) throws Exception, InvalidNameException {
		super(path);
	}

	public JSONObject getResources(){
		JSONObject resp=new JSONObject();
		JSONArray allpaths = database.rrGetAllPaths();
		UUID pubid=null;
		try {
			for(int i=0; i<allpaths.size(); i++){
				JSONObject info = new JSONObject();
				String thisPath = (String)allpaths.get(i);
				int rtype = ResourceUtils.translateType(database.getRRType(thisPath));
				switch(rtype){
					case ResourceUtils.DEFAULT_RSRC:
						info.put("type","default");
						break;
					case ResourceUtils.DEVICES_RSRC:
						info.put("type","devices");
						break;
					case ResourceUtils.DEVICE_RSRC:
						info.put("type", "device");
						break;
					case ResourceUtils.PUBLISHER_RSRC:
						pubid = database.isRRPublisher2(thisPath);
						if(pubid!=null)
							info.put("type", "stream");
						else
							info.put("type", "default");
						break;
					case ResourceUtils.GENERIC_PUBLISHER_RSRC:
						pubid = database.isRRPublisher2(thisPath);
						if(pubid!=null)
							info.put("type", "stream");
						else
							info.put("type", "default");
						break;
					case ResourceUtils.SUBSCRIPTION_RSRC:
						UUID subid = database.getSubId(thisPath);
						if(subid != null)
							info.put("type", "pipe");
						else
							info.put("type","default");
						break;
					case ResourceUtils.SYMLINK_RSRC:
						String link = database.getSymlinkAlias(thisPath);
						if(!link.equals("") && link.startsWith("http://")){
							try {
								info.put("type", "symlink");
								info.put("url",link);
							} catch(Exception e){
								info.put("type","default");
							}
						} else if(!link.equals("") && link.startsWith("/")){
							info.put("type","symlink");
							info.put("uri", link);
						} else{
							info.put("type","default");
						}
						break;
					default:
						info.put("type","default");
						break;
				}

				resp.put(thisPath, info);
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return resp;
	}

	public void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 200, getResources().toString(), internalCall, internalResp);
	}


	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 504, null, internalCall, internalResp);
	}

	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 504, null, internalCall, internalResp);
	}

	public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 504, null, internalCall, internalResp);
	}
}
