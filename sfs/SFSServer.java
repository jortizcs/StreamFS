package sfs;

import sfs.logger.SFSLogger;
import sfs.db.MySqlDriver;
import sfs.util.ResourceUtils;
import sfs.util.DBQueryTypes;
import sfs.query.QueryHandler;
import sfs.types.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.UUID;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;

import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;


public class SFSServer implements Container {
    private static Logger logger = Logger.getLogger(SFSServer.class.getPackage().getName());
    private static MySqlDriver mysqlDB = null;
    public static final long start_time = System.currentTimeMillis()/1000;
    private static ExecutorService executor=null;

    //core paths
    private static final String[] corepaths = {"/ibus", "/", "/pub", "/sub", "/pub/all", "/time", 
                        "/sub/all", "/proc", "/admin", "/admin/data", "/admin/properties",
                        "/admin/data/indices", "/admin/properties/indices", "/admin/listrsrcs"};

    //parameters to set up https connection handling
    public static String EMTPY_STRING = "";
    public static String KEYSTORE_PROPERTY = "javax.net.ssl.keyStore";
    public static String KEYSTORE_PASSWORD_PROPERTY = "javax.net.ssl.keyStorePassword";
    public static String KEYSTORE_TYPE_PROPERTY = "javax.net.ssl.keyStoreType";
    public static String KEYSTORE_ALIAS_PROPERTY = "javax.net.ssl.keyStoreAlias";

    //query handler
    private static QueryHandler qh = QueryHandler.getInstance();
    
    //utils
    private static ResourceUtils utils = null;

    //json parser
    private static JSONParser parser = new JSONParser();

    public static void main(String[] list) throws Exception {
        //set server
        SFSServer server = new SFSServer();
        server.executor = Executors.newCachedThreadPool();

        //setup db
        mysqlDB = MySqlDriver.getInstance();

        //init core files
        initCoreFiles();

        //resource utils
        utils = ResourceUtils.getInstance();

        //http
        Connection connection = new SocketConnection((Container)server);
        SocketAddress address = new InetSocketAddress(8080);
        connection.connect(address);
        logger.info("Listening for connection on 8080");

        //https
        System.setProperty(KEYSTORE_PROPERTY, "sfs/security/mySrvKeystore");
        System.setProperty(KEYSTORE_PASSWORD_PROPERTY, "123456");
        SocketAddress address2 = new InetSocketAddress(8081);
        SSLContext sslContext = createSSLContext();
        Connection connectionHttps = new SocketConnection((Container)server);
        connectionHttps.connect(address2, sslContext);
        logger.info("Listening for connection on 8081");
    }

    public void handle(Request request, Response response) {
         AsyncTask t = new AsyncTask(request, response);
         executor.submit(t);
    }

    private static SSLContext createSSLContext() throws Exception {

        String keyStoreFile = System.getProperty(KEYSTORE_PROPERTY);
        String keyStorePassword = System.getProperty(KEYSTORE_PASSWORD_PROPERTY,EMTPY_STRING);
        String keyStoreType = System.getProperty(KEYSTORE_TYPE_PROPERTY, KeyStore.getDefaultType());

        KeyStore keyStore = loadKeyStore(keyStoreFile, keyStorePassword, null);
        FileInputStream keyStoreFileInpuStream = null;
        try {
            if (keyStoreFile != null) {
                keyStoreFileInpuStream = new FileInputStream(keyStoreFile);

                keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(keyStoreFileInpuStream, keyStorePassword.toCharArray());
            }
        } finally {
            if (keyStoreFileInpuStream != null) {
                keyStoreFileInpuStream.close();
            }
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        return sslContext;
    }

    private static KeyStore loadKeyStore(final String keyStoreFilePath, final String keyStorePassword,
            final String keyStoreType) throws Exception {
        KeyStore keyStore = null;
        File keyStoreFile = new File(keyStoreFilePath);

        if (keyStoreFile.isFile()) {
            keyStore = KeyStore.getInstance(keyStoreType != null ? keyStoreType : KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword != null ? keyStorePassword
                    .toCharArray() : EMTPY_STRING.toCharArray());
        }

        return keyStore;
    }

    private static void initCoreFiles(){
        long lobs = 0L;
        for(int i=0; i<corepaths.length; i++){
            String path = corepaths[i];
            if(!mysqlDB.rrPathExists(path)){
                UUID oid = new UUID(0L,lobs);
                mysqlDB.rrPutPath(path, oid.toString());
                if(path.equals("/ibus"))
                    mysqlDB.setRRType(path, ResourceUtils.GENERIC_PUBLISHER_RSRC_STR);
                lobs=lobs+(1L<<32);
            }
        }
    }

    public class AsyncTask implements Runnable{
        private Request request = null;
        private Response response = null;
        public AsyncTask(Request req, Response resp){
            request = req;
            response =resp;
        }
        public void run(){
            try {
                String path = utils.cleanPath(request.getPath().getPath());
                Pattern p = Pattern.compile("([/?a-zA-Z0-9-]+)");
                Matcher m  = p.matcher(path);
                logger.info(path + "::match? " + m.matches());
                String type =  mysqlDB.getRRType(path);
                String method = request.getMethod();
                Query query = request.getQuery();
                logger.info("query_string=" + query.toString().length());
                if((path.equals("") || m.matches()) && type !=null){
                    logger.info("MATCH");
                    if(type!=null){
                        logger.info("Query handler dealing with request");
                        qh.executeQuery(request, response, method, path, type, false, null);
                    }
                    else 
                        utils.sendResponse(request, response, 404, null, false, null);
                } else if(type!=null){ //no match, maybe it's a regular expression
                    logger.info("NO MATCH!");
                    JSONObject respobj = new JSONObject();
                    JSONArray allpaths = mysqlDB.rrGetAllPaths();
                    for(int i=0; i<allpaths.size(); i++){
                        String thispath = (String)allpaths.get(i);
                        String thistype = mysqlDB.getRRType(thispath);
                        m = p.matcher(thispath);
                        if(m.matches()){
                            JSONObject internalResp = new JSONObject();
                            qh.executeQuery(request, response, method, thispath, thistype, true, internalResp);
                            respobj.put(thispath, internalResp);
                        }
                    }
                    utils.sendResponse(request, response, 200, respobj.toString(), false, null);
                } else { //no match and/or unknown type
                    utils.sendResponse(request, response, 404, null, false, null);
                }
            } catch(Exception e){
                logger.log(Level.WARNING, "", e);
                utils.sendResponse(request, response, 404, null, false, null);
            }
        }
    }

    
}
