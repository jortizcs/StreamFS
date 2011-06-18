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

import is4.*;
import local.db.*;
import local.metadata.context.*;
import local.rest.resources.util.*;

import net.sf.json.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*;

/**
 *  Device instance resource.
 */
public class DeviceInstanceResource extends Resource{
	protected static transient Logger logger = Logger.getLogger(DeviceInstanceResource.class.getPackage().getName());
	private static MySqlDriver database = (MySqlDriver)DBAbstractionLayer.database;

	private int TYPE = ResourceUtils.DEVICE_RSRC;

	public DeviceInstanceResource(String path) throws Exception, InvalidNameException{
		super(path);

		//extract the name of this device
		StringTokenizer tokenizer = new StringTokenizer(path, "/");
		Vector<String> tokensVec = new Vector<String>(tokenizer.countTokens());
		while(tokenizer.hasMoreTokens())
			tokensVec.add(tokenizer.nextToken());
		String deviceName = tokensVec.get(tokensVec.size()-1);

		if(!database.deviceEntryExists(deviceName, path)){
			//insert the devices into the devices table
			database.addDeviceEntry(deviceName, path, null);
			logger.info("Added device entry");
		}

		//update resource type
		database.setRRType(URI, ResourceUtils.translateType(TYPE).toLowerCase());

		logger.info("Instantiated device instance resource: " + path);
	}

	public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		database.removeDeviceEntry(URI);
		super.delete(exchange, internalCall, internalResp);
	}

}
