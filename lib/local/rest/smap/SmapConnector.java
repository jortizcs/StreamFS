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
 * StreamFS release version 2.2
 */

package local.rest.smap;

import local.db.*;
import java.util.*;
import java.net.*;
import net.sf.json.*;
import java.io.*;
import java.util.logging.*;

public class SmapConnector extends TimerTask{
	private static Logger logger = Logger.getLogger(SmapConnector.class.getPackage().getName());
	private static MySqlDriver database = (MySqlDriver)DBAbstractionLayer.database;
	private static boolean timerSet = false;
	private static String is4_hostname = "184.106.204.181";
	private static int is4_port = 8080;
	private static boolean sfsInfoSet = false;

	public SmapConnector(){
		super();
		reschedule();
		setSFSConnInfo();
	}
	
	protected static void setSFSConnInfo(){
		if(!sfsInfoSet){
			String hostname = System.getenv().get("IS4_HOSTNAME");
			String portStr = System.getenv().get("IS4_PORT");
		
			if(hostname != null && portStr != null){
				is4_hostname = hostname;
				try {
					is4_port = Integer.parseInt(portStr);
				} catch(Exception e){
					logger.log(Level.WARNING, portStr + " not a valid Integer value", e);
				}
			}
			
			sfsInfoSet = true;
		}
	}

	public static String smapServerPost(String smapUrl, JSONObject postData){
		setSFSConnInfo();
		InputStream in = null;
		OutputStream out = null;
		if(smapUrl != null && postData != null){
			try{
				URL smapUrlObj = new URL(smapUrl);
				URLConnection smapConn = smapUrlObj.openConnection();
				smapConn.setConnectTimeout(5000);
				smapConn.setDoOutput(true);
				smapConn.setUseCaches(false);
				smapConn.setRequestProperty("Connection", "close");
				smapConn.connect();

				out = smapConn.getOutputStream();
				OutputStreamWriter wr = new OutputStreamWriter(out);
				wr.write(postData.toString());
				wr.flush();

				//POST reply
				in =  smapConn.getInputStream();
				InputStreamReader ir = new InputStreamReader(in);
				BufferedReader reader = new BufferedReader(ir);
				StringBuffer lineBuffer = new StringBuffer();
				String line = null;
				while((line = reader.readLine()) != null)
					lineBuffer.append(line);
				line = lineBuffer.toString();

				//get set for gc cleanup
				ir = null;
				lineBuffer = null;
				wr = null;
				smapConn = null;
				smapUrlObj = null;

				return line;
				
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
				return null;
			} finally {
				try {
					if(out !=null)
						out.close();
				} catch (Exception e){
					logger.log(Level.WARNING, "Problems closing smapServerPost outputstream", e);
				}
				out=null;

				try {
					if(in!=null)
						in.close();
				} catch (Exception e){
					logger.log(Level.WARNING, "Problems closing smapServerPost inputstream", e);
				}
				in=null;
			}
		}
		logger.warning("smapUrl and/or postData is null");
		return null;
	}

	/**
	 * Does an HTTP GET to the smapUrl.
	 * @return The reply received from the smap server.
	 */
	public static String smapServerGet(String smapUrl){
		setSFSConnInfo();
		InputStream in = null;
		OutputStream out = null;
		if(smapUrl != null){
			try{
				URL smapUrlObj = new URL(smapUrl);
				URLConnection smapConn = smapUrlObj.openConnection();
				smapConn.setConnectTimeout(5000);
				smapConn.setUseCaches(false);
				smapConn.setRequestProperty("Connection", "close");
				smapConn.connect();

				//GET reply
				in =  smapConn.getInputStream();
				InputStreamReader ir = new InputStreamReader(in);
				BufferedReader reader = new BufferedReader(ir);
				StringBuffer lineBuffer = new StringBuffer();
				String line = null;
				while((line = reader.readLine()) != null)
					lineBuffer.append(line);
				line = lineBuffer.toString();

				//get set for gc cleanup
				ir = null;
				lineBuffer = null;
				smapConn = null;
				smapUrlObj = null;
				
				reader.close();
				return line;
				
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
				return null;
			} finally {
				try {
					if(out !=null)
						out.close();
				} catch (Exception e){
					logger.log(Level.WARNING, "Problems closing smapServerGet outputstream", e);
				}
				out=null;

				try {
					if(in!=null)
						in.close();
				} catch (Exception e){
					logger.log(Level.WARNING, "Problems closing smapServerGet inputstream", e);
				}
				in=null;
			}
		}
		logger.warning("smapUrl and/or postData is null");
		return null;
	}

	public static String smapServerDelete(String smapUrl){
		setSFSConnInfo();
		InputStream in = null;
		OutputStream out = null;
		boolean okresp = false;
		if(smapUrl != null){
			try{
				URL smapUrlObj = new URL(smapUrl);
				logger.info("DELETE " + smapUrlObj.toString());
				HttpURLConnection smapConn = (HttpURLConnection)smapUrlObj.openConnection();
				smapConn.setConnectTimeout(5000);
				smapConn.setRequestMethod("DELETE");
				smapConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				smapConn.setUseCaches(false);
				smapConn.setRequestProperty("Connection", "close");
				smapConn.connect();

				//GET reply
				if(smapConn.getResponseCode()==HttpURLConnection.HTTP_OK){
					okresp=true;
				}
				in = smapConn.getInputStream();
				InputStreamReader ir = new InputStreamReader(in);
				BufferedReader reader = new BufferedReader(ir);
				StringBuffer lineBuffer = new StringBuffer();
				String line = null;
				while((line = reader.readLine()) != null)
					lineBuffer.append(line);
				line = lineBuffer.toString();
				reader.close();
				
				if((line==null || line.equals("")) && okresp){
					line="{}";
				}

				//get set for gc cleanup
				ir = null;
				lineBuffer = null;
				smapConn = null;
				smapUrlObj = null;

				return line;
				
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
				return null;
			} finally {
				try {
					if(out !=null)
						out.close();
				} catch (Exception e){
					logger.log(Level.WARNING, "Problems closing smapServerGet outputstream", e);
				}
				out=null;

				try {
					if(in!=null)
						in.close();
				} catch (Exception e){
					logger.log(Level.WARNING, "Problems closing smapServerGet inputstream", e);
				}
				in=null;
			}
		}
		logger.warning("smapUrl and/or postData is null");
		return null;
	}

	public static boolean deleteReport(UUID pubid, String rrPath){
		setSFSConnInfo();
		String dbSmapUrl = null;
		String smapReportId = null;
		try{
			//get report id for this publisher
			if((dbSmapUrl = database.isSmapPublisher(pubid)) != null &&
				(smapReportId = database.getSmapReportId(pubid)) != null &&
				!database.isBulkReport(smapReportId))  {
				
				//send delete report to smap server
				URL smapUrlObj = new URL(dbSmapUrl);
				String baseUrlStr = smapUrlObj.getHost();
				int basePort = (smapUrlObj.getPort()>0)?smapUrlObj.getPort():80;
				String basePortStr = (new Integer(basePort)).toString();
				String baseUri = smapUrlObj.getPath();
				logger.info("Sending delete to: http://" + baseUrlStr + ":" + basePortStr + baseUri.substring(0, baseUri.indexOf("data")) +
						"reporting/reports/" + smapReportId);
				smapUrlObj = new URL("http://" + baseUrlStr + ":" + basePortStr + baseUri.substring(0, baseUri.indexOf("data")) +
						"reporting/reports/" + smapReportId);

				String deleteResp = SmapConnector.smapServerDelete(smapUrlObj.toString());
										
				if(deleteResp !=null && deleteResp.equals("{}")){
					logger.info("Deleted report " + smapReportId + 
							" from " + dbSmapUrl + " for pubid [" + pubid.toString() +
							"] successfully");
					return true;
				}
				else {
					logger.warning("Could not delete report " + smapReportId + 
							" from " + dbSmapUrl + " for pubid [" + pubid.toString() +
							"]");
				}
				//gc cleanup setup
				smapUrlObj=null;
				return false;
	
			} else if(dbSmapUrl != null && smapReportId != null && database.isBulkReport(smapReportId)){
				int pubCount = database.publisherCount(smapReportId);
				if(pubCount <2) {  //1 or 0
					//send delete report to smap server
					URL smapUrlObj = new URL(dbSmapUrl);
					String baseUrlStr = smapUrlObj.getHost();
					int basePort = (smapUrlObj.getPort()>0)?smapUrlObj.getPort():80;
					String baseUri = smapUrlObj.getPath();
					String basePortStr = (new Integer(basePort)).toString();
					logger.info("Sending delete to: http://" + baseUrlStr + ":" + basePortStr + baseUri.substring(0, baseUri.indexOf("data")) +
							"/reporting/reports/" + smapReportId);
					smapUrlObj = new URL("http://" + baseUrlStr + ":" + basePortStr + baseUri.substring(0, baseUri.indexOf("data"))+
						       	"reporting/reports/" + smapReportId);

					String deleteResp = SmapConnector.smapServerDelete(smapUrlObj.toString());
											
					if(deleteResp !=null && deleteResp.equals("{}")){
						database.deleteBulkReportById(smapReportId);
						logger.info("Deleted report " + smapReportId + 
								" from " + dbSmapUrl + " for pubid [" + pubid.toString() +
								"] successfully");
						return true;
					}
					else {
						logger.warning("Could not delete report " + smapReportId + 
								" from " + dbSmapUrl + " for pubid [" + pubid.toString() +
								"]");
					}
					//gc cleanup setup
					smapUrlObj=null;
					return false;
				}
			}

			logger.warning("Invalid Pubid or SmapReportId:  Could not delete report " + smapReportId + 
							" from " + dbSmapUrl + " for pubid [" + pubid+
							"]");
			return false;
				
		} catch(Exception e){
			//logger.log(Level.WARNING, "Could not delete report " + smapReportId + 
			//				" from " + dbSmapUrl + " for pubid [" + pubid.toString() +
			//				"]", e);
			logger.log(Level.WARNING, "", e);
		}

		return false;
	}

	public static String installBulkReport(String smapUrl, String muxStreamMsg){

		setSFSConnInfo();
		String reportId = null;
		InputStream in=null;
		OutputStream out=null;
		try{
			URL smapUrlObj = new URL(smapUrl);

			//construct report
			JSONObject report = new JSONObject();
			report.put("ReportResource", smapUrlObj.getPath());
			report.put("ReportDeliveryLocation", "http://" + is4_hostname + ":" + is4_port +
					"/is4/pub/smap/demux?type=smap&smapurl=" + smapUrl);
			report.put("Period", 0);
			
			String rootResource = null;
			int index = smapUrl.indexOf("data");
			if(index <=0){
				//delete database entry in publishers table
				//delete database entry in rest_resources table
				return null;
			}
			rootResource = smapUrl.substring(0, index);
			//POST the report
			URL smapServer = new URL(rootResource + "reporting/create");
			URLConnection smapConn = smapServer.openConnection();
			smapConn.setConnectTimeout(5000);
			smapConn.setDoOutput(true);
			out= smapConn.getOutputStream();
			OutputStreamWriter wr = new OutputStreamWriter(out);
			wr.write(report.toString());
			wr.flush();
			
			
			//GET reply
			in = smapConn.getInputStream();
			InputStreamReader ir = new InputStreamReader(in);
			BufferedReader reader = new BufferedReader(ir);
			StringBuffer lineBuffer = new StringBuffer();
			String line = null;
			while((line = reader.readLine()) != null)
				lineBuffer.append(line);
			line = lineBuffer.toString();
			reader.close();
			logger.info("SMAP_REPLY: " + line);
			JSONObject reply = new JSONObject();
			if(line.length()>0)
				reply.put("response", line);
			JSONArray reportIdArray = reply.optJSONArray("response");
			if(reportIdArray != null)
				reportId = (String)reportIdArray.get(0);
			if(reportId != null && reportId.length()>0){
				logger.info("Updating entry for " + smapUrl + ", report_id=" + reportId);

				//insert into bulk reports
				database.inputBulkReportsEntry(smapUrl, reportId, muxStreamMsg);
			}

			//get set for gc cleanup
			ir = null;
			lineBuffer = null;
			smapConn = null;
			smapUrlObj = null;
			
		}catch(Exception e){
			logger.log(Level.WARNING, "", e);
			if(e instanceof MalformedURLException){
				logger.warning("Malformed URL: " + smapUrl);
				//delete database entry in publishers table
				//database.removePublisher(smapUrl);
				//delete database entry in rest_resources table
				//database.removeRestResource(pubPath);
			} else if (e instanceof SocketTimeoutException){
				logger.info("Smap connection timeout");
			}
		} finally {
			try {
				if(out !=null)
					out.close();
			} catch (Exception e){
				logger.log(Level.WARNING, "Problems closing smapServerGet outputstream", e);
			}
			out=null;

			try {
				if(in!=null)
					in.close();
			} catch (Exception e){
				logger.log(Level.WARNING, "Problems closing smapServerGet inputstream", e);
			}
			in=null;
		}

		return reportId;
	}
	
	public static String installReport(String smapUrl, UUID pubid, String pubPath){

		setSFSConnInfo();
		String reportId = null;
		InputStream in=null;
		OutputStream out=null;
		try{
			String rootResource = null;
			int index = smapUrl.indexOf("data");
			if(index <=0){
				//delete database entry in publishers table
				//delete database entry in rest_resources table
				return null;
			}
			rootResource = smapUrl.substring(0, index);
			String reportResource = smapUrl.substring(index, smapUrl.length());
			logger.info("SMAP_URL=" + smapUrl + "\nROOT_RESOURCE=" + rootResource +
					"\nREPORT_RESOURCE=" + rootResource + "reporting/create");
			if(!reportResource.startsWith("/"))
				reportResource = "/" + reportResource;

			//construct report
			JSONObject report = new JSONObject();
			report.put("ReportResource", reportResource);
			report.put("ReportDeliveryLocation", "http://" + is4_hostname + ":" + is4_port +
					pubPath + "?type=smap&pubid=" + pubid.toString());
			report.put("Period", 0);
			//report.put("Minimum",0);
			//report.put("Maximum", 0);

			//POST the report
			URL smapServer = new URL(rootResource + "reporting/create");
			URLConnection smapConn = smapServer.openConnection();
			smapConn.setConnectTimeout(5000);
			smapConn.setUseCaches(false);
			smapConn.setRequestProperty("Connection", "close");
			smapConn.setDoOutput(true);
			out = smapConn.getOutputStream();
			OutputStreamWriter wr = new OutputStreamWriter(out);
			wr.write(report.toString());
			wr.flush();

			//GET reply
			in = smapConn.getInputStream();
			InputStreamReader ir = new InputStreamReader(in);
			BufferedReader reader = new BufferedReader(ir);
			StringBuffer lineBuffer = new StringBuffer();
			String line = null;
			while((line = reader.readLine()) != null)
				lineBuffer.append(line);
			line = lineBuffer.toString();
			reader.close();
			logger.info("SMAP_REPLY: " + line);
			JSONObject reply = new JSONObject();
			if(line.length()>0)
				reply.put("response", line);
			JSONArray reportIdArray = reply.optJSONArray("response");
			if(reportIdArray != null)
				reportId = (String)reportIdArray.get(0);
			/*if(reportId != null && reportId.length()>0){
				//logger.info("Updating entry for " + smapUrl + ", report_id=" + reportId);
				//update publisher entry for this smap publisher with the report id
				//database.updatePublisherEntry(smapUrl, reportId);
				logger.info("Inserting entry for " + smapUrl + ", report_id=" + reportId + " pubid= " + pubid.toString());
				database.insertPublisherEntry(smapUrl, reportId, pubid);
			}*/
			//get set for gc cleanup
			ir = null;
			lineBuffer = null;
			smapConn = null;
			smapServer = null;
		} catch (Exception e) {
			logger.log(Level.WARNING, "", e);
			if(e instanceof MalformedURLException){
				//delete database entry in publishers table
				database.removePublisher(smapUrl);
				//delete database entry in rest_resources table
				database.removeRestResource(pubPath);
			} else if (e instanceof SocketTimeoutException){
				logger.info("Smap connection timeout");
			}
		} finally {
			try {
				if(out !=null)
					out.close();
			} catch (Exception e){
				logger.log(Level.WARNING, "Problems closing smapServerGet outputstream", e);
			}
			out=null;

			try {
				if(in!=null)
					in.close();
			} catch (Exception e){
				logger.log(Level.WARNING, "Problems closing smapServerGet inputstream", e);
			}
			in=null;
		}

		return reportId;
	}

	/**
	 *  This function is used to resolve a smap uri with the '*' character in it.
	 *  @param uriStr The sMAP uri string (usually with stars in it).
	 *  @param smapResp The sMAP document returns when you GET the uri on the associated sMAP server
	 *  @returns A json object of the format:
	 *
	 *	{"resolved_uri":"data"}
	 *
	 *	Where the resolved uri is the uri without the stars in it and the data is json object or
	 *	array associated with that particular resource.
	 */
	public static JSONObject resolveSmapUri(String uriStr, JSONObject smapResp){
		
		setSFSConnInfo();
		JSONObject response = new JSONObject();
		response.put(uriStr, smapResp);
		
		while(moreToResolve(response)){
			JSONObject newJ = new JSONObject();
			Iterator keys = response.keys();
			while(keys.hasNext()){
				String thisKey = (String) keys.next();
				JSONObject thisVal = response.optJSONObject(thisKey);
				if(thisVal == null){
					JSONArray thisValArray = response.getJSONArray(thisKey);
					newJ.put(thisKey, thisValArray);
				}

				if(thisVal !=null){
					JSONObject e = processSmapUris(thisKey, thisVal);
					newJ.putAll(e);
				}
			}
			response.clear();
			response.putAll(newJ);
		}

		return response;
	}

	private static JSONObject processSmapUris(String uri, JSONObject val){
		
		setSFSConnInfo();
		JSONObject r = new JSONObject();
		logger.info("ProcessingSmapUris; uri=" + uri + " val: " + val.toString());
		if(uri.contains("*") && !val.isNullObject()){
			Iterator keys = val.keys();
			while(keys.hasNext()){
				String thisKey = (String) keys.next();
				String star = "\\*";
				String newUri = uri.replaceFirst(star, thisKey);
				JSONObject thisVal = val.optJSONObject(thisKey);
				if(thisVal == null){
					JSONArray thisValArray = null;
					thisValArray = val.optJSONArray(thisKey);
					if(thisValArray != null){
						r.put(newUri, thisValArray);
					}
				}

				if(thisVal != null)
					r.put(newUri, thisVal);
			}
		}else if(!uri.contains("*")){
			r.put(uri, val);
		}
		return r;
	}

	private static boolean moreToResolve(JSONObject j){
		
		setSFSConnInfo();
		Iterator keys = j.keys();
		while(keys.hasNext()){
			String thisKey = (String) keys.next();
			if(thisKey.contains("*"))
				return true;
		}
		return false;
	}

	private void reschedule(){
		
		setSFSConnInfo();
		if(!timerSet){
			//try again in 5 minutes
			Timer timer = new Timer();
			Date alarmTime = new Date((new Date()).getTime() + 300000);
			timer.schedule(this, alarmTime);
			timerSet = true;
		}
	}

	public void run(){
		
		setSFSConnInfo();
		timerSet = false;
		//get all unresponsive smap sources from publishers table
		JSONObject idSmapUrls = database.getInactiveSmapPubs();
		if(idSmapUrls != null){
			Set<String> ids = idSmapUrls.keySet();
			Iterator<String> ids_iterator = ids.iterator();
			JSONArray list = new JSONArray();
			Vector<String> pathsVec = new Vector<String>();
			while (ids_iterator.hasNext()){
				String thisId = ids_iterator.next();
				String thisPubPath = database.getPathWithId((new Integer(thisId)).intValue());
				pathsVec.addElement(thisPubPath);
				list.add(idSmapUrls.getString(thisId));
			}

			for(int i=0; i<list.size(); i++){
				String smapurl = (String) list.get(i);
				Date created = database.getPublisherCreated(smapurl);
				logger.info("QUERY_RESULT:" + created.toString());
				Date now = new Date();
				Date expireTime = new Date(created.getTime() + 86400000);

				UUID pubid = null;
				String pubPath = null;
				if(now.before(expireTime) && 
						(pubid=database.isPublisher(smapurl, true))!=null &&
						(pubPath = (String)pathsVec.elementAt(i))!= null){
					this.installReport(smapurl, pubid, pubPath);
				} else {
					//delete database entry in publishers table
					database.removePublisher(smapurl);
					//delete database entry in rest_resources table
					database.removeRestResource(pubPath);
				}
			}
		}
	}
}
