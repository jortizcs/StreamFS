package local.rest.resources.proc;

import local.rest.resources.Resource;
import local.db.*;
import local.rest.*;
import local.rest.resources.util.*;
import local.rest.interfaces.*;
import is4.*;

import net.sf.json.*;

import java.net.*;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*; 

public class ProcessManagerResource extends Resource {
	
	protected static transient final Logger logger = Logger.getLogger(ProcessManagerResource.class.getPackage().getName());

	//model instances root
	public static final String PROC_ROOT = "/proc";

    private static JSONArray serverList = null;
    private static JSONObject properties = null;

    private static Hashtable<String, Socket> connections = new Hashtable<String, Socket>();

    public void setup(){
        properties= new JSONObject();
        properties.put("status", "inactive");
        setNewProperties(properties);
       
        JSONObject configJsonObj=null; 
        //load processing layer config information
        try{
			String home=null;
            String configPath="";
			if((home=System.getenv().get("IS4HOME")) != null)
				configPath = home + "/lib/local/rest/resources/proc/config/serverlist.json";
			File configFile = new File(configPath);
			FileReader cFileReader = new FileReader(configFile);
			BufferedReader bufReader = new BufferedReader(cFileReader);
			StringBuffer strBuf = new StringBuffer();
			String line = null;
			while((line=bufReader.readLine())!=null)
				strBuf.append(line).append(" ");
			configJsonObj = (JSONObject)JSONSerializer.
								toJSON(strBuf.toString());
			cFileReader.close();
			bufReader.close();
        } catch(Exception e){
            //problem loading config file
            logger.log(Level.WARNING, "Could not load config information", e);
        }

        try {
            serverList = configJsonObj.getJSONArray("procservers");
            boolean initiated = false;
            for(int i =0; i<serverList.size(); i++){
                JSONObject thisServer = (JSONObject) serverList.get(i);
                boolean stat = false;
                if(!(stat = initiate(thisServer)))
                    logger.warning("Could not initiate process server: " + 
                            thisServer.toString());
                else
                    logger.info("Establish connection: " + thisServer.toString());
                initiated |= stat;
            }

            if(initiated){
                //status:
                //  initiated - a connection has been establiashed
                //              with at least one process server.
                //  active - at least one process is running
                //  inactive - a connection has not yet been established with a process server
                properties.put("status", "active");
                setNewProperties(properties);
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "Missing 'procservers' array in config file.", e);
        }
    }
	
	public ProcessManagerResource() throws Exception, InvalidNameException {
		super(PROC_ROOT);
        setup();
	}

    public void setNewProperties(JSONObject props){
        database.rrPutProperties(URI, properties);
	    updateProperties(properties);
    }

    public boolean initiate(JSONObject configServerEntry){
        if(configServerEntry ==null || !configServerEntry.containsKey("host") ||
                !configServerEntry.containsKey("port") || 
                !configServerEntry.containsKey("name"))
            return false;

        try {
            String name = configServerEntry.getString("name");
            String host = configServerEntry.getString("port");
            int port = configServerEntry.getInt("port");
            JSONObject initObj = new JSONObject();
            initObj.put("command", "init");
            initObj.put("sfsname", "jortiz");
            Socket socket = new Socket(host, port);
            if(socket.isConnected()){
                connections.put(name, socket);
            }

            logger.info("closing socket");
            socket.close();
        } catch(Exception e){
            return false;
        }
        return true;
    }
	
	//public void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){}
	
	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
		post(exchange, data, internalCall, internalResp);
	}
	
	public void post(HttpExchange exchange, String data, 
            boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
		try{
			if(data != null){
				JSONObject dataObj = (JSONObject) JSONSerializer.toJSON(data);
				String op = dataObj.optString("operation");
				String name = dataObj.optString("name");
				if(op.equalsIgnoreCase("create_model")){
					
					if(!ResourceUtils.devNameIsUnique(PROC_ROOT,name)){
						errors.add("There's already a model named " + name + "; try another name");
						resp.put("status","fail");
						resp.put("errors",errors);
						sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
						return;
					}
					
					JSONObject scriptObj = dataObj.optJSONObject("script");
				
					if(!scriptObj.containsKey("winsize") || !scriptObj.containsKey("func")){
						errors.add("script object must have winsize and func attributes");
						resp.put("status", "fail");
						resp.put("errors",errors);
						sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
						return;
					}
				
					int winsize = scriptObj.optInt("winsize", 0);
					String scriptStr = scriptObj.optString("func");
					boolean materialize = scriptObj.optBoolean("materialize", false);
					
					String cleanScript = scriptObj.toString().trim().replaceAll("\t", " ").replaceAll("\n", " ");
					cleanScript = cleanScript.trim().replace("\\t", " ").replaceAll("\"", "");
					logger.info("CleanScript:" + cleanScript);
				
					/*ModelResource newModelResource = new ModelResource(PROC_ROOT + name + "/", cleanScript, materialize);
					RESTServer.addResource(newModelResource);*/
					sendResponse(exchange, 201, null, internalCall, internalResp);
				}
				
				else {
					errors.add("Unknown operation");
					resp.put("status", "fail");
					resp.put("errors",errors);
					sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
				}
			}
		} catch(Exception e){
			if(e instanceof JSONException){
				errors.add("Invalid JSON");
			} else {
				errors.add(e.getMessage());
			}
			resp.put("status","fail");
			resp.put("errors",errors);
			sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
			
		}
	}
	
	public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
		sendResponse(exchange, 400, null, internalCall, internalResp);
	}
}
