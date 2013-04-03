package sfs.proc;

import sfs.proc.msg.DataMessageProto.*;
import sfs.proc.msg.ProcessMessageProto.*;
import sfs.proc.console.msg.ConsoleMessageProto.*;

import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.*;
import org.json.simple.parser.*;

public class SfsExtProcMngr {
    private static boolean active = false;
    private static final int sfsproc_port = 7762;
    private static final int admin_port = 7763;
    private static Socket connectionSocket = null;
    private static String configPath = "config/config.json";
    private static String configFileContents = null;
    private static UUID key = null;
    private static String sfsproc_host = "localhost";

    private static ConcurrentHashMap<String, ProcessInfo> processMap = null;
    private static final JSONParser parser = new JSONParser();

    public SfsExtProcMngr(String host, int port){
        processMap = new ConcurrentHashMap<String, ProcessInfo>();
        active = true;
    }

    public static void main(String[] args){
        SfsExtProcMngr mngr = new SfsExtProcMngr("localhost", sfsproc_port);
        mngr.start();
    }

    public void start(){
        try {
            ConsoleThread consoleThread = new ConsoleThread();
            consoleThread.start();
        } catch(Exception e){
            e.printStackTrace();
        }
        setSfsInfo();
        InputStream input=null;
        OutputStream output=null;
        boolean registered = false;
        while(active){
            try {
                if(connectionSocket==null || connectionSocket.isClosed() || 
                        !connectionSocket.isConnected()){
                    System.out.println("connection socket is null");
                    connectionSocket = null;
                    InetAddress addr = InetAddress.getByName(sfsproc_host);
                    connectionSocket = new Socket(addr, sfsproc_port);
                    input = connectionSocket.getInputStream();
                    output = connectionSocket.getOutputStream();
                    registered = false;
                }

                //fetch the registration key from streamfs
                if(key==null){
                    fetchKey(output);
                } else if(!registered){
                    registered=register(input, output);
                }

                System.out.println("waiting for a data from server");

                //get the next response from streamfs
                ProcessMessage pmsg = ProcessMessage.parseDelimitedFrom(input);
                System.out.println("Client.msg_received_event::" + pmsg);
                switch(pmsg.getType()){
                    case START:
                        System.out.println("Starting message received");
                        ProcessMessage pm=null;
                        if(handleStart(pmsg)){
                            pm = ProcessMessage.newBuilder()
                                .setPath(cleanPath(pmsg.getPath()))
                                .setType(ProcessMessage.ProcessMessageType.START_OK)
                                .setKey(key.toString())
                                .build();
                        } else {
                            pm = ProcessMessage.newBuilder()
                                .setPath(cleanPath(pmsg.getPath()))
                                .setType(ProcessMessage.ProcessMessageType.START_FAIL)
                                .setKey(key.toString())
                                .build();
                        }
                        System.out.println("Sending::\n" + pm);
                        pm.writeDelimitedTo(output);
                        output.flush();
                        break;
                    case DATA:
                        handleData(pmsg);
                        break;
                    case DESTROY:
                        handleDestroy(pmsg);
                        break;
                    case PING:
                        handlePing(pmsg);
                        break;
                    case KEY:
                        if(!saveKey(pmsg)){
                            System.err.println("Could not attain key from StreamFS server");
                            System.exit(1);
                        }
                        break;
                }
            } catch(Exception e){
                //e.printStackTrace();
                try {
                    Thread.sleep(10*1000);
                } catch(Exception e2){
                    e2.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    public boolean register(InputStream input, OutputStream output){
        System.out.println("registering new process");
        boolean anyok = false;
        try {
            String configStr = getConfigFileContents();
            JSONParser parser = new JSONParser();
            JSONObject configObj = (JSONObject)parser.parse(configStr);
            JSONArray procArray = (JSONArray)configObj.get("processes");
            for (int i=0; i<procArray.size(); i++){
                JSONObject thisProc = (JSONObject)procArray.get(i);
                String thisName = (String)thisProc.get("name");
                String thisPath = cleanPath((String)thisProc.get("path"));
                String desc = null;
                if(thisProc.containsKey("description"))
                    desc = (String)thisProc.get("description");
                System.out.println("name=" + thisName + "; path=" + thisPath + "; desc=" + desc);
                ProcessMessage.Builder mb = ProcessMessage.newBuilder()
                                        .setType(ProcessMessage.ProcessMessageType.INSTALL)
                                        .setProcname(thisName)
                                        .setPath(thisPath);
                if(desc!=null)
                    mb.setData(desc);
                ProcessMessage m = mb.build();
                System.out.println("Sending_message::" + m);
                m.writeDelimitedTo(output);
                output.flush();
                System.out.println("flushed " + m);// + "\t" + output + "::end");

                //get the next response from streamfs
                ProcessMessage pmsg = ProcessMessage.parseDelimitedFrom(input);
                System.out.println("Data received::\n" + pmsg);
                switch(pmsg.getType()){
                    case INSTALL_OK:
                        if(pmsg.hasData()){
                            anyok |= false;
                            System.out.println(pmsg.getData());
                        } else{
                            anyok |= true;
                            ProcessInfo pinfo = new ProcessInfo(null, thisPath, thisProc);
                            processMap.put(thisPath, pinfo);
                        }
                        break;
                    default:
                        anyok |= false;
                        break;
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return anyok;
    }
    
    public void fetchKey(OutputStream output){
        System.out.println("fetching key");
        try {
            ProcessMessage m = ProcessMessage.newBuilder()
                                    .setType(ProcessMessage.ProcessMessageType.KEY)
                                    .build();
            m.writeDelimitedTo(output);
            output.flush();
            System.out.println("flushed " + m);// + "\t" + output);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    
    private boolean saveKey(ProcessMessage keyMsg){
        try {
            if(keyMsg.hasKey()){
                String keyStr = keyMsg.getKey();
                key = UUID.fromString(keyStr);
                System.out.println("Key::" + key.toString());
                return true;
            } else {
                System.out.println("NO_Key");
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return false;

    }

    private boolean handleStart(ProcessMessage startMsg){
        boolean started = false;
        try {
            String path = cleanPath(startMsg.getPath());
            String ppath = getParent(path);

            System.out.println(processMap);
            System.out.println(ppath);
            System.out.println(processMap.containsKey(ppath));
            if(processMap.containsKey(ppath)){
                ProcessInfo parentinfo = processMap.get(ppath);
                JSONObject thisProc = parentinfo.getProcessDef();

                //start it
                try {
                    String workingDir = (String)thisProc.get("working_dir");
                    String cmd = (String) thisProc.get("command");
                    JSONArray argList = (JSONArray) thisProc.get("arguments");
                    JSONArray env = (JSONArray) thisProc.get("env");

                    //build command list and pass it to process builder
                    ArrayList<String> procCmds = new ArrayList<String>();
                    procCmds.add(cmd);
                    for(int j=0; j<argList.size(); j++)
                        procCmds.add((String)argList.get(j));
                    System.out.println(procCmds);

                    //start the process
                    ProcessBuilder pbuilder = new ProcessBuilder(procCmds);
                    pbuilder.directory(new File(workingDir));
                    pbuilder.redirectErrorStream(true);
                    Process p = pbuilder.start();
                    started = isRunning(p);

                    if(started){
                        //record it
                        ProcessInfo newProcInfo = new ProcessInfo(p, path, null);
                        processMap.put(path, newProcInfo);

                        //set up a thread to communicate with it
                        ProcessCommThread pcommt = new ProcessCommThread(path, p);
                        pcommt.start();
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            } else {
                System.out.println("what the fuck");
                System.exit(1);
            }

        } catch(Exception e){
            e.printStackTrace();
            if(e instanceof NullPointerException){
            } else if(e instanceof IndexOutOfBoundsException){
            } else if(e instanceof SecurityException){
            } else if(e instanceof IOException){
            }
        }
        return started;
    }

    private void handleData(ProcessMessage pMsg){
        try {
            System.out.println("handleData()::pMsg=" + pMsg);
            String path = cleanPath(pMsg.getPath());
            if(processMap.containsKey(path)){
                ProcessInfo pinfo = processMap.get(path);
                Process thisproc = pinfo.getProcess();
                OutputStream output = thisproc.getOutputStream();

                //get the timestamp-value pairs
                JSONObject dataObj = (JSONObject)parser.parse(pMsg.getData());
                System.out.println("dataObj=" + dataObj.toString());
                long ts = ((Long)dataObj.get("ts")).longValue();
                Object vobj = dataObj.get("value");
                TimeValSet.Builder tvb = TimeValSet.newBuilder();
                tvb=tvb.setTs(ts);
                if(vobj instanceof JSONArray){
                    JSONArray vals = (JSONArray)vobj;
                    for(int i=0; i<vals.size(); i++)
                        tvb.addVal(((Double)vals.get(i)).longValue());
                } else {
                    long v = ((Long)vobj).longValue();
                    tvb =tvb.addVal(v);
                }
               
                //build the data values 
                TimeValSet tv = tvb.build();
                //construct the message to send to the process
                DataMessage dm = DataMessage.newBuilder()
                    .setType(DataMessage.DataMessageType.DATA_IN)
                    .addData(tv)
                    .build();

                //send it to the process
                dm.writeDelimitedTo(output);
                output.flush();
            }
        } catch(Exception e){
            e.printStackTrace();
            if(e instanceof IOException){
                //buffer the message
                //try to restart it and send it the data
                //if it fails, tell streamfs it failed.
            }
        }
    }

    private void handleDestroy(ProcessMessage destroyMsg){
        String inputPath = cleanPath(destroyMsg.getPath());
        Process p = null;
        System.out.println("ContainsKey?" + processMap.containsKey(inputPath));
        System.out.println("Has the process? " + processMap.get(inputPath).getProcess());
        if(processMap.containsKey(inputPath) && (p=processMap.get(inputPath).getProcess())!=null){
            p.destroy();
            processMap.remove(inputPath);
            System.out.println("Killed process; path=" + inputPath);
        }
    }

    private void handlePing(ProcessMessage pingMsg){
    }

    private static String getConfigFileContents(){
        if(configFileContents ==null){
            File aFile = new File(configPath);
            StringBuilder contents = new StringBuilder();
            
            try {
                //use buffering, reading one line at a time
                //FileReader always assumes default encoding is OK!
                BufferedReader input =  new BufferedReader(new FileReader(aFile));
                try {
                    String line = null; //not declared within while loop
                    /*
                     * readLine is a bit quirky :
                     * it returns the content of a line MINUS the newline.
                     * it returns null only for the END of the stream.
                     * it returns an empty String if two newlines appear in a row.
                     */
                    while (( line = input.readLine()) != null){
                        contents.append(line);
                        contents.append(System.getProperty("line.separator"));
                    }
                }
                finally {
                    input.close();
                }
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
            configFileContents = contents.toString();
        }
        return configFileContents;
    }


    private boolean setSfsInfo(){
        String contents = getConfigFileContents();
        if(contents !=null){
            try{
                JSONParser parser = new JSONParser();
                JSONObject obj = (JSONObject)parser.parse(contents);
                if(obj.containsKey("sfshost")){
                    sfsproc_host = (String)obj.get("sfshost");
                    return true;
                }else {
                    System.err.println("Config file must specify `sfshost' and `sfsport'");
                    System.exit(1);
                }
            }catch(Exception e){
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.err.println("No contents in config file");
            System.exit(1);
        }
        return false;
    }

    private void reload(){
        String contents = getConfigFileContents();
		try {
			//parse it
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(contents);
            JSONArray procs = (JSONArray)obj.get("processes");
            for(int j=0; j<procs.size(); j++){
                JSONObject thisproc = (JSONObject)procs.get(j);
                String path = (String)thisproc.get("path");
                if(processMap.containsKey(path)){
                    //build the process
                    StringBuffer fullcmd = new StringBuffer();
                    String cmd = (String)obj.get("command");
                    JSONArray argList =  (JSONArray)obj.get("arguments");
                    fullcmd.append(cmd).append(" ");
                    for(int i=0; i<argList.size(); i++)
                        fullcmd.append((String)argList.get(i)).append(" ");
                    ProcessBuilder pb = new ProcessBuilder(fullcmd.toString());
                    Map<String, String> env = pb.environment();
                    JSONArray evars = (JSONArray)obj.get("env");
                    for(int i=0; i<evars.size(); i++)
                        env.putAll((Map)evars.get(i));

                    if(obj.containsKey("working_dir"))
                        pb.directory(new File((String)obj.get("working_dir")));
                }
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private static boolean isRunning(Process p){
        try {
            p.exitValue();
            return false;
        } catch(IllegalThreadStateException e) {
            return true;
        }
    }

    public static String cleanPath(String path){
        //clean up the path
        if(path == null)
            return path;

        if(!path.startsWith("/"))
            path = "/" + path;
        path = path.replaceAll("/+", "/");
        /*if(path.endsWith("/"))
            path = path.substring(0, path.length()-1);*/
        if(!path.endsWith("/"))
            path += "/";
        return path;
    }

    public static String getParent(String path){
        path = cleanPath(path);
        if(path==null || path == "/")
            return "/";
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        Vector<String> tokens = new Vector<String>();
        while(tokenizer.hasMoreTokens())
            tokens.add(tokenizer.nextToken());
        StringBuffer buf = new StringBuffer();
        if(tokens.size()==1){
            buf.append("/");
        } else {
            for(int i=0; i<tokens.size()-1; i++)
                buf.append("/").append(tokens.elementAt(i));
        }
        return cleanPath(buf.toString());
    }


    /**
     * Holds all process related information in a single object;
     * If the process is null, this is not an active process; only a definition object.
     */
    public class ProcessInfo{
        private Process p = null;
        private String streamPath = null;
        private JSONObject procDef = null;

        public ProcessInfo(Process proc, String path, JSONObject pdef){
            p = proc;
            path = streamPath;
            procDef = pdef;
        }

        public Process getProcess(){
            return p;
        }

        public String getSfsPath(){
            return streamPath;
        }

        public JSONObject getProcessDef(){
            return procDef;
        }
    }

    /**
     * Process comm thread.  This thread is spawn to communicate with the
     * process that has been started locally.  It accepts data from the process
     * and forwards it to streamfs.
     */
    public class ProcessCommThread extends Thread{
        public Process proc = null;
        public String procpath = null;
        
        public ProcessCommThread(String path, Process p){
            proc = p;
            procpath = cleanPath(path);
        }

        public void run(){
            while(true){
                try {
                    System.out.println("Waiting for data from process...");
                    InputStream inputStream = proc.getInputStream();
                    //read it from the process
                    DataMessage dm = DataMessage.parseDelimitedFrom(inputStream);
                    System.out.println("ProcessCommThread.in:: " + dm);
                    if(dm.getType()==DataMessage.DataMessageType.DATA_OUT){
                        //construct the data object to send to sfs server
                        JSONObject dataObj = new JSONObject();
                        JSONArray tss = new JSONArray();
                        JSONArray allvals = new JSONArray();
                        JSONArray vals = new JSONArray();
                        int datacnt = dm.getDataCount();
                        for(int i=0; i<datacnt; i++){
                            TimeValSet thisTs = dm.getData(i);
                            tss.add(thisTs.getTs());
                            int datapts = thisTs.getValCount();
                            for(int j=0; j<datapts; j++)
                                vals.add(thisTs.getVal(j));
                            dataObj.put("tss", tss);
                            allvals.add(vals);
                        }
                        dataObj.put("vals", allvals);

                        //construct the message to send to streamfs
                        ProcessMessage pm = ProcessMessage.newBuilder()
                            .setType(ProcessMessage.ProcessMessageType.DATA)
                            .setPath(procpath)
                            .setKey(key.toString())
                            .setData(dataObj.toString())
                            .build();
                        OutputStream outToSfs = connectionSocket.getOutputStream();
                        pm.writeDelimitedTo(outToSfs);
                        outToSfs.flush();
                    }
                } catch(Exception e){
                    e.printStackTrace();
                    //do some cleanup here, if there's some kind of ioexception
                    processMap.remove(procpath);

                    //send a remove to streamfs
                    try {
                        ProcessMessage pm = ProcessMessage.newBuilder()
                            .setPath(procpath)
                            .setType(ProcessMessage.ProcessMessageType.DESTROY)
                            .build();
                        OutputStream out = connectionSocket.getOutputStream();
                        pm.writeDelimitedTo(out);
                        out.flush();
                    } catch(Exception e2){
                        e2.printStackTrace();
                    }

                    //call destroy on the process for cleanup
                    try {
                        if(isRunning(proc))
                            proc.destroy();
                    } catch(Exception e3){
                        e3.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Console.
     */
    public class ConsoleThread extends Thread{
        private ServerSocket consoleServerSock = null;
        private Socket consoleSocket = null;

        public ConsoleThread(){}

        public void run(){
            try {
                consoleServerSock = new ServerSocket(admin_port, 1, 
                                            InetAddress.getByName("localhost"));
            } catch(Exception e){
                e.printStackTrace();
                return;
            }
            InputStream input =null;
            OutputStream output = null;
            while(true){
                try {
                    if(consoleSocket==null || consoleSocket.isClosed()){
                        consoleSocket=null;
                        consoleSocket = consoleServerSock.accept();
                    } 
                    input = consoleSocket.getInputStream();
                    output = consoleSocket.getOutputStream();
                } catch(Exception e){
                    e.printStackTrace();
                    try{
                        if(consoleSocket!=null)
                            consoleSocket.close();
                    } catch(Exception e2){
                    }
                    consoleSocket=null;
                }
                
                
                while(consoleSocket!=null && !consoleSocket.isClosed()){
                    try {
                        ConsoleMessage cm = ConsoleMessage.parseDelimitedFrom(input);
                        switch(cm.getType()){
                            case RELOAD:
                                if(cm.hasProcPath()){
                                    //reload(cm.getProcPath());
                                } else {
                                    //reload(null);
                                }
                                break;
                            case INFO:
                                try {
                                    //String info = getInfo();
                                    String info = "info here";
                                    ConsoleMessage outmsg = ConsoleMessage.newBuilder()
                                        .setType(ConsoleMessage.ConsoleMessageType.INFO)
                                        .setData(info).build();
                                    outmsg.writeDelimitedTo(output);
                                    output.flush();
                                } catch(Exception e){
                                    e.printStackTrace();
                                }
                                break;
                            case QUIT:
                                try {
                                    consoleSocket.close();
                                } catch(Exception e){
                                    e.printStackTrace();
                                }
                                break;
                        }
                    } catch(Exception e){
                        try {
                            if(consoleSocket!=null && !consoleSocket.isClosed())
                                consoleSocket.close();
                        } catch(Exception e2){
                        }
                        consoleSocket = null;
                    }
                }
            }
        }
    }



}
