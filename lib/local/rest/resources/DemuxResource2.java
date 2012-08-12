package local.rest.resources;

import is4.*;
import local.db.*;
import local.rest.smap.*;
import local.rest.*;

import net.sf.json.*;
import java.net.*;
import java.util.*;
import java.lang.Runnable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.util.logging.Logger;
import java.util.logging.Level;
import javax.naming.InvalidNameException;

//import org.simpleframework.transport.connect.Connection;
//import org.simpleframework.transport.connect.SocketConnection;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;

public class DemuxResource2 extends Resource {
	private static transient final Logger logger = Logger.getLogger(DemuxResource.class.getPackage().getName());
	private static ExecutorService executorService = null;

	//root path where new devices and stream files are saved
	private static String ROOT = null;

	public DemuxResource2() throws Exception, InvalidNameException{
		super("/pub/smap2/demux/");
		executorService = Executors.newCachedThreadPool();
	}

	public void put(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
        Query query = m_request.getQuery();
        String rootPathParam=null;
        try {rootPathParam = (String)query.get("root");} catch(Exception e){}
		if(rootPathParam !=null){
			ROOT=rootPathParam;
			logger.info("SMAP_ROOT_PATH CHANGED::" + ROOT);
			
		}
		if(data!=null && data.length()>0)
			post(m_request, m_response, path, data, internalCall, internalResp);
		else
			sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
	}

	public void post(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		/*logger.info("PUT/POST Smap2 Demultiplexor; " + exchange.getLocalAddress().getHostName() + ":" + exchange.getLocalAddress().getPort() + "->" + 
					exchange.getRemoteAddress() + ":" + exchange.getRemoteAddress().getPort() );//+ "\nDataPosted::" + data);*/
		JSONArray errors =  new JSONArray();
		JSONObject response = new JSONObject();

		try {
			//submit post data task here and reply
			PostBulkDataTask postDataTask = new PostBulkDataTask(m_request, m_response, path, data, ROOT);
			executorService.submit(postDataTask);
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}

		try {
			
			response.put("status", "success");
			super.sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} /*finally {
			try {
				if(exchange !=null){
					exchange.getRequestBody().close();
					exchange.getResponseBody().close();
					exchange.close();
				} 
				exchange=null;
			} catch(Exception e){
				logger.log(Level.WARNING, "Trouble closing exchange in Resource", e);
			}
		}*/

	}

	private class PostBulkDataTask implements Runnable{

		private String bulkReport = null;
		private String rootPath = "/inventory";
        private Request request = null;
        private Response response = null;
        private String path =null;

		public PostBulkDataTask(Request m_request, Response m_response, String ppath, String dataStr, String root){
            request = m_request;
            response = m_response;
            path =ppath;
			bulkReport = dataStr;
			if(root!=null && !root.equals(""))
				rootPath=root;
		}

		public void run(){
			try {
				parseBulk();
			} catch(Exception e){
				logger.log(Level.WARNING, "Error in postDataTask", e);
			}
		}


		/**
		 * Parses the bulk data, placing the associated timeseries data
		 * into the resource for that data source in streamfs.  The main attribute
		 * key is translated from a smap path to an sfs path.  The uuid assocaited with
		 * the smap source is recorded as a property of the data source.  If the data source
		 * contains a timestamp, sfs DOES NOT include an extra timestamp.
		 *
		 * Data Example:
		 * 		
		 *	 {
		 *		"/fitpc_acmes/5d6/true_power": {
		 *			"uuid": "8e75fd0f-2935-5055-9d64-e32cc53527db",
		 *			"Readings": 
		 *				[
		 *					[1320588916000,2496,1401528],
		 *					[1320588926000,2653,1401528],
		 *				]
		 *		}
		 *		"/fitpc_acmes/6b6/true_power": {
		 *			"uuid": "a04c31b3-ce29-5bba-912e-76ee3c7b4e29",
		 *			"Readings": 
		 *			[
		 *				[1320588913000,531,1347527],
		 *				[1320588923000,581,1347527]
		 *			]
		 *		}
		 *	 }
		 * 
		 */
		public void parseBulk(){
			try {
				if(rootPath==null)
					return;
				if(!rootPath.endsWith("/"))
					rootPath=rootPath + "/";
				//JSONObject reportObj = new JSONObject(bulkReport);
				JSONObject reportObj = (JSONObject)JSONSerializer.toJSON(bulkReport);
				Iterator keys = reportObj.keys();
				while(keys.hasNext()){
					String thisKey = (String) keys.next();
					StringTokenizer tokenizer = new StringTokenizer(thisKey, "/");
					Vector<String> tokens = new Vector<String>();
					while(tokenizer.hasMoreElements())
						tokens.addElement(tokenizer.nextToken());

					if(tokens.size()<2)
						break;

					String chanName = tokens.lastElement();
					String spName = tokens.elementAt(tokens.size()-2);
					String path = rootPath + spName + "/" + chanName;

					logger.info("Does " + spName + " exists?");
					logger.info("\tNo:\tCreate " + spName + " in " + rootPath);
					logger.info("\t\tCreate	" + chanName + " in " + rootPath + spName + "/");
					logger.info("\tYes: Does " + chanName + " exist in " + rootPath + spName + "/" + "?");
					logger.info("\t\tNo: Create " + chanName + " in " + rootPath + spName + "/" );

					Resource devRsrc = null;
					Resource chanRsrc = null;
					if((devRsrc=RESTServer.getResource(rootPath + spName)) == null){
						Resource rootRsrc = RESTServer.getResource(rootPath);
						if(rootRsrc==null){
							logger.warning(rootPath + " does not exists; Data lost permanently");
							return;
						}
						JSONObject crtReqObj = new JSONObject();
						crtReqObj.put("operation", "create_resource");
						crtReqObj.put("resourceName", spName);
						crtReqObj.put("resourceType", "default");
						JSONObject internalResp = new JSONObject();
						rootRsrc.put(request, response, path, crtReqObj.toString(), true, internalResp);

						// check if creating it was successful
						devRsrc = RESTServer.getResource(rootPath + spName);
						if(devRsrc==null){
							logger.warning("Could not create " + rootPath + spName);
							return;
						}
					}

					String pubid = null;
					if((chanRsrc=RESTServer.getResource(rootPath + spName + "/" + chanName))==null){
						JSONObject crtReqObj = new JSONObject();
						crtReqObj.put("operation", "create_generic_publisher");
						crtReqObj.put("resourceName", chanName);
						JSONObject internalResp = new JSONObject();
						devRsrc.put(request, response, path, crtReqObj.toString(), true, internalResp);
						pubid = internalResp.getString("PubId");
						
						// check if creating it was successful
						chanRsrc = RESTServer.getResource(rootPath + spName + "/" + chanName);
						if(chanRsrc==null){
							logger.warning("Could not create " + rootPath + spName + "/" + chanName);
							return;
						}
					} else {
						pubid = ((GenericPublisherResource)chanRsrc).publisherId.toString();
					}

					JSONObject thisReportObj = reportObj.getJSONObject(thisKey);
					String chanuuid = thisReportObj.getString("uuid");
					JSONObject props = new JSONObject();
					props.put("smap_url","Unspecified");
					props.put("smap_channel_path", thisKey);
					props.put("smap_uuid", chanuuid);
					props.put("smap_units","Unspecified");
					props.put("pubid", pubid);

					JSONObject propsReq = new JSONObject();
					propsReq.put("operation", "overwrite_properties");
					propsReq.put("properties", props);

					JSONObject internalResp = new JSONObject();
					chanRsrc.handlePropsReq(request, response, propsReq.toString(), true, internalResp);
					logger.info(path + ", setProps::" + props.toString());
					
					JSONArray data = thisReportObj.getJSONArray("Readings");
					ArrayList<JSONObject> bulk = new ArrayList<JSONObject>();
					ArrayList<Long> timestamps = new ArrayList<Long>();
					for(int i=0; i<data.size(); i++){
						
						JSONArray ptArray = data.getJSONArray(i);
						Long ts = ptArray.getLong(0);
						Double val = ptArray.getDouble(1);
						
						JSONObject dataPt = new JSONObject();
						dataPt.put("pubid", pubid);
						dataPt.put("ts", ts.longValue());
						dataPt.put("value", val);
						timestamps.add(ts);
						bulk.add(dataPt);
					}
					logger.info(path + ", adding bulkData::" + bulk.toString() + "\n\n");
					mongoDriver.putTsEntries(bulk);

					//set the last received timestamps value to the largest timestamp in the bulk report
					Object[] tsvals = timestamps.toArray();
					Arrays.sort(tsvals);
					((GenericPublisherResource)chanRsrc).last_data_ts = ((Long)tsvals[tsvals.length-1]).longValue();
					logger.fine("Set last_data_ts=" + ((GenericPublisherResource)chanRsrc).last_data_ts);
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}

}
