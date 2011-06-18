import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import net.sf.json.*;
import org.mozilla.javascript.*;

public class ThreadedConsumer extends Thread {
	private Pipe.SourceChannel source = null;
	
	public ThreadedConsumer(Pipe.SourceChannel sourcechannel){
		source = sourcechannel;
	}
	
	public void run(){		
		System.out.println("consumer started");
		Context cx = Context.enter();
		try{
			Scriptable scope = cx.initStandardObjects();
			
			//get the processing script
			String pScriptStr = this.readFileAll("processor.js");
			String pScriptObjStr = "var pscript="+pScriptStr+"; pscript";
			Scriptable pScript = (Scriptable) cx.evaluateString(scope,pScriptObjStr, "<pscript>", 1, null);
			
			int idx =0;
			int bufferSize = (new Integer(pScript.get("winsize", pScript).toString())).intValue();
			JSONArray dataBuffer = new JSONArray();
			
			boolean run = true;
			while(run && source.isOpen()){
				//System.out.println("here1:  " + source.isOpen());
				ByteBuffer sizebuf = ByteBuffer.allocate(4);
				int r=source.read(sizebuf);
				sizebuf.rewind();
				//System.out.println("here2");
				
				if(r<=0){
					System.err.println("Exiting 1");
					System.exit(1);
				}
				
				int size = sizebuf.getInt();
				if(size<65536){
					//System.out.println("Transmit size: "+size);
					ByteBuffer rdata = ByteBuffer.allocate(size);
					r=source.read(rdata);
					rdata.rewind();
				
					if(r<=0){
						System.err.println("Exiting 2");
						System.exit(1);
					}
				
					String dataStr = new String(rdata.array());
					//System.out.println("Read: " + dataStr);
				
					JSONObject dataJsonObj = (JSONObject) JSONSerializer.toJSON(dataStr);
					String op = dataJsonObj.optString("operation");
					if(op.equals("")){
						dataBuffer.add(dataJsonObj);
						idx+=1;
						//System.out.println("idx:"+idx+"\tbufferSize:"+bufferSize);
						if(idx==bufferSize){
							this.processIt(cx, scope, pScriptStr, dataBuffer);
							dataBuffer.clear();
							idx=0;
						}
					} else if(op.equalsIgnoreCase("kill")){
						run=false;
						source.close();
					}
				} else {
					source.close();
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		} finally{
			Context.exit();
		}
	}
	
	public String readFileAll(String filename) throws IOException{
		FileReader freader = new FileReader(filename);
		BufferedReader breader = new BufferedReader(freader);
		String line = breader.readLine();
		StringBuffer o=new StringBuffer();
		while(line != null){
			line = line.trim();
			o.append(line);
			line=breader.readLine();
		}
		freader.close();
		return o.toString();
	}
	
	
	public void processIt(Context cx, Scriptable scope, String process, JSONArray buffer){
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






