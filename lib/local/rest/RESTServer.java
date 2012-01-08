/*
 * "Copyright (c) 2010-11 The Regents of the University  of California. 
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Author:  Jorge Ortiz (jortiz@cs.berkeley.edu)
 * IS4 release version 2.0
 */

package local.rest;

import local.metadata.context.*;
import local.rest.handlers.*;
import local.db.*;
import local.rest.resources.*;
import local.rest.resources.util.*;
import local.analytics.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

import is4.*;
import net.sf.json.*;
import com.sun.net.httpserver.*;

import javax.naming.InvalidNameException;

public class RESTServer {

	private String bindAddress = "localhost";
	private int port = 8080;
	protected static Logger logger = Logger.getLogger(RESTServer.class.getPackage().getName());
	private static HttpServer httpServer = null;

	private static Hashtable<String, String> baseResources =  new Hashtable<String, String>();
	private static Hashtable<String, Resource> resourceTree = new Hashtable<String, Resource>();

	public static final long start_time = (new Date().getTime())/1000;
	
	private static final String rootPath = "/";
	
	private static MongoDBDriver mdriver = new MongoDBDriver();

    private static InputStream routerInput = null;
    private static OutputStream routerOutput = null;
    private static Router router =null;
	private static MetadataGraph metadataGraph = null;
    public static boolean tellRouter = true;

    PipedInputStream myEnd_pipedInput = null;
    PipedOutputStream myEnd_pipedOut = null;


	public RESTServer(){}

	public RESTServer(String address, int p){
		logger = Logger.getLogger(RESTServer.class.getPackage().getName());
		bindAddress = address;
		port = p;
	}

	public static void main(String[] args){
		RESTServer.logger = Logger.getLogger(RESTServer.class.getPackage().getName());
		RESTServer restSvr = new RESTServer();
		restSvr.start();
	}

	public void start(){

		try {
			System.setProperty("http.keepAlive", "false");
			System.setProperty("http.maxConnections", "1");
			System.setProperty("sun.net.http.errorstream.enableBuffering", "true");

			logger.config("Starting RESTServer on HOST " + bindAddress + " PORT " + port);
			//InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(bindAddress), port);
			//httpServer = HttpServer.create(addr, 0);
			httpServer = HttpServer.create();
			DBAbstractionLayer dbAbstractionLayer = new DBAbstractionLayer();
			
			//Root handler
			RootHandler handler = new RootHandler(rootPath);
			RESTServer.addResource(handler);
			baseResources.put(rootPath,"");
			
			//Information bus resource
			InfoBusResource ibus = InfoBusResource.getInstance(rootPath + "ibus/");
			RESTServer.addResource(ibus);
			baseResources.put(rootPath + "ibus/","");

			//action handlers
			StreamHandler streamHdlr = new StreamHandler();
			SmapSourceHandler smapSourceHandler = new SmapSourceHandler(rootPath + "pub/smap/");
			baseResources.put(rootPath + "pub/smap", "");

			httpServer.createContext(rootPath + "streamtest", streamHdlr);
			baseResources.put(rootPath + "streamtest/","");
			
			//Resync smap
			ResyncSmapStreams resyncResource = new ResyncSmapStreams(rootPath + "resync/");
			baseResources.put(rootPath + "resync/", "");
			RESTServer.addResource(resyncResource);

			//Add filter for parsing URL parameters
			String pubPath = rootPath + "pub/";
			PubHandler pubHandler = new PubHandler(pubPath);
			RESTServer.addResource(pubHandler);
			baseResources.put(rootPath + "pub/","");

			//Add filter for parsing URL parameters
			HttpContext context2 = httpServer.createContext(rootPath + "pub/smap/", smapSourceHandler);
			context2.getFilters().add(smapSourceHandler);
			baseResources.put(rootPath + "pub/smap/", "");
			
			//httpServer.createContext(rootPath + "sub", subHandler);
			SubHandler subHandler = new SubHandler(rootPath +"sub/");
			RESTServer.addResource(subHandler);
			baseResources.put(rootPath + "sub/","");

			//information handlers
			StreamInfoHandler streamInfoHandler = new StreamInfoHandler(rootPath + "pub/all/");
			RESTServer.addResource(streamInfoHandler);

			//get the current time from this resource, for time-series queries
			TimeResource timeResource = new TimeResource(rootPath + "time/");
			RESTServer.addResource(timeResource);
			baseResources.put(rootPath + "time/","");

			//SubInfoHandler subInfoHandler = new SubInfoHandler();
			SubInfoHandler subInfoHandler = new SubInfoHandler(rootPath + "sub/all/");
			RESTServer.addResource(subInfoHandler);
			baseResources.put(rootPath + "sub/all","");
		
			httpServer.createContext(rootPath + "sub/mypublist",subInfoHandler);
			baseResources.put(rootPath + "pub/all","");
			baseResources.put(rootPath + "sub/mypublist", "");

			//unpub and unsub
			/*UnpubHandler unpubHandler = new UnpubHandler();
			UnsubHandler unsubHandler = new UnsubHandler();
			httpServer.createContext(rootPath + "unpub", unpubHandler);
			httpServer.createContext(rootPath + "unsub", unsubHandler);
			baseResources.put(rootPath + "unpub", "");
			baseResources.put(rootPath + "unsub", "");

			//sub control
			SubControlHandler subCtrlHdlr = new SubControlHandler();
			httpServer.createContext(rootPath + "sub/control", subCtrlHdlr);
			baseResources.put(rootPath + "sub/control", "");*/

			//Smap Message Demultiplexer for smap reports
			DemuxResource demuxResource = new DemuxResource();
			RESTServer.addResource(demuxResource);

			//Smap2 Message Demultiplexer for smap reports
			DemuxResource2 demuxResource2 = new DemuxResource2();
			RESTServer.addResource(demuxResource2);
			
			//Model manager
			ModelManagerResource mmr = new ModelManagerResource();
			baseResources.put(mmr.getURI(),"");
			RESTServer.addResource(mmr);

			//setup admin resources
			Resource adminResource = new Resource(rootPath + "admin/");
			Resource tsResource = new Resource(adminResource.getURI() + "data/");
			Resource propsResource = new Resource(adminResource.getURI() + "properties/");
			Resource dataAdminResource = new AdminDataReposIndexesResource();
			Resource propsAdminResource = new AdminPropsReposIndexesResource();
			Resource allNodesResource = new AllNodesResource(rootPath + "admin/listrsrcs/");
			baseResources.put(adminResource.getURI(), "");
			baseResources.put(tsResource.getURI(), "");
			baseResources.put(propsResource.getURI(), "");
			baseResources.put(dataAdminResource.getURI(),"");
			baseResources.put(propsAdminResource.getURI(),"");
			baseResources.put(allNodesResource.getURI(),"");
			RESTServer.addResource(adminResource);
			RESTServer.addResource(tsResource);
			RESTServer.addResource(propsResource);
			RESTServer.addResource(dataAdminResource);
			RESTServer.addResource(propsAdminResource);
			RESTServer.addResource(allNodesResource);

			//load saved resources
			loadResources();

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
			
			httpServer.setExecutor(Executors.newCachedThreadPool());
			//httpServer.setExecutor(Executors.newFixedThreadPool(1));

			logger.info("Binding to port: " + port);
			InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(bindAddress), port);
			httpServer.bind(addr,0);
			httpServer.start();
			System.out.println("Server is listening on port " + port );
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "", e);
			//e.printStackTrace();
		}
	}

	public static HttpServer getHttpServer(){
		return httpServer;
	}

	public static void addResource(Resource resource){
		if(resource != null && !baseResources.contains(resource.getURI()) && 
				!resource.getURI().equals("") ){

			logger.config("Adding contextHandler: \"" + resource.getURI() + 
					"\"\tLENGTH: " + resource.getURI().length());
			HttpContext resourceContext = httpServer.createContext(resource.getURI(), resource);
			resourceContext.getFilters().add(resource);

			//add it to local resourceTree hashtable
			resourceTree.put(resource.getURI(), resource);
			logger.info("resourceTree.add: " + resource.getURI().toString());

			//handle requests that end with and without "/"
			String otherUrl = null;
			if(resource.getURI().endsWith("/") && !resource.getURI().equals("/")){
				otherUrl = resource.getURI().substring(0, resource.getURI().length()-1);
				logger.config("Adding contextHandler: " + otherUrl);
				resourceContext = httpServer.createContext(otherUrl, resource);
				resourceContext.getFilters().add(resource);

				//add it to local resourceTree hashtable
				resourceTree.put(otherUrl, resource);
				logger.info("resourceTree.add: " + otherUrl);

			} else if(!resource.getURI().endsWith("/")){
				otherUrl = resource.getURI() + "/";
				logger.config("Adding contextHandler: " + otherUrl);
				resourceContext = httpServer.createContext(otherUrl, resource);
				resourceContext.getFilters().add(resource);

				//add it to local resourceTree hashtable
				resourceTree.put(otherUrl, resource);
				logger.info("resourceTree.add: " + otherUrl);
			}

		}
	}

	public static void removeResource(Resource resource){
		try {
			if(resource != null && !baseResources.contains(resource.getURI()) ){
				httpServer.removeContext(resource.getURI());
				if(resource.getURI().endsWith("/")){
					try{httpServer.removeContext(resource.getURI());}
					catch(Exception e){}
					try{httpServer.removeContext(resource.getURI().
								substring(0, resource.getURI().length()-1));}
					catch(Exception e){}
					resourceTree.remove(resource.getURI());
					resourceTree.remove(resource.getURI().
								substring(0, resource.getURI().length()-1));
				}else{
					try{httpServer.removeContext(resource.getURI() + "/");}
					catch(Exception e){}
					try{httpServer.removeContext(resource.getURI());}
					catch(Exception e){}
					resourceTree.remove(resource.getURI());
					resourceTree.remove(resource.getURI() + "/");
				}
			}
		} catch(Exception e){
			if(!(e instanceof java.lang.IllegalArgumentException))
				logger.log(Level.WARNING, "", e);
		}
	}

	public static boolean isBaseResource(String path){
		return baseResources.contains(path);
	}

	public static Resource getResource(String path){
		if(path == null)
			return null;
		logger.info("resourceTree.get: " + path);
		return resourceTree.get(path);
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
					case ResourceUtils.DEVICES_RSRC:
						logger.info("Loading devices resource: " + thisPath);
						resource = new DevicesResource(thisPath);
						break;
					case ResourceUtils.DEVICE_RSRC:
						logger.info("Loading device resource: " + thisPath);
						resource = new DeviceInstanceResource(thisPath);
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

		public void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
			JSONObject getSubsObj = new JSONObject();
			getSubsObj.put("operation","get_all_subscribers");
			getSubsObj.put("status", "success");
			getSubsObj.put("subscribers",getSubsJSONArray());

			sendResponse(exchange, 200, getSubsObj.toString(), internalCall, internalResp);
		}

		public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
			
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
					sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
				}else{
					JSONObject response = new JSONObject();
					response.put("operation", "my_stream_list");
					response.put("status","fail");
					response.put("error", "Invalid subscriber id: " + thisSubId);
					sendResponse(exchange, 200, response.toString(), internalCall, internalResp);
				}

			}else{
				sendResponse(exchange, 401, null, internalCall, internalResp);
			}
		}

		public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
			put(exchange, data, internalCall, internalResp);
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

		public  void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
			try {
				JSONObject getStreamsObj = new JSONObject();
				getStreamsObj.put("operation","get_all_streams");
				getStreamsObj.put("status", "success");
				getStreamsObj.put("streams",getStreamIdsJSONArray());
				sendResponse(exchange, 200, getStreamsObj.toString(), internalCall, internalResp);
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
			}
		}
		public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
			sendResponse(exchange, 200, null, internalCall, internalResp);
		}
		public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp){
			put(exchange, data, internalCall, internalResp);
		}
		public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
			sendResponse(exchange, 200, null, internalCall, internalResp);
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

}
