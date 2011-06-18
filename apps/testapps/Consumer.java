import java.io.*;
import java.net.*;
import net.sf.json.*;
import org.mozilla.javascript.*;

public class Consumer {
	public Consumer(){}
	
	public static void main(String[] args){
		
		Context cx = Context.enter();
		try{
			Scriptable scope = cx.initStandardObjects();
			
			//get the processing script
			String pScriptStr = Consumer.readFileAll("processor.js");
			String pScriptObjStr = "var pscript="+pScriptStr+"; pscript";
			Scriptable pScript = (Scriptable) cx.evaluateString(scope,pScriptObjStr, "<pscript>", 1, null);
			
			int idx =0;
			int bufferSize = (new Integer(pScript.get("winsize", pScript).toString())).intValue();
			JSONArray dataBuffer = new JSONArray();
			
			ServerSocket serverSock  = new ServerSocket(Integer.parseInt(args[0]));	
			Socket s = serverSock.accept();
			
			InputStream is = s.getInputStream();
			while(true){
				byte[] barray=new byte[600];
				int r= is.read(barray);
				if(r<=0)
					System.exit(1);
				
				String dataStr = new String(barray);
				JSONObject dataJsonObj = (JSONObject) JSONSerializer.toJSON(dataStr);
				dataBuffer.add(dataJsonObj);
				idx+=1;
				if(idx==bufferSize){
					Consumer.processIt(cx, scope, pScriptStr, dataBuffer);
					dataBuffer.clear();
					idx=0;
					dataBuffer.add(dataJsonObj);
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		} finally{
			Context.exit();
		}
	}
	
	public static String readFileAll(String filename) throws IOException{
		FileReader freader = new FileReader(filename);
		BufferedReader breader = new BufferedReader(freader);
		String line = breader.readLine();
		StringBuffer o=new StringBuffer();
		while(line != null){
			line = line.trim();
			o.append(line);
			line=breader.readLine();
		}
		
		return o.toString();
	}
	
	
	public static void processIt(Context cx, Scriptable scope, String process, JSONArray buffer){
		String e = "var buf = "+buffer.toString()+"; var p="+process+"; p.agg(buf)";
		//System.out.println("var buf = "+buffer.toString()+";\nvar p="+process+";\n\np.select(buf)");
		Scriptable procItScript = (Scriptable) cx.evaluateString(scope,e, "<procitscript>", 1, null);
		Object[] ids = procItScript.getIds();
		JSONObject pData = new JSONObject();
		for(int k=0; k<ids.length; k++){
			String thisKey = ids[k].toString();
			String thisValue = procItScript.get(ids[k].toString(), procItScript).toString();
			pData.put(thisKey, thisValue);
		}
		System.out.println(pData.toString());
	}
}






