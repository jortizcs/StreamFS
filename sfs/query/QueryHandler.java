package sfs.query;

import sfs.types.*;
import sfs.db.*;
import sfs.security.*;
import sfs.util.ResourceUtils;

import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

public class QueryHandler{

    protected static transient final Logger logger = Logger.getLogger(QueryHandler.class.getPackage().getName());
    private static QueryHandler qh =null;
    private static final JSONParser parser = new JSONParser();
    private static final MySqlDriver mysqlDB = MySqlDriver.getInstance();
    private static final MongoDBDriver mongodb = MongoDBDriver.getInstance();
    private static final ResourceUtils utils = ResourceUtils.getInstance();

    //security
    private static final SecurityManager secmngr = SecurityManager.getInstance();

    public static QueryHandler getInstance(){
        if(qh == null)
            qh = new QueryHandler();
        return qh;
    }

    public void executeQuery(Request request, Response response, String method, String path, String type, boolean internalCall, JSONObject internalResp){
        logger.info("path=" + path + "\tmethod="+ method + "\ttype=" + type);

        //if it's a secure connection, the user id must be included
        long userid = -1;
        try {
            if(request.isSecure())
                userid = request.getValue("uid").parseLong();
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            if(!internalCall){
                //method not allowed error should be returned to the user
                response.set("Allow", ""); //no methods allowed
                utils.sendResponse(request, response, 405, null, false, null);
            }
            return;
        }

        //get the operation type
        Operation op = decipherOperation(request, method);

        //toggle the right handler
        if(method.equalsIgnoreCase("get") && secmngr.hasPermission(uid, op, path)){
            if(type.equalsIgnoreCase("default")){
                Default.get(request, response, path, internalCall, internalResp);
            } else if(type.equalsIgnoreCase("stream") || type.equalsIgnoreCase("generic_publisher")){
            } else if(type.equalsIgnoreCase("control")){
            } else if(type.equalsIgnoreCase("subscription")){
            } else if(type.equalsIgnoreCase("symlink")){
            } else if(type.equalsIgnoreCase("process")){
            } else if(type.equalsIgnoreCase("process_code")){
            } else {
            }
        } else if(method.equalsIgnoreCase("put") && secmngr.hasPermission(uid, op, path)){
            if(type.equalsIgnoreCase("default")){
                try {
                    Default.put(request, response, path, request.getContent(), false, null);
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                    utils.sendResponse(request, response, 500, null, false, null);
                }
            } else if(type.equalsIgnoreCase("stream") || type.equalsIgnoreCase("generic_publisher")){
            } else if(type.equalsIgnoreCase("control")){
            } else if(type.equalsIgnoreCase("subscription")){
            } else if(type.equalsIgnoreCase("symlink")){
            } else if(type.equalsIgnoreCase("process")){
            } else if(type.equalsIgnoreCase("process_code")){
            } else{
            }
        } else if(method.equalsIgnoreCase("post")){
            if(type.equalsIgnoreCase("default")){
                //Default.post(request, response, path, false, null);
            } else if(type.equalsIgnoreCase("stream") || type.equalsIgnoreCase("generic_publisher")){
            } else if(type.equalsIgnoreCase("control")){
            } else if(type.equalsIgnoreCase("subscription")){
            } else if(type.equalsIgnoreCase("symlink")){
            } else if(type.equalsIgnoreCase("process")){
            } else if(type.equalsIgnoreCase("process_code")){
            } else{
            }
        } else if(method.equalsIgnoreCase("delete")){
            if(type.equalsIgnoreCase("default")){
                Default.delete(request, response, path, internalCall, internalResp);
            } else if(type.equalsIgnoreCase("stream") || type.equalsIgnoreCase("generic_publisher")){
            } else if(type.equalsIgnoreCase("control")){
            } else if(type.equalsIgnoreCase("subscription")){
            } else if(type.equalsIgnoreCase("symlink")){
            } else if(type.equalsIgnoreCase("process")){
            } else if(type.equalsIgnoreCase("process_code")){
            } else{
            }
        } else if(method.equalsIgnoreCase("move")){
            if(type.equalsIgnoreCase("default")){
                //Default.move(request, response, path, false, null);
            } else if(type.equalsIgnoreCase("stream") || type.equalsIgnoreCase("generic_publisher")){
            } else if(type.equalsIgnoreCase("control")){
            } else if(type.equalsIgnoreCase("subscription")){
            } else if(type.equalsIgnoreCase("symlink")){
            } else if(type.equalsIgnoreCase("process")){
            } else if(type.equalsIgnoreCase("process_code")){
            } else{
            }
        }
    }

    //evaluate query
	public void query(Request request, Response response, String path, Query qry, boolean internalCall, JSONObject internalResp){
		JSONObject resp = new JSONObject();
		JSONArray errors = new JSONArray();
        //set last_props_ts
		long last_props_ts = mysqlDB.getLastPropsTs(path);
		if(last_props_ts==0 && mongodb.getPropsHistCount(path)>0){
			logger.info("Fetching oldest properties values");
			last_props_ts = mongodb.getMaxTsProps(path);
			JSONObject propsEntry = mongodb.getPropsEntry(path, last_props_ts);
			propsEntry.remove("_id");
			propsEntry.remove("timestamp");
			propsEntry.remove("is4_uri");
            propsEntry.remove("_keywords");
			mysqlDB.rrPutProperties(path, propsEntry);
			mysqlDB.updateLastPropsTs(path, last_props_ts);
		}
		try{
			JSONObject propsQueryObj = new JSONObject();

			//get query object from input data
            String data = request.getContent();
			if(request.getMethod().equalsIgnoreCase("post") && 
                data != null && !data.equals("")){
				JSONObject dataJsonObj = (JSONObject) parser.parse(data);
                String type = mysqlDB.getRRType(request.getPath().getPath());
                int TYPE = utils.translateType(type);
				if(TYPE == utils.PUBLISHER_RSRC || TYPE == utils.GENERIC_PUBLISHER_RSRC){
                    Object pqueryobj = dataJsonObj.get("props_query");
                    if(pqueryobj != null){
                        JSONObject dataPropsQuery = (JSONObject) parser.parse(pqueryobj.toString());
                        propsQueryObj.putAll(dataPropsQuery);
                    }
				} else {
					propsQueryObj.putAll(dataJsonObj);
				}
			}

			Iterator keys = qry.keySet().iterator();
			logger.fine("REQUEST_KEYS::" + keys.toString());
			Vector<String> attributes = new Vector<String>();
			Vector<String> values = new Vector<String>();
			while(keys.hasNext()){
				String thisKey = (String) keys.next();
				logger.fine("Keys found!; thisKey=" + thisKey);
				if(thisKey.startsWith("props_")){
					String str = "props_";
					String queryKey = thisKey.substring(
                            thisKey.indexOf(str)+str.length(), thisKey.length());
                    Object queryvalueobj = qry.get(thisKey);
                    String queryValue ="";
                    if(queryvalueobj !=null)
					    queryValue = (String)queryvalueobj;

					logger.info("Query Value: " + queryValue);
					JSONObject conditions = genJSONClause(queryValue);
					logger.info("Conditions: " + conditions);
					if(conditions!=null)
                		propsQueryObj.put(queryKey, conditions);
					else
						propsQueryObj.put(queryKey, queryValue);
				} else if(thisKey.startsWith("props")){
                    Object queryValObj = qry.get(thisKey);
					String queryValue = "";
                    if(queryValObj !=null)
                        queryValue = (String)queryValObj;

					JSONObject conditions = genJSONClause(queryValue);
					if(conditions!=null)
						propsQueryObj.putAll(conditions);
					else
						logger.warning("Invalid conditions set for generic props query");
				}
			}

			logger.fine("Props Query: " + propsQueryObj.toString());

			if(!propsQueryObj.toString().equals("{}")){
				propsQueryObj.put("is4_uri", request.getPath().getPath());
				if(last_props_ts>0)
					propsQueryObj.put("timestamp", last_props_ts);
				
                logger.info("Props Query: " + propsQueryObj.toString());
				JSONObject mqResp = mongodb.queryProps(propsQueryObj.toString());
				logger.fine("mqResp: " + mqResp.toString());
				JSONArray propsRespObjArray = (JSONArray)parser.parse(mqResp.get("results").toString());
				if(propsRespObjArray.size()>0){
					JSONObject propsRespObj = (JSONObject) propsRespObjArray.get(0);
					propsRespObj.remove("is4_uri");
					propsRespObj.remove("timestamp");
					resp.putAll(propsRespObj);
				}
			} else {
				errors.add("Empty or invalid query");
				
				logger.warning(errors.toString());
				resp.put("errors", errors);
			}
        } catch (Exception e){
			logger.log(Level.WARNING, "", e);
			if(e instanceof ParseException){
				errors.add("Invalid JSON for POST data; url params ignored");
				resp.put(errors, errors);
				utils.sendResponse(request, response, 200, resp.toString(), internalCall, internalResp);
				return;
			}
		}
		utils.sendResponse(request, response, 200, resp.toString(), internalCall, internalResp);
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
	 * 		..props_label=in:val1,val2,val3&props_timestamp=gt:12345,lt:23456
	 *		..props=or:[label:one|title:two]
	 *
	 * 	, is used to separate values in an $or, $and array condition or to separate
	 *		conditions on a value.
	 * 	| is used between [] to separate conditional JSON objects
	 *
	 */
	private JSONObject genJSONClause(String clause){
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
							Date date = new Date();
							long timestamp = date.getTime()/1000;
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

    private boolean isNumber(String val){
		try {
			Long.parseLong(val);
			return true;
		} catch(Exception e){
			return false;
		}
	}

    private Operation decipherOperation(Request request, String method){

        //get request translation
        if(method.equalsIgnoreCase("get") && request.getValue("ts_timestamp")!=null){
            return Operation.EXECUTE;
        } else if(method.equalsIgnoreCase("get") && request.getValue("ts_timestamp")==null){
            return Operation.READ;
        }

        //put request translation
        else if(method.equalsIgnoreCase("put")){
            return Operation.WRITE;
        }

        //post request translation
        else if(method.equalsIgnoreCase("post")){
            return Operation.WRITE;
        }

        //delete request translation
        else if(method.equalsIgnoreCase("post")){
            if(request.getContentLength()>0){
                try {
                    JSONObject contentObj = parser.parse(request.getContent());
                    if(contentObj.containsKey("props_query")){
                        return Operation.EXECUTE;
                    }
                } catch(Exception e){
                    logger.log(Level.WARNING, "", e);
                }
            }
            return Operation.WRITE;
        }

        //move request translation
        else if(method.equalsIgnoreCase("move")){
            return Operation.WRITE;
        }
    }





}
