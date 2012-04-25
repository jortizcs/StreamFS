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
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.naming.InvalidNameException;
import java.io.*; 
import java.util.UUID;
import com.sun.net.httpserver.*;

public class ProcessResource extends Resource {
	
	protected static transient final Logger logger = Logger.getLogger(ProcessResource.class.getPackage().getName());

    protected String scriptStr=null;
    private String lastError = null;

    public ProcessResource(String path, String scriptObjStr) throws Exception, InvalidNameException{
        super(path);
        scriptStr = scriptObjStr;
        TYPE = ResourceUtils.PROCESS_RSRC;
        database.setRRType(URI, ResourceUtils.translateType(TYPE).toLowerCase());
        JSONObject properties = new JSONObject();
        properties.put("script", translateScript(scriptObjStr));
        setNewProperties(properties);
    }

    private String translateScript(String scriptObjStr){
        String ret=scriptObjStr;
        try {
            StringBuffer retbuf = new StringBuffer();
            JSONObject s = (JSONObject)JSONSerializer.toJSON(scriptObjStr);
            JSONObject func = s.getJSONObject("func");
            s.discard("func");
            if(scriptObjStr != null){
                retbuf.append("function(");
                JSONArray params = func.getJSONArray("params");
                for(int i=0; i<params.size(); i++){
                    retbuf.append((String)params.get(i));
                    if(i<params.size()-1)
                        retbuf.append(",");
                }
                retbuf.append("){").append(func.getString("text")).append("}");
                retbuf.toString();
                ret = s.toString().substring(0, s.toString().length()-1) + ",func:\"" + retbuf.toString() + "\"";
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
        return ret;
    }

    public boolean startNewProcess(String subid){
        //create a new publisher associated with the output of running this process
        //pass the new path to processing layer for POSTing output to it.
        //  + pass in the materialize option as the default (can be changed on a specific publisher)
        //  + pass in the associated subscription id
        return true;
    }

    public void put(HttpExchange exchange, String data, boolean internalCall, 
                    JSONObject internalResp)
    {
        //To update the definition of the process.
    }

    public void post(HttpExchange exchange, String data, boolean internalCall, 
                    JSONObject internalResp)
    {
        put(exchange, data, internalCall, internalResp);
    }

    /*public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp){
    }*/

    public UUID createPublisher(String pubName, boolean materialize){
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
				ProcessPublisherResource p = new ProcessPublisherResource(pubUri, thisPubIdUUID, 
                                                                                    materialize);
				RESTServer.addResource(p);
				return thisPubIdUUID;
			} else {
                String msg ="Publisher already registered or publisher name already "+
						"child of " + this.URI + "\nPubName = " + pubName +
						"\n\tChildren: " + children.toString();
				logger.warning(msg);
                lastError = msg;
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "",e); 
            String msg = "Internal Error; check logs";
            lastError = msg;
        } 
        return null;
	}
}
