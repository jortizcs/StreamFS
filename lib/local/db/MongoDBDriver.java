package local.db;

import net.sf.json.*;

import com.mongodb.*;

import java.util.*;
import java.util.logging.*;
import java.io.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MongoDBDriver implements Is4Database {

	private static transient final Logger logger = Logger.getLogger(MongoDBDriver.class.getPackage().getName());

	public static String serverAddress = null;//"localhost";
	public static int port = 0;//27017;
	public static String login = "";
	public static String password = "";
    public static boolean replicaSetEnabled = false;
    private static JSONArray replicas = null;

	private static int openConns =0;

    private static boolean WATCHDOG_ACTIVE = false;
    private static int ACTIVE_CONN_CNT = 0;
    private static final Lock LOCK = new ReentrantLock();

	//database
	private static String dataRepository = "is4_data_repos";

	//The timeseries data stream collected from stream resource nodes
	private static String mainCollection  = "is4_main_coll";

	//The properties for every resource in the hierarchy
	private static String rsrcPropsCollection = "is4_rsrc_props_coll";

	//The hierarchy snapshot whenever any structural changes are made to the hierarchy (r/s nodes added/deleted)
	private static String snapshotCollection = "is4_hier_snapshots_coll";

	//Compressed timeseries collection
	private static String tsCollection = "sfs_timeseries_coll";
	
	//Models scripts collection
	private static String modelsCollection  = "is4_models_coll";

	//Configuration file
	private String dbConfigFile = "/project/eecs/tinyos/is4/lib/local/db/db_config/db_info.json";

	private static Mongo m = null;

	protected static boolean inited = false;

	public MongoDBDriver(){
        try {
            if(!inited){
                setupGlobals();
                ShutdownHook shutdown = new ShutdownHook(this);
                Runtime.getRuntime().addShutdownHook(shutdown);
            }

            if(!inited){
                DB dataRepos = m.getDB(dataRepository);
                if(!login.equals("") && !password.equals("")){
                    boolean auth = dataRepos.authenticate(login, password.toCharArray());
                    if(auth != true){
                        logger.severe("Could not authenticate on DB::" + dataRepository + 
                                " with login=" + login + ",pw=" + password);
                        System.exit(1);
                    }
                }
                DBCollection dataCollection = dataRepos.getCollection(mainCollection);
                DBCollection propsCollection = dataRepos.getCollection(rsrcPropsCollection);
                DBCollection mCollection  = dataRepos.getCollection(modelsCollection);
                DBCollection tsDataCollection  = dataRepos.getCollection(tsCollection);
                
                BasicDBObject indicesObj = new BasicDBObject();
                indicesObj.append("PubId", new Integer(1));
                indicesObj.append("timestamp", new Integer(1));
                dataCollection.ensureIndex(indicesObj);

                //setup props indices
                BasicDBObject propsIndicesObj = new BasicDBObject();
                propsIndicesObj.append("is4_uri", new Integer(1));
                propsIndicesObj.append("timestamp", new Integer(1));
                propsIndicesObj.append("_keywords", new Integer(1));
                propsCollection.ensureIndex(propsIndicesObj);

                //setup models collection indices
                BasicDBObject modelsIndicesObj = new BasicDBObject();
                modelsIndicesObj.append("is4_uri", new Integer(1));
                modelsIndicesObj.append("timestamp", new Integer(1));
                mCollection.ensureIndex(propsIndicesObj);

                //Setup timeseries data collection
                BasicDBObject tsIndicesObj = new BasicDBObject();
                tsIndicesObj.append("pubid", new Integer(1));
                tsIndicesObj.append("ts", new Integer(1));
                tsDataCollection.ensureIndex(indicesObj);

                Timer t = new Timer();
                t.scheduleAtFixedRate(new ConnectionWatchdog(), 1000000L, 1000000L);
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            System.exit(1);
        } finally {
            inited=true;
        }

	}

	private void setupGlobals(){
		try{
			String home=null;
			if((home=System.getenv().get("IS4HOME")) != null)
				dbConfigFile = home + "/lib/local/db/db_config/db_archive_info.json";
			File configFile = new File(dbConfigFile);
			FileReader cFileReader = new FileReader(configFile);
			BufferedReader bufReader = new BufferedReader(cFileReader);
		
			StringBuffer strBuf = new StringBuffer();
			String line = null;
			while((line=bufReader.readLine())!=null)
				strBuf.append(line).append(" ");
			JSONObject configJsonObj = (JSONObject)JSONSerializer.
								toJSON(strBuf.toString());
			cFileReader.close();
			bufReader.close();
            if(configJsonObj.containsKey("address"))
			    serverAddress = configJsonObj.optString("address");
            else if(configJsonObj.containsKey("replicaSet") && 
                    configJsonObj.getJSONArray("replicaSet").size()>0){
                replicas = configJsonObj.getJSONArray("replicaSet");
            } else{
                logger.warning("must contain either \"address\" attribute or " +
                        "\"replicaSet\" array with a list of atleast 2 hosts");
                System.exit(1);
            }

			if(configJsonObj.optInt("port") != 0)
				port = configJsonObj.optInt("port");
			login = configJsonObj.optString("login");
			password = configJsonObj.optString("pw");

			if(m == null ){
				MongoOptions options = new MongoOptions();
				options.connectionsPerHost=1000;
                //options.safe = true;
                options.connectTimeout = 10000;
                //options.socketTimeout = 500;
                if(serverAddress != null){
                    ServerAddress serverAddr = new ServerAddress(serverAddress, port);
                    m = new Mongo(serverAddr,options);
                } else {
                    ArrayList<ServerAddress> replicaHosts = new ArrayList<ServerAddress>();
                    for(int i=0; i<replicas.size(); i++){
                        JSONObject thisHostEntry = replicas.getJSONObject(i);
                        ServerAddress thisSvrAddr = null;
                        String thisSvrAddrStr = thisHostEntry.getString("host");
                        int port =  thisHostEntry.optInt("port");
                        if(port !=0)
                            thisSvrAddr = new ServerAddress(thisSvrAddrStr, port);
                        else
                            thisSvrAddr = new ServerAddress(thisSvrAddrStr);
                        replicaHosts.add(thisSvrAddr);
                    }
                    m = new Mongo(replicaHosts, options);
                }
				/*dataRepos = m.getDB(dataRepository);
                if(!login.equals("") && !password.equals("")){
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
				tsDataCollection  = dataRepos.getCollection(tsCollection);*/
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

    protected void shutdown(){
        try {
            if(m!=null)
                m.close();
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
    }

	public static String getRsrcPropsCollName(){
		return rsrcPropsCollection;
	}

	public static String getStructureLogCollName(){
		return snapshotCollection;
	}

	/*public static void main(String[] args){
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
			//mDriver.closeConn(m);
		} catch(Exception e){
			e.printStackTrace();
		}
	}*/

	public Mongo openConn(){
		try {
			if(m == null)
				m = new Mongo(serverAddress);
			
			/*openConns +=1;
			logger.info("Mongo Open: conn_count=" + openConns);*/
			return m;
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
            System.exit(1);
		}
		return null;
	}

	public void closeConn(Mongo m_input){
		//if(m != m_input && m_input != null){
		//m_input.close();
		//if(m == m_input)
		//	m = null;
		//openConns -=1;
		//logger.info("Mongo Close: conn_count=" + openConns);
		//}
	}

	protected void enter(){
        try {
            if(WATCHDOG_ACTIVE == true && ACTIVE_CONN_CNT==0){
                LOCK.lock();
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            System.exit(1);
        }
        synchronized(this){
            ACTIVE_CONN_CNT +=1;
        }
    } 

    protected void leave(){
        synchronized(this){
            ACTIVE_CONN_CNT -= 1;
            if(ACTIVE_CONN_CNT<0 && WATCHDOG_ACTIVE){
                ACTIVE_CONN_CNT=0;
                LOCK.unlock();
            }
        }
    }

	public JSONArray getIndexInfo(String collectionName){
	    enter();	
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
		} finally {
            leave();
        }
		return indexesArray;
	}

   

	public void setIndexes(String collectionName, JSONObject indexes){
        enter();
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
		} finally {
            leave();
        }
	}

	public void removeIndex(String collectionName, String indexName){
        enter();
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
		} finally {
            leave();
        }
	}

	/************************************/
	/**  Access/query data collection  **/
	/************************************/

	public void putEntry(JSONObject entry){
        enter();
		WriteResult result = null;
        DB dataRepos = null;
		try {
            BasicDBObject dataObj = new BasicDBObject((Map)entry);
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection tsDataCollection  = dataRepos.getCollection(tsCollection);
            
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
		}finally{
            dataRepos.requestDone();
            leave();
        }
	}

	public void putTsEntry(JSONObject entry){
        enter();
		WriteResult result = null;
        DB dataRepos = null;
		try {
			JSONObject strippedEntry = stripEntry(entry);
			if(!strippedEntry.toString().equals("{}")){
				BasicDBObject dataObj = new BasicDBObject((Map)strippedEntry);
                dataRepos = m.getDB(dataRepository);
                if(!login.equals("") && !password.equals("")){
                    boolean auth = dataRepos.authenticate(login, password.toCharArray());
                    if(auth != true){
                        logger.severe("Could not authenticate on DB::" + dataRepository + 
                                " with login=" + login + ",pw=" + password);
                        System.exit(1);
                    }
                }
                DBCollection tsDataCollection  = dataRepos.getCollection(tsCollection);
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
            leave();
        }
	}

	public void putTsEntries(ArrayList<JSONObject> entries){
        enter();
		ArrayList<DBObject> bulkEntry=new ArrayList<DBObject>();
		WriteResult result = null;
		int bulkBytes = 0;
        DB dataRepos = null;
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection tsDataCollection  = dataRepos.getCollection(tsCollection);
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
            leave();
        }
	}

	private JSONObject stripEntry(JSONObject entry){
        enter();
		JSONObject s = new JSONObject();
        try {
            if(entry.containsKey("Reading")){
                s.put("value", entry.get("Reading"));
            } else if(entry.containsKey("value") && !Double.isNaN(entry.optDouble("value"))){
                s.put("value",entry.optDouble("value"));
            } else {
                s.put("value", entry.optInt("value"));
            }

            if(entry.containsKey("ReadingTime")){
                s.put("ReadingTime", entry.optInt("ReadingTime"));
            } else if(entry.containsKey("timestamp")){
                s.put("timestamp", entry.optInt("timestamp"));
            }

            try{
                s.put("pubid", entry.getString("pubid"));
                s.put("ts", entry.getLong("ts"));
            } catch(Exception e){
                logger.log(Level.WARNING, "", e);
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
        finally {
            leave();
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
        DB dataRepos = null;
        enter();
		try {
			dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection tsDataCollection  = dataRepos.getCollection(tsCollection);
			if(query != null && m!= null){
				JSONObject queryObj = (JSONObject) JSONSerializer.toJSON(query);
				BasicDBObject dataObj = new BasicDBObject((Map)queryObj);
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(mainCollection);
				dataRepos.requestStart();
				dataReposOpen=true;
				dbCursor = tsDataCollection.find(dataObj);
				dbCursorOpen=true;
				results = new JSONArray();
				while(dbCursor.hasNext()){
					JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next());
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
            dbCursor.close();
            dataRepos.requestDone();
            leave();
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
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection tsDataCollection  = dataRepos.getCollection(tsCollection);
			if(query != null && keys!=null && m!= null){
				JSONObject queryObj = (JSONObject) JSONSerializer.toJSON(query);
				JSONObject keysObj = (JSONObject) JSONSerializer.toJSON(keys);
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
					JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next());
					thisJSONObj.remove("_id");
					results.add(thisJSONObj);
				}
				queryResults.put("results", results);
				//closeConn(m);
				return queryResults;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			//closeConn(m);
		} finally {
            dbCursor.close();
            dataRepos.requestDone();
            leave();
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
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection tsDataCollection  = dataRepos.getCollection(tsCollection);
			if(query != null && keys!=null && m!= null){
				JSONObject queryObj = (JSONObject) JSONSerializer.toJSON(query);
				JSONObject keysObj = (JSONObject) JSONSerializer.toJSON(keys);
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
					JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next());
					//thisJSONObj.remove("_id");
					results.add(thisJSONObj);
				}
				return results;
			}
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			//closeConn(m);
		} finally {
            dbCursor.close();
            dataRepos.requestDone();
            leave();
		}
		
		return null;
	}

	public JSONObject queryWithLimit(String queryString, String orderBy, int limit_){

		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor =null;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection tsDataCollection  = dataRepos.getCollection(tsCollection);
			if(queryString != null && m!= null){
				JSONObject queryObj = (JSONObject) JSONSerializer.toJSON(queryString);
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
					JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next());
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
            dbCursor.close();
            dataRepos.requestDone();
            leave();
		}
		
		return null;
	}


	/****************************************/
	/** Access/Query properties collection **/
	/****************************************/

	public JSONObject queryProps(String query){
		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor =null;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection propsCollection = dataRepos.getCollection(rsrcPropsCollection);
			if(query != null && m!= null){
				JSONObject queryObj = (JSONObject) JSONSerializer.toJSON(query);
				BasicDBObject queryDBObj = new BasicDBObject((Map)queryObj);
				dataRepos.requestStart();
				dataReposOpen=true;
				//DB database = m.getDB(dataRepository);
				//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
				dbCursor = propsCollection.find(queryDBObj);
				dbCursorOpen=true;
				results = new JSONArray();
				while(dbCursor.hasNext()){
					JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next());
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
            dbCursor.close();
            dataRepos.requestDone();
            leave();
		}

		
		//closeConn(m);
		return null;
	}

	public JSONObject queryPropsWithLimit(String queryString, String orderBy, int limit_){

		JSONObject queryResults = new JSONObject();
		JSONArray results = new JSONArray();
		//Mongo m = openConn();
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor =null;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection propsCollection = dataRepos.getCollection(rsrcPropsCollection);
			if(queryString != null && m!= null){
				dataRepos.requestStart();
				dataReposOpen=true;
				JSONObject queryObj = (JSONObject) JSONSerializer.toJSON(queryString);
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
					JSONObject orderByJSON = (JSONObject) JSONSerializer.toJSON(orderBy);
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
					JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next());
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
			//closeConn(m);
		}finally {
            dbCursor.close();
            dataRepos.requestDone();
            leave();
		}

		
		return null;
	}

	public long getPropsHistCount(String uri){
		long count = 0;
		boolean dataReposOpen=false;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection propsCollection = dataRepos.getCollection(rsrcPropsCollection);
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else 
				uri2 = uri + "/";
			String[] inJArrayStr = {uri, uri2};	

			dataRepos.requestStart();
			dataReposOpen=true;
			QueryBuilder qb = QueryBuilder.start("is4_uri").in(inJArrayStr);
			DBObject query = qb.get();
			dataReposOpen=true;
			logger.info("MQuery: " + query.toString());
			//DB database = m.getDB(dataRepository);
			//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
			count = propsCollection.find(query).count();	
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
            dataRepos.requestDone();
            leave();
		}
		return count;
	}

	public long getMaxTsProps(String uri){
		long maxts=0;
		boolean dataReposOpen=false;
        DB dataRepos =null;
        enter();
		try{
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection propsCollection = dataRepos.getCollection(rsrcPropsCollection);
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			String[] inJArrayStr = {uri, uri2};

			dataRepos.requestStart();
			dataReposOpen=true;
			String map = "function() { emit(this.is4_uri, this.timestamp); }";
			String reduce = "function(keys, values) { return Math.max.apply(Math, values); }";
			//String mrResults = "max_props_timestamps";
			String mrResults = "mrTemp";
			QueryBuilder qb = QueryBuilder.start("is4_uri").in(inJArrayStr);
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
					maxts = dbJObj.getLong("value");
				}
			}
			mrOutput.drop();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
            dataRepos.requestDone();
            leave();
		}
		return maxts;
	}

	public JSONObject getMetadata(String pubId){
		return null;
	}

	public void putPropsEntry(JSONObject entry){
		//Mongo m = openConn();
		boolean dataReposOpen=false;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection propsCollection = dataRepos.getCollection(rsrcPropsCollection);
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
            dataRepos.requestDone();
            leave();
		}
		//closeConn(m);

	}

	public JSONObject getPropsEntry(String uri, long timestamp){
		JSONObject results = new JSONObject();
		boolean dataReposOpen=false;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection propsCollection = dataRepos.getCollection(rsrcPropsCollection);
			if(m != null){
				String uri2 = null;
				if(uri.endsWith("/"))
					uri2 = uri.substring(0, uri.length()-1);
				else 
					uri2 = uri + "/";
				String[] inJArrayStr = {uri, uri2};
			
				dataRepos.requestStart();
				dataReposOpen=true;	
				QueryBuilder qb = QueryBuilder.start("is4_uri").in(inJArrayStr);
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
            dataRepos.requestDone();
            leave();
		}
		return results;
	}

    public void move(String srcPath, String dstPath){
		boolean dbCursorOpen=false;
		boolean dataReposOpen=false;
		DBCursor dbCursor =null;
        WriteResult writeResults =null;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection propsCollection = dataRepos.getCollection(rsrcPropsCollection);
            String altPath =srcPath.endsWith("/")?srcPath.substring(0, srcPath.length()-1):srcPath+"/";
			if(m!= null){
                JSONObject queryObj = new JSONObject();
                JSONArray orArray = new JSONArray();
                JSONObject cond1 = new JSONObject();
                JSONObject cond2 = new JSONObject();
                cond1.put("is4_uri", srcPath);
                cond2.put("is4_uri", altPath);
                orArray.add(cond1);
                orArray.add(cond2);
                queryObj.put("$or", orArray);
				BasicDBObject queryDBObj = new BasicDBObject((Map)queryObj);
				dataRepos.requestStart();
                dataReposOpen= true;
				dbCursor = propsCollection.find(queryDBObj);
				dbCursorOpen=true;
			    if(dbCursor.hasNext()){
					JSONObject thisJSONObj = (JSONObject) JSONSerializer.toJSON(dbCursor.next());
                    thisJSONObj.put("is4_uri", dstPath);
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
            dbCursor.close();
            dataRepos.requestDone();
            leave();
		}
	}
	
	
	
	/*****************************************/
	/** Access/Query models collection     **/
	/****************************************/
	public void putModelEntry(JSONObject entry){
		boolean dataReposOpen=false;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection mCollection  = dataRepos.getCollection(modelsCollection);
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
            dataRepos.requestDone();
            leave();
		}
	}
	
	public JSONObject getModelEntry(String uri, long timestamp){
		JSONObject results = new JSONObject();
		boolean dataReposOpen=false;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection mCollection  = dataRepos.getCollection(modelsCollection);
			if(m != null){
				String uri2 = null;
				if(uri.endsWith("/"))
					uri2 = uri.substring(0, uri.length()-1);
				else 
					uri2 = uri + "/";
				String[] inJArrayStr = {uri, uri2};
				dataRepos.requestStart();
				dataReposOpen=true;
				QueryBuilder qb = QueryBuilder.start("is4_uri").in(inJArrayStr);
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
            dataRepos.requestDone();
            leave();
		}
		return results;
	}
	
	public long getMaxTsModels(String uri){
		long maxts=0;
		boolean dataReposOpen=false;
        DB dataRepos = null;
        enter();
		try{
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection mCollection  = dataRepos.getCollection(modelsCollection);
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else
				uri2 = uri + "/";
			String[] inJArrayStr = {uri, uri2};

			dataRepos.requestStart(); dataReposOpen=true;
			//DB database = m.getDB(dataRepository);
			//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
			String map = "function() { emit(this.is4_uri, this.timestamp); }";
			String reduce = "function(keys, values) { return Math.max.apply(Math, values); }";
			//String mrResults = "max_props_timestamps";
			String mrResults = "mrResults";
			QueryBuilder qb = QueryBuilder.start("is4_uri").in(inJArrayStr);
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
					maxts = dbJObj.getLong("value");
				}
			}
			mrOutput.drop();
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
            dataRepos.requestDone();
            leave();
		}
		return maxts;
	}
	
	public long getModelHistCount(String uri){
		long count = 0;
		boolean dataReposOpen=false;
        DB dataRepos = null;
        enter();
		try {
            dataRepos = m.getDB(dataRepository);
            if(!login.equals("") && !password.equals("")){
                boolean auth = dataRepos.authenticate(login, password.toCharArray());
                if(auth != true){
                    logger.severe("Could not authenticate on DB::" + dataRepository + 
                            " with login=" + login + ",pw=" + password);
                    System.exit(1);
                }
            }
            DBCollection mCollection  = dataRepos.getCollection(modelsCollection);
			if (m==null){
				MongoDBDriver mdriver = new MongoDBDriver();
			}
			String uri2 = null;
			if(uri.endsWith("/"))
				uri2 = uri.substring(0, uri.length()-1);
			else 
				uri2 = uri + "/";
			String[] inJArrayStr = {uri, uri2};	

			dataRepos.requestStart(); dataReposOpen=true;
			QueryBuilder qb = QueryBuilder.start("is4_uri").in(inJArrayStr);
			DBObject query = qb.get();
			logger.info("MQuery: " + query.toString());
			//DB database = m.getDB(dataRepository);
			//DBCollection dbCollection = database.getCollection(rsrcPropsCollection);
			if(mCollection != null)
			count = mCollection.find(query).count();	
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		} finally {
            dataRepos.requestDone();
            leave();
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

    public class ConnectionWatchdog extends TimerTask {
        public ConnectionWatchdog(){}
        public void run(){
            try {
                WATCHDOG_ACTIVE = true;
                LOCK.lock(); 
                if(m!=null){
                    m.close();
                }
                MongoOptions options = new MongoOptions();
				options.connectionsPerHost=1000;
                //options.safe = true;
                options.connectTimeout = 10000;
                //options.socketTimeout = 500;
                if(serverAddress != null){
                    ServerAddress serverAddr = new ServerAddress(serverAddress, port);
                    m = new Mongo(serverAddr,options);
                } else {
                    ArrayList<ServerAddress> replicaHosts = new ArrayList<ServerAddress>();
                    for(int i=0; i<replicas.size(); i++){
                        JSONObject thisHostEntry = replicas.getJSONObject(i);
                        ServerAddress thisSvrAddr = null;
                        String thisSvrAddrStr = thisHostEntry.getString("host");
                        int port =  thisHostEntry.optInt("port");
                        if(port !=0)
                            thisSvrAddr = new ServerAddress(thisSvrAddrStr, port);
                        else
                            thisSvrAddr = new ServerAddress(thisSvrAddrStr);
                        replicaHosts.add(thisSvrAddr);
                    }
                    m = new Mongo(replicaHosts, options);
                 }
            } catch(Exception e){
                logger.log(Level.WARNING, "", e);
                System.exit(1);
            } finally {
                WATCHDOG_ACTIVE = false;
                LOCK.unlock();
            }
        }        
    }
}
