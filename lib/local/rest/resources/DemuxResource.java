package local.rest.resources;

import is4.*;
import local.db.*;
import local.rest.smap.*;
import local.rest.*;

import com.sun.net.httpserver.*;
import net.sf.json.*;
import java.net.*;
import java.util.*;
import java.lang.Runnable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.util.logging.Logger;
import java.util.logging.Level;
import javax.naming.InvalidNameException;

public class DemuxResource extends Resource {
	private static transient final Logger logger = Logger.getLogger(DemuxResource.class.getPackage().getName());
	private static ExecutorService executorService = null;

	public DemuxResource() throws Exception, InvalidNameException{
		super("/is4/pub/smap/demux/");
		executorService = Executors.newCachedThreadPool();
	}

	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		post(exchange, data, internalCall, internalResp);
	}

	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		logger.info("PUT/POST Demultiplexor; " + exchange.getLocalAddress().getHostName() + ":" + exchange.getLocalAddress().getPort() + "->" + 
					exchange.getRemoteAddress() + ":" + exchange.getRemoteAddress().getPort());
		JSONArray errors =  new JSONArray();
		JSONObject response = new JSONObject();

		String typeParam = null;
		String smapurlParam = null;
		try {
			typeParam = (String) exchange.getAttribute("type");
			smapurlParam = (String) exchange.getAttribute("smapurl");

			if(typeParam != null && smapurlParam != null && typeParam.equalsIgnoreCase("smap")){
				//submit post data task here and reply
				PostBulkDataTask postDataTask = new PostBulkDataTask(smapurlParam, data);
				executorService.submit(postDataTask);
			} else {
				if(typeParam == null)
					errors.add("Missing type parameter");
				if(smapurlParam == null)
					errors.add("Missing smapurl parameter");
				if(typeParam != null && !typeParam.equalsIgnoreCase("smap"))
					errors.add("Invalid type value");
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			if(e instanceof MalformedURLException)
				errors.add("Invalid url format for smapurl parameter");
			if(e instanceof JSONException)
				errors.add("Invalid JSON format for posted data");
		}

		try {
			if(errors.size()>0){
				response.put("status", "fail");
				response.put("errors", errors);
			} else {
				response.put("status", "success");
			}
			super.sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
			//super.sendResponse(exchange, 200,"", internalCall, internalResp);
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
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
		}

	}

	private class PostBulkDataTask implements Runnable{

		private String smapurlParam = null;
		private String data = null;

		public PostBulkDataTask(String smapUrlParam, String dataStr){
			smapurlParam = smapUrlParam;
			data = dataStr;
		}

		public void run(){
			try {
				URL smapUrlObj = new URL(smapurlParam);
				String smapUriStr = smapUrlObj.getPath();
				JSONObject dataObj = (JSONObject) JSONSerializer.toJSON(data);
				JSONObject resObj = SmapConnector.resolveSmapUri(smapUriStr, dataObj);
				Iterator keys = resObj.keys();
				while(keys.hasNext()){
					String thisUri = (String)keys.next();
					int port = (smapUrlObj.getPort()<0)?80:smapUrlObj.getPort();
					String thisPubSmapUrlStr = "http://" + smapUrlObj.getHost() + ":" + port + thisUri;
					URL thisPubSmapUrl = new URL(thisPubSmapUrlStr);
					JSONObject uriDataVal = resObj.optJSONObject(thisUri);
					//post the data for this publisher
					//add timestamp
					Date date = new Date();
					long timestamp = date.getTime()/1000;
					postDataToPublisher(thisPubSmapUrl, uriDataVal, timestamp);
				}
			} catch(Exception e){
				logger.log(Level.WARNING, "Error in postDataTask", e);
			}
		}

		public void postDataToPublisher(URL pubSmapUrl, JSONObject uriDataVal, long timestamp){
			//Registrar registrar = Registrar.registrarInstance();

			UUID publisherId = database.isPublisher(pubSmapUrl.toString(), true);
			if(publisherId != null) {

				//uriDataVal.put("timestamp", timestamp);
				String is4Uri = database.getIs4RRPath(publisherId);

				//get this publisher resource
				PublisherResource pr= (PublisherResource) RESTServer.getResource(is4Uri);
				if(pr==null)
					logger.info("Publisher resource is null: " + is4Uri);
				else
					pr.handleIncomingData(uriDataVal);
			} else {
				logger.warning("No publisher associated with " + pubSmapUrl.toString());
			}
		}
	}
}
