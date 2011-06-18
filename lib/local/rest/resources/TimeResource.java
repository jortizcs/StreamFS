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

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;
import java.net.*;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*; 

public class TimeResource extends Resource{
	protected static transient Logger logger = Logger.getLogger(TimeResource.class.getPackage().getName());

	public TimeResource(String uri) throws Exception, InvalidNameException{
		super(uri);
	}

	public  void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		JSONObject response = new JSONObject();
		Date date = new Date();
		response.put("Now", date.getTime()/1000);
		sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
	}

	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 501, null, internalCall, internalResp);
	}

	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 501, null, internalCall, internalResp);
	}

	public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 501, null, internalCall, internalResp);
	}

}

