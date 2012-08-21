package local.rest;

import local.metadata.context.*;
import local.rest.handlers.*;
import local.db.*;
import local.rest.resources.*;
import local.rest.resources.proc.*;
import local.rest.resources.util.*;
import local.analytics.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.*;

import is4.*;
import net.sf.json.*;

import javax.naming.InvalidNameException;

import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;


public class RESTServer implements Container{

	private String bindAddress = "localhost";
	private int port = 8080;
	protected static Logger logger = Logger.getLogger(RESTServer.class.getPackage().getName());

	private static ConcurrentHashMap<String, String> baseResources =  
                                                        new ConcurrentHashMap<String, String>();
	private static ConcurrentHashMap<String, Resource> resourceTree = 
                                                        new ConcurrentHashMap<String, Resource>();

	public static final long start_time = System.currentTimeMillis()/1000;
	
	private static final String rootPath = "/";
	
	private static MongoDBDriver mdriver = new MongoDBDriver();

    private static InputStream routerInput = null;
    private static OutputStream routerOutput = null;
    private static Router router =null;
	private static MetadataGraph metadataGraph = null;
    public static boolean tellRouter = false;

    public static String EMTPY_STRING = "";
    public static String KEYSTORE_PROPERTY = "javax.net.ssl.keyStore";
    public static String KEYSTORE_PASSWORD_PROPERTY = "javax.net.ssl.keyStorePassword";
    public static String KEYSTORE_TYPE_PROPERTY = "javax.net.ssl.keyStoreType";
    public static String KEYSTORE_ALIAS_PROPERTY = "javax.net.ssl.keyStoreAlias";

    PipedInputStream myEnd_pipedInput = null;
    PipedOutputStream myEnd_pipedOut = null;

    private static Object lock = new Object();

    //private static ConcurrentHashMap<String, Container> urlRouteTable = new ConcurrentHashMap<String, Container>();
    protected static Connection connection = null;
    protected static Connection connectionHttps = null;
    public static ExecutorService executor=null;
    
	public RESTServer(){}

	public RESTServer(String address, int p){
		logger = Logger.getLogger(RESTServer.class.getPackage().getName());
		bindAddress = address;
		port = p;
	}

    public static void shutdown(){
        try { connection.close();} catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
        try { connectionHttps.close();} catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
    }

	public static void main(String[] args){
		RESTServer.logger = Logger.getLogger(RESTServer.class.getPackage().getName());
		RESTServer restSvr = new RESTServer();
		restSvr.start();
	}

    public void handle(Request request, Response response){
        logger.info("Heard something");
        AsyncTask t = new AsyncTask(request, response);
        System.out.println("Async task initalized");
        executor.submit(t);
        //executor.execute(t);
    }

	public void start(){

		try {
			System.setProperty("http.keepAlive", "false");
			System.setProperty("http.maxConnections", "150");
			System.setProperty("sun.net.http.errorstream.enableBuffering", "true");

			logger.config("Starting RESTServer on HOST " + bindAddress + " PORT " + port);
			DBAbstractionLayer dbAbstractionLayer = new DBAbstractionLayer();
			
			//Root handler
			RootHandler roothandler = new RootHandler(rootPath);
			
			//Information bus resource
			InfoBusResource ibus = InfoBusResource.getInstance(rootPath + "ibus");

			//action handlers
			/*StreamHandler streamHdlr = new StreamHandler();
			SmapSourceHandler smapSourceHandler = new SmapSourceHandler(rootPath + "pub/smap/");
			baseResources.put(rootPath + "pub/smap", "");

			httpServer.createContext(rootPath + "streamtest", streamHdlr);
			baseResources.put(rootPath + "streamtest/","");*/
			
			//Resync smap
			ResyncSmapStreams resyncResource = new ResyncSmapStreams(rootPath + "resync");

			//Add filter for parsing URL parameters
			String pubPath = rootPath + "pub";
			PubHandler pubHandler = new PubHandler(pubPath);

			//Add filter for parsing URL parameters
			/*HttpContext context2 = httpServer.createContext(rootPath + "pub/smap/", smapSourceHandler);
			context2.getFilters().add(smapSourceHandler);
			baseResources.put(rootPath + "pub/smap/", "");*/
			
			//httpServer.createContext(rootPath + "sub", subHandler);
			SubHandler subHandler = new SubHandler(rootPath +"sub");

			//information handlers
			StreamInfoHandler streamInfoHandler = new StreamInfoHandler(rootPath + "pub/all");

			//get the current time from this resource, for time-series queries
			TimeResource timeResource = new TimeResource(rootPath + "time");

			//SubInfoHandler subInfoHandler = new SubInfoHandler();
			SubInfoHandler subInfoHandler = new SubInfoHandler(rootPath + "sub/all");
		
			//httpServer.createContext(rootPath + "sub/mypublist",subInfoHandler);
			//Smap Message Demultiplexer for smap reports
			DemuxResource demuxResource = new DemuxResource();

			//Smap2 Message Demultiplexer for smap reports
			DemuxResource2 demuxResource2 = new DemuxResource2();
			
			//Model manager
			ModelManagerResource mmr = new ModelManagerResource();

            //Process manager
            ProcessManagerResource pmr = new ProcessManagerResource();

			//setup admin resources
			Resource adminResource = new Resource(rootPath + "admin");
			Resource tsResource = new Resource(adminResource.getURI() + "data");
			Resource propsResource = new Resource(adminResource.getURI() + "properties");
			Resource dataAdminResource = new AdminDataReposIndexesResource();
			Resource propsAdminResource = new AdminPropsReposIndexesResource();
			Resource allNodesResource = new AllNodesResource(rootPath + "admin/listrsrcs");
            
			RESTServer.addResource(mmr);
			RESTServer.addResource(pubHandler);
			RESTServer.addResource(resyncResource);
			RESTServer.addResource(streamInfoHandler);
			RESTServer.addResource(subHandler);
			RESTServer.addResource(timeResource);
			RESTServer.addResource(ibus);
			RESTServer.addResource(subInfoHandler);
			RESTServer.addResource(demuxResource);
			RESTServer.addResource(demuxResource2);
            RESTServer.addResource(pmr);
            RESTServer.addResource(adminResource);
			RESTServer.addResource(tsResource);
			RESTServer.addResource(propsResource);
			RESTServer.addResource(dataAdminResource);
			RESTServer.addResource(propsAdminResource);
			RESTServer.addResource(allNodesResource);
            RESTServer.addResource(roothandler);

            String timepath = 	timeResource.getURI();
            String timepath2 = timeResource.getURI().substring(0, timeResource.getURI().length()-1);
            baseResources.put(timeResource.getURI(), "");	
            baseResources.put(timeResource.getURI().substring(0, timeResource.getURI().length()-1), "");
            logger.info("type=" + timeResource.getClass().getName() + "\ttimepath=" + timepath + "\ttimestamp2=" + timepath2);
            //System.exit(1);
            baseResources.put(roothandler.getURI(),"");
            baseResources.put(roothandler.getURI().substring(0, roothandler.getURI().length()-1), "");

            baseResources.put(demuxResource.getURI(),"");
            baseResources.put(demuxResource.getURI().substring(0, demuxResource.getURI().length()-1), "");

            baseResources.put(demuxResource2.getURI(),"");
            baseResources.put(demuxResource2.getURI().substring(0, demuxResource2.getURI().length()-1), "");

            baseResources.put(subHandler.getURI(),"");
            baseResources.put(subHandler.getURI().substring(0, subHandler.getURI().length()-1), "");

            baseResources.put(resyncResource.getURI(),"");
            baseResources.put(resyncResource.getURI().substring(0, resyncResource.getURI().length()-1), "");

            baseResources.put(streamInfoHandler.getURI(),"");
            baseResources.put(streamInfoHandler.getURI().substring(0, streamInfoHandler.getURI().length()-1), "");

            baseResources.put(pubHandler.getURI(),"");
            baseResources.put(pubHandler.getURI().substring(0, pubHandler.getURI().length()-1), "");

            baseResources.put(subInfoHandler.getURI(),"");
            baseResources.put(subInfoHandler.getURI().substring(0, subInfoHandler.getURI().length()-1), "");

			baseResources.put(mmr.getURI(),"");
            baseResources.put(mmr.getURI().substring(0, mmr.getURI().length()-1), "");

            baseResources.put(pmr.getURI(), "");
            baseResources.put(pmr.getURI().substring(0, pmr.getURI().length()-1), "");        

            baseResources.put(rootPath,"");

			baseResources.put(adminResource.getURI(), "");
            baseResources.put(adminResource.getURI().substring(0, adminResource.getURI().length()-1),"");

			baseResources.put(tsResource.getURI(), "");
            baseResources.put(tsResource.getURI().substring(0, tsResource.getURI().length()-1), "");

			baseResources.put(propsResource.getURI(), "");
            baseResources.put(propsResource.getURI().substring(0, propsResource.getURI().length()-1),"");

			baseResources.put(dataAdminResource.getURI(),"");
            baseResources.put(dataAdminResource.getURI().substring(0, dataAdminResource.getURI().length()-1), "");

			baseResources.put(propsAdminResource.getURI(),"");
            baseResources.put(propsAdminResource.getURI().substring(0, propsAdminResource.getURI().length()-1),"");

			baseResources.put(allNodesResource.getURI(),"");
            baseResources.put(allNodesResource.getURI().substring(0, allNodesResource.getURI().length()-1), "");

            baseResources.put(rootPath + "pub/all","");
            baseResources.put(rootPath + "pub/all/","");

			baseResources.put(rootPath + "sub/mypublist", "");
            baseResources.put(rootPath + "sub/mypublist/", "");

			baseResources.put(rootPath + "time","");
            baseResources.put(rootPath + "time/","");

			baseResources.put(rootPath + "sub/all","");
            baseResources.put(rootPath + "sub/all/","");

			baseResources.put(rootPath + "sub","");
            baseResources.put(rootPath + "sub/","");

            baseResources.put(rootPath + "resync", "");
			baseResources.put(rootPath + "resync/", "");

            baseResources.put(rootPath + "pub","");
			baseResources.put(rootPath + "pub/","");

            baseResources.put(rootPath + "ibus","");
			baseResources.put(rootPath + "ibus/","");

			//load saved resources
			loadResources();
            pmr.loadPrevState();

            //Start aggregation thread
            /*logger.info("STARTING ROUTER THREAD");
            myEnd_pipedInput = new PipedInputStream();
            PipedOutputStream yourEnd_pipedOut = new PipedOutputStream();
            myEnd_pipedInput.connect(yourEnd_pipedOut);

            PipedInputStream yourEnd_pipedInput = new PipedInputStream();
            myEnd_pipedOut = new PipedOutputStream();
            yourEnd_pipedInput.connect(myEnd_pipedOut);

            router = new Router(yourEnd_pipedInput, yourEnd_pipedOut);*/
            router = new Router();
            logger.info("Router instantiated!");

			//load into in-memory metadata graph
            logger.info("LOADING METADATA GRAPH");
			metadataGraph = MetadataGraph.getInstance();
			Resource.setMetadataGraph(metadataGraph);

            Thread routerThread = new Thread(router);
            logger.info("BERORE HERE");
            routerThread.start();
            logger.info("HERE!");
            //Thread.sleep(1000);

            //metadataGraph.setRouterCommInfo(myEnd_pipedInput, myEnd_pipedOut);
            //metadataGraph.setRouterCommInfo("localhost", 9999);
            logger.info("POPULATING INTERNAL");
            metadataGraph.populateInternalGraph(tellRouter);
            logger.info("DONE POP");
			
			//httpServer.setExecutor(Executors.newCachedThreadPool());
			//httpServer.setExecutor(Executors.newFixedThreadPool(1));

            //register a shutdown hook
            ShutdownProc shutdown = new ShutdownProc(this);
            Runtime.getRuntime().addShutdownHook(shutdown);

			logger.info("Binding to port: " + port);
			/*InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(bindAddress), port);
			httpServer.bind(addr,0);
			httpServer.start();*/

            //http
            //RESTServer server = new RESTServer();
            logger.info("Address=" + bindAddress + "; port=" + port);
            connection = new SocketConnection((Container)this);
            SocketAddress address = new InetSocketAddress(bindAddress, port);
            connection.connect(address);
            logger.info("Listening for connection on " + bindAddress + ":" + port);

            //https
            System.setProperty(KEYSTORE_PROPERTY, "mySrvKeystore");
            System.setProperty(KEYSTORE_PASSWORD_PROPERTY, "123456");
            SocketAddress address2 = new InetSocketAddress(bindAddress, port+1);
            SSLContext sslContext = createSSLContext();
            connectionHttps = new SocketConnection((Container)this);
            connectionHttps.connect(address2, sslContext);
            logger.info("Listening for connection on " + bindAddress + ":" + (port+1));
			System.out.println("Server is listening on port " + (port+1) );
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "", e);
			//e.printStackTrace();
            System.exit(1);
		}
	}

	/*public static HttpServer getHttpServer(){
		return httpServer;
	}*/

	public static void addResource(Resource resource){
		if(resource != null && !baseResources.containsKey(resource.getURI()) && 
				!resource.getURI().equals("") ){

			//add it to local resourceTree hashtable
			resourceTree.put(resource.getURI(), resource);
			logger.info("resourceTree.add: " + resource.getURI().toString());

			//handle requests that end with and without "/"
			String otherUrl = null;
			if(resource.getURI().endsWith("/") && !resource.getURI().equals("/")){
				//add it to local resourceTree hashtable
				otherUrl = resource.getURI().substring(0, resource.getURI().length()-1);
				resourceTree.put(otherUrl, resource);
				logger.info("resourceTree.add: " + otherUrl);

			} else if(!resource.getURI().endsWith("/")){
				//add it to local resourceTree hashtable
				otherUrl = resource.getURI() + "/";
				resourceTree.put(otherUrl, resource);
				logger.info("resourceTree.add: " + otherUrl);
			}
		} 
	}

	public static void removeResource(Resource resource){
		try {
			if(resource != null && !baseResources.contains(resource.getURI()) ){
				if(resource.getURI().endsWith("/")){
					resourceTree.remove(resource.getURI());
					resourceTree.remove(resource.getURI().
								substring(0, resource.getURI().length()-1));
				}else{
					resourceTree.remove(resource.getURI());
					resourceTree.remove(resource.getURI() + "/");
				}
			}
		} catch(Exception e){
			if(!(e instanceof java.lang.IllegalArgumentException))
				logger.log(Level.WARNING, "", e);
		}
	}

    public static boolean moveResource(String srcPath, String dstPath){
        if(srcPath == null || dstPath == null || !isResource(srcPath) || 
            baseResources.contains(srcPath) || baseResources.contains(dstPath))
            return false;

        //check that the parent exists
        StringTokenizer tokenizer = new StringTokenizer(dstPath, "/");
        Vector<String> tokens = new Vector<String>();
        while(tokenizer.hasMoreTokens())
            tokens.add(tokenizer.nextToken());
        StringBuffer parentPathBuf = new StringBuffer();
        for(int i=0; i<tokens.size()-1; ++i)
            parentPathBuf.append("/").append(tokens.get(i));
        parentPathBuf.append("/");
        Resource r = RESTServer.getResource(parentPathBuf.toString());
        if(r==null || r.TYPE!=ResourceUtils.DEFAULT_RSRC)
            return false;

        boolean retval=false;

        //clean the path
        srcPath = srcPath.replaceAll("/+", "/");
        dstPath = dstPath.replaceAll("/+", "/");
        if(!srcPath.endsWith("/"))
            srcPath += "/";
        if(!dstPath.endsWith("/"))
            dstPath += "/";
       
        //update 
        MySqlDriver database = (MySqlDriver)DBAbstractionLayer.database;
        synchronized(lock){
            r = RESTServer.getResource(srcPath);
            if(r!=null){
                //change it in MongoDB
                HashMap<String, String> affectedPaths = database.move(srcPath,dstPath);
               
                if(affectedPaths != null){ 
                    //for all affected paths run the update
                    Iterator<String> keys = affectedPaths.keySet().iterator();
                    while(keys.hasNext()){
                        String thisSrc = keys.next();
                        String thisDst = affectedPaths.get(thisSrc);
                        mdriver.move(thisSrc, thisDst);

                        r = RESTServer.getResource(thisSrc);
                        if(r !=null) {
                            //update the local web server mappings
                            RESTServer.removeResource(r);
                            r.setURI(thisDst);
                            RESTServer.addResource(r);
                        }
                    }
                    retval = true;
                }
            } else
                retval = false;
        }
        return retval;
    }

	public static boolean isBaseResource(String path){
		return baseResources.contains(path);
	}

	public static Resource getResource(String path){
		if(path == null)
			return null;
		logger.info("resourceTree.get: " + path);
		//return resourceTree.get(path);
        return longestPrefixMatch(path);
	}

	public static boolean isResource(String path){
		return resourceTree.containsKey(path);
	}

	public void loadResources(){
		MySqlDriver database = (MySqlDriver)DBAbstractionLayer.database;
		JSONArray allpaths = database.rrGetAllPaths();
		logger.info("All_paths:\n" + allpaths.toString());
		//logger.info("ALLPATHS: " + allpaths.toString());
		UUID pubid=null;
		try {
			Vector<String> subsCreated = new Vector<String>();
			logger.fine("Allpath_size: " + allpaths.size());
			for(int i=0; i<allpaths.size(); i++){
				logger.fine("getting path at position: " + i);
				boolean alreadyAdded = false;
				Resource resource = null;
				String thisPath = (String)allpaths.get(i);
				int rtype = ResourceUtils.translateType(database.getRRType(thisPath));
				switch(rtype){
					case ResourceUtils.DEFAULT_RSRC:
						logger.info("Loading default resource: " + thisPath);
						resource = new Resource(thisPath);
						break;
					case ResourceUtils.PUBLISHER_RSRC:
						logger.info("Loading publisher resource: " + thisPath);
						pubid = database.isRRPublisher2(thisPath);
						if(pubid!=null)
							resource = new PublisherResource(thisPath, pubid);
						else
							resource = new Resource(thisPath);
						break;
					case ResourceUtils.GENERIC_PUBLISHER_RSRC:
						logger.info("Loading publisher resource: " + thisPath);
						pubid = database.isRRPublisher2(thisPath);
						boolean modelstream = thisPath.startsWith("/models");
						if(pubid!=null && !modelstream){
							resource = new GenericPublisherResource(thisPath, pubid);
						} else if(!modelstream){
							resource = new Resource(thisPath);
						}
						break;
					case ResourceUtils.SUBSCRIPTION_RSRC:
						logger.info("Loading subscription resource: " + thisPath);
						//get the subid associated with this uri
						UUID subid = database.getSubId(thisPath);
						if(subid != null)
							resource = new SubscriptionResource(subid, thisPath);
						else
							resource = new Resource(thisPath);
						break;
					case ResourceUtils.SYMLINK_RSRC:
						String link = database.getSymlinkAlias(thisPath);
						logger.info("Loading symlink resource: " + thisPath + " links_to:" + link);
						if(!link.equals("") && link.startsWith("http://")){
							try {
								URL thisUrl = new URL(link);
								resource = new SymlinkResource(thisPath, thisUrl);
							} catch(Exception e){
								logger.log(Level.WARNING, "", e);
								resource = new Resource(thisPath);
								logger.info("Could not create symlink1");
							}
						} else if(!link.equals("") && link.startsWith("/")){
							resource =  new SymlinkResource(thisPath, link);
						} else{
							resource = new Resource(thisPath);
							logger.info("Could not create symlink2");
						}
						break;
						
					case ResourceUtils.MODEL_RSRC:
						logger.info("Loading model resource: " + thisPath);
						
						//resource may have been loaded by child, check if already registered
						resource = RESTServer.getResource(thisPath);
						boolean createView = false;
						if(resource == null){
							long last_model_ts = mdriver.getMaxTsModels(thisPath);
							JSONObject mscript = mdriver.getModelEntry(thisPath, last_model_ts);
							logger.info("mscript: " + mscript);
							createView = (boolean)mscript.getBoolean("createview");
							String rawscript = database.rrGetPropertiesStr(thisPath);
							resource = new ModelResource(thisPath, rawscript, createView);
							this.addResource(resource);
							SubMngr submngr = SubMngr.getSubMngrInstance();
							if(submngr.restartActiveModels((ModelResource)resource)){
								logger.info("All active models for " + thisPath + " restarted and added successfully!");
							} else {
								logger.warning("Could not restart active models for: " + thisPath);
							}
							alreadyAdded=true;
						}
						break;

                    case ResourceUtils.PROCESS_PUBLISHER_RSRC:
                        try {
                            pubid = database.isRRPublisher2(thisPath);
                            resource = new ProcessPublisherResource(thisPath, pubid, false);
                            String subidStr = (String) (database.getSubIdByDstPubPath(thisPath)).get(0);
                            ((ProcessPublisherResource)resource).setAssociatedSubId(UUID.fromString(subidStr));
                            this.addResource(resource);    
                        } catch(Exception e){
                            logger.log(Level.WARNING, "",e);
                            System.exit(1);
                        }
                        break;

                    case ResourceUtils.PROCESS_RSRC:
                        try {
                            String script = database.rrGetPropertiesStr(thisPath);
                            JSONObject scriptObj = (JSONObject)JSONSerializer.toJSON(script);
                            resource = new ProcessResource(thisPath, script);
                            this.addResource(resource);
                        } catch(Exception e){
                            logger.log(Level.WARNING, "", e);
                            System.exit(1);
                        }
                        break;
						
					default:
						resource = new Resource(thisPath);
						break;
				}

				if(!alreadyAdded && resource != null){
					logger.info("Adding: " + resource.getURI());
					this.addResource(resource);
				}
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public class SubInfoHandler extends Resource{
		public SubInfoHandler(String path) throws Exception, InvalidNameException{
			super(path);
		}

		public void get(Request m_request, Response m_response, boolean internalCall, JSONObject internalResp){
			JSONObject getSubsObj = new JSONObject();
			getSubsObj.put("operation","get_all_subscribers");
			getSubsObj.put("status", "success");
			getSubsObj.put("subscribers",getSubsJSONArray());

			sendResponse(m_request, m_response, 200, getSubsObj.toString(), internalCall, internalResp);
		}

		public void put(Request m_request, Response m_response, String data, boolean internalCall, JSONObject internalResp){
			
			JSONObject infoReq = (JSONObject) JSONSerializer.toJSON(data);
			if(infoReq.getString("name").equalsIgnoreCase("my_stream_list")){
				String thisSubId = infoReq.getString("SubId");
				SubMngr subMngr = SubMngr.getSubMngrInstance();
				if(subMngr.isSubscriber(thisSubId)){
					JSONArray publist = new JSONArray();
					publist.addAll((Collection<String>) subMngr.getStreamIds(thisSubId));
					JSONObject response = new JSONObject();
					response.put("operation", "my_stream_list");
					response.put("status","success");
					response.put("PubList", publist);
					sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
				}else{
					JSONObject response = new JSONObject();
					response.put("operation", "my_stream_list");
					response.put("status","fail");
					response.put("error", "Invalid subscriber id: " + thisSubId);
					sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
				}

			}else{
				sendResponse(m_request, m_response, 401, null, internalCall, internalResp);
			}
		}

		public void post(Request m_request, Response m_response, String data, boolean internalCall, JSONObject internalResp){
			put(m_request, m_response, data, internalCall, internalResp);
		}

		public JSONArray getSubsJSONArray(){
			JSONArray subIdsJSONObj = new JSONArray();
			SubMngr subManager = SubMngr.getSubMngrInstance();
			List<String> streamIds = subManager.getSubIds();

			if(streamIds != null){
				subIdsJSONObj.addAll(streamIds);
			}

			return subIdsJSONObj;
		}
		
	}

	public class StreamInfoHandler extends Resource{

		protected transient Logger logger = Logger.getLogger(StreamInfoHandler.class.getPackage().getName());

		public StreamInfoHandler(String uri) throws Exception, InvalidNameException{
			super(uri);
		}

		public  void get(Request m_request, Response m_response, boolean internalCall, JSONObject internalResp){
			try {
				JSONObject getStreamsObj = new JSONObject();
				getStreamsObj.put("operation","get_all_streams");
				getStreamsObj.put("status", "success");
				getStreamsObj.put("streams",getStreamIdsJSONArray());
				sendResponse(m_request, m_response, 200, getStreamsObj.toString(), internalCall, internalResp);
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
			}
		}
		public void put(Request m_request, Response m_response, String data, boolean internalCall, JSONObject internalResp){
			sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
		}
		public void post(Request m_request, Response m_response, String data, boolean internalCall, JSONObject internalResp){
			put(m_request, m_response, data, internalCall, internalResp);
		}
		public void delete(Request m_request, Response m_response, boolean internalCall, JSONObject internalResp){
			sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
		}

		public JSONArray getStreamIdsJSONArray(){
			JSONArray streamIdsJSONObj = new JSONArray();
			Registrar pubManager = Registrar.registrarInstance();
			ArrayList<String> streamIds = (ArrayList<String>) pubManager.getPubIds();

			if(streamIds != null){
				streamIdsJSONObj.addAll(streamIds);
			}

			return streamIdsJSONObj;
		}
	}

    public static class ShutdownProc extends Thread{
        RESTServer server = null;
        public ShutdownProc(RESTServer svr){
            server = svr;
        }

        public void run(){
            server.shutdown();
        }
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

    private static Resource longestPrefixMatch(String path){
            Resource matchRes = null;

            if(path!=null){
                StringTokenizer tokenizer = new StringTokenizer(path, "/");
                Vector<String> tokens = new Vector<String>();
                Vector<String> paths = new Vector<String>();
                while(tokenizer.hasMoreTokens())
                    tokens.add(tokenizer.nextToken());
                StringBuffer s = new StringBuffer("/"); 
                paths.add(s.toString());
                for(int i=0; i<tokens.size(); i++){
                    if(i==0)
                        paths.add(s.append(tokens.get(i)).toString());
                    else
                        paths.add(s.append("/").append(tokens.get(i)).toString());
                }
                for(int j=paths.size()-1; j>=0; j--){
                    Resource thisResource = resourceTree.get(paths.get(j));
                    if(thisResource!=null){
                        matchRes=thisResource;
                        break;
                    }
                }
            }
            return matchRes;
        }

    public class AsyncTask implements Runnable{
        private Request request = null;
        private Response response = null;
        private MySqlDriver database = (MySqlDriver)DBAbstractionLayer.database;
        public AsyncTask(Request req, Response resp){
            request = req;
            response =resp;
            logger.info("Async task created: path=" + request.getPath().getPath());
        }
        
        public void run(){
            logger.info("Running async task");
            try {
                String path = ResourceUtils.cleanPath(request.getPath().getPath());
                logger.info("Path=" + path);
                Query query = request.getQuery();
                logger.info("query_string=" + query.toString().length());
                Resource r = longestPrefixMatch(path);
                if(r==null){
                    logger.info("Invoking ROOT handler");
                    Resource root = resourceTree.get(rootPath);
                    root.handle(request, response);
                } else {
                    logger.info("Invoking handler: " + r.getURI());
                    r.handle(request, response);
                }
                
            } catch(Exception e){
                logger.log(Level.WARNING, "", e);
                Resource.sendResponse(request, response, 500, null, false, null);
            }
        }
    }



}
