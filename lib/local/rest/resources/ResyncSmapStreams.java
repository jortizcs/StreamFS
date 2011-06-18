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
import local.rest.*;
import local.rest.resources.util.*;
import local.db.*;
import local.metadata.context.*;
import local.rest.smap.*;

import net.sf.json.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;
import java.net.URL;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*;

/**
 *  Checks each smap stream (publisher) to see if the report is still registered in the smap server.
 *  If the report is in the system, nothing is done.  If the report has been removed from the associated
 *  smap server, a new identical report is installed.  All associated metadata is updated.  If the smap
 *  server is down resync runs as a background task for a day.  If not response is heard from smap
 *  server after a day, all stream resources associated with the smap server are deleted.
 */
public class ResyncSmapStreams extends Resource{
	
	protected static transient final Logger logger = Logger.getLogger(ResyncSmapStreams.class.getPackage().getName());
	private long timestamp = -1;
	
	public ResyncSmapStreams(String path) throws Exception, InvalidNameException{
		super(path);
	}
	
	public void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		String resyncAttrStr = (String) exchange.getAttribute("resync");
		if(resyncAttrStr != null && resyncAttrStr.equalsIgnoreCase("true")){
			exchange.setAttribute("resync", "");
			//perform the resync
			JSONArray smapReportIds = database.getUniqueSmapReportIds();
			logger.info("Count(SmapReports)="+ smapReportIds.size());
			//for(int i=0; i<smapReportIds.size(); i++){
			for(int i=0; i<5; i++){
				String thisReportId = (String) smapReportIds.get(i);
				logger.info("Checking SmapReportId:" + thisReportId);
				boolean isBulk = database.isBulkReport(thisReportId);
			
				logger.info("SmapReportId:" + thisReportId + " isBulkReport:" + isBulk);
				//handle bulk reports
				if(isBulk){
					handleBulkReinstall(thisReportId);
				} else { //handle individual reports
					handleSmapReinstall(thisReportId);
				}
			}
			response.put("status", "success");
			sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
			
		} else {
			//return resync statistics
			sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
		}
		timestamp = (new Date()).getTime()/1000;
	}
	
	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 400, null, internalCall, internalResp);
	}
	
	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 400, null, internalCall, internalResp);
	}
	
	public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 400, null, internalCall, internalResp);
	}
	
	private void handleBulkReinstall(String thisReportId){
		//bulk smap url
		String assocBulkSmapUrl = database.getBulkReportSmapUrl(thisReportId);
		logger.info("associate bulk smap url: " + assocBulkSmapUrl);
		
		String smapRoot = assocBulkSmapUrl.substring(0, assocBulkSmapUrl.indexOf("data"));
		String reportsPath = smapRoot + "reporting/reports";
		String reports = SmapConnector.smapServerGet(reportsPath);
		if(reports != null){
			reports = "{\"array\":" + reports + "}";
			JSONObject reportsJSON = (JSONObject) JSONSerializer.toJSON(reports);
			JSONArray reportArray = reportsJSON.getJSONArray("array");
			
			if(!reportArray.contains(thisReportId)){
				logger.info("Updating Bulk Report (URL): " + assocBulkSmapUrl);
				String muxSmapReport = SmapConnector.smapServerGet(assocBulkSmapUrl);
				if(muxSmapReport != null){
					//install new report
					String newReportId = SmapConnector.installBulkReport(assocBulkSmapUrl, muxSmapReport);
					if(newReportId !=null){
						//update publisher and bulk report entries
						logger.info("oldId:" + thisReportId +" ,newId:" + newReportId);
						database.updatePublisherEntry(thisReportId, newReportId);
						database.deleteBulkReportById(thisReportId);
					}
				} else {
					logger.warning("Could not fetch Bulk Report(URL): " + assocBulkSmapUrl);
				}	
			}
		} else {
			logger.warning("Could not contact " + smapRoot);
		}
	}
	
	private void handleSmapReinstall(String thisReportId){
		JSONArray thisPubidArray = database.getAssocSmapReportPubIds(thisReportId);
		if(thisPubidArray.size()>0){
			UUID pubid = UUID.fromString((String)thisPubidArray.get(0));
			String assocSmapUrl = database.getSmapUrl(pubid);
		
			String smapRoot = assocSmapUrl.substring(0, assocSmapUrl.indexOf("data"));
			String reportsPath = smapRoot + "reporting/reports";
			String reports = SmapConnector.smapServerGet(reportsPath);
			if(reports != null){
				reports = "{\"array\":" + reports + "}";
				JSONObject reportsJSON = (JSONObject) JSONSerializer.toJSON(reports);
				JSONArray reportArray = reportsJSON.getJSONArray("array");
				String reportUri = database.getSmapReportPath(thisReportId);
			
				if(!reportArray.contains(thisReportId) && reportUri != null){
					logger.info("Updating Report (URL): " + assocSmapUrl);
					//install new report
					URL smapURL = null;
					try {
						smapURL = new URL(assocSmapUrl);
					} catch(Exception e){
						logger.log(Level.WARNING, "Invalid sMAP URL", e);
						return;
					}
					String newReportId = SmapConnector.installReport(assocSmapUrl, pubid, reportUri);
					if(newReportId !=null){
						//update publisher and smap report entries
						database.updatePublisherEntry(assocSmapUrl, newReportId);
					} else {
						logger.warning("Could not fetch Report(URL): " + assocSmapUrl);
					}	
				}
			} else {
				logger.warning("Count not contact " + smapRoot);
			}
		} else {
			logger.warning("Could not find pubid for smap report Id: " + thisReportId);
		}
	}
}




