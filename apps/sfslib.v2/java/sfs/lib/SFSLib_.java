package sfs.lib;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SFSLib{
	
	public static void main(String [ ] args) throws IOException 
	{
		test();
	}
	
	private static void test() throws IOException {
	 	SFSLib sfs = new SFSLib("jortiz81.homelinux.com", "8081");
	 	
	 	// Test 1 works:
	 	// System.out.println(sfs.mkrsrc("/is4/buildings/jeffApt", "closet", "default")); 
	 	
	 	// Test 2 works:
	 	// System.out.println(sfs.mksymlink("/is4/buildings/jeffApt", "/is4/buildings/jorgeApt", "symlinkJorgeApt"));
	 	// System.out.println(sfs.mksymlink("/is4/buildings/jeffApt", "http://jortiz81.homelinux.com:8081/is4/buildings/jorgeApt", "symlinkJorgeApt2"));
	 	
	 	// Test 3 works? [Received an OK]:
	 	// System.out.println(sfs.mksmappub("/is4/buildings/jeffApt", "http://www.jeffhsu.com"));
	 	
	 	// Test 4 works:
        // JSONObject jsonObj1 = new JSONObject();
        // jsonObj1.put("Hello", "World");
        // System.out.println(sfs.overwriteProps("/is4/buildings/jeffApt", jsonObj1.toString()));
	 	
	 	// Test 5 works:
        // JSONObject jsonObj2 = new JSONObject();
        // jsonObj2.put("Bye", "World");
        // System.out.println(sfs.updateProps("/is4/buildings/jeffApt", jsonObj2.toString()));
	}

	private static String host;
	private static String port;
	
	public SFSLib (String hostString, String portString){
		host = hostString;
		port = portString;
	}
	
	public String mkrsrc(String path, String name, String type) throws IOException {
		JSONObject jsonObj = new JSONObject();
		if (type.equals("default")) {
			jsonObj.put("operation", "create_resource");
			jsonObj.put("resourceName", name);
			jsonObj.put("resourceType", type);
		} else if (type.equals("devices")) {
			jsonObj.put("operation", "create_resource");
			jsonObj.put("resourceName", "devices");
			jsonObj.put("resourceType", type);
		} else if (type.equals("device")) {
			jsonObj.put("operation", "create_resource");
			jsonObj.put("resourceName", name);
			jsonObj.put("deviceName", name);
			jsonObj.put("resourceType", "default");
		} else if (type.equals("genpub")) {
			jsonObj.put("operation", "create_generic_resource");
			jsonObj.put("resourceName", name);
		} 
		
		String url = "http://" + host + ":" + port + path;
		return CurlOps.put (jsonObj.toString(), url);
	}
	
	public String mksymlink(String path, String target, String linkname) throws IOException{
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("operation", "create_symlink");
		jsonObj.put("name", linkname);
		if (target.startsWith("/")) {
			jsonObj.put("uri", target);
		} else {
			jsonObj.put("url", target);
		}
		String url = "http://" + host + ":" + port + path;
		return CurlOps.put (jsonObj.toString(), url);
	}
	
	public String mksmappub(String path, String smapurl) throws IOException {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("operation", "create_smap_publisher");
		jsonObj.put("smap_urls", (new JSONArray()).add(smapurl));
		String url = "http://" + host + ":" + port + path;
		return CurlOps.put (jsonObj.toString(), url);
	}

	public String overwriteProps(String path, String props) throws IOException {
		JSONObject jsonObj = new JSONObject();
		JSONObject propsObj = new JSONObject();
		jsonObj.put("operation", "overwrite_properties");
		propsObj.put("desc", props);
		jsonObj.put("properties", propsObj);
		String url = "http://" + host + ":" + port + path;
		return CurlOps.post (jsonObj.toString(), url);
	}

	public String updateProps(String path, String props) throws IOException {
		JSONObject jsonObj = new JSONObject();
		JSONObject propsObj = new JSONObject();
		jsonObj.put("operation", "update_properties");
		propsObj.put("description", props);
		jsonObj.put("properties", propsObj);
		String url = "http://" + host + ":" + port + path;
		return CurlOps.post (jsonObj.toString(), url);
	}
}


