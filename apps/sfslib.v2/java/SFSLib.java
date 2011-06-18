import java.io.*;
import java.net.*;
import net.sf.json.*;
import java.util.Vector;

public class SFSLib{
	private static String host = null;
	private static int port = -1;
	private static URL sfsurl = null;
	
	public SFSLib(String h, int p){
		try {
			host = h;
			port = p;
			sfsurl= new URL("http://"+h+":"+port);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void print(String s){
		System.out.println(s);
	}
	
	public static void main(String[] args){
		SFSLib sfslib = new SFSLib("is4server.com", 8080);
		if(sfslib.exists("/is4/feeds/")){
			JSONObject nowObj = sfslib.getSFSTime();
			long now = nowObj.getLong("Now");
			print("### Testing System time ###");
			print(new String("Now="+now));
			print("\n###Testing ts range query 1###");
			JSONObject qresults = sfslib.tsRangeQuery(
											"/is4/feeds/Dent_5PA_elt-E_Circuits_8_10-12_WP53/A_sensor_apparent_pf",
											now-60, false, 
											now, false);
			print(new String("qresults: " + qresults.toString()));
		}
	}
	
	public Vector<Object> mkrsrc(String path, String name, String type){
		JSONObject request = new JSONObject();
		if(type.equalsIgnoreCase("default")){
			request.put("operation", "create_resource");
			request.put("resourceName", name);
			request.put("resourceType", type);
		} else if(type.equalsIgnoreCase("devices")){
			request.put("operation", "create_resource");
			request.put("resourceName", name);
			request.put("resourceType", type);
		} else if(type.equalsIgnoreCase("device")){
			request.put("operation", "create_resource");
			request.put("resourceName", name);
			request.put("deviceName", name);
			request.put("resourceType", type);
		} else if(type.equalsIgnoreCase("genpub")){
			request.put("operation", "create_generic_resource");
			request.put("resourceName", name);
		}
		
		try {
			URL u = new URL(sfsurl.toString() + path);
			return HttpOps.put(u, request.toString());
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public  Vector<Object> mksmappub(String path, URL smapurl){
		JSONObject request = new JSONObject();
		request.put("operation", "create_smap_publisher");
		request.put("smap_urls", smapurl.toString());
		
		try {
			URL u = new URL(sfsurl.toString() + path);
			return HttpOps.put(u, request.toString());
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public Vector<Object> overwriteProps(String path, String propsStr){
		JSONObject request = new JSONObject();
		request.put("operation", "overwrite_properties");
		JSONObject props = null;
		try {
			props = (JSONObject)JSONSerializer.toJSON(propsStr);
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
		request.put("properties", props);
		try {
			URL u = new URL(sfsurl.toString() + path);
			return HttpOps.put(u, request.toString());
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public Vector<Object> updateProps(String path, String propsObj){
		JSONObject request = new JSONObject();
		request.put("operation", "update_properties");
		JSONObject props = null;
		try {
			props = (JSONObject)JSONSerializer.toJSON(propsObj);
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
		request.put("properties", props);
		try {
			URL u = new URL(sfsurl.toString() + path);
			return HttpOps.put(u, request.toString());
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean exists(String path){
		try {
			String url = new String(sfsurl.toString() + path);
			URL u = new URL(url);
			Vector<Object> v = HttpOps.get(u);
			if(v != null && v.size()>0)
				return true;
		} catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	public JSONObject tsQuery(String path, long timestamp){
		try {
			String url = new String(sfsurl.toString() + path + "?query=true&ts_timestamp=" + timestamp);
			URL u = new URL(url);
			Vector v = HttpOps.get(u);
			if(v!=null)
				return (JSONObject)JSONSerializer.toJSON(v.get(1));
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONObject tsRangeQuery(String path, long tslowerbound, boolean includelb, 
									long tsupperbound, boolean includeub){
		String queryParams = "?query=true&";
		if(includelb){
			queryParams = new String(queryParams+"ts_timestamp=gte:"+tslowerbound);
		} else {
			queryParams = new String(queryParams+"ts_timestamp=gt:"+tslowerbound);
		}
		
		if(includeub){
			queryParams =  new String(queryParams+",lte:"+tsupperbound);
		} else {
			queryParams = new String (queryParams+",lt:"+tsupperbound);
		}
		
		try {
			String url = new String(sfsurl.toString() + path + queryParams);
			URL u = new URL(url);
			Vector<Object> v = HttpOps.get(u);
			
			if(v!= null){
				return (JSONObject)JSONSerializer.toJSON(v.get(1));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONObject tsNowRangeQuery(String path, long tslowerbound, boolean includelb, 
									long tsupperbound, boolean includeub){
		String queryParams = "?query=true&";
		if(includelb){
			queryParams = new String(queryParams+"ts_timestamp=gte:"+tslowerbound);
		} else {
			queryParams = new String(queryParams+"ts_timestamp=gt:"+tslowerbound);
		}
		
		if(includeub){
			queryParams =  new String(queryParams+",lte:"+tsupperbound);
		} else {
			queryParams = new String (queryParams+",lt:"+tsupperbound);
		}
		
		try {
			String url = new String(sfsurl.toString() + path + queryParams);
			URL u = new URL(url);
			Vector v= HttpOps.get(u);
			if(v!=null)
				return (JSONObject)JSONSerializer.toJSON(v.get(1));
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONObject getSFSTime(){
		try {
			String url = new String(sfsurl.toString() + "/is4/time");
			URL u = new URL(url);
			Vector<Object> v = HttpOps.get(u);
			if(v!=null)
				return (JSONObject)JSONSerializer.toJSON(v.get(1));
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}