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
 *  Any resource with a devices "folder" will have this folder's requests handled by
 *  the DevicesResource class.  DevicesResource handles requests adding devices and their associated
 *  publishers (streaming data sources).
 */
public class DevicesResource extends Resource{

	protected static transient Logger logger = Logger.getLogger(DevicesResource.class.getPackage().getName());
	private static MySqlDriver database = (MySqlDriver) DBAbstractionLayer.database;
	private static SmapConnector smapConnector = new SmapConnector();
	private static MetadataGraph metadataGraph = MetadataGraph.getInstance();

	public final int TYPE = ResourceUtils.DEVICES_RSRC;

	public DevicesResource(String path) throws Exception, InvalidNameException {
		super(path);	
		if(!path.endsWith("devices") && !path.endsWith("devices/")){
			throw new Exception("Invalid path uri suffix; must end with \"devices\"");
		}

		//set type
		database.setRRType(URI, ResourceUtils.translateType(TYPE).toLowerCase());
	}

	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		try {
			logger.info("PUT requested");
			JSONObject request = (JSONObject) JSONSerializer.toJSON(data);
			String op = request.optString("operation");
			if(op != null && !op.equals("") && op.equalsIgnoreCase("create_resource")){
				super.put(exchange, data, internalCall, internalResp);
				return;
			}
			JSONObject newIs4Uris = new JSONObject();
			boolean anySuccess = addNewDevice(request, errors, newIs4Uris);
			if(anySuccess)
				response.put("status", "success");
			else
				response.put("status", "fail");
			if(errors.size()>0)
				response.put("errors", errors);
			if(newIs4Uris != null && !newIs4Uris.toString().equals("{}")){
				setFormatting(newIs4Uris);
				response.put("new_publishers", newIs4Uris);
			}
			else{
				logger.info("newIs4Uris: " + newIs4Uris);
			}
			sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
		} catch(Exception e){
			response.put("status", "fail");
			errors.add("Invalid JSON Request");
			response.put("errors", errors);
			sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
		}
	}

	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		try{
			JSONObject request = null;
			if(data != null && data.length()>0)
				request = (JSONObject) JSONSerializer.toJSON(data);

			if(exchange.getAttribute("query") != null){
				super.query(exchange, data, internalCall, internalResp);
			} else {
				logger.info("POST " + this.URI + "\nDATA: " + data);
				
				try {
					JSONObject newIs4Uris = new JSONObject();
					boolean anySuccess = addPublishersToDevice(request, errors, newIs4Uris);
					if(anySuccess)
						response.put("status", "success");
					else
						response.put("status", "fail");
					if(errors.size()>0)
						response.put("errors", errors);
					if(newIs4Uris != null && !newIs4Uris.toString().equals("{}")){
						//update the properties for each publisher
						setFormatting(newIs4Uris);
						response.put("new_publishers", newIs4Uris);
					}
					logger.info("POST addPublisherToDevice Response: " + response.toString());
					sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
				} catch(Exception e){
					response.put("status", "fail");
					errors.add("Invalid JSON Request");
					response.put("errors", errors);
					sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
				}
			}
		} catch (Exception e){
			
			response.put("status", "fail");
			if(e instanceof JSONException)
				errors.add("Invalid JSON");
			sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
			logger.log(Level.WARNING, "", e);
		}
	}

	private boolean addNewDevice(JSONObject newDevReq, JSONArray errors, JSONObject newIs4Uris){
		try{
			if(newIs4Uris == null)
				newIs4Uris = new JSONObject();

			//Request should contain the device name and either
			//an array of publisher ids, an array of smap urls,
			//a single publisher id or a single smap_url
			String devName = newDevReq.getString("deviceName");
			if(devNameIsUnique(devName)){

				//optJSONArray return null if there is not value
				JSONArray pubids = newDevReq.optJSONArray("publishers");
				JSONArray smapurls = newDevReq.optJSONArray("smap_urls");
				JSONArray aliases = newDevReq.optJSONArray("aliases");

				//optString returns "" if there is no value
				String pubid = newDevReq.optString("publisher");
				String smapurl = newDevReq.optString("smap_url");
				String alias = newDevReq.optString("alias");

				//add aliases to alias array is alias array is not defined
				if(aliases == null && alias.length()>0){
					aliases = new JSONArray();
					aliases.add(alias);
				}

				//populate pubids array if the smapurls is undefined and the pubid is defined
				if(pubids == null && pubid.length()>0 && smapurls == null){
					pubids = new JSONArray();
					pubids.add(pubid);
				}

				//populate smapurls array if the pubids, pubid, 
				//and smapurls are not defined, but the smapurl is defined
				if(pubids == null && pubid.length()==0 && smapurls==null && smapurl.length()>0){
					smapurls = new JSONArray();
					smapurls.add(smapurl);
				}
			
				//handle all cases
				if(pubids != null && pubids.size()>0 && checkPubIds(pubids)){
					Resource deviceInstanceResource = new Resource(this.URI + devName + "/");
					RESTServer.addResource(deviceInstanceResource);

					//add to internal graph
					metadataGraph.addNode(this.URI + devName + "/");

					handlePubIdsReq(devName, pubids, aliases, errors, newIs4Uris);
					return true;
				} else if(smapurls != null && smapurls.size()>0){
					Resource deviceInstanceResource = new Resource(this.URI + devName + "/");
					RESTServer.addResource(deviceInstanceResource);
					
					//add to internal graph
					metadataGraph.addNode(this.URI + devName + "/");
					
					handleSmapUrlsReq(devName, smapurls, aliases, errors, newIs4Uris);
					
					return true;
				} else {
					errors.add("No publishers specified for device;  Must include publisher id(s) or smap url(s) in request");
				}
			} else {
				errors.add("Device name must be unique");
				return false;
			}
			
		} catch(Exception e){
			if(e instanceof JSONException)
				errors.add("Missing field: \"deviceName\"");
		}
		return false;
	}

	public static boolean addSmapPublishersToDevice(String is4DeviceUri, JSONObject newDevReq, JSONArray errors, JSONObject newIs4Uris){
		try{
			if(newIs4Uris == null)
				newIs4Uris = new JSONObject();

			String devName = newDevReq.getString("deviceName");
			if(database.getRRType(is4DeviceUri).equalsIgnoreCase(ResourceUtils.DEVICE_RSRC_STR)) {
				logger.info("Adding publishers to: " + devName);

				//optJSONArray return null if there is not value
				JSONArray smapurls = newDevReq.optJSONArray("smap_urls");
				JSONArray aliases = newDevReq.optJSONArray("aliases");

				//optString returns "" if there is no value
				String smapurl = newDevReq.optString("smap_url");
				String alias = newDevReq.optString("alias");

				//add aliases to alias array is alias array is not defined
				if(aliases == null && alias.length()>0){
					aliases = new JSONArray();
					aliases.add(alias);
				}

				//populate smapurls array if the pubids, pubid, 
				//and smapurls are not defined, but the smapurl is defined
				if(smapurls==null && smapurl.length()>0){
					smapurls = new JSONArray();
					smapurls.add(smapurl);
				}
			
				//handle all cases
				if(smapurls != null && smapurls.size()>0){
					handleSmapUrlsReqStat(is4DeviceUri, devName, smapurls, aliases, errors, newIs4Uris);
					return true;
				} else {
					errors.add("No publishers specified for device;  Must include smap url(s) in request");
				}
				return false;
			} else {
				errors.add("No device " + is4DeviceUri + " to add publishers into.");
				return false;
			}
			
		} catch(Exception e){
			if(e instanceof JSONException)
				errors.add("Missing field: \"deviceName\"");
		}
		return false;
	}

	private boolean addPublishersToDevice(JSONObject newDevReq, JSONArray errors, JSONObject newIs4Uris){
		try{
			if(newIs4Uris == null)
				newIs4Uris = new JSONObject();

			//Request should contain the device name and either
			//an array of publisher ids, an array of smap urls,
			//a single publisher id or a single smap_url
			String devName = newDevReq.getString("deviceName");
			if(!devNameIsUnique(devName)){
				logger.info("Adding publishers to: " + devName);

				//optJSONArray return null if there is not value
				JSONArray pubids = newDevReq.optJSONArray("publishers");
				JSONArray smapurls = newDevReq.optJSONArray("smap_urls");
				JSONArray aliases = newDevReq.optJSONArray("aliases");

				//optString returns "" if there is no value
				String pubid = newDevReq.optString("publisher");
				String smapurl = newDevReq.optString("smap_url");
				String alias = newDevReq.optString("alias");

				//add aliases to alias array is alias array is not defined
				if(aliases == null && alias.length()>0){
					aliases = new JSONArray();
					aliases.add(alias);
				}

				//populate pubids array if the smapurls is undefined and the pubid is defined
				if(pubids == null && pubid.length()>0 && smapurls == null){
					pubids = new JSONArray();
					pubids.add(pubid);
				}

				//populate smapurls array if the pubids, pubid, 
				//and smapurls are not defined, but the smapurl is defined
				if(pubids == null && pubid.length()==0 && smapurls==null && smapurl.length()>0){
					smapurls = new JSONArray();
					smapurls.add(smapurl);
				}
			
				//handle all cases
				if(pubids != null && pubids.size()>0 && checkPubIds(pubids)){
					handlePubIdsReq(devName, pubids, aliases, errors, newIs4Uris);
					return true;
				} else if(smapurls != null && smapurls.size()>0){
					handleSmapUrlsReq(devName, smapurls, aliases, errors, newIs4Uris);
					return true;
				} else {
					errors.add("No publishers specified for device;  Must include publisher id(s) or smap url(s) in request");
				}
			} else {
				errors.add("No device " + this.URI + devName + " to add publishers into.");
				return false;
			}
			
		} catch(Exception e){
			if(e instanceof JSONException)
				errors.add("Missing field: \"deviceName\"");
		}
		return false;
	}

	private void handlePubIdsReq(String deviceName, JSONArray pubids, JSONArray aliases, JSONArray errors, JSONObject newIs4Uris){
		try {
			if(newIs4Uris == null)
				newIs4Uris = new JSONObject();
			//create device resource with device name
			DevicesResource newDevice = new DevicesResource(this.URI + "/" + deviceName + "/");
			RESTServer.addResource(newDevice);

			//add to internal graph
			metadataGraph.addNode(this.URI + "/" + deviceName + "/");

			for(int i=0; i<pubids.size(); i++){
				UUID thisPubIdUUID = null;
				String thisAssocSmapUrl = null;
				if((thisPubIdUUID = convertToUUID((String)pubids.get(i))) != null){
					if((thisAssocSmapUrl=database.isSmapPublisher(thisPubIdUUID)) != null){
						//pubid is valid uuid and has an associated publisher
						//NOTES: make the bind exclusive?  not yet, but consider
						PublisherResource thisPublisher = null;
						String pubPath = null;
						if (i<aliases.size() && (aliases.get(i) != null)){
							pubPath = this.URI + "/" + deviceName + "/" + (String)aliases.get(i) + "/";
						} else {
							pubPath = this.URI + "/" + deviceName + "/" + thisPubIdUUID.toString() + "/";
																
						}
						thisPublisher = new PublisherResource(pubPath, thisPubIdUUID);
						RESTServer.addResource(thisPublisher);
						
						//adding to list of newly created Is4 resources
						newIs4Uris.put(thisPubIdUUID.toString(), pubPath);

						//bind the device to this context
						database.addDeviceEntry(deviceName, pubPath, thisPubIdUUID);

						//add to internal graph
						metadataGraph.addNode(pubPath);
					} else {
						//Unknown publisher
						errors.add("Unknown publisher: " + pubids.get(i));
					}
				} else {
					errors.add("Invalid format: " + (String)pubids.get(i));
				}
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}


	/**
	 * Make sure at least one pubid is valid.
	 */
	private boolean checkPubIds(JSONArray pubids){
		//make sure at least one pubid is valid
		String smapurl = null;
		for(int i=0; i<pubids.size(); i++){
			try {
				UUID pubuuid = UUID.fromString((String)pubids.get(i));
				if((smapurl = database.isSmapPublisher(pubuuid)) != null)
					return true;
			} catch(Exception e) {}
		}
		return false;
	}

	private boolean devNameIsUnique(String devName){
		JSONArray myChildren = database.rrGetChildren(URI);
		Vector<String> myChildrenVec = new Vector<String>(myChildren);
		for(int i=0; i<myChildrenVec.size(); i++)
			System.out.println(i +"\t" + myChildrenVec.elementAt(i));
		//System.out.println(!myChildrenVec.contains(devName));
		return !myChildrenVec.contains(devName);
	}

	private static boolean devNameIsUniqueStat(String is4DeviceUri, String devName){
		logger.info("is4DeviceUri: " + is4DeviceUri + ", devName: " + devName);
		//get the parent of this Device Uri
		//String parentUri = is4DeviceUri.substring(0, is4DeviceUri.lastIndexOf(devName));

		JSONArray myChildren = database.rrGetChildren(is4DeviceUri);
		Vector<String> myChildrenVec = new Vector<String>(myChildren);
		for(int i=0; i<myChildrenVec.size(); i++)
			System.out.println(i +"\t" + myChildrenVec.elementAt(i));
		//System.out.println(!myChildrenVec.contains(devName));
		return !myChildrenVec.contains(devName);
	}

	private void handleSmapUrlsReq(String deviceName, JSONArray smapurls, JSONArray aliases, JSONArray errors, JSONObject newIs4Uris){
		try {
			JSONObject r = resolveAllSmapUris(deviceName, smapurls, errors);
			if(r != null){
				logger.info("Resolved all smap urls: " + r.toString());
				newIs4Uris.accumulateAll(r);
			}
			newIs4Uris.accumulateAll(regSmapUrls(deviceName, smapurls, aliases, errors, true, null));
		} catch (Exception e){
			logger.log(Level.WARNING, "",e);
		}
	}

	private static void handleSmapUrlsReqStat(String is4DevUri, String deviceName, JSONArray smapurls, JSONArray aliases, JSONArray errors, JSONObject newIs4Uris){
		try {
			JSONObject r = resolveAllSmapUrisStat(is4DevUri, deviceName, smapurls, errors);
			if(r != null){
				logger.info("Resolved all smap urls: " + r.toString());
				newIs4Uris.accumulateAll(r);
			}
			newIs4Uris.accumulateAll(regSmapUrlsStat(is4DevUri, deviceName, smapurls, aliases, errors, true, null));
		} catch (Exception e){
			logger.log(Level.WARNING, "",e);
		}
	}


	/**
	 * Takes each smap url in the JSONArray and creates a publisher for it.  The smap urls
	 * in the array cannot contain stars.
	 * @param smapUrls Full urls to a smap source
	 * 		Example: 	http://local.cs.berkeley.edu:8005/basement-1/elt-B/data/A/sensor/displacement_pf/reading
	 * 
	 * @param aliases A human-readable name for the resource that references this publisher
	 *  		
	 * @param errors A json array that is populated with errors, while processing, if there are any
	 * 
	 * @param installReport true, if you want to install a report for each smapUrl, false otherwise
	 *
	 * @return A JSONObject where the attributes are publisher ids for the newly created publishers and the associated
	 *		values are the newly create Is4 Uris.
	 */
	public static JSONObject regSmapUrlsStat(String is4DeviceUri, String deviceName, JSONArray smapurls, JSONArray aliases, JSONArray errors, boolean installReport,
			String bulkReportId){

		JSONObject is4PubsAdded = new JSONObject();
		try{
			for(int i=0; i<smapurls.size(); i++){
				String thisAssocSmapUrl = (String) smapurls.get(i);
				
				if(!thisAssocSmapUrl.contains("*")){
					UUID thisPubIdUUID = null;
					Registrar registrar = Registrar.registrarInstance();
					
					if((thisPubIdUUID=database.isPublisher(thisAssocSmapUrl, true)) == null){
						//register the publisher
						String newPubId = registrar.registerDevice(thisAssocSmapUrl);
						try{
							thisPubIdUUID = UUID.fromString(newPubId);
						} catch(Exception e){
							logger.log(Level.WARNING, "", e);
						}
					}
	
					PublisherResource thisPublisher = null;
					String pubPath = null;
					String slash = "";
					String newAlias = null;
					if(!is4DeviceUri.endsWith("/"))
						slash = "/";
					if (aliases != null && i<aliases.size() && (aliases.get(i) != null)){
						//System.out.println("###############  i= " + i + "size= " + aliases.size());
						newAlias = (String) aliases.get(i);
						pubPath = is4DeviceUri +  slash + (String)aliases.get(i) + "/";
					} else {
						//System.out.println("###############Constructing  PUB_PATH with pubid:" + thisPubIdUUID.toString());
						newAlias = generateNewAlias(is4DeviceUri, thisAssocSmapUrl);
						
						if(newAlias != null){
							pubPath = is4DeviceUri + slash + newAlias + "/";
						} else {
							newAlias = thisPubIdUUID.toString();
							pubPath = is4DeviceUri + slash + thisPubIdUUID.toString() + "/";
						}
															
					}
					logger.info("PubPath: " + pubPath);
					
					if(installReport){
						logger.info("Installing Report for publisher: (" + pubPath + ", " + 
								thisPubIdUUID.toString() + ")");
						//Install smap report
						String reportId = SmapConnector.installReport(thisAssocSmapUrl, thisPubIdUUID, pubPath);
						if(reportId != null){
							logger.info("PUB_PATH: " + pubPath);
							thisPublisher = new PublisherResource(pubPath, thisPubIdUUID);
							RESTServer.addResource(thisPublisher);

							//add to JSONObject
							is4PubsAdded.put(thisPubIdUUID.toString(), pubPath);
			
							//add to smap publishers table
							database.addPublisher(thisPubIdUUID, newAlias, thisAssocSmapUrl, pubPath, reportId);

							//bind the device to this context
							database.addDeviceEntry(deviceName, pubPath, thisPubIdUUID);

							//add to internal graph
							metadataGraph.addNode(pubPath);
						} else {
							//if registration failed, unregister this publisher
							registrar.unregisterDevice(thisPubIdUUID.toString());
						}
					} else {
						if(bulkReportId != null) {
							logger.info("PUB_PATH: " + pubPath);
							thisPublisher = new PublisherResource(pubPath, thisPubIdUUID);
							RESTServer.addResource(thisPublisher);

							//add to JSONObject
							is4PubsAdded.put(thisPubIdUUID.toString(), pubPath);

							//add to smap publishers table
							database.addBulkPublisher(thisPubIdUUID, newAlias, thisAssocSmapUrl, pubPath, bulkReportId);
			
							//bind the device to this context
							database.addDeviceEntry(deviceName, pubPath, thisPubIdUUID);

							//add to internal graph
							metadataGraph.addNode(pubPath);
						} else {
							logger.warning("Bulk Report Id is NULL for " + pubPath);
						}
					}
				}
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		return is4PubsAdded;
	}

	
	/**
	 * Takes each smap url in the JSONArray and creates a publisher for it.  The smap urls
	 * in the array cannot contain stars.
	 * @param smapUrls Full urls to a smap source
	 * 		Example: 	http://local.cs.berkeley.edu:8005/basement-1/elt-B/data/A/sensor/displacement_pf/reading
	 * 
	 * @param aliases A human-readable name for the resource that references this publisher
	 *  		
	 * @param errors A json array that is populated with errors, while processing, if there are any
	 * 
	 * @param installReport true, if you want to install a report for each smapUrl, false otherwise
	 *
	 * @return A JSONObject where the attributes are publisher ids for the newly created publishers and the associated
	 *		values are the newly create Is4 Uris.
	 */
	public JSONObject regSmapUrls(String deviceName, JSONArray smapurls, JSONArray aliases, JSONArray errors, boolean installReport,
			String bulkReportId){

		JSONObject is4PubsAdded = new JSONObject();
		try{
			for(int i=0; i<smapurls.size(); i++){
				String thisAssocSmapUrl = (String) smapurls.get(i);
				
				if(!thisAssocSmapUrl.contains("*")){
					UUID thisPubIdUUID = null;
					Registrar registrar = Registrar.registrarInstance();
					
					if((thisPubIdUUID=database.isPublisher(thisAssocSmapUrl, true)) == null){
						//register the publisher
						String newPubId = registrar.registerDevice(thisAssocSmapUrl);
						try{
							thisPubIdUUID = UUID.fromString(newPubId);
						} catch(Exception e){
							logger.log(Level.WARNING, "", e);
						}
					}
	
					PublisherResource thisPublisher = null;
					String pubPath = null;
					String slash = "";
					String newAlias = null;
					if(!this.URI.endsWith("/"))
						slash = "/";
					if (aliases != null && i<aliases.size() && (aliases.get(i) != null)){
						//System.out.println("###############  i= " + i + "size= " + aliases.size());
						newAlias = (String) aliases.get(i);
						pubPath = this.URI + slash + deviceName + "/" + (String)aliases.get(i) + "/";
					} else {
						//System.out.println("###############Constructing  PUB_PATH with pubid:" + thisPubIdUUID.toString());
						newAlias = generateNewAlias(thisAssocSmapUrl);
						if(newAlias != null){
							pubPath = this.URI + slash + deviceName + "/" + newAlias + "/";
						} else {
							newAlias = thisPubIdUUID.toString();
							pubPath = this.URI + slash + deviceName + "/" + thisPubIdUUID.toString() + "/";
						}
															
					}
					logger.info("PubPath: " + pubPath);
					
					if(installReport){
						logger.info("Installing Report for publisher: (" + pubPath + ", " + 
								thisPubIdUUID.toString() + ")");
						//Install smap report
						String reportId = smapConnector.installReport(thisAssocSmapUrl, thisPubIdUUID, pubPath);
						if(reportId != null){

							logger.info("PUB_PATH: " + pubPath);
							//add to smap publishers table
							database.addPublisher(thisPubIdUUID, newAlias, thisAssocSmapUrl, pubPath, reportId);

							//create publisher resource
							thisPublisher = new PublisherResource(pubPath, thisPubIdUUID);
							RESTServer.addResource(thisPublisher);

							//add to JSONObject
							is4PubsAdded.put(thisPubIdUUID.toString(), pubPath);
			
							//bind the device to this context
							database.addDeviceEntry(deviceName, pubPath, thisPubIdUUID);

							//add to internal graph
							metadataGraph.addNode(pubPath);
						} else {
							//if registration failed, unregister this publisher
							registrar.unregisterDevice(thisPubIdUUID.toString());
						}
					} else {
						if(bulkReportId != null) {
							logger.info("PUB_PATH: " + pubPath);
							thisPublisher = new PublisherResource(pubPath, thisPubIdUUID);
							RESTServer.addResource(thisPublisher);

							//add to JSONObject
							is4PubsAdded.put(thisPubIdUUID.toString(), pubPath);

							//add to smap publishers table
							database.addBulkPublisher(thisPubIdUUID, newAlias, thisAssocSmapUrl, pubPath, bulkReportId);
			
							//bind the device to this context
							database.addDeviceEntry(deviceName, pubPath, thisPubIdUUID);

							//add to internal graph
							metadataGraph.addNode(pubPath);
						} else {
							logger.warning("Bulk Report Id is NULL for " + pubPath);
						}
					}
				}
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}

		return is4PubsAdded;
	}

	private static String generateNewAlias(String is4DeviceUri, String smapUrlStr){
		String alias = null;
		try{
			//1.	Extract the uri from the url
			URL smapURL = new URL(smapUrlStr);
			String smapUri = smapURL.getPath();
			String testName = "";
			String endPath = "";
			
			if(smapUri.contains("data")){
				//get the sense-point
				String sensePointName = null;
				
				//2.	Check if this is a "sensor" resource or a "meter" resource (it should be)
				if(smapUri.contains("sensor")){
					sensePointName = smapUri.substring(smapUri.indexOf("data")+5, smapUri.indexOf("sensor")-1);
					logger.info("Sensepoint: " + sensePointName);
					endPath = smapUri.substring(smapUri.indexOf("sensor")+6, smapUri.length());
					logger.info("Endpath: " + endPath);
					testName = sensePointName + "_sensor_";
				} else if(smapUri.contains("meter")){
					sensePointName = smapUri.substring(smapUri.indexOf("data")+5, smapUri.indexOf("meter")-1);
					logger.info("Sensepoint: " + sensePointName);
					endPath = smapUri.substring(smapUri.indexOf("meter")+5, smapUri.length());
					logger.info("Endpath: " + endPath);
					testName = sensePointName + "_meter_";
				} else if (smapUri.contains("control")){
					sensePointName = smapUri.substring(smapUri.indexOf("data")+7, smapUri.indexOf("control")-1);
					logger.info("Sensepoint: " + sensePointName);
					endPath = smapUri.substring(smapUri.indexOf("control")+7, smapUri.length());
					logger.info("Endpath: " + endPath);
					testName = sensePointName + "_control_";
				} else if (smapUri.contains("status")){
				} else if(smapUri.contains("reporting")){
				} else if(smapUri.contains("context")){
				}
			}

			//extract the channel name
			if(!endPath.equals("") && endPath.contains("/")){
					StringTokenizer tok = new StringTokenizer(endPath, "/");
					String channelName = tok.nextToken();
					testName = testName + channelName;
					if(devNameIsUniqueStat(is4DeviceUri,testName))
						alias = testName;
			} else {
				int count =0;
				String streamName = "stream_" + count;
				while(!devNameIsUniqueStat(is4DeviceUri, streamName)){
					count +=1;
					streamName = "stream_" + count;
				}
				alias = streamName;
			}
			
			//3.	If it's either, add a unique integer at the end of sensor/meter string and 
			//		set that as the alias
			//4.	If it's neither a sensor/meter resource, just give it the pubid as the name
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		logger.info("New Alias: " + alias);
		return alias;
	}


	private String generateNewAlias(String smapUrlStr){
		String alias = null;
		try{
			//1.	Extract the uri from the url
			URL smapURL = new URL(smapUrlStr);
			String smapUri = smapURL.getPath();
			String testName = "";
			String endPath = "";
			
			if(smapUri.contains("data")){
				//get the sense-point
				String sensePointName = null;
				
				//2.	Check if this is a "sensor" resource or a "meter" resource (it should be)
				if(smapUri.contains("sensor")){
					sensePointName = smapUri.substring(smapUri.indexOf("data")+5, smapUri.indexOf("sensor")-1);
					logger.info("Sensepoint: " + sensePointName);
					endPath = smapUri.substring(smapUri.indexOf("sensor")+6, smapUri.length());
					logger.info("Endpath: " + endPath);
					testName = sensePointName + "_sensor_";
				} else if(smapUri.contains("meter")){
					sensePointName = smapUri.substring(smapUri.indexOf("data")+5, smapUri.indexOf("meter")-1);
					logger.info("Sensepoint: " + sensePointName);
					endPath = smapUri.substring(smapUri.indexOf("meter")+5, smapUri.length());
					logger.info("Endpath: " + endPath);
					testName = sensePointName + "_meter_";
				} else if (smapUri.contains("control")){
					sensePointName = smapUri.substring(smapUri.indexOf("data")+7, smapUri.indexOf("control")-1);
					logger.info("Sensepoint: " + sensePointName);
					endPath = smapUri.substring(smapUri.indexOf("control")+7, smapUri.length());
					logger.info("Endpath: " + endPath);
					testName = sensePointName + "_control_";
				} else if (smapUri.contains("status")){
				} else if(smapUri.contains("reporting")){
				} else if(smapUri.contains("context")){
				}
			}

			//extract the channel name
			if(!endPath.equals("") && endPath.contains("/")){
					StringTokenizer tok = new StringTokenizer(endPath, "/");
					String channelName = tok.nextToken();
					testName = testName + channelName;
					if(devNameIsUnique(testName))
						alias = testName;
			} else {
				int count =0;
				String streamName = "stream_" + count;
				while(!devNameIsUnique(streamName)){
					count +=1;
					streamName = "stream_" + count;
				}
				alias = streamName;
			}
			
			//3.	If it's either, add a unique integer at the end of sensor/meter string and 
			//		set that as the alias
			//4.	If it's neither a sensor/meter resource, just give it the pubid as the name
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		logger.info("New Alias: " + alias);
		return alias;
	}

	/**
	 *  Resolves all the smap urls that contains stars.  Updates the input JSONArray by removing
	 *  all the starred urls.  These are handled through the "/is4/pub/smap/demux" resource.
	 *  When a bulk smap report is POSTed to this resource it is demultiplexed and tagged according
	 *  to the corresponding publisher.
	 */
	private static JSONObject resolveAllSmapUrisStat(String is4DeviceUri, String deviceName, JSONArray smapurls, JSONArray errors){
		
		Vector<String> strsToRemove = new Vector<String>(smapurls.size());

		Vector<String> starredUrlsVec = new Vector<String>();
		Vector<String> muxStreamMsgsVec =  new Vector<String>();
		Vector<Vector<String>> resolvedUrls = new Vector<Vector<String>>();

		int i;
		
		for(i=0; i<smapurls.size(); i++){
			String thisSmapUrlStr = (String) smapurls.get(i);
			if(thisSmapUrlStr.contains("*")){
				strsToRemove.add(thisSmapUrlStr);

				try {
					String muxStreamMsg = SmapConnector.smapServerGet(thisSmapUrlStr);
					URL smapURLObj = new URL(thisSmapUrlStr);
					String smapHost = smapURLObj.getHost();
					int port = (smapURLObj.getPort()<0)?80:smapURLObj.getPort();
					String uriStr = smapURLObj.getPath();
					if(muxStreamMsg != null){

						JSONObject muxStreamMsgJObj = (JSONObject) JSONSerializer.toJSON(muxStreamMsg);
						//populate the starred uri vector and the muxstream vector
						starredUrlsVec.add(thisSmapUrlStr);
						muxStreamMsgsVec.add(muxStreamMsg);
						JSONObject resSmapUris = SmapConnector.resolveSmapUri(uriStr, muxStreamMsgJObj);
						Iterator keys = resSmapUris.keys();
						Vector<String> theseResVec = new Vector<String>();

						while(keys.hasNext()){
							String resSmapUrl = "http://" + smapHost + ":" + port + ((String) keys.next());
							theseResVec.add(resSmapUrl);
						}
						resolvedUrls.add(theseResVec);
					} else {
						errors.add("Could not contact: " + thisSmapUrlStr);
					}
				}catch(Exception e){
					logger.log(Level.WARNING, "",e);
				}
			}
		}
		
		//remove all starred smap urls from inputted array
		for(i=0; i<strsToRemove.size(); i++){
			smapurls.remove(((String)strsToRemove.elementAt(i)));
		}


		JSONObject newIs4Uris = new JSONObject();
		//then install bulk report for that url
		for(i=0; i<starredUrlsVec.size(); i++){
			String reportId = SmapConnector.installBulkReport(starredUrlsVec.elementAt(i), muxStreamMsgsVec.elementAt(i));
			if(reportId != null) {
				Vector<String> resUrls = resolvedUrls.elementAt(i);

				//create a publisher for each smap url that the starred url resolved to --
				JSONArray uriList = new JSONArray();
				JSONArray aliases = new JSONArray();
				uriList.addAll(resUrls);

				//but do not install reports for them since we already installed a bulk report for all of them. 
				newIs4Uris = regSmapUrlsStat(is4DeviceUri, deviceName, uriList, aliases, errors, false, reportId);
			}
		}

		return newIs4Uris;
	}

	/**
	 *  Resolves all the smap urls that contains stars.  Updates the input JSONArray by removing
	 *  all the starred urls.  These are handled through the "/is4/pub/smap/demux" resource.
	 *  When a bulk smap report is POSTed to this resource it is demultiplexed and tagged according
	 *  to the corresponding publisher.
	 */
	private JSONObject resolveAllSmapUris(String deviceName, JSONArray smapurls, JSONArray errors){
		
		Vector<String> strsToRemove = new Vector<String>(smapurls.size());

		Vector<String> starredUrlsVec = new Vector<String>();
		Vector<String> muxStreamMsgsVec =  new Vector<String>();
		Vector<Vector<String>> resolvedUrls = new Vector<Vector<String>>();

		int i;
		
		for(i=0; i<smapurls.size(); i++){
			String thisSmapUrlStr = (String) smapurls.get(i);
			if(thisSmapUrlStr.contains("*")){
				strsToRemove.add(thisSmapUrlStr);

				try {
					String muxStreamMsg = SmapConnector.smapServerGet(thisSmapUrlStr);
					URL smapURLObj = new URL(thisSmapUrlStr);
					String smapHost = smapURLObj.getHost();
					int port = (smapURLObj.getPort()<0)?80:smapURLObj.getPort();
					String uriStr = smapURLObj.getPath();
					if(muxStreamMsg != null){

						JSONObject muxStreamMsgJObj = (JSONObject) JSONSerializer.toJSON(muxStreamMsg);
						//populate the starred uri vector and the muxstream vector
						starredUrlsVec.add(thisSmapUrlStr);
						muxStreamMsgsVec.add(muxStreamMsg);
						JSONObject resSmapUris = SmapConnector.resolveSmapUri(uriStr, muxStreamMsgJObj);
						Iterator keys = resSmapUris.keys();
						Vector<String> theseResVec = new Vector<String>();

						while(keys.hasNext()){
							String resSmapUrl = "http://" + smapHost + ":" + port + ((String) keys.next());
							theseResVec.add(resSmapUrl);
						}
						resolvedUrls.add(theseResVec);
					} else {
						errors.add("Could not contact: " + thisSmapUrlStr);
					}
				}catch(Exception e){
					logger.log(Level.WARNING, "",e);
				}
			}
		}
		
		//remove all starred smap urls from inputted array
		for(i=0; i<strsToRemove.size(); i++){
			smapurls.remove(((String)strsToRemove.elementAt(i)));
		}


		JSONObject newIs4Uris = new JSONObject();
		//then install bulk report for that url
		for(i=0; i<starredUrlsVec.size(); i++){
			String reportId = SmapConnector.installBulkReport(starredUrlsVec.elementAt(i), muxStreamMsgsVec.elementAt(i));
			if(reportId != null) {
				Vector<String> resUrls = resolvedUrls.elementAt(i);

				//create a publisher for each smap url that the starred url resolved to --
				JSONArray uriList = new JSONArray();
				JSONArray aliases = new JSONArray();
				uriList.addAll(resUrls);

				//but do not install reports for them since we already installed a bulk report for all of them. 
				newIs4Uris = regSmapUrls(deviceName, uriList, aliases, errors, false, reportId);
			}
		}

		return newIs4Uris;
	}

	private void setFormatting(JSONObject newIs4Uris){
		try{
			//update the properties for each publisher
			Iterator keys = newIs4Uris.keys();
			while(keys.hasNext()){
				String thisPubid = (String) keys.next();
				UUID thisPubidUUID = UUID.fromString(thisPubid);
				String smapUrlStr = database.getSmapUrl(thisPubidUUID);
				if(smapUrlStr.contains("reading")){

					//replace /reading with /formatting and fetch formatting information
					smapUrlStr = smapUrlStr.replace("reading", "formatting");
					String formatStr = SmapConnector.smapServerGet(smapUrlStr);

					if(formatStr != null && !formatStr.equals("")){
						//get current properties and update them in the database
						String thisIs4UriStr = newIs4Uris.getString(thisPubid);
						JSONObject propsJObj = database.rrGetProperties(thisIs4UriStr);
						propsJObj.put("formatting", formatStr);

						logger.info("Updating properties for: " + thisIs4UriStr);

						//update the properties for this resource in the database
						//database.rrPutProperties(thisIs4UriStr, propsJObj);
						updateProperties(propsJObj);
					}
				}
			}
		}catch(Exception e){
			logger.warning("Error updating properties for newly added sMAP publishers");
		}
	}

	
	private UUID convertToUUID(String pubid){
		UUID pubid_uuid = null;
		try {
			if(pubid != null)
				pubid_uuid = UUID.fromString(pubid);
		} catch(Exception e){
		}
		return pubid_uuid;
	}

	private void removeDevice(JSONObject remDevReq, JSONArray errors){
		try{
		} catch(Exception e){
		}
	}
}
