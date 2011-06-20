package is4.stats;

import net.sf.json.*;
import com.mongodb.*;
import org.bson.BasicBSONObject;
import java.util.Map;
import java.util.Date;

public class SFSStatistics {
	private static long bytesSent=0;
	private static long bytesWrittenToDB=0;
	private static long bytesReceived=0;
	private static long startTime = 0;
	private static long docsReceived=0;
	private static long docsSent=0;

	private static long getCnt = 0;
	private static long putCnt = 0;
	private static long postCnt = 0;
	private static long deleteCnt = 0;

	private static Mongo mongo = null;

	private static SFSStatistics sfsStats = null;

	private SFSStatistics(Mongo m){
		if(m != null)
			mongo = m;
		Date d = new Date();
		startTime = (d.getTime())/1000;
		d=null;
	}

	public static SFSStatistics getInstance(Mongo m){
		if(sfsStats == null){
			sfsStats = new SFSStatistics(m);
		}
		return sfsStats;
	}

	public static void docReceived(String docStr){
		if(docStr ==null)
			return;
		int docByteLength = docStr.getBytes().length;
		bytesReceived += docByteLength;
		docsReceived +=1;
	}

	public static void docWrittenToDB(JSONObject doc){
		if(doc==null)
			return;
		int docByteLength = doc.toString().getBytes().length;
		bytesWrittenToDB += docByteLength;
	}

	public static void docSent(String docStr){
		if(docStr==null)
			return;
		int docByteLength = docStr.getBytes().length;
		bytesSent += docByteLength;
		docsSent += 1;
	}

	public static void incGet(){
		getCnt +=1;
	}

	public static void incPut(){
		putCnt +=1;
	}

	public static void incPost(){
		postCnt +=1;
	}

	public static void incDelete(){
		deleteCnt +=1;
	}

	public static long getGetCnt(){
		return getCnt;
	}

	public static long getPutCnt(){
		return putCnt;
	}

	public static long getPostCnt(){
		return postCnt;
	}

	public static long getDeleteCnt(){
		return deleteCnt;
	}

	public static double getAvgGetRate(){
		Date d = new Date();
		long timeElapsed = (d.getTime()/1000)-startTime;
		return (double)getCnt/(double)timeElapsed;
	}

	public static double getAvgPutRate(){
		Date d = new Date();
		long timeElapsed = (d.getTime()/1000)-startTime;
		return (double)putCnt/(double)timeElapsed;
	}

	public static double getAvgPostRate(){
		Date d = new Date();
		long timeElapsed = (d.getTime()/1000)-startTime;
		return (double)postCnt/(double)timeElapsed;
	}

	public static double getAvgDeleteRate(){
		Date d = new Date();
		long timeElapsed = (d.getTime()/1000)-startTime;
		return (double)deleteCnt/(double)timeElapsed;
	}

	public static double avgIncomingDataRate(){
		Date d = new Date();
		long timeElapsed = (d.getTime()/1000)-startTime;
		return (double)bytesReceived/(double)timeElapsed;
	}

	public static double avgOutgoingDataRate(){
		Date d = new Date();
		long timeElapsed = (d.getTime()/1000)-startTime;
		return (double)bytesSent/(double)timeElapsed;
	}

	public static JSONObject getDBStats(){
		JSONObject obj = new JSONObject();
		if(mongo == null)
			return obj;
		DB db = mongo.getDB("is4_data_repos");
		if (db==null)
			return obj;

		BasicBSONObject statsObj = (BasicBSONObject)db.getStats();
		if(statsObj.containsKey("raw")){
			statsObj.removeField("raw");
		}

		statsObj.removeField("numExtents");
		statsObj.removeField("indexes");
		statsObj.removeField("indexSizes");
		statsObj.removeField("fileSize");
		statsObj.removeField("ok");

		Map statsObjMap = statsObj.toMap();
		obj.accumulateAll(statsObjMap);

		return obj;
	}

}
