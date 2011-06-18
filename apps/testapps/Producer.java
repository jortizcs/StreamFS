import java.io.*;
import java.net.*;
import java.util.*;

import net.sf.json.*;

public class Producer {
	String consumerIp = null;
	int consumerPort = -1;
	Socket s = null;
	String dataFile = null;
	int sendMax = 0;
	
	public Producer(){}
	
	public static void main(String[] args){
		Producer p = new Producer();
		
		if (args.length==4){
			p.consumerIp = args[0];
			p.consumerPort = Integer.parseInt(args[1]);
			p.dataFile = args[2];
			System.out.println("Datafile:" + p.dataFile);
			p.sendMax = Integer.parseInt(args[3]);
		
			try{
				if(p.consumerPort>2000) {
					if(p.consumerIp.contains(".")){
						StringTokenizer t = new StringTokenizer(p.consumerIp, ".");
						Vector<String> toks = new Vector<String>(4);
						while(t.hasMoreTokens())
							toks.add(t.nextToken());
						byte[] addr = new byte[4];
						for(int i=0; i<4; i++)
							addr[i] = (new Integer(((String)toks.get(i)))).byteValue();
						
						p.s = new Socket(InetAddress.getByAddress(addr), p.consumerPort, 
											InetAddress.getByName("localhost"), 2000);
					} else if (p.consumerIp.equals("localhost")){
							p.s = new Socket(InetAddress.getByName(p.consumerIp), p.consumerPort,
									InetAddress.getByName("localhost"), 2000);
					} else{
						System.out.println("Invalid hostname or ip address");
						System.exit(1);
					}
				} else {
					System.out.println("Port must be >2000");
					System.exit(1);
				}
				
				//populate data item
				byte[] data;
				
				FileReader freader = new FileReader(p.dataFile);
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
				
				data = jdata.toString().getBytes();
				OutputStream os = p.s.getOutputStream();
				
				while(sent<p.sendMax || p.sendMax ==0){
					os.write(data);
					os.flush();
					sent +=1;
					Thread.sleep(1000);
					
				    jdata = (JSONObject) JSONSerializer.toJSON(o.toString());
					jdataProps = jdata.getJSONObject("properties");
					jdataHead = jdataProps.getJSONObject("head");
					jdataHead.put("timestamp", sent);
					jdataProps.put("head",jdataHead);
					jdata.put("properties", jdataProps);

					data = jdata.toString().getBytes();
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		} else{
			System.out.println("java Producer [IP/Host] [port] [datafile] [maxSend]");
		}
	}
}