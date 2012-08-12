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
import java.util.Vector;
import java.util.StringTokenizer;

import javax.naming.InvalidNameException;
import java.io.*; 
import java.util.*;

import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;



public class ProcessManagerResource extends Resource {
	
	protected static transient final Logger logger = Logger.getLogger(ProcessManagerResource.class.getPackage().getName());

	//model instances root
	public static final String PROC_ROOT = "/proc/";

    private static JSONArray serverList = null;
    private static JSONObject properties = null;

    //the name of the server (as specified in the config file and the assocaited socket
    private static Hashtable<String, Socket> connections = new Hashtable<String, Socket>();

    //the subid and name of the processing machine where that process runs
    private static Hashtable<String, String> procAssignment = new Hashtable<String, String>();

    public void setup(){
        properties= new JSONObject();
        properties.put("status", "inactive");
        setNewProperties(properties);
        loadConfigFile();
    }

	public ProcessManagerResource() throws Exception, InvalidNameException {
		super(PROC_ROOT);
        setup();
	}

    public void loadConfigFile(){
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
            initObj.put("name", "jortiz");
            initObj.put("host", System.getenv().get("IS4_HOSTNAME"));
            initObj.put("port", System.getenv().get("IS4_PORT"));
            Socket socket = new Socket(host, port);

            if(socket.isConnected()){
                BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                byte[] data = initObj.toString().getBytes();
                bos.write(data, 0, data.length);
                bos.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = null;
                StringBuffer linebuf = new StringBuffer();
                boolean end = false;
                while(!end && (line=reader.readLine())!=null){
                    linebuf.append(line);
                    logger.info("Read line:" + line);
                    try {
                        JSONObject o = (JSONObject)JSONSerializer.toJSON(linebuf.toString());
                        end=true;
                    } catch(Exception e){
                        logger.info("Could not parse: "+ linebuf.toString());
                    }
                }
                JSONObject respObj = (JSONObject)JSONSerializer.toJSON(linebuf.toString());
                if(respObj.getString("stat").equalsIgnoreCase("ok"))
                    connections.put(name, socket);
            } else {
                logger.info("Socket is NOT connected");
            }

        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            return false;
        }
        return true;
    }
	
	
	public void put(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		post(m_request, m_response, path, data, internalCall, internalResp);
	}

    private static void reconnect(String subid){
        String n = procAssignment.get(subid);
        if(n != null){
            Socket s = connections.get(n);
            if(s == null)
                pickServerToRunProcess(subid);
            else if(s!=null && !s.isConnected()){
                try {s.close();}catch(Exception e){}
                connections.remove(n);
                //find the entry and reconnet
                for(int i=0; i<serverList.size(); i++){
                    try {
                        JSONObject thisServerObj = (JSONObject) JSONSerializer.
                                                    toJSON(serverList.get(i));
                        if(thisServerObj.getString("name").equalsIgnoreCase(n)){
                            String host = thisServerObj.getString("host");
                            int port = thisServerObj.getInt("port");
                            Socket sock = new Socket(host, port);
                            if(sock.isConnected())
                                connections.put(n, sock);
                        }
                    } catch(Exception e){
                        logger.log(Level.WARNING, "",e);
                    }
                }
                
            }
        }
    }
	
	public void post(Request m_request, Response m_response, String path, String data, 
            boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
		try{
			if(data != null){
				JSONObject dataObj = (JSONObject) JSONSerializer.toJSON(data);
				String op = dataObj.optString("operation");
				
				if(op.equalsIgnoreCase("save_proc")){
                    handleSaveProc(data, m_request, m_response, internalCall, internalResp);
				} 

                else if(op.equalsIgnoreCase("add_server")){ //&& Security.allowed(POST, "add_server", key)
                    //adds server to the config file
                    //connects to it
                    sendResponse(m_request, m_response, 403, null, internalCall, internalResp);
                } 

                else if(op.equalsIgnoreCase("remove_server")){//&& Security.allowed(POST, "add_server", key)
                    //removes server to the config file
                    //connects to it
                    sendResponse(m_request, m_response, 403, null, internalCall, internalResp);
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
                    sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
                }
				
				else {
					errors.add("Unknown operation");
					resp.put("status", "fail");
					resp.put("errors",errors);
					sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
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
			sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
			
		}
	}
	
	public void delete(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
		sendResponse(m_request, m_response, 403, null, internalCall, internalResp);
	}

    private void handleSaveProc(String dataObjStr, Request m_request, Response m_response, boolean internalCall, JSONObject internalResp){
        JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
        try {
            JSONObject dataObj = (JSONObject)JSONSerializer.toJSON(dataObjStr);
            String name = dataObj.optString("name");
            if(name.equals("") || !ResourceUtils.devNameIsUnique(PROC_ROOT,name)){
                errors.add("There's already a process named " + name + "or it's an empty string; try another name");
                resp.put("status","fail");
                resp.put("errors",errors);
                sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
                return;
            }
            
            JSONObject scriptObj = dataObj.optJSONObject("script");
            if(scriptObj == null){
                errors.add("script object must have 'script' object attribute defined");
                resp.put("status", "fail");
                resp.put("errors",errors);
                sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
                return;
            }
        
            if(!scriptObj.containsKey("winsize") || !scriptObj.containsKey("func")){
                errors.add("script object must have winsize and func attributes");
                resp.put("status", "fail");
                resp.put("errors",errors);
                sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
                return;
            }
        
            int winsize = scriptObj.optInt("winsize", 1);
            String scriptStr = scriptObj.optString("func");
            boolean materialize = scriptObj.optBoolean("materialize", false);
            long timeout = scriptObj.optLong("timeout", 0L);
            
            ProcessResource newProc = new ProcessResource(PROC_ROOT + name + "/", dataObjStr);
            RESTServer.addResource(newProc);
            sendResponse(m_request, m_response, 201, null, internalCall, internalResp);
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            sendResponse(m_request, m_response, 500, null, internalCall, internalResp);
        }
    }

    /**
     * Sends a kill command to the process server for the given subid.
     * @param subid the subscription identifier used to identify the process associated with this subscription.
     */
    public static void killProcessing(String subid){
        //send a message to the server to kill the process that was pushing post-processed
        //data to this associated publisher
        JSONObject killProcessReq = new JSONObject();
        killProcessReq.put("command", "kill");
        killProcessReq.put("name", "jortiz");
        killProcessReq.put("subid", subid);
        sendToProcServer(subid, killProcessReq);
        procAssignment.remove(subid);
    }

    public static void dataReceived(String subid, JSONObject data){
        JSONObject dataFwdObj = new JSONObject();
        dataFwdObj.put("command", "data");
        dataFwdObj.put("name", "jortiz");
        dataFwdObj.put("subid", subid);
        dataFwdObj.put("data", data);
        sendToProcServer(subid, dataFwdObj);
    }

    /**
     * Each process is done on a collection of streams, and a collection of streams is contained within 
     * a subscription; that's why we used the subid to refer to the process.
     * 
     * @param subid the subscription id associated with the process to be started on the process server.
     * @param scriptObjStr the script object in specified Process file creation request.
     * @param pubPath the path for the publisher that represents the output stream of the process.
     */
    public static void startProcess(String subid, String scriptObjStr, String pubPath){
        //communicate with the nodejs processing layer
        logger.info("subid=" + subid + ", pubpath=" + pubPath);
        ProcessPublisherResource p = (ProcessPublisherResource) RESTServer.getResource(pubPath);
        if(p!=null)
            logger.info("Publisher for pubpath=" + pubPath + " is not null");
        String loc = pubPath + "?type=generic&pubid=" + p.getPubId();
        JSONObject startProcReq= new JSONObject();
        startProcReq.put("command", "procinstall");
        startProcReq.put("name", "jortiz");
        JSONObject dstinfo = new JSONObject();
        dstinfo.put("host", System.getenv().get("IS4_HOSTNAME"));
        dstinfo.put("port", System.getenv().get("IS4_PORT"));
        dstinfo.put("path", loc);
        startProcReq.put("dest", dstinfo);
        startProcReq.put("subid", subid);

        String startProcReqStr = startProcReq.toString();
        startProcReqStr = startProcReqStr.substring(0, startProcReqStr.lastIndexOf("}"));
        startProcReqStr = startProcReqStr + ", \"proc_config\":" + scriptObjStr + "}";

        /*System.out.println("HERE::"+startProcReqStr);
        try{JSONObject test = (JSONObject)JSONSerializer.toJSON(startProcReqStr);}
        catch(Exception e){  e.printStackTrace(); System.exit(1);}*/
        //startProcReq.put("script", scriptObjStr);

        if(pickServerToRunProcess(subid))
            sendToProcServer(subid, startProcReqStr);
    }

    public static boolean updateSubEntry(String subid){
        //if successful, save the assignment in the database 
        //(in the subscriptions table)
        logger.info("ServerName[subid=" + subid + "]=" + procAssignment.get(subid));
        String name =procAssignment.get(subid);
        if(name != null){
            Socket sock = connections.get(name);
            String host =null;
            int port = -1;
            if(sock==null){
                logger.warning("Socket is null!");
                return false;
            } else {
                port = sock.getPort(); 
                String hstr = sock.getInetAddress().toString();
                StringTokenizer tokenizer = new StringTokenizer(hstr, "/");
                Vector<String> tokens = new Vector<String>();
                while(tokenizer.hasMoreElements())
                    tokens.add(tokenizer.nextToken());
                host = tokens.get(0);
                logger.info("sock.host=" + host + "\tsock.port=" + sock.getPort());
            }
            return database.updateProcSvrAssignment(subid, procAssignment.get(subid), host, port);
        } else {
            return false;
        }
    }

    private static boolean pickServerToRunProcess(String subid){
        //pick the server to run the process on and associate this id this that server
        int idx = (new java.util.Random()).nextInt(serverList.size());
        JSONObject s = (JSONObject)serverList.get(idx);
        String serverName = s.getString("name");
        if(serverName!=null){
            Socket sock = connections.get(serverName);
            if(sock!=null && sock.isConnected()){
                procAssignment.put(subid,serverName);
                logger.info("assigned::[subid=" + subid + ", server=" + serverName);
                return true;
            }
        }
        return false;
    }

    public static String sendToProcServer(String subid, JSONObject obj){
        String machine_name = procAssignment.get(subid);
        if(machine_name != null){
            Socket sock = connections.get(machine_name);
            if(sock !=null && sock.isConnected()){
                try {
                    BufferedWriter bos = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
                    String data = obj.toString().trim().replaceAll("[^\\x20-\\x7e]", "").trim().replaceAll(" +", " ");;
                    bos.write(data, 0, data.length());
                    bos.flush();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    String line = null;
                    StringBuffer linebuf = new StringBuffer();
                    boolean end = false;
                    while(!end && (line=reader.readLine())!=null){
                        linebuf.append(line);
                        logger.info("Read line:" + line);
                        try {
                            JSONObject o = (JSONObject)JSONSerializer.toJSON(linebuf.toString());
                            end=true;
                            logger.info("end=true");
                        } catch(Exception e){
                            logger.info("Could not parse: "+ linebuf.toString());
                        }
                    }
                    return linebuf.toString();
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                }
            } else if(sock!=null && !sock.isConnected()){
                logger.warning("Socket for subid " + subid + " machine: [" + 
                                sock.getInetAddress().toString() + ", " + sock.getPort() + "]");
              }
        } else {
            logger.info("UNKNOWN::Could not find " + subid + " on any machine");
        }
        return null;
    }

    public static String sendToProcServer(String subid, String obj){
        String machine_name = procAssignment.get(subid);
        if(machine_name != null){
            Socket sock = connections.get(machine_name);
            if(sock !=null && sock.isConnected()){
                try {
                    BufferedWriter bos = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
                    int length1 = obj.getBytes().length;
                    String data = obj.trim().replaceAll("[^\\x20-\\x7e]", "").trim().replaceAll(" +", " ");
                    int length2 = data.getBytes().length;
                    
                    /*byte[] bytes = obj.getBytes();
                    for(int i=0; i<bytes.length; i++)
                        System.out.println(new Byte(bytes[i]).toString());*/
                    System.out.println(data + "\nlength_before=" + length1 + ", length_after=" + length2);
                    //System.exit(1);
                    

                    bos.write(data, 0, data.length());
                    bos.flush();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    String line = null;
                    StringBuffer linebuf = new StringBuffer();
                    boolean end = false;
                    while(!end && (line=reader.readLine())!=null){
                        linebuf.append(line);
                        logger.info("Read line:" + line);
                        try {
                            JSONObject o = (JSONObject)JSONSerializer.toJSON(linebuf.toString());
                            end=true;
                            logger.info("end=true");
                        } catch(Exception e){
                            logger.info("Could not parse: "+ linebuf.toString());
                        }
                    }
                    return linebuf.toString();
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                }
            } else if(sock!=null && !sock.isConnected()){
                logger.warning("Socket for subid " + subid + " machine: [" + 
                                sock.getInetAddress().toString() + ", " + sock.getPort() + "]");
              }
        } else {
            logger.info("UNKNOWN::Could not find " + subid + " on any machine");
        }
        return null;
    }

    private static String getStartProcStr(String subid){
        try {
                String pubPath = database.getSubDestUriStr(UUID.fromString(subid));
                if(pubPath!=null){
                    ProcessPublisherResource p = (ProcessPublisherResource) RESTServer.getResource(pubPath);
                    logger.info("Publisher for pubpath=" + pubPath + " is not null");
                    String loc = pubPath + "?type=generic&pubid=" + p.getPubId();
                    JSONObject startProcReq= new JSONObject();
                    startProcReq.put("command", "procinstall");
                    startProcReq.put("name", "jortiz");
                    JSONObject dstinfo = new JSONObject();
                    dstinfo.put("host", System.getenv().get("IS4_HOSTNAME"));
                    dstinfo.put("port", System.getenv().get("IS4_PORT"));
                    dstinfo.put("path", loc);
                    startProcReq.put("dest", dstinfo);
                    startProcReq.put("subid", subid);

                    String startProcReqStr = startProcReq.toString();
                    startProcReqStr = startProcReqStr.substring(0, startProcReqStr.lastIndexOf("}"));
                    StringTokenizer tokenizer = new StringTokenizer(pubPath, "/");
                    Vector<String> tokens = new Vector<String>();
                    while(tokenizer.hasMoreTokens())
                        tokens.add(tokenizer.nextToken());
                    String parent = "";
                    for(int i=0; i<tokens.size()-1; i++)
                        parent += "/" + tokens.get(i);
                    ProcessResource pparent = (ProcessResource)RESTServer.getResource(parent);
                    startProcReqStr = startProcReqStr + ", \"proc_config\":" +pparent. scriptStr + "}";

                    return startProcReqStr;
                }
        } catch(Exception e){
            logger.log(Level.WARNING, "",e);
        }
        return null;
    }

    public static void loadPrevState(){
        JSONArray v = database.getSubIdToProcServerInfo();
        //logger.info(v.toString());
        //System.exit(1);
        for(int i=0; i<v.size(); i++){
            try {
                JSONObject thisObj = (JSONObject)v.get(i);
                String name = thisObj.getString("name");
                String subid = thisObj.getString("subid");
                procAssignment.put(subid,name);
                logger.info("ProcessManager::PUT[subid=" + subid + ", name=" + name);
                JSONObject existReq = new JSONObject();
                existReq.put("command", "job_status");
                existReq.put("name", "jortiz");
                existReq.put("subid", subid);
                String resp = sendToProcServer(subid, existReq);
                logger.info("resp=" + resp);
                if(resp !=null){
                    String startProcReqStr = getStartProcStr(subid);
                    logger.info("startProcReqStr=" + startProcReqStr);
                    JSONObject respObj = (JSONObject)JSONSerializer.toJSON(resp);
                    if(respObj.getString("stat").equalsIgnoreCase("fail") &&
                        respObj.getInt("code") == 1 && startProcReqStr !=null){
                        sendToProcServer(subid, startProcReqStr);
                    }
                }
            } catch(Exception e){
                logger.log(Level.WARNING, "", e);
            }
        }
    }


    protected class ConnectionCheckerTask extends TimerTask{
        public void run(){
            //checks if every connection is still alive, if not it removes the
            //connection from the list of active connections and
            // re-assigns all the task to other servers
            for(int i=0; i<serverList.size(); i++){
                try {
                    JSONObject configServerEntry = (JSONObject)serverList.get(i);
                    String name = configServerEntry.getString("name");
                    Socket s = connections.get(name);
                    if(s!=null && !s.isConnected()){
                        if(!initiate(configServerEntry)){
                            //pickServerToRunProcess(subid);
                        }
                    }
                } catch(Exception e){
                }
            }
            /*JSONArray v = database.getSubIdToProcServerInfo();
            for(int i=0; i<v.size(); i++){
                try {
                    JSONObject thisObj = (JSONObject)v.get(i);
                    String name = thisObj.getString("name");
                    String subid = thisObj.getString("subid");
                    procAssignment.put(subid,name);
                    logger.info("ProcessManager::PUT[subid=" + subid + ", name=" + name);
                    JSONObject existReq = new JSONObject();
                    existReq.put("command", "job_status");
                    existReq.put("name", "jortz");
                    existReq.put("subid", subid);
                    String resp = sendToProcServer(subid, existReq);
                    if(resp !=null){
                        String startProcReqStr = getStartProcStr(subid);
                        JSONObject respObj = (JSONObject)JSONSerializer.toJSON(resp);
                        if(respObj.getString("stat").equalsIgnoreCase("fail") &&
                            respObj.getInt("code") == 1 && startProcReqStr !=null){
                            sendToProcServer(subid, startProcReqStr);
                        }
                    }
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                }
            }*/
        }
    }

}
