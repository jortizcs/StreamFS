package sfs.db;

import com.mongodb.*;

import java.util.*;
import java.util.logging.*;
import java.io.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class MongoDBDriver {

	private static transient final Logger logger = Logger.getLogger(MongoDBDriver.class.getPackage().getName());
    private static final JSONParser parser = new JSONParser();

	public static String serverAddress = null;//"localhost";
	public static int port = 0;//27017;
	public static String login = "";
	public static String password = "";
    public static boolean replicaSetEnabled = false;
    private static JSONArray replicas = null;

	private static int openConns =0;

	//database
	private static String dataRepository = "data_repos";

	//The timeseries data stream collected from stream resource nodes
	private static String mainCollection  = "main_coll";

	//The properties for every resource in the hierarchy
	private static String rsrcPropsCollection = "rsrc_props_coll";

	//The hierarchy snapshot whenever any structural changes are made to the hierarchy (r/s nodes added/deleted)
	private static String snapshotCollection = "hier_snapshots_coll";

	//Compressed timeseries collection
	private static String tsCollection = "timeseries_coll";
	
	//Models scripts collection
	private static String modelsCollection  = "models_coll";

	//Configuration file
	private String dbConfigFile = "sfs/db/db_config/db_info.json";

	private static Mongo m = null;
	private static DB dataRepos = null; 
	private static DBCollection dataCollection = null;
	private static DBCollection propsCollection = null;
	private static DBCollection hierCollection = null;
	private static DBCollection mCollection=null;
	private static DBCollection tsDataCollection = null;

	protected static boolean inited = false;

    private static MongoDBDriver driver = null;

    public static MongoDBDriver getInstance(){
        if(driver==null)
            driver = new MongoDBDriver();
        return driver;
    }

    protected void shutdown(){
        try {
            if(m!=null)
                m.close();
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
    }

	private MongoDBDriver(){
		if(!inited){
			setupGlobals();
            ShutdownHook shutdown = new ShutdownHook(this);
            Runtime.getRuntime().addShutdownHook(shutdown);
		}

		if(!inited){
			//setup indices
			BasicDBObject indicesObj = new BasicDBObject();
			indicesObj.append("oid", new Integer(1));
			indicesObj.append("timestamp", new Integer(1));
			dataCollection.ensureIndex(indicesObj);

			//Setup timeseries data collection
			BasicDBObject tsIndicesObj = new BasicDBObject();
			tsIndicesObj.append("oid", new Integer(1));
			tsIndicesObj.append("ts", new Integer(1));
			tsDataCollection.ensureIndex(indicesObj);

			//setup props indices
			BasicDBObject propsIndicesObj = new BasicDBObject();
			propsIndicesObj.append("oid", new Integer(1));
			propsIndicesObj.append("timestamp", new Integer(1));
			propsIndicesObj.append("_keywords", new Integer(1));
			propsCollection.ensureIndex(propsIndicesObj);

			//setup models collection indices
			BasicDBObject modelsIndicesObj = new BasicDBObject();
			modelsIndicesObj.append("oid", new Integer(1));
			modelsIndicesObj.append("timestamp", new Integer(1));
			mCollection.ensureIndex(propsIndicesObj);
			inited = true;
		}
	}

	private void setupGlobals(){
		try{
			String home=null;
			if((home=System.getenv().get("SFSHOME")) != null)
				dbConfigFile = home + "/sfs/db/db_config/db_archive_info.json";
            
            //logger.info("home=" + home); System.exit(1);
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
            if(configJsonObj.containsKey("address"))
			    serverAddress = (String)configJsonObj.get("address");
            else if(configJsonObj.containsKey("replicaSet") && 
                    ((JSONArray)configJsonObj.get("replicaSet")).size()>0){
                replicas = (JSONArray)configJsonObj.get("replicaSet");
            } else{
                logger.warning("must contain either \"address\" attribute or " +
                        "\"replicaSet\" array with a list of atleast 2 hosts");
                System.exit(1);
            }

            Integer portint = ((Long)configJsonObj.get("port")).intValue();
			if(portint !=null && portint.intValue() != 0)
				port = portint.intValue();
			login = (String)configJsonObj.get("login");
			password = (String)configJsonObj.get("pw");

			if(m == null ){
				MongoOptions options = new MongoOptions();
				options.connectionsPerHost=1000;
                if(serverAddress != null){
                    ServerAddress serverAddr = new ServerAddress(serverAddress, port);
                    m = new Mongo(serverAddr,options);
                } else {
                    ArrayList<ServerAddress> replicaHosts = new ArrayList<ServerAddress>();
                    for(int i=0; i<replicas.size(); i++){
                        JSONObject thisHostEntry = (JSONObject)replicas.get(i);
                        ServerAddress thisSvrAddr = null;
                        String thisSvrAddrStr = (String)thisHostEntry.get("host");
                        Integer prt = (Integer)thisHostEntry.get("port");
                        int port = -1;
                        if(prt !=null)
                            port =  prt.intValue();
                        if(port !=0)
                            thisSvrAddr = new ServerAddress(thisSvrAddrStr, port);
                        else
                            thisSvrAddr = new ServerAddress(thisSvrAddrStr);
                        replicaHosts.add(thisSvrAddr);
                    }
                    m = new Mongo(replicaHosts, options);
                }
				dataRepos = m.getDB(dataRepository);
                if(login !=null && password !=null){
                    boolean auth = dataRepos.authenticate(login, password.toCharArray());
                    if(auth != true){
                        logger.severe("Could not authenticate on DB::" + dataRepository + 
                                " with login=" + login + ",pw=" + password);
                        System.exit(1);
                    }
                }
				dataCollection = dataRepos.getCollection(mainCollection);
				propsCollection = dataRepos.getCollection(rsrcPropsCollection);
				hierCollection = dataRepos.getCollection(snapshotCollection);
				mCollection  = dataRepos.getCollection(modelsCollection);
				tsDataCollection  = dataRepos.getCollection(tsCollection);
				logger.info("(2) New Mongo instance created: server= "+serverAddress + "; port= " + port);
			}


		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public static String getDBName(){
		return dataRepository;
	}

	public static String getDataCollName(){
		return mainCollection;
	}

	public static String getRsrcPropsCollName(){
		return rsrcPropsCollection;
	}

	public static String getStructureLogCollName(){
		return snapshotCollection;
	}

	public static void main(String[] args){
		try{
			MongoDBDriver mDriver = new MongoDBDriver();
			//Mongo m = mDriver.openConn();
			JSONObject testobject = new JSONObject();
			testobject.put("a",1);
			testobject.put("b",0);
			mDriver.putEntry(testobject);
			//mDriver.closeConn(m);

			m = mDriver.openConn();
			JSONObject queryObj = new JSONObject();
			JSONObject condition = new JSONObject();
			condition.put("$gte", 0);
			queryObj.put("a", condition);
			JSONObject keys = new JSONObject();
			keys.put("_id",0);
			System.out.println( mDriver.query(queryObj.toString(), keys.toString()) );
			mDriver.closeConn(m);
		} catch(Exception e){
			e.printStackTrace();
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
				DB database = m.getDB(dataRepository);
				DBCollection thisCollection = database.getCollection(collectionName);
				logger.info("Getting index information from " + dataRepository + " for collection " + collectionName);
				if(thisCollection!=null){
					List<DBObject> indexes = thisCollection.getIndexInfo();
					for(int i=0; i<indexes.size(); i++){
						DBObject thisIndex = indexes.get(i);
						JSONObject thisIndexJObj = new JSONObject();
						thisIndexJObj.putAll(thisIndex.toMap());
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
				DB database = m.getDB(dataRepository);
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
				DB database = m.getDB(dataRepository);
				DBCollection thisCollection = database.getCollection(collectionName);
				logger.info("Dropping index " + indexName + " from " + collectionName);
				thisCollection.dropIndex(indexName);
				logger.info("Done dropping index(es)");
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	/************************************/
	/**  Access/query data collection  **/
	/************************************/

	public void putEntry(JSONObject entry){
		WriteResult result = null;
		try {
			/*String entryStr = entry.toString();
			entryStr = entryStr.replaceAll("\\$", "\\_");
			logger.finer("EntryStr: " + entryStr);
			JSONObject entryObj  = (JSONObject) parser.parse(entryStr);*/
			BasicDBObject dataObj = new BasicDBObject((Map)entry);
			if(m != null){
				//DB database = m.getDB(dataRepository);
				dataRepos.requestStart();
				//result = dataCollection.insert(dataObj);
				result = tsDataCollection.save(dataObj);
				dataObj = null;
				logger.info("Inserted mongo entry in main data collection: " + entry.toString());
			} else {
				logger.warning("Mongo connection came back NULL");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "Exception thrown while inserting entry into Mongo",e);
			if(e instanceof MongoException && result!=null)
				logger.info("Error? " + result.getError());
		} finally {
	        dataRepos.requestDone();
        }
	}

	public void putTsEntry(JSONObject entry){
		WriteResult result = null;
		try {
			JSONObject strippedEntry = stripEntry(entry);
			if(!strippedEntry.toString().equals("{}")){
				BasicDBObject dataObj = new BasicDBObject((Map)strippedEntry);
				if(m != null){
					dataRepos.requestStart();
					result = tsDataCollection.save(dataObj);
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
		} finally {
			dataRepos.requestDone();
        }
	}

	public void putTsEntries(ArrayList<JSONObject> entries){
		ArrayList<DBObject> bulkEntry=new ArrayList<DBObject>();
		WriteResult result = null;
		int bulkBytes = 0;
		try {
			for(int i=0; i<entries.size(); ++i){
				JSONObject entry = entries.get(i);
				logger.info("ThisEntry: " + entry.toString());
				JSONObject strippedEntry = stripEntry(entry);
				if(!strippedEntry.toString().equals("{}")){
					BasicDBObject dataObj = new BasicDBObject((Map)strippedEntry);
					bulkEntry.add(dataObj);
					bulkBytes += strippedEntry.toString().getBytes().length;
				}
			}
			if(m != null){
				dataRepos.requestStart();
				result = tsDataCollection.insert(bulkEntry);
				logger.info("Inserted bulk Ts entry in Ts data collection: " + bulkBytes + " bytes");
			} else {
				logger.warning("Mongo connection came back NULL");
			}
			
		} catch (Exception e){
			logger.log(Level.WARNING, "Exception thrown while inserting entry into Mongo",e);
			if(e instanceof MongoException && result!=null)
				logger.info("Error? " + result.getError());
		} finally {
			dataRepos.requestDone();
        }
	}

	private JSONObject stripEntry(JSONObject entry){
		JSONObject s = new JSONObject();
        Object valueobj = entry.get("value");

		if(entry.containsKey("Reading"))
			s.put("value", entry.get("Reading"));
		else if(valueobj!=null && valueobj.getClass().equals(Double.TYPE) && 
                    !Double.isNaN((Double) valueobj))
			s.put("value",(Double)valueobj);
		else if(valueobj!=null && valueobj.getClass().equals(Integer.TYPE))
			s.put("value", (Integer)valueobj);

		if(entry.containsKey("ReadingTime")){
            Integer i = (Integer)entry.get("ReadingTime");
            if(i!=null)
			    s.put("ReadingTime", i);
		} else if(entry.containsKey("timestamp")){
            Integer i = (Integer)entry.get("timestamp");
            if(i!=null)
			    s.put("timestamp", i);
		}

		try{
			s.put("pubid", (String)entry.get("pubid"));
			s.put("ts", (Long)entry.get("ts"));
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		return s;
	}

	public JSONObject getEntry(String name){
		return null;
	}

	public JSONObject query(String query){
		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dataReposOpen=false;
		boolean dbCursorOpen = false;
		DBCursor dbCursor = null;
		try {
			
			if(query != null && m!= null){
				JSONObject queryObj = (JSONObject) parser.parse(query);
				BasicDBObject dataObj = new BasicDBObject((Map)queryObj);
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(mainCollection);
				dataRepos.requestStart();
				dataReposOpen=true;
				dbCursor = tsDataCollection.find(dataObj);
				dbCursorOpen=true;
				results = new JSONArray();
				while(dbCursor.hasNext()){
					JSONObject thisJSONObj = (JSONObject) parser.parse(dbCursor.next().toString());
					thisJSONObj.remove("_id");
					results.add(thisJSONObj);
				}
				dataObj = null;
				queryResults.put("results", results);
				//closeConn(m);
				return queryResults;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		//closeConn(m);
		return null;
	}

	

	public JSONObject query(String query, String keys){
		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor=null;
		try {
			if(query != null && keys!=null && m!= null){
				JSONObject queryObj = (JSONObject) parser.parse(query);
				JSONObject keysObj = (JSONObject) parser.parse(keys);
				BasicDBObject queryDBObj  = new BasicDBObject((Map)queryObj);
				BasicDBObject keysDBObj = new BasicDBObject((Map)keysObj);
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(mainCollection);
				dataRepos.requestStart();
				dataReposOpen=true;
				dbCursor = tsDataCollection.find(queryDBObj,keysDBObj);
				dbCursorOpen=true;
				results = new JSONArray();
				while(dbCursor.hasNext()){
					JSONObject thisJSONObj = (JSONObject) parser.parse(dbCursor.next().toString());
					thisJSONObj.remove("_id");
					results.add(thisJSONObj);
				}
				queryResults.put("results", results);
				//closeConn(m);
				return queryResults;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			closeConn(m);
		} finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		
		return null;
	}

	public JSONArray queryTsColl(String query, String keys){
		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor=null;
		try {
			if(query != null && keys!=null && m!= null){
				JSONObject queryObj = (JSONObject) parser.parse(query);
				JSONObject keysObj = (JSONObject) parser.parse(keys);
				BasicDBObject queryDBObj  = new BasicDBObject((Map)queryObj);
				BasicDBObject keysDBObj = new BasicDBObject((Map)keysObj);
			
				keysDBObj.put("_id", new Integer(0));	
				dataRepos.requestStart();
				dataReposOpen=true;
				BasicDBObject sortByObj = new BasicDBObject("ts", "1");
				dbCursor = tsDataCollection.find(queryDBObj,keysDBObj).sort(sortByObj);
				dbCursorOpen=true;
				results = new JSONArray();
				while(dbCursor.hasNext()){
					JSONObject thisJSONObj = (JSONObject) parser.parse(dbCursor.next().toString());
					//thisJSONObj.remove("_id");
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
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		
		return null;
	}

	public synchronized JSONObject queryWithLimit(String queryString, String orderBy, int limit_){

		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor =null;
		try {
			if(queryString != null && m!= null){
				JSONObject queryObj = (JSONObject) parser.parse(queryString);
				BasicDBObject queryDBObj = new BasicDBObject((Map)queryObj);
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(mainCollection);

				logger.info("MongoQuery: " + queryString);
				long startTime = System.currentTimeMillis();
				dataRepos.requestStart();
				dataReposOpen=true;
				dbCursor = tsDataCollection.find(queryDBObj);
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
					JSONObject orderByJSON = (JSONObject) parser.parse(orderBy);
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
					JSONObject thisJSONObj = (JSONObject) parser.parse(dbCursor.next().toString());
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
				//closeConn(m);
				return queryResults;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			//closeConn(m);
		} finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		
		return null;
	}


	/****************************************/
	/** Access/Query properties collection **/
	/****************************************/

	public synchronized JSONObject queryProps(String query){
		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor =null;
		try {
			if(query != null && m!= null){
				JSONObject queryObj = (JSONObject) parser.parse(query);
				BasicDBObject queryDBObj = new BasicDBObject((Map)queryObj);
				dataRepos.requestStart();
				dataReposOpen=true;
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
				dbCursor = propsCollection.find(queryDBObj);
				dbCursorOpen=true;
				results = new JSONArray();
				while(dbCursor.hasNext()){
					JSONObject thisJSONObj = (JSONObject) parser.parse(dbCursor.next().toString());
					thisJSONObj.remove("_id");
					thisJSONObj.remove("_keywords");
					results.add(thisJSONObj);
				}
				queryResults.put("results", results);
				//closeConn(m);
				return queryResults;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		} finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(dataReposOpen)
				dataRepos.requestDone();
		}

		
		//closeConn(m);
		return null;
	}

	public synchronized JSONObject queryPropsWithLimit(String queryString, String orderBy, int limit_){

		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor =null;
		try {
			if(queryString != null && m!= null){
				dataRepos.requestStart();
				dataReposOpen=true;
				JSONObject queryObj = (JSONObject) parser.parse(queryString);
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);

				logger.info("MongoQuery: " + queryString);
				long startTime = System.currentTimeMillis();
				dbCursor = propsCollection.find(new BasicDBObject((Map)queryObj));
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
					JSONObject orderByJSON = (JSONObject) parser.parse(orderBy);
					dbCursor = dbCursor.sort(new BasicDBObject((Map)orderByJSON));
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
					JSONObject thisJSONObj = (JSONObject) parser.parse(dbCursor.next().toString());
					thisJSONObj.remove("_id");
					thisJSONObj.remove("_keywords");
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
				//closeConn(m);
				return queryResults;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			closeConn(m);
		}finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(dataReposOpen)
				dataRepos.requestDone();
		}

		
		return null;
	}

	public long getPropsHistCount(String oid){
		long count = 0;
		boolean dataReposOpen=false;
		try {
			if (m==null){
				MongoDBDriver mdriver = new MongoDBDriver();
			}
			
			String[] inJArrayStr = {oid};	

			dataRepos.requestStart();
			dataReposOpen=true;
			QueryBuilder qb = QueryBuilder.start("oid").in(inJArrayStr);
			DBObject query = qb.get();
			dataReposOpen=true;
			logger.info("MQuery: " + query.toString());
			//DB database = m.getDB(dataRepository);
			//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
			count = propsCollection.find(query).count();	
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		return count;
	}

	public long getMaxTsProps(String oid){
		long maxts=0;
		boolean dataReposOpen=false;
		try{
			
			String[] inJArrayStr = {oid};

			dataRepos.requestStart();
			dataReposOpen=true;
			String map = "function() { emit(this.oid, this.timestamp); }";
			String reduce = "function(keys, values) { return Math.max.apply(Math, values); }";
			//String mrResults = "max_props_timestamps";
			String mrResults = "mrTemp";
			QueryBuilder qb = QueryBuilder.start("oid").in(inJArrayStr);
			DBObject query = qb.get();
			logger.info("Query on mapreduce results: " + query.toString());
			MapReduceOutput mrOutput = propsCollection.mapReduce(map, reduce, mrResults, query);
			Iterable<DBObject> dbcIt = mrOutput.results();
			if(dbcIt!=null){
				Iterator<DBObject> dbc = dbcIt.iterator();
				if(dbc.hasNext()){
					DBObject dbObj = dbc.next();
					logger.fine("MapReduce Result Obtained: " + dbObj.toString());
					JSONObject dbJObj = new JSONObject();
					dbJObj.putAll((Map)dbObj);
					maxts = (Long)dbJObj.get("value");
				}
			}
			mrOutput.drop();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		return maxts;
	}

	public synchronized void putPropsEntry(JSONObject entry){
		//Mongo m = openConn();
		boolean dataReposOpen=false;
		try {
			if(m != null){
				//DB database = m.getDB(dataRepository);
				dataRepos.requestStart();
				dataReposOpen=true;
				//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
				WriteResult result = propsCollection.insert(new BasicDBObject((Map) entry));

				logger.info("Inserted mongo entry in resource properties collection: " + entry.toString());
				logger.info("Error? " + result.getError());
			} else {
				logger.warning("Mongo connection came back NULL");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "Exception thrown while inserting entry into Mongo",e);
		} finally {
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		//closeConn(m);

	}

	public synchronized JSONObject getPropsEntry(String oid, long timestamp){
		JSONObject results = new JSONObject();
		boolean dataReposOpen=false;
		try {
			if(m != null){
				String[] inJArrayStr = {oid};
			
				dataRepos.requestStart();
				dataReposOpen=true;	
				QueryBuilder qb = QueryBuilder.start("oid").in(inJArrayStr);
				qb = qb.put("timestamp").is(new Long(timestamp));
				DBObject query = qb.get();
				logger.info("MQuery: " + query.toString());
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
				BasicDBObject dbObj = (BasicDBObject) propsCollection.findOne(query);
				if(dbObj != null)
					results.putAll((Map)dbObj);
			} else {
				logger.warning("Mongo connection NULL");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		return results;
	}

    public void move(String old_oid, String new_oid){
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor =null;
        WriteResult writeResults =null;
		try {
			if(m!= null){
                JSONObject queryObj = new JSONObject();
                JSONArray orArray = new JSONArray();
                JSONObject cond1 = new JSONObject();
                JSONObject cond2 = new JSONObject();
                cond1.put("oid", old_oid);
                cond2.put("oid", new_oid);
                orArray.add(cond1);
                orArray.add(cond2);
                queryObj.put("$or", orArray);
				BasicDBObject queryDBObj = new BasicDBObject((Map)queryObj);
				dataRepos.requestStart();
                dataReposOpen= true;
				dbCursor = propsCollection.find(queryDBObj);
				dbCursorOpen=true;
			    if(dbCursor.hasNext()){
					JSONObject thisJSONObj = (JSONObject) parser.parse(dbCursor.next().toString());
                    thisJSONObj.put("oid", new_oid);
                    BasicDBObject updatedObj = new BasicDBObject((Map)thisJSONObj);
                    writeResults = propsCollection.update(queryDBObj, updatedObj);
				}
			}
			
		} catch(Exception e){
            if(writeResults !=null)
			    logger.log(Level.WARNING, writeResults.getError(), e);
            else
                logger.log(Level.WARNING, "", e);
		} finally {
			if(dbCursorOpen)
				dbCursor.close();
			if(dataReposOpen)
				dataRepos.requestDone();
		}
	}
	
	
	
	/*****************************************/
	/** Access/Query models collection     **/
	/****************************************/
	public synchronized void putModelEntry(JSONObject entry){
		boolean dataReposOpen=false;
		try {
			if(m != null){
				dataRepos.requestStart();
				dataReposOpen=true;
				WriteResult result = mCollection.insert(new BasicDBObject((Map) entry));

				logger.info("Inserted mongo entry in resource models collection: " + entry.toString());
				logger.info("Error? " + result.getError());
			} else {
				logger.warning("Mongo connection came back NULL");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "Exception thrown while inserting entry into Mongo",e);
		} finally {
			if(dataReposOpen)
				dataRepos.requestDone();
		}
	}
	
	public synchronized JSONObject getModelEntry(String oid, long timestamp){
		JSONObject results = new JSONObject();
		boolean dataReposOpen=false;
		try {
			if(m != null){
				String[] inJArrayStr = {oid};
				dataRepos.requestStart();
				dataReposOpen=true;
				QueryBuilder qb = QueryBuilder.start("oid").in(inJArrayStr);
				qb = qb.put("timestamp").is(new Long(timestamp));
				DBObject query = qb.get();
				logger.info("MQuery: " + query.toString());
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
				BasicDBObject dbObj = (BasicDBObject) mCollection.findOne(query);
				if(dbObj != null)
					results.putAll((Map)dbObj);
			} else {
				logger.warning("Mongo connection NULL");
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		return results;
	}
	
	public long getMaxTsModels(String oid){
		long maxts=0;
		boolean dataReposOpen=false;
		try{
			String[] inJArrayStr = {oid};

			dataRepos.requestStart(); dataReposOpen=true;
			//DB database = m.getDB(dataRepository);
			//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
			String map = "function() { emit(this.oid, this.timestamp); }";
			String reduce = "function(keys, values) { return Math.max.apply(Math, values); }";
			//String mrResults = "max_props_timestamps";
			String mrResults = "mrResults";
			QueryBuilder qb = QueryBuilder.start("oid").in(inJArrayStr);
			DBObject query = qb.get();
			logger.info("Query on mapreduce results: " + query.toString());
			MapReduceOutput mrOutput = mCollection.mapReduce(map, reduce, mrResults, query);
			Iterable<DBObject> dbcIt = mrOutput.results();
			if(dbcIt!=null){
				Iterator<DBObject> dbc = dbcIt.iterator();
				if(dbc.hasNext()){
					DBObject dbObj = dbc.next();
					logger.fine("MapReduce Result Obtained: " + dbObj.toString());
					JSONObject dbJObj = new JSONObject();
					dbJObj.putAll((Map)dbObj);
					maxts = (Long)dbJObj.get("value");
				}
			}
			mrOutput.drop();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		return maxts;
	}
	
	public long getModelHistCount(String oid){
		long count = 0;
		boolean dataReposOpen=false;
		try {
			if (m==null){
				MongoDBDriver mdriver = new MongoDBDriver();
            }
			String[] inJArrayStr = {oid};	

			dataRepos.requestStart(); dataReposOpen=true;
			QueryBuilder qb = QueryBuilder.start("oid").in(inJArrayStr);
			DBObject query = qb.get();
			logger.info("MQuery: " + query.toString());
			//DB database = m.getDB(dataRepository);
			//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
			if(mCollection != null)
			count = mCollection.find(query).count();	
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
			if(dataReposOpen)
				dataRepos.requestDone();
		}
		return count;
	}

    public class ShutdownHook extends Thread{
        MongoDBDriver driver = null;
        public ShutdownHook(MongoDBDriver mdriver){
            driver = mdriver;
        }

        public void run(){
            if(driver!=null){
                driver.shutdown();
            }
        }
    }
	
}
