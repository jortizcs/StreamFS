package sfs;

import sfs.logger.SFSLogger;
import sfs.db.MySqlDriver;
import sfs.util.ResourceUtils;

import java.lang.StringBuffer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.PrintStream;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SFSServer implements Container {
    private static Logger logger = Logger.getLogger(SFSServer.class.getPackage().getName());
    private static MySqlDriver mysqlDB = null;
    private static ExecutorService executor=null;

    public class AsyncTask implements Runnable{
        private Request request = null;
        private Response response = null;
        public AsyncTask(Request req, Response resp){
            request = req;
            response =resp;
        }
        public void run(){
            try {
                String path = ResourceUtils.cleanPath(request.getPath().getPath());
                logger.fine("GETTING RESOURCES: " + path);
                JSONObject respObj = new JSONObject();
                JSONArray subResourceNames = mysqlDB.rrGetChildren(path);
                //logger.fine(subResourceNames.toString());
                respObj.put("status", "success");
                //response.put("children",subResourceNames);
                //response.put("uptime", ((new Date()).getTime()/1000) - RESTServer.start_time);
                //response.put("uptime_units", "seconds");
                //response.put("activeResources", database.rrGetAllPaths().size());
                sendResponse(response, 200, respObj.toString(), false, new JSONObject());
                return;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while responding to GET request",e);
                //sendResponse(exchange, 200, null, internalCall, internalResp);
                response.setCode(200);
            }
        }
    } 

    public void handle(Request request, Response response) {
         AsyncTask t = new AsyncTask(request, response);
         executor.submit(t);
    }

    public static void main(String[] list) throws Exception {
        SFSServer server = new SFSServer();
        server.executor = Executors.newCachedThreadPool();
        server.setupMysqlDB();
        Connection connection = new SocketConnection((Container)server);
        SocketAddress address = new InetSocketAddress(8080);

        connection.connect(address);
    }

    public static void sendResponse(Response response, int code, String data, 
            boolean internalCall, JSONObject internalResp){
        long time = System.currentTimeMillis();
        response.set("Content-Type", "application/json");
        response.set("Server", "StreamFS/2.0 (Simple 4.0)");
        response.set("Connection", "close");
        response.setDate("Date", time);
        response.setDate("Last-Modified", time);
        response.setCode(code);

        try {
            if(!internalCall){
                PrintStream body = response.getPrintStream();
                body.println(data);
                    body.close();
            } else {
                //copyResponse(data, internalResp);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void copyResponse(String respStr, JSONObject internalResp){
		try{
			if(internalResp != null){
				if(respStr != null){
					/*logger.fine("Copying response to internal buffer");
					JSONObject respObj = (JSONObject) JSONSerializer.toJSON(response);
                    internalResp.accumulateAll((Map)respObj);*/
					/*Iterator keys = respObj.keys();
					while(keys.hasNext()){
						String thisKey = (String) keys.next();
						internalResp.put(thisKey, respObj.get(thisKey));
					}*/
				} else {
					//logger.fine("Response was null");
				}
			} else {
				//logger.fine("Internal buffer is null");
			}

		} catch(Exception e){
			//logger.log(Level.WARNING, "",e);
		}
	}

    public static MySqlDriver getMysqlDatabase(){
        return mysqlDB;
    }

    private void setupMysqlDB(){
        try{
            JSONParser parser = new JSONParser();
            String home=null;
            String dbConfigFile = "sfs/db/db_config/db_info.json";
            if((home=System.getenv().get("SFSHOME")) != null)
                dbConfigFile = home + "/sfs/db/db_config/db_info.json";
            logger.info("home: " + System.getenv().get("SFSHOME") + "; config: " + dbConfigFile);
            File configFile = new File(dbConfigFile);
            FileReader cFileReader = new FileReader(configFile);
            BufferedReader bufReader = new BufferedReader(cFileReader);
        
            StringBuffer strBuf = new StringBuffer();
            String line = null;
            while((line=bufReader.readLine())!=null)
                strBuf.append(line).append(" ");
            JSONObject configJsonObj = (JSONObject)parser.parse(strBuf.toString());
                                
            cFileReader.close();
            bufReader.close();
            String addr = (String)configJsonObj.get("address");
            int port = ((Long)configJsonObj.get("port")).intValue();
            String dbName = (String)configJsonObj.get("dbname");
            String username = (String)configJsonObj.get("login");
            String password = (String)configJsonObj.get("password");

            String parseStatus = "Addr: " + addr + "\nPort: " + port + "\nlogin: " + username + "\npw: " + password + " dbName: " + dbName;
            logger.info(parseStatus);
            if(!username.equalsIgnoreCase("")){
                mysqlDB = new MySqlDriver(addr, port, username, password, dbName);
            }
            else {
                mysqlDB = new MySqlDriver(addr, port);
            }
        } catch(Exception e){
            e.printStackTrace();
            System.out.println("FATAL ERROR: Error instantiating DBAbstraction Layer");
            System.exit(1);
        }
    }
}
