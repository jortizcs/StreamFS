import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

import net.sf.json.*;

public class ThreadedProducer extends Thread {
	private String dataFile = null;
	private int sendMax = 0;
	private Pipe.SinkChannel t = null;
	
	public ThreadedProducer(String datafile, int sendmax, Pipe.SinkChannel sink){
		this.dataFile = datafile;
		this.sendMax = sendmax;
		this.t = sink;
	}
	
	public void run(){
		try{
			System.out.println("Producer started");
			//populate data item
			byte[] data;
			
			FileReader freader = new FileReader(dataFile);
			BufferedReader breader = new BufferedReader(freader);
			String line = breader.readLine();
			StringBuffer o=new StringBuffer();
			while(line != null){
				line = line.trim();
				o.append(line);
				line=breader.readLine();
			}
			
			int sent=1;
			JSONObject jdata = (JSONObject) JSONSerializer.toJSON(o.toString());
			JSONObject jdataProps = jdata.getJSONObject("properties");
			JSONObject jdataHead = jdataProps.getJSONObject("head");
			jdataHead.put("timestamp", sent);
			jdataProps.put("head",jdataHead);
			jdata.put("properties", jdataProps);
			
			byte[] fdata = jdata.toString().getBytes();
			int bufferLength = fdata.length + 4;
			//System.out.println("bufferLength:" + bufferLength);
			data= new byte[bufferLength];
			ByteBuffer dataByteBuf = ByteBuffer.wrap(data);
			dataByteBuf = dataByteBuf.putInt(bufferLength);
			/*System.out.println("written:"+dataByteBuf.position() + ", capacity:" + dataByteBuf.capacity() + 
							" remaining:" + dataByteBuf.remaining() + ", length: " + fdata.length);*/
			dataByteBuf=dataByteBuf.put(fdata);
						
			while(sent<=sendMax || sendMax ==0 && t.isOpen()){
				dataByteBuf.rewind();
				//System.out.println("source.remaining: " + dataByteBuf.remaining());
				int v = t.write(dataByteBuf);
				//System.out.println("Producer wrote: "  +v + "\tdataByteBuf.size="+dataByteBuf.array().length);
				sent +=1;
				this.sleep(1000);
				
				
				//update sent attribute value
				jdata = (JSONObject) JSONSerializer.toJSON(o.toString());
				jdataProps = jdata.getJSONObject("properties");
				jdataHead = jdataProps.getJSONObject("head");
				jdataHead.put("timestamp", sent);
				jdataProps.put("head",jdataHead);
				jdata.put("properties", jdataProps);

				fdata = jdata.toString().getBytes();
				bufferLength = fdata.length + 4;
				data= new byte[bufferLength];
				dataByteBuf = ByteBuffer.wrap(data);
				dataByteBuf.putInt(bufferLength);
				dataByteBuf.put(fdata);
			}
			
			//send kill op to producer
			dataByteBuf.rewind();
			dataByteBuf.clear();
			JSONObject c = new JSONObject();
			c.put("operation", "kill");
			dataByteBuf = null;
			dataByteBuf = ByteBuffer.wrap(c.toString().getBytes());
			dataByteBuf.rewind();
			int v = t.write(dataByteBuf);
			this.yield();
			t.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}