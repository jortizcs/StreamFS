package local.rest.resources.proc;

import local.rest.resources.*;
import local.db.*;
import local.rest.*;
import local.rest.resources.util.*;
import local.rest.interfaces.*;
import is4.*;

import sfs.proc.msg.ProcessMessageProto.ProcessMessage;

import net.sf.json.*;

import java.net.*;
import java.util.concurrent.*;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.naming.InvalidNameException;
import java.io.*; 
import java.util.*;
import java.nio.channels.*;

import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;



public class ExtProcessManager extends Thread{
	
	protected static transient final Logger logger = Logger.getLogger(ExtProcessManager.class.getPackage().getName());

    //main port
    private static String sfsproc_host = "localhost";
    private static final int sfsproc_port = 7762;
    private static ServerSocket serverSocket = null;

    private static ExtProcessManager extProcMngr = null;

    protected static MySqlDriver database = (MySqlDriver) DBAbstractionLayer.database;   

    //root resource
    private static Resource root=null;

    //default key
    private static UUID key = new UUID(1L, 0L);

    //active bit
    private static boolean active = false;

    /*************************************/
    //  Path and process management
    /*************************************/
    //paths associated with external processes
    private static ArrayList<String> parentPaths = new ArrayList<String>();

    //paths assocaited with process instances running on those machines
    //the parentPath is a substring of each instancePath
    private static ArrayList<String> childPaths = new ArrayList<String>();

    //the socket associated with an external process
    //indexed by parent process name
    private static ConcurrentHashMap<String, Socket> assocProcSocket = null;

    //maps subid to the syncPath (publisher associated with the instance of the running
    //external process)
    private static ConcurrentHashMap<UUID, String> subToProcMap = null;
    /******************************/

    public static void main(String[] args){
        ExtProcessManager extProcManager = ExtProcessManager.getInstance();
        ExtProcessManager extProcManager2 = ExtProcessManager.getInstance();
    }

    public static String getKeyId(){
        return key.toString();
    }

    public static ExtProcessManager getInstance(){
        if(extProcMngr == null)
            extProcMngr = new ExtProcessManager();
        if(!active)
            extProcMngr.start();
        return extProcMngr;
    }

    public void dataReceived(String subid, JSONObject dataObj){
        //forward it to the associated process
        Socket s=null;
        try {
            UUID subId = UUID.fromString(subid);
            if(subToProcMap.containsKey(subId)){
                String path = ResourceUtils.cleanPath(subToProcMap.get(subId));
                String ppath = ResourceUtils.getParent(path);
                if(assocProcSocket.containsKey(ppath)){

                    logger.info("associated process socket found!\npath=" + path + "\nparent=" + ppath);
                    ProcessMessage pm = ProcessMessage.newBuilder()
                        .setType(ProcessMessage.ProcessMessageType.DATA)
                        .setPath(path)
                        .setSubid(subid)
                        .setData(dataObj.toString())
                        .build();
                    s = assocProcSocket.get(ppath);
                    OutputStream out = s.getOutputStream();
                    pm.writeDelimitedTo(out);
                    out.flush();
                } else{
                    logger.info("Could not locate socket for process path: " + path +
                            "\tsubid=" + subid);
                }
            } else {
                logger.info("Could not forward; unknown subid: " + subid.toString());
            }
        } catch(Exception e){
            e.printStackTrace();
            if(e instanceof IOException && (s.isClosed() || !s.isConnected())){
                //clean it up
                String path = subToProcMap.get(UUID.fromString(subid));
                Resource thisResource = null;
                JSONObject respObj = new JSONObject();
                if(parentPaths.contains(path)){
                    subToProcMap.remove(UUID.fromString(subid));
                    parentPaths.remove(path);
                    assocProcSocket.remove(path);

                    //remove the associated files and child paths in local list
                    for(int i=0; i<childPaths.size(); i++){
                        String thisPath = childPaths.get(i);
                        if(thisPath.startsWith((path))){
                            childPaths.remove(path);

                            //remove from streamfs
                            thisResource = RESTServer.getResource(thisPath);
                            String p1 = ResourceUtils.cleanPath(thisResource.getURI());
                            if(thisResource!=null && thisPath.equals(p1))
                                thisResource.delete(null, null, thisPath, true, respObj);
                        }
                    }

                    //remove from streamfs
                    thisResource = RESTServer.getResource(path);
                    String p1 = ResourceUtils.cleanPath(thisResource.getURI());
                    if(thisResource!=null && p1.equals(path))
                        thisResource.delete(null, null, path, true, respObj);
                } else if(childPaths.contains(path)){
                    //remove the associated file
                    childPaths.remove(path);

                    //remove it from streamfs
                    thisResource = RESTServer.getResource(path);
                    String p1 = ResourceUtils.cleanPath(thisResource.getURI());
                    if(thisResource!=null && path.equals(p1))
                        thisResource.delete(null, null, path, true, respObj);
                }
                //close it
                try{
                    s.close();
                } catch(Exception e2){
                    e2.printStackTrace();
                }
            }
        }
    }

    public void remove(String path, boolean close){
        //normalize the path
        path = ResourceUtils.cleanPath(path);

        //remove this resource reference, locally, and ...
        Resource r = RESTServer.getResource(path);
        String rpath = ResourceUtils.cleanPath(r.getURI());
        JSONArray children = database.rrGetChildren(path);
        logger.info("path=" + path + "; rpath=" + rpath + ";\tchildren: " + children);
        Socket s=null;
        try {
            if(children.size()==0 && rpath.equals(path)){
                //grab socket reference
                assocProcSocket.get(path);
                if(s==null){
                    String ppath = ResourceUtils.getParent(path);
                    s = assocProcSocket.get(ppath);
                }

                if(s!=null){
                    //send a destroy message to the remote client
                    ProcessMessage m = ProcessMessage.newBuilder()
                            .setType(ProcessMessage.ProcessMessageType.DESTROY)
                            .setPath(path)
                            .build();
                    //tell the associated client, close the socket, remove association
                    OutputStream out = s.getOutputStream();
                    m.writeDelimitedTo(out);
                    out.flush();
                } else {
                    logger.info("socket is null");
                }

                if(r.getType()==ResourceUtils.EXTPROC_RSRC){
                    //remove the socket reference
                    assocProcSocket.remove(path);

                    //remove the resource reference
                    synchronized(parentPaths){
                        parentPaths.remove(path);
                    }
                } else {
                    //tell the associated client to stop running this process
                    //remove the resource reference
                    synchronized(childPaths){
                        childPaths.remove(path);
                    }

                    r= RESTServer.getResource(path);
                    String p1 = ResourceUtils.cleanPath(r.getURI());
                    System.out.println(r + "\np1=" + p1 + "\tpath=" + path);
                    if(r!=null && path.equals(p1)){
                        GenericPublisherResource streamr = (GenericPublisherResource)r;
                        System.out.println("subids=" + database.getAllSubIds());
                        JSONArray subidsArray = database.getSubIdByDstPubPath(path);
                        System.out.println(subidsArray);
                        for(int i =0; i<subidsArray.size(); i++){
                            try {
                                String subidStr = (String)subidsArray.get(i);
                                String subPath = database.getSubUriBySubId(
                                        UUID.fromString(subidStr));
                                //remove the subscription associated with it
                                if(subPath !=null){
                                    r = RESTServer.getResource(subPath);
                                    if(r !=null && r instanceof SubscriptionResource){
                                        JSONObject inresp = new JSONObject();
                                        SubscriptionResource sr  = (SubscriptionResource)r;
                                        System.out.println("deleting " + sr.getURI());
                                        if(sr!=null)
                                            sr.delete(null, null, sr.getURI(), true, inresp);
                                    }
                                }
                            } catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            try {
                if(close)
                    s.close();
            } catch(Exception e2){
            }
        }
    }

    public boolean addSub(UUID subid, String sinkPath){

        String sinkPathNorm = ResourceUtils.cleanPath(sinkPath);
        String parentSinkPath = ResourceUtils.getParent(sinkPathNorm);
        logger.info("subid=" + subid.toString() + ", sinkPathNorm=" + sinkPathNorm + 
                "\tparentSinkPath=" + parentSinkPath + 
                "\tcontains(" +parentSinkPath+")?=" + assocProcSocket.containsKey(parentSinkPath)); 
        if(!subToProcMap.containsKey(subid) && 
                assocProcSocket.containsKey(parentSinkPath)){

            //get the parent and send it to the process there to start it
            Socket s = assocProcSocket.get(parentSinkPath);
            logger.info(s.toString());
            if(s!=null){

                //send a response back to the client process
                ProcessMessage m = ProcessMessage.newBuilder()
                            .setType(ProcessMessage.ProcessMessageType.START)
                            .setPath(sinkPath)
                            .setSubid(subid.toString())
                            .build();
                logger.info("Sending::\n\n" + m + "\n\n");
                try{
                    OutputStream out = s.getOutputStream();
                    m.writeDelimitedTo(out);
                    out.flush();


                    //assume success -- response handled in main thread
                    //save the reference locally
                    synchronized(childPaths){
                        childPaths.add(sinkPathNorm);
                    }

                    //save the subid reference
                    subToProcMap.put(subid, sinkPathNorm);
                    logger.info("addSub::added: " + subid + "; path=" + sinkPathNorm);
                    return true;
                } catch(Exception e){
                    //nothing
                    e.printStackTrace();
                    Resource r = RESTServer.getResource(sinkPath);
                    if(r!=null && 
                            ResourceUtils.cleanPath(r.getURI()).equals(
                                ResourceUtils.cleanPath(sinkPath)))
                    {
                        r.delete(null, null, sinkPath, true, new JSONObject());
                        logger.info(subid.toString() + " install FAILED; path=" + sinkPath);
                    }
                    
                    return false;
                }
            }
        } else if(subToProcMap.containsKey(subid) && assocProcSocket.containsKey(parentSinkPath)){
            //no need to start the process -- it has already been started.
            logger.info(subid.toString() + " installed; path=" + sinkPath);
            //save the reference locally
            synchronized(childPaths){
                childPaths.add(sinkPathNorm);
            }
            return true;
        } else {
            logger.info("Something happened");
        }
        return false;
    }

    public void removeSub(UUID subid){
        if(subid!=null && subToProcMap.containsKey(subid)){
            String sinkPathNorm = ResourceUtils.cleanPath(subToProcMap.get(subid));
            String parentSinkPath = ResourceUtils.getParent(sinkPathNorm);
            if(subToProcMap.containsKey(subid) && 
                    assocProcSocket.containsKey(parentSinkPath)){
                //get the parent and send it to the process there to start it
                Socket s = assocProcSocket.get(parentSinkPath);
                if(s!=null){

                    //send a response back to the client process
                    ProcessMessage m = ProcessMessage.newBuilder()
                                .setType(ProcessMessage.ProcessMessageType.DESTROY)
                                .setPath(sinkPathNorm)
                                .setSubid(subid.toString())
                                .build();
                    try{
                        OutputStream out = s.getOutputStream();
                        m.writeDelimitedTo(out);
                        out.flush();
                    } catch(Exception e){
                        e.printStackTrace();
                    }

                    //save the reference locally
                    childPaths.remove(sinkPathNorm);

                    //save the subid reference
                    subToProcMap.remove(subid);

                    //remove the streamfs file
                    Resource r = RESTServer.getResource(sinkPathNorm);
                    String p1=null;
                    if(r!=null && (p1 = ResourceUtils.cleanPath(r.getURI()))!=null && p1.equals(sinkPathNorm))
                        r.delete(null, null, sinkPathNorm, true, new JSONObject());
                }
            }
        }
    }

	private ExtProcessManager() {
        subToProcMap = new ConcurrentHashMap<UUID, String>();
        assocProcSocket  = new ConcurrentHashMap<String, Socket>();
        active = true;
        start();
	}

    public void run(){
        logger.info("Starting");
        while(active){
            try {
                if(serverSocket==null){
                    InetAddress addr = InetAddress.getByName(sfsproc_host);
                    serverSocket = new ServerSocket(sfsproc_port, 0, addr);
                }
                logger.info("listening...");
                Socket connectionSocket = serverSocket.accept();
                logger.info("connection made");
                ProcIOHandlerThread t = new ProcIOHandlerThread(connectionSocket);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public class ProcIOHandlerThread extends Thread{
        private boolean started = false;
        private Socket connectionSocket = null;

        public ProcIOHandlerThread(Socket s){
            try {
                if(connectionSocket == null)
                    connectionSocket = s;
                if(!started){
                    started = true;
                    start();
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        public void run(){
            logger.info("Started ProcIOHandlerThread");
            while(process()){}
        }

        public boolean process() {
            try {
                logger.info("Processing");
                if(!connectionSocket.isClosed()){
                    //get the next response from streamfs
                    logger.info("reading 1");
                    InputStream input = connectionSocket.getInputStream();
                    logger.info("reading 2::available=" + input.available());
                    ProcessMessage pmsg = ProcessMessage.parseDelimitedFrom(input);
                    logger.info("reading 3");

                    switch(pmsg.getType()){
                        case INSTALL:
                            logger.info("INSTALL");
                            handleInstall(connectionSocket, pmsg);
                            break;
                        case DATA:
                            logger.info("DATA");
                            handleData(connectionSocket, pmsg);
                            break;
                        case DESTROY:
                            logger.info("DESTROY");
                            handleDestroy(connectionSocket, pmsg);
                            break;
                        case PING:
                            logger.info("PING");
                            handlePing(connectionSocket, pmsg);
                            break;
                        case KEY:
                            logger.info("KEY");
                            handleKey(connectionSocket, pmsg);
                            break;
                        case START_OK:
                            logger.info("Process started;");
                            break;
                        case START_FAIL:
                            logger.info("Process start failed");
                            break;
                        default:
                            logger.info("nothing");
                            break;
                    }
                    return true;
                } else {
                    return false;
                }
            } catch(Exception e){
                e.printStackTrace();
                if(e instanceof NullPointerException)
                    return false;
                return true;
            }

        }

        public void handlePing(Socket s, ProcessMessage msg){
        }

        public void handleInstall(Socket s, ProcessMessage msg){
            try {
                String name = msg.getProcname();
                String homePath = ResourceUtils.cleanPath(msg.getPath());
                logger.info("name=" + name + "; path=" + homePath);
                Resource testR;
                if((testR=RESTServer.getResource(homePath))!=null && 
                        !ResourceUtils.cleanPath(testR.getURI()).equals(homePath)){
                    //create the default resource and update the maps
                    if(root==null)
                        root = RESTServer.getResource("/");
                    logger.info("root=" + root);
                    JSONObject dat = new JSONObject();
                    dat.put("operation", "create_resources");
                    JSONArray list = new JSONArray();
                    JSONObject hpath = new JSONObject();
                    hpath.put("path", homePath);
                    hpath.put("type", "default");
                    list.add(hpath);
                    dat.put("list", list);
                    logger.info("Sending to root:\n" + dat.toString());
                    JSONObject ret = new JSONObject();
                    root.put(null, null, "/", dat.toString(), true, ret);

                    logger.info("return values: " + ret.toString());

                    //Resource r=null;
                    ProcessMessage m = null;
                    Resource r=null;
                    if((r=RESTServer.getResource(homePath))!=null){

                        //add it to list of parent paths
                        String ppath = ResourceUtils.cleanPath(r.getURI());
                        if(!parentPaths.contains(ppath)){
                            synchronized(parentPaths){
                                parentPaths.add(ppath);
                            }
                        }

                        //add it to socket mapping
                        logger.info("Adding " + ppath);
                        if(!assocProcSocket.containsKey(ppath))
                            assocProcSocket.put(ppath, s);

                        //update the resource properties and set type
                        System.out.print(homePath + " created");
                        logger.info("\tr.uri=" + r.getURI());
                        if(msg.hasData()){
                            try {
                                JSONObject p = new JSONObject();
                                String description = msg.getData();
                                p.put("description", description);
                                dat.clear();
                                dat.put("operation", "update_properties");
                                dat.put("properties", p);
                                logger.info(dat.toString());
                                r.handlePropsReq(null, null, dat.toString(), true, ret);
                                logger.info(ret.toString());

                                //change to external processes type
                                r.setAsExtProc();
                            } catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                        //send a response back to the client process
                        m = ProcessMessage.newBuilder()
                                        .setType(ProcessMessage.ProcessMessageType.INSTALL_OK)
                                        .setPath(homePath)
                                        .build();
                    } else {
                        //send a response back to the client process
                        m = ProcessMessage.newBuilder()
                                        .setType(ProcessMessage.ProcessMessageType.INSTALL_FAILED)
                                        .setPath(homePath)
                                        .setData(ret.toString())
                                        .build();
                    }
                    OutputStream out = s.getOutputStream();
                    m.writeDelimitedTo(out);
                    out.flush();
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        public void handleData(Socket s, ProcessMessage msg){
            try {
                /*
                path: "/proclib/pure_consumer/1e0b1c8d/"
                data: "{\"tss\":[0],\"vals\":[[1]]}"
                type: DATA
                key: "00000000-0000-0001-0000-000000000000"
                */

                logger.info("Data received: " + msg);
                Resource r = RESTServer.getResource(msg.getPath());
                String rpath = ResourceUtils.cleanPath(r.getURI());
                String mpath = ResourceUtils.cleanPath(msg.getPath());
                if(msg.getKey().equals(key.toString()) && rpath.equals(mpath) && r instanceof GenericPublisherResource){
                    JSONObject dataobj = new JSONObject();
                    UUID pubid = ((GenericPublisherResource)r).getPubId();
                    dataobj.put("path", rpath);
                    dataobj.put("pubid", pubid.toString());
                    
                    JSONArray dataArray = new JSONArray();
                    JSONObject msgDataObj = (JSONObject)JSONSerializer.toJSON(msg.getData());
                    JSONArray tssArray = msgDataObj.getJSONArray("tss");
                    JSONArray valsArray = msgDataObj.getJSONArray("vals");
                    //for now we'll only do this for one timestamp, later we'll do it for a multi-stream resource
                    for(int i=0; i<1/*tssArray.size()*/; i++){
                        JSONObject dataptObj = new JSONObject();
                        long thisTs = tssArray.getLong(i);
                        JSONArray theseVals = valsArray.getJSONArray(i);
                        for(int j=0; j<theseVals.size(); j++){
                            double thisVal = theseVals.getDouble(j);
                            if(thisTs>0)
                                dataptObj.put("ts", thisTs);
                            dataptObj.put("value", thisVal);
                            dataArray.add(dataptObj);
                        }
                    }
                    dataobj.put("data", dataArray);
                    logger.info("POST::path=" + mpath + "; pubid=" + pubid + ";data=" + dataobj);
                    JSONObject internalRespObj = new JSONObject();
                    ((GenericPublisherResource)r).handleBulkDataPost(null, null, rpath,
                        dataobj.toString(), true, internalRespObj);
                }
                else {
                    logger.warning("Could not post the data");
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        public void handleDestroy(Socket s, ProcessMessage msg){
            try {
                logger.info("Data received: " + msg);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        
        public void handleKey(Socket s, ProcessMessage msg){
            try {
                ProcessMessage m = ProcessMessage.newBuilder()
                                    .setType(ProcessMessage.ProcessMessageType.KEY)
                                    .setKey(key.toString())
                                    .build();
                OutputStream out = s.getOutputStream();
                m.writeDelimitedTo(out);
                out.flush();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
