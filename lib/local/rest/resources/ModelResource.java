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
 * StreamFS release version 2.0
 */
package local.rest.resources;

import local.db.*;
import local.rest.*;
import local.rest.resources.*;
import local.rest.resources.util.*;
import local.rest.interfaces.*;
import local.json.javascript.Js2JSONUtils;
import is4.*;

import net.sf.json.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;
import java.lang.reflect.Array;

import javax.naming.InvalidNameException;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import org.mozilla.javascript.*;

import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;

public class ModelResource extends Resource implements Runnable {
	
	//more complex functions can be referenced using LiveConnect
	//i.e.  var jsonarray = net.sf.json.JSONArray();
	//		var jsonlib = net.sf.json;
	//		var jsonarray_class = jsonlib.JSONArray;
	//		var jarray = jsonarray_class();
	
	//default processing parameters
	private static final int DEFAULT_WINSIZE = 1;
	private static final boolean DEFAULT_MAKEVIEW = false;
	private static final int DEFAULT_TIMEOUT = 0;
	
	//script tags
	private static final String TAG_PROCEDURE = "proc";
	private static final String TAG_WINSIZE = "winsize";
	private static final String TAG_TIMEOUT = "timeout"; //in ms
	private static final String TAG_VIEW = "makeview";
	
	//script
	private JSONObject scriptJSON = null;
	private String rawScript = null;
	
	//last update timestamp
	protected long last_model_ts=0;
	
	//Boolean if this is the root Instance
	private boolean isRootInstance = false;
	
	//Pipe
	protected Pipe.SourceChannel sourceChannel = null;
	//private PipeInputStream source = null;
	Semaphore sem = null;
	
	//Thread object reference
	private Thread myThread = null;
	
	//create a view of the model output (publisher)
	boolean materialize = false;
	
	public ModelResource(String path, String script, boolean createview) 
					throws Exception, InvalidNameException, InstantiationException {
		
		//catch a json formatting error
		try{
			scriptJSON = (JSONObject) JSONSerializer.toJSON(script);
			rawScript = script;
			logger.info("ModelPath: " + path + "Rawscript:" + rawScript);
		}catch(JSONException e){
			throw new InstantiationException("JSON formatting error in script object");
		}
	
		//check the script
		if(!isValidScript(script)){
			logger.warning("Javascript error");
			throw new InstantiationException("Javascript error");
		}
		
		//set up the resource
		super.resourceSetup(path);
						
		//set last_model_ts
		last_model_ts = database.getLastModelTs(URI);
		long modelHistCount = mongoDriver.getModelHistCount(URI);
		logger.info("ModelPath:" + URI + ", timestamp:"+ last_model_ts + ", numPrevEntries:" + modelHistCount);
		if(last_model_ts==0 && modelHistCount>0 && scriptJSON == null){
			logger.info("Fetching oldest model values");
			last_model_ts = mongoDriver.getMaxTsModels(URI);
			JSONObject modelEntry = mongoDriver.getModelEntry(URI, last_model_ts);
			if(!modelEntry.toString().equals("{}")){
				modelEntry.remove("_id");
				modelEntry.remove("timestamp");
				modelEntry.remove("is4_uri");
				modelEntry.remove("createview");
				//database.rrPutProperties(URI, modelEntry);
				database.rrPutProperties(URI, rawScript);
				database.updateLastModelTs(URI, last_model_ts);
				scriptJSON.accumulateAll(modelEntry.getJSONObject("script"));
			} else {
				logger.warning("ModelPath:" + URI + "; WARNING model entry empty {}");
			}
		} else if(last_model_ts==0 && modelHistCount>0 && scriptJSON !=null){
			logger.info("Fetching oldest model values 2");
			last_model_ts = mongoDriver.getMaxTsModels(URI);
			JSONObject modelEntry = mongoDriver.getModelEntry(URI, last_model_ts);
			if(!modelEntry.toString().equals("{}")){
				logger.info("populating model with stored values");
				modelEntry.remove("_id");
				modelEntry.remove("timestamp");
				modelEntry.remove("is4_uri");
				modelEntry.remove("createview");
				//database.rrPutProperties(URI, modelEntry);
				database.updateLastModelTs(URI, last_model_ts);
				database.rrPutProperties(URI, rawScript);
				scriptJSON.accumulateAll(modelEntry.getJSONObject("script"));
			} else {
				logger.warning("ModelPath:" + URI + "; WARNING model entry empty {}");
			}
		} else if (last_model_ts ==0 && modelHistCount==0 && scriptJSON==null){
			throw new InstantiationException("Associated script missing");
		}
		
		//save the script if this is a new model
		boolean valid = isValidScript(scriptJSON.toString());
		logger.info("Saving new script: \n\tscriptJSON:" + scriptJSON + "\n\tvalid=" + valid + "\n\tmodeHistcount=" + modelHistCount);
		if(scriptJSON != null &&  valid && last_model_ts==0 && modelHistCount==0){
			JSONObject scriptRecord = new JSONObject();
			Date date = new Date();
			long timestamp = date.getTime()/1000;
			scriptRecord.put("is4_uri", URI);
			scriptRecord.put("createview", createview);
			scriptRecord.put("timestamp", timestamp);
			//scriptRecord.put("script", scriptJSON);
			scriptRecord.put("script", rawScript);
			database.rrPutProperties(URI, rawScript);
			mongoDriver.putModelEntry(scriptRecord);
			
			last_model_ts = timestamp;
			database.updateLastModelTs(URI, last_model_ts);

			logger.info("Updated records for this model");
		} else if (scriptJSON == null || !valid){
			throw new InstantiationException("Invalid script");
		}
		
		//set type
		TYPE = ResourceUtils.MODEL_RSRC;
		database.setRRType(URI, ResourceUtils.translateType(TYPE).toLowerCase());
		
		//set as root instance
		this.isRootInstance = true;
		
	}
	
	private ModelResource(Semaphore s, Pipe.SourceChannel t, ModelResource rootInstance) throws InstantiationException {
		if(t == null)
			throw new InstantiationException("Source channel is NULL");
		
		if(rootInstance == null)
			throw new InstantiationException("Invalid root Insance");

		try {	
			TYPE = ResourceUtils.MODEL_INSTANCE_RSRC;
			this.scriptJSON = rootInstance.scriptJSON;
			this.rawScript = rootInstance.rawScript;
			this.sourceChannel = t;
			//this.souce = t;
			this.isRootInstance = false;
			this.sem = s;

			t.configureBlocking(true);
		} catch(Exception e){
			logger.log(Level.WARNING, "Could not set sourceChannel to blocking", e);
		}
	}
		
	public static Thread startNewModelThread(Semaphore s, Pipe.SourceChannel source, ModelResource rootInstance)
		throws InstantiationException {
		ModelResource mrInstance = new ModelResource(s, source, rootInstance);
		
		//create associate publisher and set the thread name to the publisher id
		UUID pubid = createModPublisher(generatePubRsrcName(UUID.randomUUID(), rootInstance), rootInstance);
		
		//create and start the thread
		if(pubid != null){
			Thread thisThread = new Thread(mrInstance, pubid.toString());
			mrInstance.myThread =thisThread;
			try {
				thisThread.start();
				return thisThread;
			} catch(Exception e){
				throw new InstantiationException("Illegal thread state on thread start; Publisher ID=" + pubid.toString());
			}
		}
		return null;
	}
	
	public static Thread restartModelThread(Semaphore s, Pipe.SourceChannel source, ModelResource rootInstance, UUID pubid)
		throws InstantiationException{

		if(pubid != null){
			try {
				//associate publisher and set the thread name to the publisher id
				String pubUri = database.getIs4RRPath(pubid);
				ModelGenericPublisherResource p = new ModelGenericPublisherResource(pubUri, pubid, rootInstance, rootInstance.materialize);
				logger.info("Model publisher uri: " + pubUri);
				RESTServer.addResource(p);

				//restart the thread and post to associated pubid
				ModelResource mrInstance = new ModelResource(s, source, rootInstance);
				//create and start the thread
				Thread thisThread = new Thread(mrInstance, pubid.toString());
				mrInstance.myThread =thisThread;
				thisThread.start();
				return thisThread;
			} catch(Exception e){
				throw new InstantiationException("Illegal thread state on thread start; Publisher ID=" + pubid.toString());
			}
		} else {
			logger.warning("Publiser id is NULL!");
			throw new InstantiationException("PubId null");
		}
		//return null;
	}
	
	public void get(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		try{
			logger.fine("GETTING RESOURCES: " + URI);
			response.put("status", "success");
			//response.put("properties", database.rrGetProperties(URI));
			response.put("children", database.rrGetChildren(URI));
			response.put("script", database.rrGetProperties(URI));
			//response.put("script", scriptJSON);
			sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
		} catch(Exception e){
			String msg = e.getMessage();
			errors.add(msg);
			response.put("status", "fail");
			response.put("errors", errors);
			sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
		}
	}
	
	public void put(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		post(m_request, m_response, path, data, internalCall, internalResp);
	}
	
	public void post(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		/*JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		try{
			
		} catch(Exception e){
			
		}*/
		sendResponse(m_request, m_response, 400, null, internalCall, internalResp);
	}
	
	public void delete(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
		//delete all incoming/outgoing pipes
		//kill all thread instances
		super.delete(m_request, m_response, path, internalCall, internalResp);
	}
	
	public void run(){
		logger.info("Starting Thread-" + myThread.getName());
		Context cx = Context.enter();
		try{
			Scriptable scope = cx.initStandardObjects();
			
			//get the processing script
			String pScriptStr = rawScript;
			String pScriptObjStr = "var pscript="+pScriptStr+"; pscript";
			Scriptable pScript = (Scriptable) cx.evaluateString(scope,pScriptObjStr, "<"+ myThread.getName() +">", 1, null);
			
			int idx =0;
			int bufferSize = (new Double(pScript.get("winsize", pScript).toString())).intValue();
			JSONArray dataBuffer = new JSONArray();
		
			//int loopCount=0;
			boolean run = true;
			while(run && sourceChannel.isOpen()){
				//wait for the subscription manager to signal the arrival of new data
				logger.finer("MODEL:BEFORE_LEASE_COUNT=" + sem.availablePermits());
				sem.acquire();	
				logger.finer("MODEL:AFTER_LEASE_COUNT=" + sem.availablePermits());
				/*loopCount+=1;
				if(loopCount>3)
					System.exit(1);*/
				logger.info("1.Running Thread-"+ myThread.getName());
				//System.out.println("here1:  " + source.isOpen());
				ByteBuffer sizebuf = ByteBuffer.allocate(4);
				int r=sourceChannel.read(sizebuf);
				sizebuf.rewind();
				logger.fine("2.ReadDone: Thread-" + myThread.getName() + " read "+ r + " bytes");
				//System.out.println("here2");
				
				if(r>0){
					int size = sizebuf.getInt();
					sizebuf.clear();
					if(size<65536){
						logger.fine("3.Thread-" + myThread.getName() + ": Transmit size: "+size);
						ByteBuffer rdata = ByteBuffer.allocate(size);
						logger.fine("4.Thread-"+myThread.getName()+":reading data");
						r=sourceChannel.read(rdata);
						rdata.rewind();
						logger.fine("5.Thread-"+myThread.getName()+":done");
				
						if(r>0){
							String dataStr = new String(rdata.array()).trim();
							logger.fine("6.Thread-" + myThread.getName() + "; Read: " + dataStr);
							rdata.clear();
				
							JSONObject dataJsonObj = (JSONObject) JSONSerializer.toJSON(dataStr);
							String op = dataJsonObj.optString("operation");
							if(op.equals("")){
								dataBuffer.add(dataJsonObj);
								idx+=1;
								//System.out.println("idx:"+idx+"\tbufferSize:"+bufferSize);
								if(idx==bufferSize){
									this.processIt(cx, scope, pScriptStr, dataBuffer);
									dataBuffer.clear();
									idx=0;
								}
							} else if(op.equalsIgnoreCase("kill")){
								logger.info("Killing self: thread-" + myThread.getName());
								run=false;
								sourceChannel.close();
							}
						} else {
							logger.warning("7.Thread-" + myThread.getName() + ": pipe closure detected; killing thread (1)");
							sourceChannel.close();
						}
					}
				} else {
					logger.warning("8.Thread-" + myThread.getName() + ": pipe closure detected; killing thread (2)");
					sourceChannel.close();
				}
			}
			logger.fine("Self,Thread-" + myThread.getName() + " dead");

		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		} finally{
			Context.exit();
		}
	}
	
	public void processIt(Context cx, Scriptable scope, String process, JSONArray buffer){
		String e = "var buf = "+buffer.toString()+"; var p="+process+"; p.func(buf)";
		e = e.trim();
		logger.fine("processIt:  " + e);
		
		Js2JSONUtils utils  = new Js2JSONUtils();

		try {
			//System.out.println("var buf = "+buffer.toString()+";\nvar p="+process+";\n\np.select(buf)");
			Scriptable procItScript = (Scriptable) cx.evaluateString(scope,e, "<procitscript>", 1, null);
			String outputStr = utils.toJSONString(procItScript);
			logger.info("processing_output:" + outputStr);
			JSONObject pData = (JSONObject)JSONSerializer.toJSON(outputStr);
			/*Object[] ids = procItScript.getIds();
			JSONObject pData = new JSONObject();
			for(int k=0; k<ids.length; k++){
				String thisKey = ids[k].toString();
				Object thisValue = procItScript.get(thisKey, procItScript);
				if(thisValue instanceof org.mozilla.javascript.NativeArray){
					NativeArray nativeArray = (NativeArray)thisValue;
					//logger.info("Blah: " + utils.toJSONString(thisValue));

					JSONArray array = new JSONArray();
					pData.put(thisKey, array);
				} else {
					pData.put(thisKey, thisValue.toString());
				}
			}*/
			logger.info("Thread-"+ myThread.getName() + " processed data: " + outputStr);
			
			UUID myPubid = UUID.fromString(myThread.getName());
			String pubPath = database.getIs4RRPath(myPubid);
			Resource p = null;
			if(pubPath !=null && (p=RESTServer.getResource(pubPath))!=null && p instanceof ModelGenericPublisherResource){
				ModelGenericPublisherResource r = (ModelGenericPublisherResource)p;
				r.saveData(pData);
			} else {
				String s = "No available publisher for thread-" + myThread.getName() + "\n\tpubPath=" + pubPath + "\n\tp=" + p +
					"\n\tinstanceof ModelGenericPub? " + (p instanceof ModelGenericPublisherResource) + "\n\ttype=" + p.TYPE;
				logger.info(s);
			}
		} catch(Exception ex){
			logger.log(Level.WARNING, "", ex);
		}
	}
	
	//check script validity
	private boolean isValidScript(String script){
		boolean valid = true;
		Context cx = Context.enter();
		try{
			JSONObject scriptJObj = (JSONObject) JSONSerializer.toJSON(script);
			Scriptable scope = cx.initStandardObjects();
			String e = "var script = "+scriptJObj.toString()+"; script;";
			Scriptable procItScript = (Scriptable) cx.evaluateString(scope,e, "<script>", 1, null);
		} catch(Exception e){
			valid = false;
		} finally{
			cx.exit();
		}
		
		return valid;
	}
	
	private static String generatePubRsrcName(UUID pubid, ModelResource parent){

		int max_retries = 10;
		int retries=0;
		//choose 7 random characters in the UUID string
		String name="";
		String pubidStr = pubid.toString();
		Random random = new Random();
		do {
			for(int i=0; i<7; i++){
				char thisChar = pubidStr.charAt(Math.abs(random.nextInt()%pubidStr.length()));

				logger.finer("i=" + i + ", name.length()=" + name.length());

				//thisChar cannot be a '-' if it's the first or last character in the name
				while((i==0 || i==6) && thisChar == '-'){
					logger.finer("here1: i=" + i + "thisChar= " + thisChar);
					thisChar = pubidStr.charAt(Math.abs(random.nextInt()%pubidStr.length()));
				}

				//thisChar cannot be a '-' if the previous character was a '-'
				while(i != 0 && i != 6 && name.charAt(i-1) == '-' && thisChar == '-'){
					logger.finer("here2: i=" + i + "thisChar= " + thisChar);
					thisChar = pubidStr.charAt(Math.abs(random.nextInt()%pubidStr.length()));
				}

				name += thisChar;
			}
			retries +=1;
		} while (!ResourceUtils.devNameIsUnique(parent.getURI(), name) && retries<max_retries); 
		
		return name;
	}	
	
	private static UUID createModPublisher(String pubName, ModelResource parent){
		try {
			UUID thisPubIdUUID = null;
			String pubUri = parent.getURI() + pubName + "/";
			if((thisPubIdUUID=database.isPublisher(pubUri, false)) == null){

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
				ModelGenericPublisherResource p = new ModelGenericPublisherResource(pubUri, thisPubIdUUID, parent, parent.materialize);
				logger.info("Model publisher uri: " + pubUri);
				RESTServer.addResource(p);
				return thisPubIdUUID;
			} else {
				logger.warning("Publisher already registered or publisher name already "+
						"child of " + parent.URI + "\nPubName = " + pubName);
			}

		} catch (Exception e){
			logger.log(Level.WARNING, "",e);
		}
		return null;
	}
	
}


