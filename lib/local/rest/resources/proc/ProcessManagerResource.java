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
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;

import com.sun.net.httpserver.*;
import javax.naming.InvalidNameException;
import java.io.*; 

public class ProcessManagerResource extends Resource {
	
	protected static transient final Logger logger = Logger.getLogger(ProcessManagerResource.class.getPackage().getName());

	//model instances root
	public static final String PROC_ROOT = "/proc/";

    private static JSONArray serverList = null;
    private static JSONObject properties = null;

    //the name of the server (as specified in the config file and the assocaited socket
    private static Hashtable<String, Socket> connections = new Hashtable<String, Socket>();

    //the pubid and name of the processing machine where that process runs
    private static Hashtable<String, String> procAssignment = new Hashtable<String, String>();

    private static Socket socket =null;

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

    public boolean initiate(JSONObject configServerEntry){
        logger.info(configServerEntry.toString());
        if(configServerEntry ==null || !configServerEntry.containsKey("host") ||
                !configServerEntry.containsKey("port") || 
                !configServerEntry.containsKey("name")){
            logger.warning("port, name, host must be set");
            return false;
        }

        try {
            String name = configServerEntry.getString("name");
            String host = configServerEntry.getString("host");
            int port = configServerEntry.getInt("port");
            JSONObject initObj = new JSONObject();
            initObj.put("command", "init");
            initObj.put("sfsname", "jortiz");
            socket = new Socket(host, port);

            if(socket.isConnected()){
                BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                byte[] data = initObj.toString().getBytes();
                bos.write(data, 0, data.length);
                bos.flush();
                connections.put(name, socket);
            } else {
                logger.info("Socket is NOT connected");
            }

            //logger.info("closing socket");
            //socket.close();
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
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
				
				if(op.equalsIgnoreCase("save_proc")){
                    handleSaveProc(dataObj, exchange, internalCall, internalResp);
				} 

                else if(op.equalsIgnoreCase("add_server")){ //&& Security.allowed(POST, "add_server", key)
                    //adds server to the config file
                    //connects to it
                    sendResponse(exchange, 403, null, internalCall, internalResp);
                } 

                else if(op.equalsIgnoreCase("remove_server")){//&& Security.allowed(POST, "add_server", key)
                    //removes server to the config file
                    //connects to it
                    sendResponse(exchange, 403, null, internalCall, internalResp);
                }

                else if(op.equalsIgnoreCase("reinit")){
                    Enumeration<String> keys = connections.keys();
                    while(keys.hasMoreElements()){
                        String thisKey = keys.nextElement();
                        Socket thisSock = connections.get(thisKey);
                        if(thisSock != null && !thisSock.isConnected()){
                            thisSock.close();
                            connections.remove(thisKey);
                        }
                    }
                    setup();
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
		sendResponse(exchange, 403, null, internalCall, internalResp);
	}

    private void handleSaveProc(JSONObject dataObj, HttpExchange exchange, boolean internalCall, JSONObject internalResp){
        JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
        try {
            String name = dataObj.optString("name");
            if(name.equals("") || !ResourceUtils.devNameIsUnique(PROC_ROOT,name)){
                errors.add("There's already a process named " + name + "or it's an empty string; try another name");
                resp.put("status","fail");
                resp.put("errors",errors);
                sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
                return;
            }
            
            JSONObject scriptObj = dataObj.optJSONObject("script");
            if(scriptObj == null){
                errors.add("script object must have 'script' object attribute defined");
                resp.put("status", "fail");
                resp.put("errors",errors);
                sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
                return;
            }
        
            if(!scriptObj.containsKey("winsize") || !scriptObj.containsKey("func")){
                errors.add("script object must have winsize and func attributes");
                resp.put("status", "fail");
                resp.put("errors",errors);
                sendResponse(exchange, 200, resp.toString(), internalCall, internalResp);
                return;
            }
        
            int winsize = scriptObj.optInt("winsize", 1);
            String scriptStr = scriptObj.optString("func");
            boolean materialize = scriptObj.optBoolean("materialize", false);
            long timeout = scriptObj.optLong("timeout", 0L);
            
            /*String cleanScript = scriptObj.toString().trim().replaceAll("\t", " ").replaceAll("\n", " ");
            cleanScript = cleanScript.trim().replace("\\t", " ").replaceAll("\"", "");
            logger.info("CleanScript:" + cleanScript);*/
        
            /*ModelResource newModelResource = new ModelResource(PROC_ROOT + name + "/", cleanScript, materialize);
            RESTServer.addResource(newModelResource);*/
            ProcessResource newProc = new ProcessResource(PROC_ROOT + name + "/", scriptObj.toString());
            RESTServer.addResource(newProc);
            sendResponse(exchange, 201, null, internalCall, internalResp);
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            sendResponse(exchange, 500, null, internalCall, internalResp);
        }
    }

    public static void killProcessing(ProcessPublisherResource r){
        //send a message to the server to kill the process that was pushing post-processed
        //data to this associated publisher
        String processServerName = procAssignment.get(r.getPubId());
        if(r!=null && processServerName!=null){
            Socket thisSocket = connections.get(processServerName);
            if(thisSocket !=null && thisSocket.isConnected()){
                try {
                    JSONObject killProcessReq = new JSONObject();
                    killProcessReq.put("command", "kill");
                    killProcessReq.put("pubid", r.getPubId());

                    BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                    byte[] data = killProcessReq.toString().getBytes();
                    bos.write(data, 0, data.length);
                    bos.flush();
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                }
            }

        }
    }

    public static void dataReceived(String pubPath, JSONObject data){
        //send it to the associated socket
        ProcessPublisherResource r = (ProcessPublisherResource)RESTServer.getResource(pubPath);
        if(r !=null){
            String pubid = r.getPubId().toString();
            String serverName = procAssignment.get(pubid);
            String loc = "http://" + System.getenv().get("IS4_HOSTNAME") + ":" +
                                System.getenv().get("IS4_PORT") + pubPath + 
                                "?type=generic&pubid=" + r.getPubId();
            if(serverName !=null){
                Socket s = connections.get(serverName);
                if(s!=null && s.isConnected()){
                    try {
                        JSONObject dataFwdObj = new JSONObject();
                        dataFwdObj.put("command", "process");
                        dataFwdObj.put("pubid", pubid);
                        dataFwdObj.put("location", loc);
                        dataFwdObj.put("data", data);

                        //write to the socket
                        BufferedOutputStream bos = new BufferedOutputStream(s.getOutputStream());
                        byte[] dataBytes = dataFwdObj.toString().getBytes();
                        bos.write(dataBytes, 0, dataBytes.length);
                        bos.flush();
                    } catch(Exception e){
                        logger.log(Level.WARNING, "", e);
                    }
                }
            }
        }
    }




}
