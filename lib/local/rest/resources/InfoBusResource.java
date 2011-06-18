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
 * StreamFS release version 2.0
 */

package local.rest.resources;

import is4.*;
import local.db.*;
import local.rest.*;
import local.rest.smap.*;

import com.sun.net.httpserver.*;
import net.sf.json.*;
import java.net.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import javax.naming.InvalidNameException;

public class InfoBusResource extends GenericPublisherResource {
	private static transient final Logger logger = Logger.getLogger(InfoBusResource.class.getPackage().getName());
	private static InfoBusResource ibusRsrc = null;

	private InfoBusResource(String path) throws Exception, InvalidNameException{
		super(path, UUID.fromString("00000000-0000-0000-0000-000000000000"));
		UUID thisPubIdUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
			
		//register the publisher
		/*Registrar registrar = Registrar.registrarInstance();
		String newPubId = registrar.registerDevice(path);
		try{ 
			thisPubIdUUID = UUID.fromString(newPubId);
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}*/

		//add to publishers table
		if(database.getIs4RRPath(thisPubIdUUID) ==null)
			database.addPublisher(thisPubIdUUID, null, null, path, null);
	}
	
	public static InfoBusResource getInstance(String path){
		try {
			if(ibusRsrc == null){
				ibusRsrc = new InfoBusResource(path);
				RESTServer.addResource(ibusRsrc);
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
			return null;
		}
		return ibusRsrc;
	}

	public void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		JSONObject response = new JSONObject();
		response.put("status", "success");
		response.put("stream_type_cnt", database.publisherCount());
		sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
	}

	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 400, null, internalCall, internalResp);
	}

	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		put(exchange, data, internalCall, internalResp);
	}

	public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 400, null, internalCall, internalResp);
	}

}
