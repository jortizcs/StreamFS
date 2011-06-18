package sfs.lib;

import net.sf.json.*;
import java.util.*;

public class Taxonomies {

	private static SFSLib sfs = null;
	private static String melsPath = "/is4/taxonomies/mels/";
	private String HOST = null;
	private int PORT = -1;

	private Hashtable<String, String> symlinks = new Hashtable<String, String>();
	private Hashtable<String, String> leaves = new Hashtable<String, String>();

	public Taxonomies(String host, int port){
		//sfs = new SFSLib(host, port);
		HOST = host;
		PORT = port;
		popLeaves();
	}

	public String popLeaves(){
		try {
			//symlink items
			String slItems = CurlOps.get("http://" + HOST + ":" + PORT + melsPath + "*?query=true&type=symlink");
			JSONObject slItemsObj = (JSONObject) JSONSerializer.toJSON(slItems);
			Iterator sPaths = slItemsObj.keys();
			while(sPaths.hasNext())
				symlinks.put((String)sPaths.next(),"");

			String taxAll = CurlOps.get("http://" + HOST + ":" + PORT + melsPath + "*");
			JSONObject taxAllObj  = (JSONObject) JSONSerializer.toJSON(taxAll);
			Iterator taxAllKeys = taxAllObj.keys();

			while(taxAllKeys.hasNext()){
				String tKey = (String)taxAllKeys.next();
				JSONObject getObj = taxAllObj.getJSONObject(tKey);
				JSONArray children = getObj.getJSONArray("children");
				int numChildren = children.size();
				if(numChildren==0 && !symlinks.contains(tKey)){
					StringTokenizer tokenizer = new StringTokenizer(tKey, "//");
					Vector allTokens = new Vector();
					while(tokenizer.hasMoreTokens())
						allTokens.addElement(tokenizer.nextToken());
					String tLeafName = (String) allTokens.elementAt(allTokens.size()-1);
					leaves.put(tLeafName, tKey);
					//System.out.println( tLeafName + " => "  +tKey );
				} else if (numChildren>0 && !symlinks.contains(tKey)){
					boolean allSymlinks = true;
					int j=0;
					while (j<numChildren){
						String thisChild = (String)children.get(j);
						if(!thisChild.contains("->")){
							allSymlinks = false;
							break;
						}
						j+=1;
					}

					if(allSymlinks) {
						StringTokenizer tokenizer = new StringTokenizer(tKey, "//");
						Vector allTokens = new Vector();
						while(tokenizer.hasMoreTokens())
							allTokens.addElement(tokenizer.nextToken());
						String tLeafName = (String) allTokens.elementAt(allTokens.size()-1);
						leaves.put(tLeafName, tKey);
						//System.out.println( tLeafName + " => "  +tKey );
					}
				}
			}

		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

	public String getPath(String leafname) {
		return leaves.get(leafname);
	}

	public JSONObject getAllLeavesToPath(){
		JSONObject a = new JSONObject();
		a.putAll(leaves);
		return a;
	}

	public static void main(String[] args){
		Taxonomies tax = new Taxonomies("is4server.com",8080);
		System.out.println("Other => "  + tax.getPath("Other"));
		//System.out.println(tax.getAllLeavesToPath().toString());
	}
}
