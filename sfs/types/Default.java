package sfs.types;

import sfs.SFSServer;
import sfs.query.QueryHandler;
import sfs.util.DBQueryTypes;

import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;

import sfs.util.ResourceUtils;
import sfs.db.MySqlDriver;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;
import java.io.*; 

public class Default{

	protected static transient final Logger logger = Logger.getLogger(Default.class.getPackage().getName());
    private static MySqlDriver mysqlDB = MySqlDriver.getInstance();
    private static final JSONParser parser = new JSONParser();
    private static final ResourceUtils utils = ResourceUtils.getInstance();
    private static final QueryHandler qh = QueryHandler.getInstance();

	public static void get(Request request, Response response, String path, boolean internalCall, JSONObject internalResp){
        try {
            Query q = request.getQuery();
            if(q!=null && q.toString().length()>0){
                Set<String> keys = q.keySet();
                String keyString = keys.toString();
                logger.info("query parameter keys=" + keys.toString());
                if(keyString.contains("props_")){
                    logger.info("Handling PROPERTIES query");
                    qh.query(request, response, path, q, internalCall, internalResp);
                }
			} else if(path.equals("/")) {
                JSONObject respObj = new JSONObject();
                logger.fine("GETTING RESOURCES: " + path);
                respObj.put("status", "success");
                JSONArray subResourceNames = mysqlDB.rrGetChildren(path);
                respObj.put("children",subResourceNames);
                JSONArray allPaths = mysqlDB.rrGetAllPaths();
                Integer activeResourceCnt = new Integer(allPaths.size());
                respObj.put("activeResources", activeResourceCnt);
                respObj.put("uptime", (System.currentTimeMillis()/1000) - SFSServer.start_time);
                respObj.put("uptime_units", "seconds");
                utils.sendResponse(request, response, 200, respObj.toString(), false, null);
                return;
            } else {
                logger.fine("GETTING RESOURCES: " + path);
				JSONObject respObj = new JSONObject();
                JSONArray subResourceNames = mysqlDB.rrGetChildren(path);
				logger.fine(subResourceNames.toString());
				respObj.put("status", "success");
				respObj.put("type", "default");
                JSONObject p = mysqlDB.rrGetProperties(path);
                p.remove("_keywords");
                p.remove("is4_uri");
                p.remove("timestamp");
				respObj.put("properties", p);
				//findSymlinks(subResourceNames);
				respObj.put("children",subResourceNames);
				utils.sendResponse(request, response, 200, respObj.toString(), internalCall, internalResp);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while responding to GET request",e);
            utils.sendResponse(request, response, 200, null, false, new JSONObject());
        }
	}

    private JSONArray getIncidentPaths(String path){
        JSONArray l = mysqlDB.getAllSymlinkAndTargetPaths();
        //check alll target paths and see if it shares a substring prefix with
        //the 'path' parameter for this function
        JSONArray a = mysqlDB.rrGetAllPaths();
    }

    /*private void findSymlinks(JSONArray children){
		JSONArray newChildren = new JSONArray();
		for(int i=0; i<children.size(); i++){
			String childName = (String) children.get(i);
			String resourcePath = URI + childName;
			boolean isSymlink = database.isSymlink(resourcePath);
			if(isSymlink){
				logger.info(resourcePath + " is a symlink");
				SymlinkResource slr = (SymlinkResource) RESTServer.getResource(resourcePath);
				if(slr != null){
					String symlinkStr = childName  + " -> " + slr.getLinkString();
					newChildren.add(symlinkStr);
				}
			} else {
				newChildren.add(childName);
			}
		}
		children.clear();
		children.addAll(newChildren);
	}*/

	/*public static void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
	}

	public static void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
	}

	public static void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
	}*/

    
}