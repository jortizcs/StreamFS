package local.rest.resources;


import local.analytics.*;
import is4.*;
import local.db.*;
import local.rest.*;
import local.rest.resources.util.*;
import local.metadata.context.*;
import com.mongodb.*;

import net.sf.json.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.StringBuffer;
import java.util.zip.GZIPOutputStream;

import javax.naming.InvalidNameException;
import java.io.*;
import java.net.*;

import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;

/**
 *  Resource object for a device.
 */
public class GenericPublisherResource extends Resource{
	protected static transient Logger logger = Logger.getLogger(GenericPublisherResource.class.getPackage().getName());
	protected static MySqlDriver database = (MySqlDriver)DBAbstractionLayer.database;
	public UUID publisherId =null;
	protected static final int headCount = 5;

	//public static int TYPE = ResourceUtils.GENERIC_PUBLISHER_RSRC;
	protected long last_data_ts = 0;
    protected double start_data_ts = 0;

    private ObjectInputStream routerIn = null;
    private ObjectOutputStream routerOut = null;

    //stream stats
    private double num_points = 0;
    private double inter_arrival_time_sum = 0;
    private double inter_arrival_time_sumsqr = 0;
    private double inter_arrival_time_avg = 0;
    private double inter_arrival_time_var = 0;
    private double inter_arrival_time_min = Double.MAX_VALUE;;
    private double inter_arrival_time_max = 0;
    private double inter_arrival_time_stddev = 0;

    private ConcurrentHashMap<Request, Response> listeners = new ConcurrentHashMap<Request, Response>();

    //last received data value
    private JSONObject lastValuesReceived = new JSONObject();

	public GenericPublisherResource(String uri, UUID pubId) throws Exception, InvalidNameException{
		super(uri);
		if (pubId != null)
			publisherId = pubId;
		else
			throw new Exception("Null pointer to pubId");

		//set type to generic_publisher
		TYPE=ResourceUtils.GENERIC_PUBLISHER_RSRC;
		database.setRRType(URI, ResourceUtils.translateType(TYPE).toLowerCase());
        start_data_ts= mongoDriver.getMinStreamTs(publisherId.toString());
	}

    public UUID getPubId(){
        return publisherId;
    }

	public void get(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){

        Query query = m_request.getQuery();
		if(query.get("query") != null &&
			((String) query.get("query")).equalsIgnoreCase("true")){
			query_(m_request, m_response, null, internalCall, internalResp);
			return;
		}

        if(query.containsKey("incident_paths")){
            super.get(m_request, m_response, path, internalCall, internalResp);
            return;
        }

        if(query.containsKey("listen")){
            boolean error = false;
            try {
                if(sendHeaderOnly(m_response))
                    listeners.put(m_request, m_response);
            } catch(Exception e){
                logger.log(Level.WARNING, "", e);
                error=true;
            } finally{
                try {
                    if(error && m_response!=null)
                        m_response.close();
                } catch(Exception e2){
                }
            }
            return;
        }
		
		logger.info("GET " + this.URI);
		JSONObject response = new JSONObject();
		try {
			JSONObject properties = database.rrGetProperties(this.URI);
			if(properties == null){
				properties = new JSONObject();
			}
			response.put("status", "success");

			UUID assocPubid = database.isRRPublisher2(this.URI);
			if(assocPubid != null){
				response.put("pubid", assocPubid.toString());
                response.put("start_ts",start_data_ts);
				response.put("head", lastValuesReceived.toString());

                if(query.containsKey("stats")){
                    JSONObject statsObj = new JSONObject();
                    statsObj.put("inter_arrival_time_avg", new Double(inter_arrival_time_avg));
                    statsObj.put("inter_arrival_time_var", new Double(inter_arrival_time_var));
                    statsObj.put("inter_arrival_time_min", new Double(inter_arrival_time_min));
                    statsObj.put("inter_arrival_time_max", new Double(inter_arrival_time_max));
                    statsObj.put("inter_arrival_time_stddev", new Double(inter_arrival_time_stddev));
                    statsObj.put("units", "ms");
                    response.put("stats", statsObj);
                }
			}
			response.put("properties", properties);
			sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);
			return;
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
		sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
	}

	public void put(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		post(m_request, m_response, path, data, internalCall, internalResp);
	}

	public void post(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		logger.info("Publisher handling PUT/POST data request");
        Query query = m_request.getQuery();
		if(query.get("query") != null &&
		    ((String) query.get("query")).equalsIgnoreCase("true")){
			query_(m_request, m_response, data, internalCall, internalResp);
		}  else if(query.get("bulk")!=null){
            handleBulkDataPost(m_request, m_response, path, data, internalCall, internalResp);
            return;
        }
        else {
			JSONObject resp = new JSONObject();
			JSONArray errors = new JSONArray();
			try{
				JSONObject dataObject = (JSONObject) JSONSerializer.toJSON(data);
				logger.info("data: " + dataObject.toString());
				String operation = dataObject.optString("operation");
				if(operation!= null && !operation.equals("")){
					if(operation.equalsIgnoreCase("create_symlink")){
						super.put(m_request, m_response, path, data, internalCall, internalResp);
					} else {
						super.handlePropsReq(m_request, m_response, data, internalCall, internalResp);
					}
				} else {
                    UUID pubid = null;
					String addts = (String) query.get("addts");
                    
                    try {
					    pubid = UUID.fromString((String) query.get("pubid"));
                    } catch(Exception b){
                        logger.warning("\"pubid\" was not set");
                        sendResponse(m_request, m_response, 500, null, internalCall, internalResp);
                    }

                    if(pubid!=null)
                        logger.info("pubid: " + pubid.toString());
					
					if(pubid != null &&  !pubid.equals("") && pubid.compareTo(publisherId)==0){

						//store and send success
						if(addts != null && !addts.equals("") && addts.equalsIgnoreCase("false"))
							handleIncomingData(dataObject, false);
						else
							handleIncomingData(dataObject, true);
						resp.put("status", "success");
						sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
					} else {
						resp.put("status", "fail");
						if(pubid == null || pubid.equals(""))
							errors.add("pubid parameter missing");
						if(pubid !=null && pubid.compareTo(publisherId) != 0)
							errors.add("pubid does not match that of this generic publisher");
						resp.put("errors", errors);
						sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
					}
                }
            }catch(Exception e){
                logger.log(Level.WARNING, "", e);
				if(e instanceof JSONException){
					errors.add("Invalid JSON");
				}
				resp.put("status", "fail");
				resp.put("errors", errors);
				sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
			}
		}
	}

	public void delete(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){

		logger.info("Handling DELETE PUBLISHER command for " + this.URI);

		//reset properties
		JSONObject emptyProps = new JSONObject();
		super.updateProperties(emptyProps);

		//remove association with device
		database.removeDeviceEntry(this.URI);

		//delete entry from publishers table
		database.removePublisher(this.publisherId);

		//delete rest_resource entry
		database.removeRestResource(this.URI);
		RESTServer.removeResource(this);
		
		//remove subscriptions to this publisher
		SubMngr submngr = SubMngr.getSubMngrInstance();
		submngr.pubRemoved(m_request, m_response, true, internalResp, publisherId.toString());

		//remove from internal graph
		this.metadataGraph.removeNode(this.URI);

		sendResponse(m_request, m_response, 200, null, internalCall, internalResp);
		
	}

    public void handleBulkDataPost(Request m_request, Response m_response, String path,
                            String data, boolean internalCall, JSONObject internalResp){
        JSONObject resp = new JSONObject();
	    JSONArray errors = new JSONArray();
        try{
            JSONObject dataObject = (JSONObject) JSONSerializer.toJSON(data);
            logger.info("bulk_request: " + dataObject.toString());
            
            try {
                String path2 = dataObject.getString("path");
                UUID pubid  = UUID.fromString(dataObject.getString("pubid"));
                ArrayList<JSONObject> bulk = new ArrayList<JSONObject>();
                ArrayList<Long> timestamps = new ArrayList<Long>();
                String p1 = ResourceUtils.cleanPath(this.URI);
                String p2 = ResourceUtils.cleanPath(path2);
                JSONArray dataArray = dataObject.getJSONArray("data");
                int totalpts = dataArray.size();
                int savedpts = 0;
                int errorpts = 0;
                if(pubid.equals(publisherId) && p1.equals(p2)){
                    for(int i=0; i<totalpts; i++){
                        try {
                            JSONObject ptObj = dataArray.getJSONObject(i);
                            long ts = ptObj.optLong("ts", -1L);
                            if(ts<0)
                                ts = System.currentTimeMillis();
                            Double val = ptObj.getDouble("value");

                            JSONObject dataPt = new JSONObject();
                            dataPt.put("pubid", pubid.toString());
                            dataPt.put("ts", ts);
                            dataPt.put("value", val);
                            timestamps.add(ts);
                            bulk.add(dataPt);

                            //forward data
                            JSONObject dataCopy = (JSONObject)JSONSerializer.toJSON(dataPt);
                            dataCopy.put("timestamp", ts);
                            dataCopy.remove("ts");
                            dataCopy.put("PubId", publisherId.toString());
                            dataCopy.remove("pubid");
                            dataCopy.put("is4_uri", this.URI.toString());
                            SubMngr submngr = SubMngr.getSubMngrInstance();
                            logger.info("SubMngr Copy: " + dataCopy.toString());
                            submngr.dataReceived(dataCopy);

                            //send it to folks tapped into this publisher
                            dataPt.put("is4_uri", this.URI.toString());
                            this.dataReceived(dataPt);

                            //calc stats
                            num_points +=1;
                            if(num_points>1){
                                double diff = ts-(double)last_data_ts;
                                double oldMean = inter_arrival_time_avg;
                                inter_arrival_time_avg = ((oldMean * (num_points-1))+diff)/num_points;
                                inter_arrival_time_sumsqr += (diff - oldMean)*(diff-inter_arrival_time_avg);
                                inter_arrival_time_var = inter_arrival_time_sumsqr/(num_points-1);
                                inter_arrival_time_stddev = Math.sqrt(inter_arrival_time_var);

                                if(diff<inter_arrival_time_min)
                                    inter_arrival_time_min=diff;
                                if(diff>inter_arrival_time_max)
                                    inter_arrival_time_max=diff;
                            }

                            //update local readings
                            dataPt.remove("is4_uri");
                            lastValuesReceived = dataPt;
                            last_data_ts = ts;
                            if(start_data_ts==0)
                                start_data_ts=ts;

                            savedpts+=1;
                            } catch(Exception b){
                            logger.warning("\"pubid\" was not set");
                            errorpts+=1;
                        }
                    }
                    mongoDriver.putTsEntries(bulk);

                    //update the last_data_ts
                    Object[] tsvals = timestamps.toArray();
					Arrays.sort(tsvals);
					last_data_ts = ((Long)tsvals[tsvals.length-1]).longValue();
                    database.updateLastRecvTs(URI, last_data_ts);
					
                    logger.fine("Set last_data_ts=" + last_data_ts);
                    resp.put("total_pts", totalpts);
                    resp.put("pts_saved", savedpts);
                    resp.put("pts_discarded", errorpts);
                }
            } catch(Exception ie){
                logger.log(Level.WARNING, "", ie);
                sendResponse(m_request, m_response, 500, resp.toString(), internalCall, internalResp);
                return;
            }
            sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
        }catch(Exception e){
            logger.log(Level.WARNING, "", e);
            if(e instanceof JSONException){
                errors.add("Invalid JSON");
            }
            resp.put("status", "fail");
            resp.put("errors", errors);
            sendResponse(m_request, m_response, 500, resp.toString(), internalCall, internalResp);
        }
    }

	protected void handleIncomingData(JSONObject data, boolean addTimestamp){

		long timestamp;
		if(addTimestamp || !data.containsKey("ts")){
			//add timestamp
			timestamp = System.currentTimeMillis();
			data.put("ts", timestamp);
			logger.info("adding ts: " + timestamp);
		} else {
			try {
				timestamp = data.getLong("ts");
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
				timestamp = 0L;
			}
		}
		data.put("pubid", publisherId.toString());

		//Forward to subscribers
		String dataStr = data.toString();
		dataStr = dataStr.replace("$","d_");
		JSONObject dataCopy = (JSONObject)JSONSerializer.toJSON(dataStr);
	
		dataCopy.put("timestamp", timestamp);
		dataCopy.put("PubId", publisherId.toString());
		dataCopy.put("is4_uri", this.URI.toString());
		SubMngr submngr = SubMngr.getSubMngrInstance();
		logger.info("SubMngr Copy: " + dataCopy.toString());
		submngr.dataReceived(dataCopy);

        //send it to folks tapped into this publisher
        if(data.containsKey("PubId"))
            data.remove("PubId");
        if(data.containsKey("ts") && data.containsKey("timestamp"))
            data.remove("timestamp");
        this.dataReceived(dataCopy);

        logger.info("Called submngr.dataReceived() with the data copy");
		//get the alias associated with this publisher
		String alias = null;
		if(URI.endsWith(publisherId.toString() + "/") ||
				URI.endsWith(publisherId.toString())){
			alias = publisherId.toString();
		} else {
			String thisuri = URI;
			if(thisuri.endsWith("/"))
				thisuri = thisuri.substring(0, thisuri.length()-1);
			alias = thisuri.substring(thisuri.lastIndexOf("/"), thisuri.length());
		}

        //forward up the olap graph
        JSONObject properties = database.rrGetProperties(this.URI);
        String unitsStr = properties.optString("units");
        if(dataCopy.containsKey("timestamp")){
            Long ts = dataCopy.getLong("timestamp");
            dataCopy.remove("timestamp");
            dataCopy.put("ts", ts);
        }
        
        if(dataCopy.containsKey("value")){
            Double v = dataCopy.getDouble("value");
            dataCopy.remove("value");
            dataCopy.put("v", v);
        } else if(dataCopy.containsKey("val")){
            Double v = dataCopy.getDouble("val");
            dataCopy.remove("val");
            dataCopy.put("v", v);
        }

        if(RESTServer.tellRouter && !unitsStr.equals(""))
           metadataGraph.streamPush(URI, unitsStr, dataCopy.toString()); 

		logger.info("Publsher PUTTING in data repository");

		//put the data entry in the database
		//database.putInDataRepository(data, publisherId, alias);
		database.updateLastRecvTs(URI, timestamp);
       
        //store in the mongodb repos
		//MongoDBDriver mongod = new MongoDBDriver();
		//mongod.putEntry(dataCopy);
		mongoDriver.putTsEntry(data);

        //calc stats
        num_points +=1;
        if(num_points>1){
            double diff = timestamp-(double)last_data_ts;
            double oldMean = inter_arrival_time_avg;
            inter_arrival_time_avg = ((oldMean * (num_points-1))+diff)/num_points;
            inter_arrival_time_sumsqr += (diff - oldMean)*(diff-inter_arrival_time_avg);
            inter_arrival_time_var = inter_arrival_time_sumsqr/(num_points-1);
            inter_arrival_time_stddev = Math.sqrt(inter_arrival_time_var);

            if(diff<inter_arrival_time_min)
                inter_arrival_time_min=diff;
            if(diff>inter_arrival_time_max)
                inter_arrival_time_max=diff;

            logger.info("Timestamp=" + timestamp + ", last_data_ts=" + last_data_ts + ", diff=" + diff + ",sum=" + inter_arrival_time_sum +
                            ", avg=" + inter_arrival_time_avg + ", sumsqr=" + inter_arrival_time_sumsqr + ", var=" + inter_arrival_time_sum);
            
        }

		//update local readings
        lastValuesReceived = data;
		last_data_ts = timestamp;
        if(start_data_ts==0)
            start_data_ts=timestamp;

        
	}

    public void setRouterCommInfo(String routerHost, int routerPort){
        try {
            Socket s = new Socket(InetAddress.getByName(routerHost), routerPort);
            routerOut = new ObjectOutputStream(s.getOutputStream());
            routerOut.flush();
            routerIn = new ObjectInputStream(s.getInputStream());
        } catch(Exception e) {
            logger.log(Level.SEVERE, "", e);
            System.exit(1);
        }
    }

	public JSONObject queryTimeseriesRepos(JSONObject queryJson){
		JSONObject queryResults = new JSONObject();
		try{
			//only run the query for this publisher
			queryJson.put("PubId", publisherId.toString());
			
			//remove the PubId key from the results
			JSONObject keys = new JSONObject();
			keys.put("PubId",0);

			logger.info("QUERY: " + queryJson.toString() + "\nKEYS:  " + keys.toString());

			JSONObject queryR = mongoDriver.query(queryJson.toString(), keys.toString());
			if(queryR != null)
				queryResults.putAll(queryR);
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return queryResults;
	}

	public JSONArray queryTimeseriesRepos2(JSONObject queryJson){
		JSONArray queryResults = new JSONArray();
		try{

			//only run the query for this publisher
			queryJson.put("pubid", publisherId.toString());

			//remove the PubId key from the results
			JSONObject keys = new JSONObject();
			keys.put("pubid",0);

			logger.info("QUERY: " + queryJson.toString() + "\nKEYS:  " + keys.toString());

			return mongoDriver.queryTsColl(queryJson.toString(), keys.toString());
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return queryResults;
	}

	public void query_(Request m_request, Response m_response, String data, boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
		resp.put("path", URI);
        Query query = m_request.getQuery();
		try{
			//JSONObject tsQueryObj = new JSONObject();
			JSONObject tsQueryObj2 = new JSONObject();
		
			//get query object from input data
			if(data != null && !data.equals("")){	
				JSONObject dataJsonObj = (JSONObject) JSONSerializer.toJSON(data);
				JSONObject dataTsQuery = dataJsonObj.optJSONObject("ts_query");
				//tsQueryObj.putAll(dataTsQuery);
				tsQueryObj2.putAll(dataTsQuery);
			}

            logger.fine("query::" + query.toString());
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
						//tsQueryObj.put(queryKey, conditions);
						if(queryKey.equalsIgnoreCase("timestamp"))
							tsQueryObj2.put("ts", conditions);
					} else{
						if(isNumber(queryValue)){
							long val = Long.parseLong(queryValue);
							//tsQueryObj.put(queryKey, val);
							if(queryKey.equalsIgnoreCase("timestamp"))
								tsQueryObj2.put("ts", val);
						} else {
							//tsQueryObj.put(queryKey, queryValue);
							if(queryKey.equalsIgnoreCase("timestamp"))
								tsQueryObj2.put("ts", queryValue);
						}
					}

				} else if(thisKey.startsWith("ts")){
					String queryValue = (String)query.get(thisKey);

					JSONObject conditions = Resource.genJSONClause(queryValue);
					if(conditions!=null){
						//tsQueryObj.putAll(conditions);
						tsQueryObj2.putAll(conditions);
					} else{
						if(isNumber(queryValue)){
							long val = Long.parseLong(queryValue);
							//tsQueryObj.put(thisKey, val);
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

			//logger.fine("Timeseries Query: " + tsQueryObj.toString());
			logger.fine("Timeseries Query2: " + tsQueryObj2.toString());

			if(!tsQueryObj2.toString().equals("{}")){
				//tsQueryObj.put("is4_uri", URI);
				/*if(last_props_ts>0)
					tsQueryObj.put("timestamp", last_props_ts);*/

				//JSONObject mqResp = queryTimeseriesRepos(tsQueryObj);
				JSONArray mqResp2 = queryTimeseriesRepos2(tsQueryObj2);
				//logger.fine("mqResp: " + mqResp.toString());
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
		JSONObject propsQueryResultsBuffer = new JSONObject();
		super.query_(m_request, m_response, data, true, propsQueryResultsBuffer);
		resp.put("props_query_results", propsQueryResultsBuffer);
		sendResponse(m_request, m_response, 200, resp.toString(), internalCall, internalResp);
	}

    public boolean sendHeaderOnly(Response m_response){
        try {
            long time = System.currentTimeMillis();
            
            m_response.set("Content-Type", "application/json");
            m_response.set("Server", "StreamFS/2.0 (Simple 4.0)");
            m_response.set("Connection", "close");
            m_response.setDate("Date", time);
            m_response.setDate("Last-Modified", time);
            m_response.setCode(200);
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            return false;
        }
        return true;
    }

    private void dataReceived(JSONObject data){
        if(data==null)
            return;
        if(data.containsKey("PubId"))
            data.remove("PubId");
        Iterator<Request> keys = listeners.keySet().iterator();
        boolean error = false;
        Request m_request = null;
        Response m_response = null;
        while(keys.hasNext()){
            m_request = keys.next();
            m_response = listeners.get(m_request);
            if(m_response!=null){
                error = false;
                
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
                        gzipos.close();
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
        }
    }

}
