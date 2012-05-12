package local.rest.handlers;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Long;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.sf.json.*;

import com.sun.net.httpserver.*;

import is4.*;
import is4.exceptions.*;
import local.html.tags.*;
import local.json.validator.*;
import local.db.*;
import local.rest.*;
import local.rest.resources.*;
import javax.naming.InvalidNameException;

public class SubHandler extends Resource {
	private Vector<String> errorVec = new Vector<String>();
	private Registrar registrar = Registrar.registrarInstance();
	private URL proxyUrl = null;
	protected static MySqlDriver database = (MySqlDriver) DBAbstractionLayer.database;
	private transient Logger logger = Logger.getLogger(SubHandler.class.getPackage().getName());

	public SubHandler(String path) throws Exception, InvalidNameException{
		super(path);
	}

	public void put(HttpExchange exchange, String data,boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
		try{
			logger.info("Request: " + data);
			JSONObject jsonReq = (JSONObject) JSONSerializer.toJSON(data);
			SubMngr subMngr = SubMngr.getSubMngrInstance();
		
			String targetStr=null;
			URL subUrl = null;
			
			//extract the URL/URI and publisher information to 'install' subscriber
			if(jsonReq.containsKey("target")){
				targetStr = jsonReq.getString("target");
				if(targetStr.startsWith("http://")){
					subUrl = new URL(targetStr);
				} else if(!targetStr.startsWith("/")){
					errors.add("Invalid \"target\" value format: absolute path or HTTP URL ONLY");
					resp.put("status", "fail");
					resp.put("errors",errors);
					sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
				}
			}

            Resource r;
			//get pubid id or absolute path
			String pidStr = jsonReq.optString("pubid");
			String sNodePathStr = jsonReq.optString("s_uri");
            if(sNodePathStr.equals(""))
                sNodePathStr = jsonReq.optString("path");
			String mNodePathStr = jsonReq.optString("m_uri");
            if(mNodePathStr.equals(""))
                mNodePathStr = jsonReq.optString("path");
			logger.fine("sNodePathStr: " + sNodePathStr + "; mNodePathStr: " + mNodePathStr);
			if(!pidStr.equals("") && sNodePathStr.equals("") && mNodePathStr.equals("") ){
				errors.add("Must include one of [pubid | s_uri | m_uri | path] string");
				resp.put("status", "fail");
				resp.put("errors", errors);
				sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
				return;
			} else if(pidStr.equals("") && (!sNodePathStr.equals("") || !mNodePathStr.equals(""))){
				JSONArray paths = new JSONArray();
				if(!sNodePathStr.equals(""))
					paths.add(sNodePathStr);
				else
					paths.add(mNodePathStr);
				logger.info("Paths: "+ paths);
				JSONArray pArray = database.getPubIdsFromSnodes(paths);
				if(pArray !=null && pArray.size()!=0){
					pidStr = (String) pArray.get(0);
				}else {
					errors.add("Invalid path " + (String)paths.get(0));
					resp.put("status", "fail");
					resp.put("errors",errors);
					sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
					return;
				}
			}
						
		
			//initialize the subscription entry
			JSONObject initSubStat = subMngr.initSubscription(UUID.fromString(pidStr), targetStr);
			if(initSubStat.containsKey("subid") && !initSubStat.containsKey("add_to_existing")){
				//update the subscription entry, create subscription resource, and send success to user
				String sid = initSubStat.getString("subid");
				logger.info("Successfully added new subscriber: [" + targetStr + ", " + sid + "]");
				SubscriptionResource subRsrc = new SubscriptionResource(UUID.fromString(sid));
				RESTServer.addResource(subRsrc);

				JSONObject subResp = new JSONObject();
				subResp.put("operation", "subscribe");
				subResp.put("status", "success");
				subResp.put("subid", sid);

				sendResponse(exchange, 200, subResp.toString(), internalCall, internalResp);
			} else if(initSubStat.containsKey("subid") && initSubStat.containsKey("add_to_existing")){
                String sid = initSubStat.getString("subid");
				logger.info("Successfully added new subscriber to existing subscription:: [" + targetStr + ", " + sid + "]");
				JSONObject subResp = new JSONObject();
				subResp.put("operation", "subscribe");
				subResp.put("status", "success");
				subResp.put("subid", sid);

				sendResponse(exchange, 200, subResp.toString(), internalCall, internalResp);
            } else{
				//subscription entry initialization failed, forward errors to user
				errors.addAll(initSubStat.getJSONArray("errors"));
				resp.put("status", "fail");
				resp.put("operations", "subscribe");
				resp.put("errors", errors);
				sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
			}
			
		}catch (Exception e){
			logger.log(Level.WARNING, "Exception thrown while adding subscriber", e);
			JSONObject subResp = new JSONObject();
			subResp.put("operation", "subscribe");
			subResp.put("status", "fail");
			
			if (e instanceof JSONException) {
				//add error to error Vector
				errorVec.addElement("JSON syntax error; Invalid JSON structure, check brackets.");
			}
			else if (e instanceof MalformedURLException){
				errorVec.addElement("Invalid URL");
			}
			
			JSONArray errorArray = new JSONArray();
			errorArray.addAll(errorVec);
			subResp.put("error", errorArray);
			
			sendResponse(exchange, 200, subResp.toString(), internalCall, internalResp);
			
			//clear the error vector
			errorVec.clear();
		}
	}
	
	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		put(exchange, data, internalCall, internalResp);
	}

}
