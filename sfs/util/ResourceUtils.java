package sfs.util;

import sfs.SFSServer;
import sfs.db.MySqlDriver;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Vector;
import java.io.PrintStream;
import java.util.zip.GZIPOutputStream;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class ResourceUtils {
	private static transient Logger logger = Logger.getLogger(ResourceUtils.class.getPackage().getName());
	protected static MySqlDriver database = MySqlDriver.getInstance();
    private static final JSONParser parser = new JSONParser();

    private static ResourceUtils resourceUtils = null;

	private ResourceUtils(){}

    public static ResourceUtils getInstance(){
        if(resourceUtils ==null)
            resourceUtils = new ResourceUtils();
        return resourceUtils;
    }

	//resource type strings
	public static final String DEFAULT_RSRC_STR = "DEFAULT";
	public static final String DEVICES_RSRC_STR = "DEVICES";
	public static final String DEVICE_RSRC_STR  = "DEVICE";
	public static final String PUBLISHER_RSRC_STR = "PUBLISHER";
	public static final String GENERIC_PUBLISHER_RSRC_STR ="GENERIC_PUBLISHER";
	public static final String SUBSCRIPTION_RSRC_STR = "SUBSCRIPTION";
	public static final String SYMLINK_RSRC_STR = "SYMLINK";
	public static final String MODEL_RSRC_STR = "MODEL";
	public static final String MODEL_INSTANCE_RSRC_STR = "MODEL_INSTANCE";
	public static final String MODEL_GENERIC_PUBLISHER_RSRC_STR ="MODEL_GENERIC_PUBLISHER";
    public static final String PROCESS_PUBLISHER_RSRC_STR = "PROCESS";
    public static final String PROCESS_RSRC_STR = "PROCESS_CODE";

	//resource type ints
	public static final int DEFAULT_RSRC = 0;
	public static final int DEVICES_RSRC = 2;
	public static final int DEVICE_RSRC  = 3;
	public static final int PUBLISHER_RSRC = 4;
	public static final int GENERIC_PUBLISHER_RSRC = 5;
	public static final int SUBSCRIPTION_RSRC = 6;
	public static final int SYMLINK_RSRC = 7;
	public static final int MODEL_RSRC = 8;
	public static final int MODEL_INSTANCE_RSRC = 9;
	public static final int MODEL_GENERIC_PUBLISHER_RSRC = 10;
    public static final int PROCESS_PUBLISHER_RSRC = 11;
    public static final int PROCESS_RSRC = 12;

	//methods
	public static boolean isValidType(String type){
		int testType = translateType(type);
		switch(testType){
			case DEFAULT_RSRC:
				return true;
			case DEVICES_RSRC:
				return true;
			case DEVICE_RSRC:
				return true;
			case PUBLISHER_RSRC:
				return true;
			case GENERIC_PUBLISHER_RSRC:
				return true;
			case SUBSCRIPTION_RSRC:
				return true;
			case SYMLINK_RSRC:
				return true;
			case MODEL_RSRC:
				return true;
			case MODEL_INSTANCE_RSRC:
				return true;
			case MODEL_GENERIC_PUBLISHER_RSRC:
				return true;
            case PROCESS_PUBLISHER_RSRC:
				return true;
            case PROCESS_RSRC:
				return true;
			default:
				return false;
		}
		
	}

	public static int translateType(String type){
		if(type != null){
			if(type.equalsIgnoreCase(DEFAULT_RSRC_STR))
				return DEFAULT_RSRC;
			else if(type.equalsIgnoreCase(DEVICES_RSRC_STR))
				return DEVICES_RSRC;
			else if(type.equalsIgnoreCase(DEVICE_RSRC_STR))
				return DEVICE_RSRC;
			else if(type.equalsIgnoreCase(PUBLISHER_RSRC_STR))
				return PUBLISHER_RSRC;
			else if(type.equalsIgnoreCase(GENERIC_PUBLISHER_RSRC_STR))
				return GENERIC_PUBLISHER_RSRC;
			else if(type.equalsIgnoreCase(SUBSCRIPTION_RSRC_STR))
				return SUBSCRIPTION_RSRC;
			else if(type.equalsIgnoreCase(SYMLINK_RSRC_STR))
				return SYMLINK_RSRC;
			else if(type.equalsIgnoreCase(MODEL_RSRC_STR))
				return MODEL_RSRC;
			else if(type.equalsIgnoreCase(MODEL_INSTANCE_RSRC_STR))
				return MODEL_INSTANCE_RSRC;
			else if(type.equalsIgnoreCase(MODEL_GENERIC_PUBLISHER_RSRC_STR))
				return MODEL_GENERIC_PUBLISHER_RSRC;
            else if(type.equalsIgnoreCase(PROCESS_PUBLISHER_RSRC_STR))
				return PROCESS_PUBLISHER_RSRC;
            else if(type.equalsIgnoreCase(PROCESS_RSRC_STR))
				return PROCESS_RSRC;
		}
		return -1;
	}

	public static String translateType(int type){
		switch(type){
			case DEFAULT_RSRC:
				return DEFAULT_RSRC_STR;
			case DEVICE_RSRC:
				return DEVICE_RSRC_STR;
			case DEVICES_RSRC:
				return DEVICES_RSRC_STR;
			case PUBLISHER_RSRC:
				return PUBLISHER_RSRC_STR;
			case GENERIC_PUBLISHER_RSRC:
				return GENERIC_PUBLISHER_RSRC_STR;
			case SUBSCRIPTION_RSRC:
				return SUBSCRIPTION_RSRC_STR;
			case SYMLINK_RSRC:
				return SYMLINK_RSRC_STR;
			case MODEL_RSRC:
				return MODEL_RSRC_STR;
			case MODEL_INSTANCE_RSRC:
				return MODEL_INSTANCE_RSRC_STR;
			case MODEL_GENERIC_PUBLISHER_RSRC:
				return MODEL_GENERIC_PUBLISHER_RSRC_STR;
            case PROCESS_PUBLISHER_RSRC:
				return PROCESS_PUBLISHER_RSRC_STR;
            case PROCESS_RSRC:
				return PROCESS_RSRC_STR;
			default:
				return null;
		}
	}

	/*public static boolean putResource(String parentPath, JSONObject rsrcAddReq){
		String myPath = parentPath;
		if(!myPath.endsWith("/"))
			myPath += "/";

		try {
			String rType = rsrcAddReq.getString("resourceType");
			if(isValidType(rType)){
				String rName = rsrcAddReq.optString("resourceName");
				if(rType.equalsIgnoreCase(DEVICES_RSRC_STR))
					rName = "devices";
				Resource resource = null;

				JSONArray children = database.rrGetChildren(myPath);
				if(children != null && !rName.equals("") && !children.contains(rName)){

					if(rType.equalsIgnoreCase(DEFAULT_RSRC_STR)){
						resource = new Resource(myPath + rName);
					}

					else if (rType.equalsIgnoreCase(DEVICES_RSRC_STR)){
						resource = new DevicesResource(myPath + rName);
					} 

					else if (rType.equalsIgnoreCase(DEVICE_RSRC_STR)){
						logger.info("CREATEING DEVICE INSTANCE RESOURCE");
						resource = new DeviceInstanceResource(myPath + rName);
					}
				}

				if(resource != null) {
					RESTServer.addResource(resource);
					return true;
				}
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "Error adding new resource to " + parentPath, e);
		}
		return false;
	}*/

	/*public static boolean devNameIsUnique(String parentResource, String childName){
		logger.info("folder: " + parentResource + ", name: " + childName);
		JSONArray myChildren = database.rrGetChildren(parentResource);
		Vector<String> myChildrenVec = new Vector<String>(myChildren);
		return !myChildrenVec.contains(childName);
	}*/

    public static String cleanPath(String path){
        //clean up the path
        if(path == null)
            return path;
        if(path.equals("") || path.equals("/"))
            return path;

        if(!path.startsWith("/"))
            path = "/" + path;
        path = path.replaceAll("/+", "/");
        if(path.endsWith("/"))
            path = path.substring(0,path.length()-1);
        return path;
    }

    public static void sendResponse(Request request, Response response, int code, String data, 
            boolean internalCall, JSONObject internalResp){
        try {
            if(!internalCall){
                long time = System.currentTimeMillis();
                String enc = request.getValue("Accept-encoding");
                boolean gzipResp = false;
                if(enc!=null && enc.indexOf("gzip")>-1)
                    gzipResp = true;
                response.set("Content-Type", "application/json");
                response.set("Server", "StreamFS/2.0 (Simple 4.0)");
                response.set("Connection", "close");
                response.setDate("Date", time);
                response.setDate("Last-Modified", time);
                response.setCode(code);
                PrintStream body = response.getPrintStream();
                if(data!=null && !gzipResp)
                    body.println(data);
                else if(data!=null && gzipResp){
                    response.set("Content-Encoding", "gzip");
                    GZIPOutputStream gzipos = new GZIPOutputStream(body);
                    gzipos.write(data.getBytes());
                    gzipos.close();
                }
                body.close();
            } else {
                logger.fine("Copying response to internal buffer");
                JSONObject respObj = (JSONObject) parser.parse(data);
                internalResp.putAll(respObj);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void copyResponse(String respStr, JSONObject internalResp){
		try{
			if(internalResp != null && respStr !=null){
                
			} 
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}
	}
}
