package local.analytics.lib.db;

import net.sf.json.*;

import com.mongodb.*;

import java.util.*;
import java.util.logging.*;
import java.io.*;

public class OlapMongoDBDriver {

	private static transient final Logger logger = Logger.getLogger(OlapMongoDBDriver.class.getPackage().getName());

	public static String serverAddress = "localhost";
	public static int port = 27017;
	public static String login = "";
	public static String password = "";

	private static int openConns =0;
    private static String[] indices = {"path", "v", "ts"};

	//database
	private static String olapRepositoryStr = "sfs_olap_repos";

	//Compressed timeseries collection
	private static String olapTsCollectionStr = "sfs_olapts_coll";
	
	//Configuration file
	private String dbConfigFile = "olap_archive_info.json";

	private static Mongo m = null;
	private static DB olapRepos = null; 
	private static DBCollection olapTsCollection = null;

	protected static boolean inited = false;

    public static void main (String[] args){
        OlapMongoDBDriver olapDBDriver = new OlapMongoDBDriver();
        try {
            JSONObject datapt = new JSONObject();
            datapt.put("ts",1326160611);
            datapt.put("val",123);
            datapt.put("path", "/123");
            olapDBDriver.putOlapTsEntry(datapt);

            JSONObject queryobj = new JSONObject();
            queryobj.put("path", "/123");
            JSONObject condobj = new JSONObject();
            condobj.put("$gte", 1326160610);
            condobj.put("$lt", 1326160999);
            queryobj.put("ts", condobj);
            logger.info(queryobj.toString());

            JSONObject res = olapDBDriver.query(queryobj.toString());
            logger.info("res::" + res.toString());
            //System.out.println(olapDBDriver.getLastResult("/aggroot/"));
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
    }

	public OlapMongoDBDriver(){
		if(!inited){
			setupGlobals();
		}

		try {
			if(m == null && port==27017 && login.equals("") && password.equals("")){
				MongoOptions options = new MongoOptions();
				options.connectionsPerHost=1000;
				m = new Mongo(serverAddress, options);
				olapRepos = m.getDB(olapRepositoryStr);
				olapTsCollection  = olapRepos.getCollection(olapTsCollectionStr);
				logger.info("(1) New Mongo instance created: server= "+serverAddress + "; port= " + port);
			}
			//todo: add more setup code
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}

		if(!inited){
			//setup indices
			BasicDBObject indicesObj = new BasicDBObject();
            for(int i=0; i<indices.length; i++)
                indicesObj.append(indices[i], new Integer(1));
			olapTsCollection.ensureIndex(indicesObj);

			inited = true;
		}
	}

	private void setupGlobals(){
		try{
			String home=null;
			if((home=System.getenv().get("IS4HOME")) != null)
				dbConfigFile = home + "/lib/local/analytics/lib/db/olap_archive_info.json";
			File configFile = new File(dbConfigFile);
			FileReader cFileReader = new FileReader(configFile);
			BufferedReader bufReader = new BufferedReader(cFileReader);
		
			StringBuffer strBuf = new StringBuffer();
			String line = null;
			while((line=bufReader.readLine())!=null)
				strBuf.append(line).append(" ");
            JSONObject configJsonObj = (JSONObject) JSONSerializer.toJSON(strBuf.toString());
			cFileReader.close();
			bufReader.close();
			serverAddress = configJsonObj.getString("address");
			if(configJsonObj.optInt("port") != 0)
				port = configJsonObj.optInt("port");
			login = configJsonObj.optString("login");
			password = configJsonObj.optString("password");

			if(m == null ){
				MongoOptions options = new MongoOptions();
				options.connectionsPerHost=1000;
				ServerAddress serverAddr = new ServerAddress(serverAddress, port);
				m = new Mongo(serverAddr,options);
                olapRepos = m.getDB(olapRepositoryStr);
				olapTsCollection  = olapRepos.getCollection(olapTsCollectionStr);
				logger.info("(2) New Mongo instance created: server= "+serverAddress + "; port= " + port);
			}


		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public synchronized Mongo openConn(){
		try {
			if(m == null)
				m = new Mongo(serverAddress);
			
			openConns +=1;
			logger.info("Mongo Open: conn_count=" + openConns);
			//}
			return m;
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return null;
	}

	public synchronized void closeConn(Mongo m_input){
		//if(m != m_input && m_input != null){
		//m_input.close();
		//if(m == m_input)
		//	m = null;
		openConns -=1;
		logger.info("Mongo Close: conn_count=" + openConns);
		//}
	}

	
	public synchronized JSONArray getIndexInfo(String collectionName){
		JSONArray indexesArray = new JSONArray();
		try {
			if(m != null){
				DB database = m.getDB(olapRepositoryStr);
				DBCollection thisCollection = database.getCollection(collectionName);
				logger.info("Getting index information from " + olapRepositoryStr + " for collection " + collectionName);
				if(thisCollection!=null){
					List<DBObject> indexes = thisCollection.getIndexInfo();
					for(int i=0; i<indexes.size(); i++){
						DBObject thisIndex = indexes.get(i);
						JSONObject thisIndexJObj = new JSONObject();
                        Set<String> keySet = thisIndex.keySet();
                        Iterator<String> keySetIt = keySet.iterator();
                        while(keySetIt.hasNext()){
                            String thisKey = keySetIt.next();
						    thisIndexJObj.put(thisKey,thisIndex.get(thisKey));
                        }

						indexesArray.add(thisIndexJObj);
					}
				}
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return indexesArray;
	}

	public synchronized void setIndexes(String collectionName, JSONObject indexes){
		try {
			if(m != null){
				DB database = m.getDB(olapRepositoryStr);
				DBCollection thisCollection = database.getCollection(collectionName);
				logger.info("Setting index(es) " + indexes.toString() + " from " + collectionName);
				thisCollection.ensureIndex(new BasicDBObject((Map)indexes));
				logger.info("Done setting index(es)");
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public synchronized void removeIndex(String collectionName, String indexName){
		try {
			if(m != null){
				DB database = m.getDB(olapRepositoryStr);
				DBCollection thisCollection = database.getCollection(collectionName);
				logger.info("Dropping index " + indexName + " from " + collectionName);
				thisCollection.dropIndex(indexName);
				logger.info("Done dropping index(es)");
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

    /*private BasicDBObject toBasicDBObject(org.json.JSONObject jsonobj){
        BasicDBObject dbobj = new BasicDBObject();
        try {
            if(jsonobj!=null){
                String[] names= JSONObject.getNames(jsonobj);
                for(int i=0; i<names.length; i++){
                    Object obj = jsonobj.get(names[i]);
                    if(obj instanceof JSONObject)
                        dbobj.put(names[i], ((JSONObject)obj).toString());
                    else
                        dbobj.put(names[i], obj);
                }
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
        return dbobj;
    }*/

	/************************************/
	/**  Access/query data collection  **/
	/************************************/

	public void putOlapTsEntry(JSONObject entry){
		WriteResult result = null;
		try {
			JSONObject strippedEntry = stripEntry(entry);
			if(!strippedEntry.toString().equals("{}")){
                BasicDBObject dataObj = new BasicDBObject((Map)strippedEntry);
				if(m != null){
					olapRepos.requestStart();
					result = olapTsCollection.save(dataObj);
					olapRepos.requestDone();
					dataObj = null;
					logger.info("Inserted mongo entry in Ts data collection: " + strippedEntry.toString());
				} else {
					logger.warning("Mongo connection came back NULL");
				}
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "Exception thrown while inserting entry into Mongo",e);
			if(e instanceof MongoException && result!=null)
				logger.info("Error? " + result.getError());
		}
	}

	public void putOlapTsEntries(ArrayList<JSONObject> entries){
		ArrayList<DBObject> bulkEntry=new ArrayList<DBObject>();
		WriteResult result = null;
		int bulkBytes = 0;
		try {
			for(int i=0; i<entries.size(); ++i){
				JSONObject entry = entries.get(i);
				logger.info("ThisEntry: " + entry.toString());
				JSONObject strippedEntry = stripEntry(entry);
				if(!strippedEntry.toString().equals("{}")){
					BasicDBObject dataObj =  new BasicDBObject((Map)strippedEntry);
					bulkEntry.add(dataObj);
					bulkBytes += strippedEntry.toString().getBytes().length;
				}
			}
			if(m != null){
				olapRepos.requestStart();
				result = olapTsCollection.insert(bulkEntry);
				olapRepos.requestDone();
				logger.info("Inserted bulk Ts entry in Ts data collection: " + bulkBytes + " bytes");
			} else {
				logger.warning("Mongo connection came back NULL");
			}
			
		} catch (Exception e){
			logger.log(Level.WARNING, "Exception thrown while inserting entry into Mongo",e);
			if(e instanceof MongoException && result!=null)
				logger.info("Error? " + result.getError());
		}
	}

	private JSONObject stripEntry(JSONObject entry){
		JSONObject s = new JSONObject();
        try {
            if(entry.has("Reading")){
                s.put("v", entry.get("Reading"));
            } else if(entry.has("value") && !Double.isNaN(entry.optDouble("value"))){
                s.put("v",entry.optDouble("value"));
            } else if(entry.has("val") && !Double.isNaN(entry.optDouble("val"))){
                s.put("v", entry.optDouble("val"));
            } else {
                s.put("v", entry.optDouble("v"));
            }

            if(entry.has("ReadingTime")){
                s.put("ReadingTime", entry.optInt("ReadingTime"));
            } else if(entry.has("timestamp")){
                s.put("ts", entry.optInt("timestamp"));
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }

		try{
			//s.put("oid", entry.getString("oid"));
            s.put("path", entry.getString("path"));
			s.put("ts", entry.getLong("ts"));
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return s;
	}

	public JSONObject query(String query){
		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		boolean olapReposOpen=false;
		boolean dbCursorOpen = false;
		DBCursor dbCursor = null;
		try {
			
			if(query != null && m!= null){
                JSONObject queryObj = (JSONObject) JSONSerializer.toJSON(query);
				BasicDBObject dataObj = new BasicDBObject((Map)queryObj);
				olapRepos.requestStart();
				olapReposOpen=true;
				dbCursor = olapTsCollection.find(dataObj);
				dbCursorOpen=true;
				results = new JSONArray();
				while(dbCursor.hasNext()){
                    JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next().toString());
					thisJSONObj.remove("_id");
					results.add(thisJSONObj);
				}
				dataObj = null;
				queryResults.put("results", results);
				return queryResults;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(olapReposOpen)
				olapRepos.requestDone();
		}
		return null;
	}

	public JSONArray queryTsColl(String query, String keys){
		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		boolean dbCursorOpen=false;
		boolean olapReposOpen=false;
		DBCursor dbCursor=null;
		try {
			if(query != null && keys!=null && m!= null){
                JSONObject queryObj = (JSONObject)JSONSerializer.toJSON(query);
                JSONObject keysObj= (JSONObject)JSONSerializer.toJSON(keys);
				BasicDBObject queryDBObj  = new BasicDBObject((Map)queryObj);
				BasicDBObject keysDBObj = new BasicDBObject((Map)keysObj);
			
				keysDBObj.put("_id", new Integer(0));	
				olapRepos.requestStart();
				olapReposOpen=true;
				BasicDBObject sortByObj = new BasicDBObject("ts", "1");
				dbCursor = olapTsCollection.find(queryDBObj,keysDBObj).sort(sortByObj);
				dbCursorOpen=true;
				results = new JSONArray();
				while(dbCursor.hasNext()){
                    JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next().toString());
					results.add(thisJSONObj);
				}
				return results;
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			closeConn(m);
		} finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(olapReposOpen)
				olapRepos.requestDone();
		}
		
		return null;
	}

	public synchronized JSONObject queryWithLimit(String queryString, String orderBy, int limit_){

		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		boolean dbCursorOpen=false;
		boolean olapReposOpen=false;
		DBCursor dbCursor =null;
		try {
			if(queryString != null && m!= null){
                JSONObject queryObj = (JSONObject) JSONSerializer.toJSON(queryString);
				BasicDBObject queryDBObj = new BasicDBObject((Map)queryObj);

				logger.info("MongoQuery: " + queryString);
				long startTime = System.currentTimeMillis();
				olapRepos.requestStart();
				olapReposOpen=true;
				dbCursor = olapTsCollection.find(queryDBObj);
				dbCursorOpen=true;
				long endTime = System.currentTimeMillis();
        			float seconds = (endTime - startTime) / 1000F;
				logger.info("Data fetch time: " + seconds + " sec");
				
				startTime = System.currentTimeMillis();
				dbCursor = dbCursor.limit(limit_);
				endTime = System.currentTimeMillis();
        			seconds = (endTime - startTime) / 1000F;
				logger.info("Set limit time: " + seconds + " sec");

				startTime = System.currentTimeMillis();
				if(dbCursor != null && orderBy != null){
                    JSONObject orderByJSON = (JSONObject) JSONSerializer.toJSON(orderBy);
					BasicDBObject orderByObj  = new BasicDBObject((Map)orderByJSON);
					dbCursor = dbCursor.sort(orderByObj);
				}
				endTime = System.currentTimeMillis();
        		seconds = (endTime - startTime) / 1000F;
				logger.info("Order-by time: " + seconds + " sec");

				startTime = System.currentTimeMillis();
				long hasNextStartTime = System.currentTimeMillis();
				results = new JSONArray();
				int thisCount = 0;
				while(dbCursor.hasNext() && thisCount<limit_){
					long hasNextEndTime = System.currentTimeMillis();
					float hasNextSeconds = (hasNextEndTime-hasNextStartTime)/1000F;
					logger.info("DBCursor.hasNext() time: " + hasNextSeconds + " seconds");
					logger.info("DBCursor.explain(): \n" + dbCursor.explain().toString());

					long startTime2 = System.currentTimeMillis();
                    JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next().toString());
					thisJSONObj.remove("_id");
					thisJSONObj.remove("pubid");
					results.add(thisJSONObj);
					thisCount+=1;
					long endTime2 = System.currentTimeMillis();
					float seconds2 = (endTime2-startTime2)/1000F;
					logger.info("DBCursor.next() loop " + thisCount + " time: " + seconds2 + " seconds");
				}
				endTime = System.currentTimeMillis();
        		seconds = (endTime - startTime) / 1000F;
				logger.info("Packing result object time: " + seconds + " sec");

				queryResults.put("results", results);
				return queryResults;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(olapReposOpen)
				olapRepos.requestDone();
		}
		
		return null;
	}

    public String getLastResult(String path){
        //run mapreduce to get the last calced result
        String resStr = "{}";
        long maxts=0;
		boolean olapReposOpen=false;
		try{
			String path2 = null;
			if(path.endsWith("/"))
				path2 = path.substring(0, path.length()-1);
			else
				path2 = path + "/";
			String[] inJArrayStr = {path, path2};

			//DB database = m.getDB(dataRepository);
			//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
			olapRepos.requestStart();
			olapReposOpen=true;
			String map = "function() { emit(this.path, this.ts); }";
			String reduce = "function(keys, values) { return Math.max.apply(Math, values); }";
			//String mrResults = "max_props_timestamps";
			String mrResults = "mrTemp";
			QueryBuilder qb = QueryBuilder.start("path").in(inJArrayStr);
			DBObject query = qb.get();
			logger.info("Query on mapreduce results: " + query.toString());
			MapReduceOutput mrOutput = olapTsCollection.mapReduce(map, reduce, 
                                                                    mrResults, query);
			Iterable<DBObject> dbcIt = mrOutput.results();
			if(dbcIt!=null){
				Iterator<DBObject> dbc = dbcIt.iterator();
				if(dbc.hasNext()){
					DBObject dbObj = dbc.next();
					logger.fine("MapReduce Result Obtained: " + dbObj.toString());
					JSONObject dbJObj = new JSONObject();
					dbJObj.putAll((Map)dbObj);
					maxts = dbJObj.getLong("value");
				}
			}
			mrOutput.drop();
            JSONObject tsquery = new JSONObject();
            tsquery.put("ts", maxts);
            JSONObject resObj = query(tsquery.toString());
            if(resObj!=null){
                JSONArray resArray = resObj.getJSONArray("results");
                if(resArray.size()>0){
                    JSONObject dp = (JSONObject)resArray.get(0);
                    dp.remove("path");
                    resStr = dp.toString();
                }
            }
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
			if(olapReposOpen)
				olapRepos.requestDone();
		}
        return resStr;
    }
}
