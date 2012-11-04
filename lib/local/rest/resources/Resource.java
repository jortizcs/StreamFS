package local.rest.resources;

import local.db.*;
import local.rest.*;
import local.rest.resources.util.*;
import local.rest.interfaces.*;
import local.metadata.context.*;
import is4.*;
import is4.stats.SFSStatistics;

import net.sf.json.*;

import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;
import java.util.zip.GZIPOutputStream;

import javax.naming.InvalidNameException;
import java.io.*; 
import java.util.regex.Pattern;

import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;


public class Resource implements Container{//extends Filter implements HttpHandler, Serializable, Is4Resource{

    protected static transient final Logger logger = Logger.getLogger(Resource.class.getPackage().getName());
    protected String URI = null;
    protected static MySqlDriver database = (MySqlDriver) DBAbstractionLayer.database;
    protected static MongoDBDriver mongoDriver = new MongoDBDriver();
    protected static SFSStatistics sfsStats = null;

    //Query structures  
    protected static final String[] props_ops = {"props_like", "props_and", "props_or", "props_not"};
    protected static final String[] ts_ops = {"ts_gt", "ts_lt", "ts_ge", "ts_le"};
    protected static Vector<String> propsOpsVec = new Vector<String>();
    protected static Vector<String> tsOpsVec = new Vector<String>();
    protected long last_props_ts = 0;

    protected static MetadataGraph metadataGraph = null;

    public int TYPE= ResourceUtils.DEFAULT_RSRC;

    protected String objectIdentifier = null;
    protected String versionNumber = null;

    public Resource(String path) throws Exception, InvalidNameException {
        path =ResourceUtils.cleanPath(path);
        resourceSetup(path);
        if(sfsStats == null)
            sfsStats = SFSStatistics.getInstance(mongoDriver.openConn());
    }

    public Resource(){
        initQueryStructs();
    }
    
    public static void setMetadataGraph(MetadataGraph mgraph){
        metadataGraph = mgraph;
    }

    public static void removeFromMetadataGraph(String uri){
        metadataGraph.removeNode(uri);
    }

    protected void resourceSetup(String path) throws Exception, InvalidNameException{
        if(path.indexOf("?") > 0 || path.indexOf("&") > 0)
            throw new Exception("Invalid path string: No '&' or '?' allowed OR path already exists");

        if(!path.endsWith("/"))
            path += "/";
        URI = path;
    
        //save if already in database   
        if(!((MySqlDriver)DBAbstractionLayer.database).rrPathExists(path))
            ((MySqlDriver)DBAbstractionLayer.database).rrPutPath(URI);

        //set last_props_ts
        last_props_ts = database.getLastPropsTs(URI);
        if(last_props_ts==0 && mongoDriver.getPropsHistCount(URI)>0){
            logger.info("Fetching oldest properties values");
            last_props_ts = mongoDriver.getMaxTsProps(URI);
            JSONObject propsEntry = mongoDriver.getPropsEntry(URI, last_props_ts);
            propsEntry.remove("_id");
            propsEntry.remove("timestamp");
            propsEntry.remove("is4_uri");
            propsEntry.remove("_keywords");
            database.rrPutProperties(URI, propsEntry);
            database.updateLastPropsTs(URI, last_props_ts);
        }
        

        initQueryStructs();
    }

    public void initQueryStructs(){
        if(propsOpsVec.size()==0){
            int i =0;
            for(i=0; i<props_ops.length; i++)
                propsOpsVec.addElement(props_ops[i]);
            for(i=0; i<ts_ops.length; i++)
                tsOpsVec.addElement(ts_ops[i]);
        }
    }

    public String getURI(){
        return URI;
    }

    public void setURI(String newPath){
        URI = newPath;
    }

    public int getType(){
        return TYPE;
    }

    public void setAsExtProc(){
        //changes the type to extproc
        database.changeToExtProc(URI);
        TYPE = ResourceUtils.EXTPROC_RSRC;
    }

    public JSONObject getProperties(){
        return database.rrGetProperties(URI);
    }

    //////////////// REST-accessible functions ////////////////
    public void get(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
        try {
            Query query = m_request.getQuery();
            if(query.containsKey("query") && 
                ((String)query.get("query")).equalsIgnoreCase("true") &&
                ((String)query.toString()).contains("props")){
                logger.info("Handling PROPERTIES query");
                query_(m_request, m_response, null, internalCall, internalResp);
            } 
            
            else if(query.containsKey("agg") && query.containsKey("units")){
                logger.info("OLAP_QUERY: aggregate data query");
                boolean queryQ = query.containsKey("query");
                String queryVal = "";
                if(queryQ)
                    queryVal = (String)query.get("query");
                queryQ &= (queryVal!=null && queryVal.equalsIgnoreCase("true"));
                if(queryQ){
                    handleTSAggQuery(m_request, m_response, null, internalCall, internalResp);
                } else {
                    String aggStr = (String)query.get("agg");
                    String units = (String)query.get("units");
                    String queryRes =metadataGraph.queryAgg(URI, aggStr, units, null);
                    if(queryRes != null){
                        JSONObject response = (JSONObject)JSONSerializer.toJSON(queryRes);
                        sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
                    } else {
                        sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
                    }
                }
            }

            else if(query.containsKey("incident_paths") && 
                    ((String)query.get("incident_paths")).equalsIgnoreCase("true")){
                JSONArray pathList = metadataGraph.getAllPathsTo(URI);
                JSONObject response = new JSONObject();
                response.put("status", "success");
                response.put("paths", pathList);
                sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
            }
            
            else {
                logger.fine("GETTING RESOURCES: " + URI);
                JSONObject response = new JSONObject();
                JSONArray subResourceNames = ((MySqlDriver)(DBAbstractionLayer.database)).rrGetChildren(URI);
                logger.fine(subResourceNames.toString());
                response.put("status", "success");
                response.put("type", ResourceUtils.translateType(TYPE));
                JSONObject p = (JSONObject) JSONSerializer.toJSON(database.rrGetProperties(URI));
                p.remove("_keywords");
                p.remove("is4_uri");
                p.remove("timestamp");
                response.put("properties", p.toString());
                findSymlinks(subResourceNames);
                response.put("children",subResourceNames);
                sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while responding to GET request",e);
        }
    }

    /*private void partialSend(JSONObject data){
        if(data==null)
            return;
        boolean error = false;
        if(m_response!=null){
            GZIPOutputStream gzipos = null; 
            String enc = m_request.getValue("Accept-encoding");
            boolean gzipResp = false;
            if(enc!=null && enc.indexOf("gzip")>-1)
                gzipResp = true;

            PrintStream body = null;
            try{
                body = m_response.getPrintStream();
                if(data!=null && !gzipResp)
                    body.println(data);
                else if(data!=null && gzipResp){
                    m_response.set("Content-Encoding", "gzip");
                    gzipos = new GZIPOutputStream((OutputStream)body);
                    gzipos.write(data.toString().getBytes());
                }
            } catch(Exception e) {
                error = true;
                logger.log(Level.WARNING, "",e);
            } finally {
                if(body!=null && error==true)
                    body.close();
                else if(body!=null)
                    body.flush();
            }
        }
    }*/

    public void put(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
        try{
            logger.info("PUT " + this.URI);
            if(data != null){// && !isScriptRequest(m_request, m_response)){
                JSONObject dataObj = (JSONObject) JSONSerializer.toJSON(data);
                String op = dataObj.optString("operation");

                String resourceName = dataObj.optString("resourceName");
                logger.info("op=" + op + ";\tdata=" + data + ";\tresourceName=" + 
                                resourceName);
                
                if(op.equalsIgnoreCase("create_resource") && !resourceName.equals("")){
                    boolean putok = ResourceUtils.putResource(this.URI, dataObj);
                    logger.info("putok=" + putok);
                    if(putok){
                        String myPath = this.URI;
                        if(!myPath.endsWith("/"))
                            myPath += "/";
                        metadataGraph.addNode(myPath + resourceName);

                        sendResponse(m_request, m_response, 201, null, internalCall, internalResp);
                    }
                }

                else if(op.equalsIgnoreCase("create_resources")){
                    JSONArray resourceList = dataObj.getJSONArray("list");
                    JSONObject totalResponse = new JSONObject();
                    logger.info("op: create_resources");
                    for(int i=0; i<resourceList.size(); i++){
                        JSONObject spec = resourceList.getJSONObject(i);
                        String tpath = spec.getString("path");
                        String type = spec.getString("type");
                        JSONArray dataArray = spec.optJSONArray("data");
                        logger.info("path=" + tpath + "; type=" + type);
                        //JSONArray dat = spec.optJSONArray("data");
                        StringTokenizer tokenizer = new StringTokenizer(tpath, "/");
                        StringBuffer strBuf = new StringBuffer();
                        while(tokenizer.hasMoreTokens()){
                            String parent = strBuf.toString();
                            if(parent.length()==0)
                                parent="/";
                            String next = tokenizer.nextToken();
                            strBuf.append("/").append(next);
                            logger.info("parent=" + parent + "; fullpath=" + strBuf.toString());
                            if(!database.rrPathExists(strBuf.toString())){
                                logger.info("creating:" + strBuf.toString());
                                JSONObject req = new JSONObject();
                                if(type.equalsIgnoreCase("default") || 
                                    type.equalsIgnoreCase("folder")){
                                    req.put("operation", "create_resource");
                                    req.put("resourceName", next);
                                    req.put("resourceType", "default");
                                } else if(type.equalsIgnoreCase("generic_publisher") || 
                                            type.equalsIgnoreCase("stream")){
                                    if(ResourceUtils.cleanPath(strBuf.toString()).equalsIgnoreCase(ResourceUtils.cleanPath(tpath))){
                                        req.put("operation", "create_generic_publisher");
                                        req.put("resourceName", next);
                                    } else {
                                        req.put("operation", "create_resource");
                                        req.put("resourceName", next);
                                        req.put("resourceType", "default");
                                    }
                                }
                                JSONObject iresp = new JSONObject();
                                Resource parentR = RESTServer.getResource(parent);
                                if(parentR!=null){
                                    parentR.put(m_request, m_response, parent, req.toString(),true, iresp);
                                    totalResponse.put(strBuf.toString(), iresp);
                                   
                                    //update the properties for this newly created file 
                                    Resource r = RESTServer.getResource(strBuf.toString());
                                    if(r!=null && spec.containsKey("properties")){
                                        try {
                                            JSONObject propsObj = spec.getJSONObject("properties");
                                            JSONObject propsRequest = new JSONObject();
                                            propsRequest.put("operation", "overwrite_properties");
                                            propsRequest.put("properties", propsObj);
                                            JSONObject propsReqRes = new JSONObject();
                                            r.post(m_request, m_response, strBuf.toString(), propsRequest.toString(), true, propsReqRes);
                                        } catch(Exception propsError){
                                            logger.log(Level.WARNING, "", propsError);
                                        }
                                    }

                                    //if this is a new publisher with specifed data, save the bulk data
                                    try{
                                        if(iresp.containsKey("PubId") && dataArray!=null && dataArray.size()>0){
                                            GenericPublisherResource newstreamfile = (GenericPublisherResource)RESTServer.getResource(tpath);
                                            if(newstreamfile!=null){
                                                spec.put("pubid", iresp.getString("PubId"));
                                                spec.remove("type");
                                                JSONObject iresp2 = new JSONObject();
                                                newstreamfile.handleBulkDataPost(m_request, m_response, tpath, spec.toString(), true, iresp2);
                                                logger.info("Bulkpost::path=" + tpath + "\nrequest=" + spec.toString() + "\nresp=" + iresp2.toString());
                                            }
                                        }
                                    } catch(Exception b){
                                        logger.log(Level.WARNING, "", b);
                                    }
                                    logger.info("totalResponse=" + totalResponse.toString());
                                } else {
                                    logger.info("parent is null");
                                }
                            } else {
                                logger.info(strBuf.toString() + " alread exists");

                                //update the properties associated with this file that already exists
                                Resource r = RESTServer.getResource(strBuf.toString());
                                if(r!=null && spec.containsKey("properties")){
                                    try {
                                        JSONObject propsObj = spec.getJSONObject("properties");
                                        JSONObject propsRequest = new JSONObject();
                                        propsRequest.put("operation", "update_properties");
                                        propsRequest.put("properties", propsObj);
                                        JSONObject propsReqRes = new JSONObject();
                                        r.post(m_request, m_response, strBuf.toString(), propsRequest.toString(), true, propsReqRes);
                                    } catch(Exception propsError){
                                        logger.log(Level.WARNING, "", propsError);
                                    }
                                }
                            }
                        }
                    }
                    sendResponse(m_request, m_response, 201, totalResponse.toString(), internalCall, internalResp);
                }

                else if ((op.equalsIgnoreCase("create_generic_publisher") || 
                            (op.equalsIgnoreCase("create_stream")) ||
                            (op.equalsIgnoreCase("create_publisher"))) &&
                    !dataObj.optString("resourceName").equals("")){
                    UUID newPubId = this.createPublisher(dataObj.optString("resourceName"));

                    if(newPubId != null && metadataGraph.addNode(this.URI + dataObj.optString("resourceName"))){
                        JSONObject response = new JSONObject();
                        response.put("status", "success");
                        response.put("is4_uri", 
                                this.URI + dataObj.optString("resourceName"));
                        response.put("PubId", newPubId.toString());
                        sendResponse(m_request, m_response, 201, response.toString(), internalCall, internalResp);
                    } else {
                        sendResponse(m_request, m_response, 500, null, internalCall, internalResp);
                    }
                }

                else if(op.equalsIgnoreCase("create_symlink")) {
                    String name = dataObj.optString("name");
                    String is4url = dataObj.optString("url");
                    String localUri = dataObj.optString("uri");

                    if(!name.equals("") && !localUri.equals("") && RESTServer.isResource(localUri)){
                        SymlinkResource symlink = new SymlinkResource(URI + name + "/", localUri);
                        RESTServer.addResource(symlink);

                        //revert the change if the new addition introduces a cycle/loop in the metadata graph
                        JSONObject resp =new JSONObject();
                        JSONArray errors = new JSONArray();
                        JSONObject internal = new JSONObject();
                        if(!metadataGraph.addNode(symlink.getURI())){
                            symlink.loopDelete();
                            errors.add("possible cycle on adding: " + symlink.getURI());
                            resp.put("status", "fail");
                            resp.put("errors", errors);
                            sendResponse(m_request, m_response, 500, resp.toString(), internalCall, internalResp);
                        } else {
                            logger.info("added symlink: " + metadataGraph.addNode(URI + name + "/"));
                            sendResponse(m_request, m_response, 201, null, internalCall, internalResp);
                        }
                    }

                    else if(!name.equals("") && !is4url.equals("")){
                        try{
                            URL is4URL = new URL(is4url);
                            SymlinkResource symlink = new SymlinkResource(URI + name + "/", is4URL);
                            RESTServer.addResource(symlink);
                            logger.info("added symlink: " + metadataGraph.addNode(URI + name + "/"));
                            sendResponse(m_request, m_response, 201, null, internalCall, internalResp);
                        } catch(Exception e){
                            logger.log(Level.WARNING, "", e);
                            sendResponse(m_request, m_response, 400, "Invalid URL format", internalCall, internalResp);
                        }
                    }
                    
                    else {
                        sendResponse(m_request, m_response, 400, 
                                "Missing name, url, and/uri fields; Invalid request", 
                                internalCall, internalResp);
                    }
                }   

                else if( op.equalsIgnoreCase("create_smap_publisher")){
                    JSONArray errors = new JSONArray();
                    JSONObject newUris = new JSONObject();
                    JSONObject deviceRequest = new JSONObject();
            
                    //get device name
                    StringTokenizer tokenizer = new StringTokenizer(this.URI, "/");
                    int idx=0;
                    int numTokens = tokenizer.countTokens();
                    String devName = null;
                    while (idx != numTokens-1){
                        tokenizer.nextToken();
                        idx++;
                    }
                    devName = tokenizer.nextToken();

                    logger.info("operation: create_smap_publisher, devname="+ devName + " type=" + ResourceUtils.translateType(this.TYPE));

                    //populate the add-smap publisher request
                    boolean smapurlsSet = false;
                    deviceRequest.put("deviceName", devName);
                    if(dataObj.optJSONArray("smap_urls") != null){
                        deviceRequest.put("smap_urls", dataObj.optJSONArray("smap_urls"));
                        smapurlsSet = true;
                    } else if(!dataObj.optString("smap_url").equals("")){
                        deviceRequest.put("smap_url", dataObj.optString("smap_url"));
                        smapurlsSet = true;
                    }

                    if(!dataObj.optString("alias").equals("")){
                        deviceRequest.put("alias", dataObj.optString("alias"));
                    } else if(dataObj.optJSONArray("aliases") != null) {
                        deviceRequest.put("aliases", dataObj.optJSONArray("aliases"));
                    }
        
                    logger.info("Data Object:" + dataObj.toString());   
                    logger.info("Device Request:" + deviceRequest.toString());  
                    //check if smap urls are set    
                    /*if(smapurlsSet){
                        DevicesResource.addSmapPublishersToDevice(this.URI, deviceRequest, errors, newUris);
                    } else {
                        errors.add("smap_url(s) not set in request");
                    }*/

                    //send response
                    JSONObject resp = new JSONObject();
                    if(errors.size()>0){
                        resp.put("status", "fail");
                        resp.put("errors", errors.toString());
                        sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
                    } else {
                        resp.put("status", "success");
                        resp.put("new_pubs", newUris);
                        sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
                    }
                }

                else if(op.equalsIgnoreCase("move")){
                    JSONArray errors=new JSONArray();
                    JSONObject resp = new JSONObject();
                    
                    if(!dataObj.optString("src").equals("") && !dataObj.optString("dst").equals("")){
                        String srcPath = dataObj.getString("src");
                        String dstPath = dataObj.getString("dst");
                        //tell RESTServer you're moving the resource
                        if(RESTServer.moveResource(srcPath, dstPath))
                            resp.put("status", "success");
                        else{
                            errors.add("Could not move resource; check src and dst paths are valid and dst parent is default resource");
                            resp.put("status", "fail");
                            resp.put("errors", errors);
                        }
                    } else {
                        errors.add("Request must contain 'src' and 'dst' attributes");
                        resp.put("status", "fail");
                        resp.put("errors", errors);
                    }
                    sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
                }

                else {
                    sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
                }
                return;
            }

        } catch (Exception e){
            logger.log(Level.WARNING, "Request document not in proper JSON format", e);
            sendResponse(m_request, m_response, 500, null, internalCall, internalResp);
            return;
        }

        //no content
        sendResponse(m_request, m_response, 204, null, internalCall, internalResp);
    }

    /*private boolean isScriptRequest(Request m_request, Response m_response){
        boolean scriptReq = false;
        scriptReq = exchange.getAttribute("script") != null && 
                ((String) exchange.getAttribute("script")).equalsIgnoreCase("true");
        if(scriptReq)
            exchange.setAttribute("script", "");
        return true;
    }*/

    public void post(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
        try {
            Query query = m_request.getQuery();
            logger.info("POST called: " + (String)query.get("requestUri"));
            if(query.containsKey("query")  && 
                    ((String)query.get("query")).equalsIgnoreCase("true") &&
                    query.toString().contains("props_")){
                logger.info("Handling PROPERTIES query");
                query_(m_request, m_response, data, internalCall, internalResp);
            }
            else if(query.containsKey("query")  && 
                    ((String)query.get("query")).equalsIgnoreCase("true")) {
                query_(m_request, m_response, data, internalCall, internalResp);
            } 
            else if(query.containsKey("set_agg") && query.containsKey("units")){
                String setAggStr = (String)query.get("set_agg");
                String unitsStr = (String)query.get("units");
                boolean setAggBool = false;
                if(setAggStr.equalsIgnoreCase("true"))
                    setAggBool = true;
                if(setAggBool && !unitsStr.equals("")){
                    metadataGraph.setAggPoint(URI, unitsStr, true);
                    logger.info("Set aggregation point: [pathname=" + URI + 
                            ", set_agg=" + setAggStr + ", units=" + unitsStr + "]"); 
                }

                //add it to the aggregation buffer array in the properties for this resource
                JSONObject currentProps = database.rrGetProperties(URI);
                boolean containsAggBufs = currentProps.containsKey("aggBufs");

                if(setAggBool && containsAggBufs){
                    JSONArray aggBufsArray = currentProps.getJSONArray("aggBufs");
                    if(!aggBufsArray.contains(unitsStr)){
                        aggBufsArray.add(unitsStr);
                        currentProps.put("aggBufs", aggBufsArray);
                        database.rrPutProperties(URI, currentProps);
                        updateProperties(currentProps);
                    }
                } else if(!setAggBool && containsAggBufs) {
                    JSONArray aggBufsArray = currentProps.getJSONArray("aggBufs");
                    if(aggBufsArray.contains(unitsStr)){
                        metadataGraph.setAggPoint(URI, unitsStr, false);
                        aggBufsArray.remove(unitsStr);
                        currentProps.put("aggBufs", aggBufsArray);
                        database.rrPutProperties(URI, currentProps);
                        updateProperties(currentProps);
                    }
                } else if(setAggBool && !containsAggBufs){
                    JSONArray aggBufsArray = new JSONArray();
                    aggBufsArray.add(unitsStr);
                    currentProps.put("aggBufs", aggBufsArray);
                    database.rrPutProperties(URI, currentProps);
                    updateProperties(currentProps);
                }
                sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
            } 
            else {
                handlePropsReq(m_request, m_response, data, internalCall, internalResp);
                //sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            sendResponse(m_request, m_response, 500, null, internalCall, internalResp);
        }

    }

    public void delete(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
        try {
            logger.info("Handling DELETE command for " + this.URI);
            JSONArray children = database.rrGetChildren(this.URI);
            if(children.size()==0){
                //reset properties
                JSONObject emptyProps = new JSONObject();
                updateProperties(emptyProps);
                //delete rest_resource entry
                database.removeRestResource(this.URI);
                RESTServer.removeResource(this);

                //remove from internal graph
                metadataGraph.removeNode(this.URI);
                
                sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
            } else {
                JSONObject response = new JSONObject();
                JSONArray errors = new JSONArray();
                response.put("status", "fail");
                errors.add("Cannot delete resource with children");
                response.put("errors", errors);
                sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
            } 
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            sendResponse(m_request, m_response, 500, null, internalCall, internalResp);
        }
    }


    //////////////// HttpHandler function implemention ////////////////
    public void handle(Request m_request, Response m_response){
        logger.info("handler: " + m_request.getMethod() + " " + 
                        URI  + " <- " + m_request.getAddress());
        try {
            Query query= m_request.getQuery();
            String obj = m_request.getContent();
            //get the uri and remove the parameters
            String eUri = ResourceUtils.cleanPath(m_request.getPath().getPath());

            if(!eUri.contains("*")){

                //does not have to be an exact match if the request is to a symlink resource
                //otherwise the request MUST match the uri of this resource EXACTLY
                logger.info("Invoked handle TYPE=" + ResourceUtils.translateType(TYPE));
                if(TYPE != ResourceUtils.SYMLINK_RSRC){
                    String URI2 = null;
                    if(this.URI.endsWith("/"))
                        URI2 = this.URI.substring(0, this.URI.length()-1);
                    else 
                        URI2 = this.URI + "/";
                    logger.info("URI=" + this.URI + "\nURI2=" + URI2 + "\nREQ_URI=" + m_request.getPath().getPath());
                    String myUri = null;
                    if(eUri.contains("?")){
                        myUri = eUri.substring(0, eUri.indexOf("?"));
                    } else {
                        myUri = eUri;
                    }

                    if((!myUri.equalsIgnoreCase(this.URI) && !myUri.equalsIgnoreCase(URI2)) ||
                            !isActiveResource(this.URI)){
                        sendResponse(m_request, m_response, 404, null, false, null);
                        return;
                    }
                }

                String path = ResourceUtils.cleanPath(m_request.getPath().getPath());
                try {
                    String requestMethod = m_request.getMethod();
                    if(requestMethod.equalsIgnoreCase("get")){
                        logger.info("handling GET");
                        sfsStats.incGet();
                        this.get(m_request, m_response, path, false, null);
                        return;
                    } else if (requestMethod.equalsIgnoreCase("put")){
                        logger.info("handling PUT");
                        sfsStats.incPut();
                        this.put(m_request, m_response, path, obj, false, null);
                        sfsStats.docReceived(obj);
                        return;
                    } else if (requestMethod.equalsIgnoreCase("post")) {
                        logger.info("handling POST");
                        sfsStats.incPost();
                        this.post(m_request, m_response, path, obj, false, null);
                        sfsStats.docReceived(obj);
                        return;
                    } else if (requestMethod.equalsIgnoreCase("delete")) {
                        logger.info("handling DELETE");
                        sfsStats.incDelete();
                        this.delete(m_request, m_response, path, false, null);
                        return;
                    }
                } catch (Exception e){
                    logger.log(Level.WARNING, "Could not carry out the Filter operation", e);
                }
            } else {
                logger.info("Calling handleRecursiveFSQuery");//; exchangeJSON::" + exchangeJSON.toString());
                handleRecursiveFSQuery(m_request, m_response, false, null);
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "",e);
            sendResponse(m_request, m_response, 500, null, false, null);
        } 
    }


    public static void sendResponse(Request m_request, Response m_response, int code, String data, boolean internalCall, JSONObject internalResp){
        GZIPOutputStream gzipos = null; 
        PrintStream body = null;
        logger.info("Sending: " + data + "\tcode=" + code);
        try{
            if(internalCall){
                copyResponse(data, internalResp);
                return;
            }

            logger.info("Sending Response: " + data);
            /*logger.info(m_request, m_responseJSON.toString());
            JSONObject header = exchangeJSON.getJSONObject("header");
            boolean gzipResp = header.containsKey("Accept-encoding") && 
                        header.getJSONArray("Accept-encoding").getString(0).contains("gzip");
            if(m_response !=null && !internalCall){
                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Connection", "close");
                responseHeaders.set("Content-Type", "application/json");
                if(gzipResp)
                    responseHeaders.set("Content-Encoding", "gzip");

                
                exchange.sendResponseHeaders(errorCode, 0);
                responseBody = exchange.getResponseBody();
                if(response!=null){
                    if(gzipResp){
                        gzipos = new GZIPOutputStream(responseBody);
                        gzipos.write(response.getBytes());
                        gzipos.close();
                    } else {
                        responseBody.write(response.getBytes());
                        responseBody.close();
                    }
                }
            }*/

            long time = System.currentTimeMillis();
            String enc = m_request.getValue("Accept-encoding");
            boolean gzipResp = false;
            if(enc!=null && enc.indexOf("gzip")>-1)
                gzipResp = true;
            m_response.set("Content-Type", "application/json");
            m_response.set("Server", "StreamFS/2.0 (Simple 4.0)");
            m_response.set("Connection", "close");
            m_response.setDate("Date", time);
            m_response.setDate("Last-Modified", time);
            m_response.setCode(code);
            body = m_response.getPrintStream();
            if(data!=null && !gzipResp)
                body.println(data);
            else if(data!=null && gzipResp){
                m_response.set("Content-Encoding", "gzip");
                gzipos = new GZIPOutputStream((OutputStream)body);
                gzipos.write(data.getBytes());
                gzipos.close();
            }
        } catch(Exception e) {
            logger.log(Level.WARNING, "Exception thrown while sending response, closing exchange object",e);
        } finally {
            if(body!=null)
                body.close();
        }

    }

    private static void copyResponse(String response, JSONObject internalResp){
        try{
            if(internalResp != null){
                if(response != null){
                    logger.fine("Copying response to internal buffer");
                    JSONObject respObj = (JSONObject) JSONSerializer.toJSON(response);
                    internalResp.accumulateAll((Map)respObj);
                    /*Iterator keys = respObj.keys();
                    while(keys.hasNext()){
                        String thisKey = (String) keys.next();
                        internalResp.put(thisKey, respObj.get(thisKey));
                    }*/
                } else {
                    logger.fine("Response was null");
                }
            } else {
                logger.fine("Internal buffer is null");
            }

        } catch(Exception e){
            logger.log(Level.WARNING, "",e);
        }
    }

    protected boolean isActiveResource(String uri){
        int id = database.getRRTableId(uri);
        if(id>=0)
            return true;
        return false;
    }

    public static void main(String[] args){
        String clause1 = "nin:1,2,3";
        String clause2 = "or:[smap:true|timestamp:gt:12]";
        String clause3 = "gt:now,lt:now+2,gte:now-10";
        String clause4 = "and:[smap:true|timestamp:12345]";
        String clause5 = "like:jo_ge,J__ge";

        JSONObject gen1 = genJSONClause(clause1);
        JSONObject gen2 = genJSONClause(clause2);
        JSONObject gen3 = genJSONClause(clause3);
        JSONObject gen4 = genJSONClause(clause4);
        //JSONObject gen5 = genJSONClause(clause5);
        
        System.out.println("clause1: " + clause1 + "\tclause1JSON: " + gen1.toString());
        System.out.println("clause2: " + clause2 + "\tclause2JSON: " + gen2.toString());
        System.out.println("clause3: " + clause3 + "\tclause3JSON: " + gen3.toString());
        System.out.println("clause4: " + clause4 + "\tclause4JSON: " + gen4.toString());
        //System.out.println("clause5: " + clause5 + "\tclause5JSON: " + gen5.toString());
    }

    public static boolean isNumber(String val){
        try {
            Long.parseLong(val);
            return true;
        } catch(Exception e){
            return false;
        }
    }

    /**
     * This function evaluates a clause string and returns a JSONObject that expresses
     * the clause as a valid MongoDB query.  The url query interface is meant to be a simple
     * query interface for quickly obtaining values.  If anything more sophististicated
     * needs to be done, you may POST a query object following the mongodb query interface.
     * 
     * ../query=true&props_val=<clause>&props=<clause>
     * 
     * example queries:
     *      ..props_label=in:val1,val2,val3&props_timestamp=gt:12345,lt:23456
     *      ..props=or:[label:one|title:two]
     *
     *  , is used to separate values in an $or, $and array condition or to separate
     *      conditions on a value.
     *  | is used between [] to separate conditional JSON objects
     *
     */
    public static JSONObject genJSONClause(String clause){
        JSONObject clauseJSON = new JSONObject();
        if(clause != null){

            //case: ..&props=or:[label:one|title:two]
            if(clause.startsWith("and:[") || clause.startsWith("or:[") && 
                    clause.endsWith("]")){
                JSONArray vals = new JSONArray();
                String valsStr = clause.substring(clause.indexOf("[")+1, clause.length()-1);
                StringTokenizer valsToks = new StringTokenizer(valsStr, "|");
                while(valsToks.hasMoreTokens()){
                    String thisToken = valsToks.nextToken();
                    StringTokenizer innerToks = new StringTokenizer(thisToken, ":");
                    if(innerToks.countTokens()==2){
                        String attr = innerToks.nextToken();
                        String valStr = innerToks.nextToken();
                        JSONObject cond = new JSONObject();

                        //convert the value to long if necessary
                        if(isNumber(valStr))
                            cond.put(attr, Long.parseLong(valStr));
                        else
                            cond.put(attr, valStr);

                        vals.add(cond);
                    } else{
                        //process inner clauses
                        JSONObject cond = new JSONObject();
                        String newToken = thisToken.substring(thisToken.indexOf(":")+1, thisToken.length());

                        JSONObject innerCond = genJSONClause(newToken);
                        if(innerCond != null){
                            cond.put(innerToks.nextToken(), innerCond);
                            vals.add(cond);
                        }
                    }
                }
                String op = "$" + clause.substring(0, clause.indexOf(":"));
                clauseJSON.put(op, vals);
                
                return clauseJSON;
            }

            //case: ..&props_label=in:val1,val2,val3
            else if(clause.startsWith("in:") || clause.startsWith("nin:") || 
                    clause.startsWith("and:") || clause.startsWith("or:")){
                JSONArray vals = new JSONArray();
                String valsStr = clause.substring(clause.indexOf(":")+1, clause.length());
                StringTokenizer valsToks = new StringTokenizer(valsStr, ",");
                while(valsToks.hasMoreTokens()){
                    String valStr = valsToks.nextToken();
                    if(isNumber(valStr))
                        vals.add(Long.parseLong(valStr));
                    else
                        vals.add(valStr);
                }
                String op = "$" + clause.substring(0, clause.indexOf(":"));
                clauseJSON.put(op, vals);

                return clauseJSON;
            }

            //case: ..&props__keywords=like:jo_ge,J__ge
            /*else if(clause.startsWith("like:") {
                StringTokenizer tokenizer = new StringTokenizer(clause,",");
                Vector<String> tokens = new Vector<String>();
                while(tokenizer.hasMoreTokens())
                    tokens.addElement(tokenizer.nextToken());
                for(int i=0; i<tokens.size(); ++i){
                    
                }
            }*/

            //case: ..&props_timestamp=gt:12345,lt:23456
            else if(clause.startsWith("gt:") || clause.startsWith("lt:") ||
                    clause.startsWith("gte:") || clause.startsWith("lte:") ||
                    clause.startsWith("ne:") ){
                StringTokenizer valToks = new StringTokenizer(clause, ",");
                while(valToks.hasMoreTokens()){
                    String thisToken = valToks.nextToken();
                    StringTokenizer innerToks = new StringTokenizer(thisToken, ":");
                    if(innerToks.countTokens()==2){
                        String op = innerToks.nextToken();
                        String valStr = innerToks.nextToken();
                        long valLong = -1;
                        if(valStr.contains("now")){
                            long timestamp = System.currentTimeMillis();
                            if(valStr.equalsIgnoreCase("now")){
                                valLong = timestamp;
                            } else if(valStr.startsWith("now-")){
                                String numStr = valStr.substring(valStr.indexOf("-")+1, valStr.length());
                                if(isNumber(numStr)){
                                    long sub = Long.parseLong(numStr);
                                    valLong = timestamp - sub;
                                    //System.out.println("parsedNum: " + num + "; " + val + "=" + val);
                                }
                            } else if(valStr.startsWith("now+")){
                                String numStr = valStr.substring(valStr.indexOf("+")+1, valStr.length());
                                if(isNumber(numStr)){
                                    long add = Long.parseLong(numStr);
                                    valLong = timestamp + add;
                                    //System.out.println("parsedNum: " + num + "; " + val + "=" + val);
                                }
                            }
                        } else if(isNumber(valStr)){
                            valLong = Long.parseLong(valStr);
                        }

                        if(valLong!=-1)
                            clauseJSON.put("$" + op, valLong);
                        else
                            clauseJSON.put("$" + op, valStr);

                    }
                }
                return clauseJSON;
            }

        }

        return null;
    }

    //evaluate query
    public void query_(Request m_request, Response m_response, String data, boolean internalCall, JSONObject internalResp){
        JSONObject resp = new JSONObject();
        JSONArray errors = new JSONArray();
        try{
            Query query = m_request.getQuery();
            JSONObject propsQueryObj = new JSONObject();

            //get query object from input data
            if(data != null && !data.equals("")){
                JSONObject dataJsonObj = (JSONObject) JSONSerializer.toJSON(data);
                if(TYPE == ResourceUtils.PUBLISHER_RSRC || TYPE == ResourceUtils.GENERIC_PUBLISHER_RSRC){
                    JSONObject dataPropsQuery = dataJsonObj.optJSONObject("props_query");
                    propsQueryObj.putAll(dataPropsQuery);
                } else {
                    propsQueryObj.putAll(dataJsonObj);
                }
            }

            Iterator keys = query.keySet().iterator();
            logger.fine("REQUEST_KEYS::" + keys.toString());
            Vector<String> attributes = new Vector<String>();
            Vector<String> values = new Vector<String>();
            while(keys.hasNext()){
                String thisKey = (String) keys.next();
                logger.fine("Keys found!; thisKey=" + thisKey);
                //exchange.setAttribute(thisKey, "");
                /*if(thisKey.startsWith("props_like_")){
                    String str = "props_like_";
                    String queryKey = thisKey.substring(thisKey.indexOf(str)+str.length(), thisKey.length());
                    String queryValue = exchangeJSON.optString(thisKey);

                    logger.info("Query Value: " + queryValue);

                    JSONObject conditions = Resource.genJSONClause(queryValue);

                    logger.info("Conditions: " + conditions);
                    if(conditions!=null)
                        propsQueryObj.put(queryKey, conditions);
                    else
                        propsQueryObj.put(queryKey, queryValue);
                } else*/ if(thisKey.startsWith("props_")){
                    String str = "props_";
                    String queryKey = thisKey.substring(thisKey.indexOf(str)+str.length(), thisKey.length());
                    String queryValue = (String)query.get(thisKey);//exchangeJSON.optString(thisKey);

                    logger.info("Query Value: " + queryValue);

                    JSONObject conditions = Resource.genJSONClause(queryValue);
                    logger.info("Conditions: " + conditions);
                    if(conditions!=null)
                        propsQueryObj.put(queryKey, conditions);
                    else
                        propsQueryObj.put(queryKey, queryValue);

                } else if(thisKey.startsWith("props")){
                    String queryValue = (String)query.get(thisKey);//exchangeJSON.optString(thisKey);

                    JSONObject conditions = Resource.genJSONClause(queryValue);
                    if(conditions!=null)
                        propsQueryObj.putAll(conditions);
                    else
                        logger.warning("Invalid conditions set for generic props query");
                }
            }

            logger.fine("Props Query: " + propsQueryObj.toString());

            if(!propsQueryObj.toString().equals("{}")){
                propsQueryObj.put("is4_uri", URI);
                if(last_props_ts>0)
                    propsQueryObj.put("timestamp", last_props_ts);
                
                //check for any regex expressions
                /*Iterator keys = propsQueryObj.keys();
                while(keys.hasNext()){
                    String thisKey =keys.next();
                }*/

                logger.info("Props Query: " + propsQueryObj.toString());
                JSONObject mqResp = mongoDriver.queryProps(propsQueryObj.toString());
                logger.fine("mqResp: " + mqResp.toString());
                JSONArray propsRespObjArray = mqResp.getJSONArray("results");
                if(propsRespObjArray.size()>0){
                    JSONObject propsRespObj = (JSONObject) propsRespObjArray.get(0);
                    propsRespObj.remove("is4_uri");
                    propsRespObj.remove("timestamp");
                    /*mqResp.put("results", propsRespObj);
                    resp.putAll(mqResp);*/
                    resp.putAll(propsRespObj);
                }
            } else {
                errors.add("Empty or invalid query");
                
                logger.warning(errors.toString());
                resp.put("errors", errors);
            }
        } catch (Exception e){
            logger.log(Level.WARNING, "", e);
            if(e instanceof JSONException){
                errors.add("Invalid JSON for POST data; url params ignored");
                resp.put(errors, errors);
                sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
                return;
            }
        }
        sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
    }

    public UUID createPublisher(String pubName){
        JSONArray children = database.rrGetChildren(this.URI);
        try {
            UUID thisPubIdUUID = null;
            String pubUri = this.URI + pubName;
            if(!children.contains(pubName) && 
                (thisPubIdUUID=database.isPublisher(pubUri, false)) == null){

                //register the publisher
                Registrar registrar = Registrar.registrarInstance();
                String newPubId = registrar.registerDevice(pubUri);
                try{ 
                    thisPubIdUUID = UUID.fromString(newPubId);
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                }

                //add to publishers table
                database.addPublisher(thisPubIdUUID, null, null, pubUri, null);
                GenericPublisherResource p = new GenericPublisherResource(pubUri, thisPubIdUUID);
                RESTServer.addResource(p);
                return thisPubIdUUID;
            } else {
                logger.warning("Publisher already registered or publisher name already "+
                        "child of " + this.URI + "\nPubName = " + pubName +
                        "\n\tChildren: " + children.toString() );
            }

            //bind the device to this context
            //database.addDeviceEntry(deviceName, this.URI, thisPubIdUUID);
        } catch (Exception e){
            logger.log(Level.WARNING, "",e); } return null;
    }

    /*protected void setExchangeJSON(JSONObject params){
        logger.fine("Setting exchangeJSON==" + params.toString());
        exchangeJSON.putAll(params);
    }*/

    /**
     * Resolves the uri.  If query has been posted, it is applied to each resource that the uri
     * solves to.  The results are returns in the following format.
     *
     * {
     *  "/is4/...":{..}
     * }
     *
     * The attribute is the uri, the value is the results of applying the query to that uri.
     */
    protected void handleRecursiveFSQuery(Request m_request, Response m_response, boolean internalCall, JSONObject internalResp){//, String uri){
        
        Query query = m_request.getQuery();
        String requestUri =  m_request.getPath().getPath();

        logger.info("FSQuery:: requestUri=" + requestUri + " internalCall: " + internalCall);

        JSONArray resolvedUris = new JSONArray();

        String uriOnly = requestUri;    
        if(!query.toString().equals("")){
            logger.info("Request URI, no params: " + uriOnly + " \ttype=" + (String) query.get("type"));
            resolvedUris.addAll( database.resolveStarredUri(uriOnly, (String) query.get("type")) );
        } else{
            resolvedUris.addAll( database.resolveStarredUri(uriOnly, null) );
        }
        logger.info("Resolved Uris: " + resolvedUris.toString());

        //get the request method
        String requestMethod =  m_request.getMethod();
        String putPostData = null;
        try {
            if(requestMethod.equalsIgnoreCase("put") || requestMethod.equalsIgnoreCase("post"))
                putPostData = m_request.getContent();
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }

        JSONObject responses = new JSONObject();
        try{
            if(requestMethod.equalsIgnoreCase("get")){
                for(int i=0; i<resolvedUris.size(); i++){
                    Resource thisResource = RESTServer.getResource(resolvedUris.getString(i));
                    if(thisResource != null){
                        JSONObject respBuffer = new JSONObject();
                        thisResource.get(m_request, m_response, resolvedUris.getString(i), 
                                            true, respBuffer);
                        if(thisResource.TYPE == ResourceUtils.SYMLINK_RSRC){
                            respBuffer.put("links_to", database.getSymlinkAlias(thisResource.URI));
                            responses.put(thisResource.URI, respBuffer);
                        } else {
                            responses.put(thisResource.URI, respBuffer);
                        }
                    }
                }
            } else if(requestMethod.equalsIgnoreCase("put")){
                for(int i=0; i<resolvedUris.size(); i++){
                    Resource thisResource = RESTServer.getResource(resolvedUris.getString(i));
                    if(thisResource != null){
                        JSONObject respBuffer = new JSONObject();
                        thisResource.put(m_request, m_response, resolvedUris.getString(i), 
                                            putPostData, true, respBuffer);
                        if(thisResource.TYPE == ResourceUtils.SYMLINK_RSRC){
                            //processResponse(thisResource, respBuffer);
                            //responses.putAll(respBuffer);
                            respBuffer.put("links_to", database.getSymlinkAlias(thisResource.URI));
                            responses.put(thisResource.URI, respBuffer);
                        } else{
                            responses.put(thisResource.URI, respBuffer);
                        }
                    }
                }
            } else if(requestMethod.equalsIgnoreCase("post")){
                for(int i=0; i<resolvedUris.size(); i++){
                    Resource thisResource = RESTServer.getResource(resolvedUris.getString(i));
                    if(thisResource != null){
                        JSONObject respBuffer = new JSONObject();
                        thisResource.post(m_request, m_response,resolvedUris.getString(i), putPostData, true, respBuffer);
                        if(thisResource.TYPE == ResourceUtils.SYMLINK_RSRC){
                            respBuffer.put("links_to", database.getSymlinkAlias(thisResource.URI));
                            responses.put(thisResource.URI, respBuffer);
                        } else {
                            responses.put(thisResource.URI, respBuffer);
                        }
                    }
                }
            } else if(requestMethod.equalsIgnoreCase("delete")){
                if(resolvedUris.size()>0)
                    depthFirstSort(resolvedUris);
                for(int i=0; i<resolvedUris.size(); i++){
                    Resource thisResource = RESTServer.getResource(resolvedUris.getString(i));
                    if(thisResource != null){
                        JSONObject respBuffer = new JSONObject();
                        thisResource.delete(m_request, m_response, resolvedUris.getString(i), true, respBuffer);
                        if(thisResource.TYPE == ResourceUtils.SYMLINK_RSRC){
                            //processResponse(thisResource, respBuffer);
                            //responses.putAll(respBuffer);
                            respBuffer.put("links_to", database.getSymlinkAlias(thisResource.URI));
                            responses.put(thisResource.URI, respBuffer);
                        } else {
                            responses.put(thisResource.URI, respBuffer);
                        }
                    }
                }
            }
        } catch (Exception e){
            logger.log(Level.WARNING, "", e);
        }

        sendResponse(m_request, m_response, 200, responses.toString(), internalCall, internalResp);
    }

    private void processResponse(Resource resource, JSONObject buffer){
        if(resource != null && buffer != null && resource.TYPE == ResourceUtils.SYMLINK_RSRC){
            Iterator keys = buffer.keys();
            Vector<String> oriKeys = new Vector<String>();
            StringBuffer allKeysStrBuf = new StringBuffer();
            while(keys.hasNext()){
                String thisKey = (String) keys.next();
                oriKeys.add(thisKey);
                allKeysStrBuf.append(thisKey + "\n");
            }
            logger.info("\n" + allKeysStrBuf.toString());
            String slinkUri = resource.getURI();
            String linksToUril = ((SymlinkResource)resource).getLinkString();
            logger.info("slink uri: " + slinkUri + " links to: " + linksToUril);
            
            if(linksToUril.startsWith("http")){
                try{
                    URL url = new URL(linksToUril);
                    linksToUril = url.getPath();
                    logger.info("Now links to: " + linksToUril);
                } catch(Exception e){}
            }

            for(int i=0; i<oriKeys.size(); i++){
                String thisKey = oriKeys.elementAt(i);
                logger.info("Old key: " + thisKey);
                JSONObject thisResp = buffer.getJSONObject(thisKey);
                buffer.remove(thisKey);
                thisKey = thisKey.replace(linksToUril, slinkUri);
                logger.info("New key: " + thisKey);
                thisKey = thisKey.replaceAll("\\/+", "/");
                buffer.put(thisKey, thisResp);
            }
        }
    }

    private void depthFirstSort(JSONArray uris){
        JSONObject buckets = new JSONObject();
        Vector<Integer> keysVec = new Vector<Integer>();
        for(int i=0; i< uris.size(); i++){
            String thisUri = uris.getString(i);
            logger.info("thisUri: " + thisUri);
            StringTokenizer tokenizer = new StringTokenizer(thisUri, "/");
            Integer tokenCount = new Integer(tokenizer.countTokens());
            JSONArray thisBucket = buckets.optJSONArray(tokenCount.toString());
            if(thisBucket == null){
                thisBucket = new JSONArray();
                keysVec.add(tokenCount);
            }
            thisBucket.add(thisUri);
            buckets.put(tokenCount.toString(), thisBucket);
        }

        Integer[] bucketSizes = new Integer[keysVec.size()];
        keysVec.toArray(bucketSizes);
        Arrays.sort(bucketSizes);

        uris.clear();
        for(int i=keysVec.size()-1; i>=0; i--){
            logger.info("Sorted list: " + bucketSizes[i] + "\nKey Vec: " + keysVec.get(i));
            JSONArray bucket = buckets.getJSONArray(bucketSizes[i].toString());
            for(int j=0; j<bucket.size(); j++)
                uris.addAll(bucket);
        }

    }

    public void updateProperties(JSONObject propsObj){
        
        //MongoDBDriver mongoDriver = new MongoDBDriver();

        //add an array to support fulltxt search
        HashMap<String, String> uniqueKeys = new HashMap<String, String>();
        JSONArray keywords = new JSONArray();
        Iterator pKeys = propsObj.keys();
        while(pKeys.hasNext()){
            String thisKey = (String)pKeys.next();
            if(!uniqueKeys.containsKey(thisKey.trim())){
                keywords.add(thisKey.trim());
                uniqueKeys.put(thisKey.trim(),"y");
            }
            String val = propsObj.get(thisKey).toString();
            val.replace("[","");val.replace("]","");val.replace("{","");val.replace("}","");
            val.replace(",","");val.replace(";","");
            String delims = "[ ]+";
            String[] tokens = val.split(delims);
            for(int i=0;i<tokens.length; ++i){
                if(!uniqueKeys.containsKey(tokens[i].trim())){
                    keywords.add(tokens[i].trim());
                    uniqueKeys.put(tokens[i].trim(),"y");
                }
            }
        }
        logger.fine(uniqueKeys.toString());
        logger.fine("_keywords:" + keywords.toString());
        propsObj.put("_keywords", keywords);

        //add is4_uri
        propsObj.put("is4_uri", URI.toString());

        //add timestamp
        long timestamp = System.currentTimeMillis();
        propsObj.put("timestamp", timestamp);

        //store in mongodb repos
        mongoDriver.putPropsEntry(propsObj);


        //save the last updated timestamp in the database
        database.updateLastPropsTs(URI, timestamp);

        last_props_ts = timestamp;

        //place it in buffer
        /*if(database.hasPropertiesBuffered(URI))
            database.insertPropertiesIntoBuffer(URI, propsObj);
        else
            database.updatePropertiesInBuffer(URI, propsObj);*/
    }

    public void handlePropsReq(Request m_request, Response m_response, String data, boolean internalCall, JSONObject internalResp){
        try{
            JSONObject dataObj = (JSONObject) JSONSerializer.toJSON(data);
            String op = dataObj.getString("operation");
            if (op.equalsIgnoreCase("update_properties")){
                logger.info("processing update_properties");
                JSONObject response = new JSONObject();
                JSONObject currentProps = database.rrGetProperties(URI);
                JSONObject properties = null;
                try{
                    properties = dataObj.getJSONObject("properties");
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                    response.put("status", "fail");
                    JSONArray errors = new JSONArray();
                    errors.add("Missing properties object");
                    response.put("errors", errors);
                    sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
                    return;
                }

                if(currentProps != null){
                    Iterator keys = properties.keys();
                    while(keys.hasNext()){
                        String thisKey = (String) keys.next();
                        currentProps.put(thisKey, properties.get(thisKey));
                    }
                    database.rrPutProperties(URI, currentProps);
                    updateProperties(currentProps);
                } else {
                    database.rrPutProperties(URI, properties);
                    updateProperties(properties);
                }
                response.put("status", "success");
                sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
            }

            else if (op.equalsIgnoreCase("overwrite_properties")){
                logger.info("processing overwrite_properties");
                JSONObject response = new JSONObject();
                JSONObject properties = null;
                try{
                    properties = dataObj.getJSONObject("properties");
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                    response.put("status", "fail");
                    JSONArray errors = new JSONArray();
                    errors.add("Missing properties object");
                    response.put("errors", errors);
                    sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
                }

                database.rrPutProperties(URI, properties);
                updateProperties(properties);
                response.put("status", "success");
                sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
            }

            else{
                sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
            }
        } catch (Exception e){
            //silent fail
            sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
        }
    }

    private void findSymlinks(JSONArray children){
        JSONArray newChildren = new JSONArray();
        for(int i=0; i<children.size(); i++){
            String childName = (String) children.get(i);
            String resourcePath = URI + childName;
            boolean isSymlink = database.isSymlink(resourcePath);
            if(isSymlink){
                logger.info(resourcePath + " is a symlink");
                SymlinkResource slr = (SymlinkResource) RESTServer.getResource(resourcePath);
                if(slr != null){
                    String symlinkStr = childName  + " -> " + slr.getLinkString();
                    newChildren.add(symlinkStr);
                }
            } else {
                newChildren.add(childName);
            }
        }
        children.clear();
        children.addAll(newChildren);
    }

    private Vector<String> createTokenVector(String target, String delim){
        StringTokenizer tokens = new StringTokenizer(target, delim);
        Vector<String> allToks  = new Vector<String>();
        while(tokens.hasMoreTokens())
            allToks.add((String)tokens.nextToken());
        return allToks;
    }

    public JSONArray queryAggTimeseries(String aggStr, String units, JSONObject queryJson){
        JSONArray queryResults = new JSONArray();
        try{
            String qres = metadataGraph.queryAgg(URI, aggStr, units, queryJson);
            logger.fine("qres=" + qres);
            if(qres !=null){
                JSONSerializer serializer = new JSONSerializer();
                JSONObject qresObj = (JSONObject)serializer.toJSON(qres);
                if(qresObj.containsKey("results"))
                    queryResults = qresObj.getJSONArray("results");
                else
                    queryResults.add(qresObj);
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
        return queryResults;
    }

    private JSONObject genTSQueryObject(Request m_request, Response m_response, String data){
        JSONObject tsQueryObj2 = new JSONObject();
        try{
        
            //get query object from input data
            if(data != null && !data.equals("")){   
                JSONObject dataJsonObj = (JSONObject) JSONSerializer.toJSON(data);
                JSONObject dataTsQuery = dataJsonObj.optJSONObject("ts_query");
                tsQueryObj2.putAll(dataTsQuery);
            }

            Query query = m_request.getQuery();
            Iterator keys = query.keySet().iterator();
            Vector<String> attributes = new Vector<String>();
            Vector<String> values = new Vector<String>();
            while(keys.hasNext()){
                String thisKey = (String) keys.next();
                logger.fine("Keys found!; thisKey=" + thisKey);
                if(thisKey.startsWith("ts_")){
                    String str = "ts_";
                    String queryKey = thisKey.substring(thisKey.indexOf(str)+str.length(), thisKey.length());
                    String queryValue = (String)query.get(thisKey);

                    logger.info("Query Value: " + queryValue);

                    JSONObject conditions = Resource.genJSONClause(queryValue);
                    logger.info("Conditions: " + conditions);
                    if(conditions!=null){
                        if(queryKey.equalsIgnoreCase("timestamp"))
                            tsQueryObj2.put("ts", conditions);
                    } else{
                        if(isNumber(queryValue)){
                            long val = Long.parseLong(queryValue);
                            if(queryKey.equalsIgnoreCase("timestamp"))
                                tsQueryObj2.put("ts", val);
                        } else {
                            if(queryKey.equalsIgnoreCase("timestamp"))
                                tsQueryObj2.put("ts", queryValue);
                        }
                    }

                } else if(thisKey.startsWith("ts")){
                    String queryValue = (String)query.get(thisKey);

                    JSONObject conditions = Resource.genJSONClause(queryValue);
                    if(conditions!=null){
                        tsQueryObj2.putAll(conditions);
                    } else{
                        if(isNumber(queryValue)){
                            long val = Long.parseLong(queryValue);
                            if(thisKey.equalsIgnoreCase("timestamp"))
                                tsQueryObj2.put("ts", queryValue);
                            else
                                tsQueryObj2.put(thisKey, val);
                        } else {
                            logger.warning("Invalid conditions set for generic props query");
                        }
                    }
                        
                }
            }
        } catch (Exception e){
            logger.log(Level.WARNING, "", e);
        }

        logger.fine("Timeseries Query2: " + tsQueryObj2.toString());
        return tsQueryObj2;
    }

    private void handleTSAggQuery(Request m_request, Response m_response, String data, boolean internalCall, JSONObject internalResp){
        JSONObject resp = new JSONObject();
        JSONArray errors = new JSONArray();
        resp.put("path", URI);
        try{
            Query query = m_request.getQuery();
            JSONObject tsQueryObj2 = genTSQueryObject(m_request, m_response, data);
            if(!tsQueryObj2.toString().equals("{}")){
                String aggStr = (String)query.get("agg");
                String units = (String)query.get("units");
                JSONArray mqResp2 = queryAggTimeseries(aggStr, units, tsQueryObj2);
                logger.fine("mqResp2: " + mqResp2.toString());
                resp.put("ts_query_results", mqResp2);
            } else {
                errors.add("TS Query Error: Empty or invalid query");
                logger.warning(errors.toString());
                resp.put("errors", errors);
            }
        } catch (Exception e){
            logger.log(Level.WARNING, "", e);
            if(e instanceof JSONException){
                errors.add("Invalid JSON for POST data; url params ignored");
                resp.put(errors, errors);
                sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
                return;
            }
        }
        sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
    }

    public static String cleanPath(String path){
        //clean up the path
        if(path == null)
            return path;

        if(!path.startsWith("/"))
            path = "/" + path;
        path = path.replaceAll("/+", "/");
        if(!path.endsWith("/"))
            path += "/";
        return path;
    }

    public void setNewProperties(JSONObject props){
        database.rrPutProperties(URI, props);
        updateProperties(props);
    }

    public void setNewScriptProperties(String props){
        database.rrPutProperties(URI, props);
        updateProperties(((JSONObject)JSONSerializer.toJSON(props)));
    }
}
