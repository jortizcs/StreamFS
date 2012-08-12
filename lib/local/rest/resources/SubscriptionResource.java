package local.rest.resources;

import local.db.*;
import local.rest.*;
import local.rest.resources.util.*;
import local.rest.interfaces.*;
import is4.*;

import net.sf.json.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;

import javax.naming.InvalidNameException;
import java.io.*; 

import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;



public class SubscriptionResource extends Resource{
	protected static transient Logger logger = Logger.getLogger(SubscriptionResource.class.getPackage().getName());
	protected static MySqlDriver database = (MySqlDriver) DBAbstractionLayer.database;

	public int TYPE= ResourceUtils.SUBSCRIPTION_RSRC;
	protected static final String SUB_ROOT = "/sub/";

	//private JSONObject SUB_REQUEST_OBJ =null;
	private String source = null;
	private String sourcePath = null;
	private String durl=null;
	private String duri=null;
	private String subname=null;
	private long installTime=-1;
	private UUID SUBID = null;
	private String wildcardPath = null;

    private boolean deleteActive = false;

	public SubscriptionResource(UUID subid) throws Exception, InvalidNameException{
		super();
		subname = generateSubRsrcName(subid);
		SUBID = subid;

		if(subname.equals(""))
			subname = subid.toString();

		String path = SUB_ROOT + subname + "/";
		if(path.indexOf("?") > 0 || path.indexOf("&") > 0)
			throw new Exception("Invalid path string: No '&' or '?' allowed OR path already exists");

		if(!path.endsWith("/"))
			path += "/";
		URI = path;
	
		//save if NOT already in database	
		if(!database.rrPathExists(URI))
			database.rrPutPath(URI);

		//set type
		database.setRRType(URI, ResourceUtils.translateType(TYPE).toLowerCase());

		//populate source and destination
		source = database.getSubSourcePubId(subid);
		if(source != null && !source.equalsIgnoreCase("wc")){
			sourcePath = database.getIs4RRPath(UUID.fromString(source));
		} else {
			wildcardPath = database.getSubSourceWildcardPath(subid);
		}
		durl = database.getSubDestUrlStr(SUBID);
		duri = database.getSubDestUriStr(SUBID);
			
		//udpate associated entry in subcribers table
		if(source !=null && !source.equalsIgnoreCase("wc")) {
			database.partialUpdateSubEntry(SUBID, subname, URI, UUID.fromString(source), null);
		} else {
			database.partialUpdateSubEntry(SUBID, subname, URI, null, wildcardPath);
			logger.info("Subscriber alias: " + subname + " URI: " + URI);
		}
	}
	
	public SubscriptionResource(UUID subid, String path) throws Exception, InvalidNameException{
		super();
		
		SUBID = subid;
		if(path.indexOf("?") > 0 || path.indexOf("&") > 0)
			throw new Exception("Invalid path string: No '&' or '?' allowed OR path already exists");

		if(!path.endsWith("/"))
			path += "/";
		URI = path;
	
		//save if NOT already in database	
		if(!database.rrPathExists(URI))
			database.rrPutPath(URI);

		//set type
		database.setRRType(URI, ResourceUtils.translateType(TYPE).toLowerCase());

		//populate source and destination
		source = database.getSubSourcePubId(subid);
		if(source != null && !source.equalsIgnoreCase("wc")){
			sourcePath = database.getIs4RRPath(UUID.fromString(source));
		} else {
			wildcardPath = database.getSubSourceWildcardPath(subid);
		}
		durl = database.getSubDestUrlStr(SUBID);
		duri = database.getSubDestUriStr(SUBID);
	}

	public void get(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
		try{
			JSONObject response = new JSONObject();
			response.put("status", "success");
			response.put("subid", SUBID.toString());
			if(durl != null)
				response.put("destination", durl);
			else
				response.put("destination", duri);

			if(source != null && !source.equalsIgnoreCase("wc")) {
                if(duri!=null){
                    JSONArray srcPaths = new JSONArray();
                    JSONArray srcPubids = database.getSubSourcePubIds(SUBID);
                    for(int i =0; i<srcPubids.size(); i++)
                        srcPaths.add(database.
                                    getIs4RRPath(UUID.fromString(
                                        (String)srcPubids.get(i))));
				    response.put("sourcePaths", srcPaths);
                }
                else{
                    response.put("sourceId", source);
                    response.put("sourcePath", sourcePath);
                }
			} else {
				response.put("wildcardPath", wildcardPath);
			}

			sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
			return;
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		sendResponse(m_request, m_response, 400, null, internalCall, internalResp);
	}

	public void put(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		post(m_request, m_response, path, data, internalCall, internalResp);
	}

	public void post(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		JSONObject dataJson = null;
		try {
            dataJson = (JSONObject)JSONSerializer.toJSON(data);
            String op = dataJson.getString("operation");
            if(op.equalsIgnoreCase("remove_src") && dataJson.containsKey("paths")){
                JSONArray paths = dataJson.getJSONArray("paths");
                JSONArray pubids = database.getPubIdsFromSnodes(paths);
                int numLeft = database.getNumSubSrcs(SUBID);
                String lastOfPubids = null;
                for(int i=0; i<pubids.size(); i++){
                    if(numLeft==1){
                        lastOfPubids = (String)pubids.get(i);
                        break;
                    } else {
                        String thisPubid = (String)pubids.get(i);
                        database.removeSubEntry(SUBID, 
                                                UUID.fromString(thisPubid));
                        numLeft = database.getNumSubSrcs(SUBID);
                    }
                    
                }

                //if there's one subscription entry left, delete the
                //subscription entirely
                if(numLeft==1) {
                    String lastPubid = database.getSubSourcePubId(SUBID);
                    if(lastPubid != null && lastPubid.equals(lastOfPubids))
                        this.delete(m_request, m_response, path, true, internalResp);
                }
                
                response.put("status", "success");
                sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
                return;
            } else {
                errors.add("Request must include: remove_src as operation as an Array of paths");
                response.put("errors", errors);
            }
        
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
            errors.add("Request must include: remove_src as operation as an Array of paths");
            response.put("errors", errors);
		}
		
		sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
	}

	public void delete(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
        synchronized(this){
            if(!deleteActive) deleteActive=true;
            else return;
        }
		SubMngr submngr = SubMngr.getSubMngrInstance();
        logger.info("Check if is subscriber:: " + submngr.isSubscriber(SUBID.toString()));
		if(submngr.isSubscriber(SUBID.toString())){
			//remove the subscription
			submngr.removeSub(SUBID);

			//delete rest_resource entry
			database.removeRestResource(this.URI);

			//remove the resource from the rest server
			RESTServer.removeResource(this);

			//remove from internal graph
			this.metadataGraph.removeNode(this.URI);
		}
		JSONObject response = new JSONObject();
		response.put("status", "success");
		sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
        try {} catch(Exception e){}
        finally{
            synchronized(this){
                deleteActive = false;
            }
        }
	}

	private String generateSubRsrcName(UUID subid){

		int max_retries = 10;
		int retries=0;
		//choose 7 random characters in the UUID string
		String name="";
		String subidStr = subid.toString();
		Random random = new Random();
		do {
			for(int i=0; i<7; i++){
				char thisChar = subidStr.charAt(Math.abs(random.nextInt()%subidStr.length()));

				logger.finer("i=" + i + ", name.length()=" + name.length());

				//thisChar cannot be a '-' if it's the first or last character in the name
				while((i==0 || i==6) && thisChar == '-'){
					logger.finer("here1: i=" + i + "thisChar= " + thisChar);
					thisChar = subidStr.charAt(Math.abs(random.nextInt()%subidStr.length()));
				}

				//thisChar cannot be a '-' if the previous character was a '-'
				while(i != 0 && i != 6 && name.charAt(i-1) == '-' && thisChar == '-'){
					logger.finer("here2: i=" + i + "thisChar= " + thisChar);
					thisChar = subidStr.charAt(Math.abs(random.nextInt()%subidStr.length()));
				}

				name += thisChar;
			}
			retries +=1;
		} while (!ResourceUtils.devNameIsUnique(SUB_ROOT, name) && retries<max_retries); 
		
		return name;
	}


	public UUID getSubId(){
		return SUBID;
	}	
}
