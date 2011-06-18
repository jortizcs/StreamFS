import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.lang.String;
import java.io.OutputStream;
import java.net.*;
import java.io.*;
import java.util.*;

import net.sf.json.*;
import local.json.validator.*;

public class RESTClient {

	private static int tryCount =1;
	private String fakeName = "fake";

	public RESTClient(){}

	public static void main(String[] args) {
		try {
			if(args.length==0){
				JSONObject joinReq = JSONSchemaValidator.fetchJSONObj("http://jortiz81.homelinux.com/schemas/protocols/join_request.json");
				
				//String jsonData = "{\"name\":\"jorge\",\"message\":\"hello\"}";
				String requestStr = joinReq.toString();//jsonData; 

				System.out.println(requestStr);
				//URL yahoo = new URL("http://smote.cs.berkeley.edu:8080/is4/join");
				URL yahoo = new URL("http://localhost:8080/is4/join");
				//URL yahoo = new URL("http://localhost:3429/stream_repos");
				URLConnection yc = yahoo.openConnection();
				yc.setRequestProperty("Content-Type", "application/json");
				yc.setDoOutput(true);
				OutputStreamWriter wr = new OutputStreamWriter(yc.getOutputStream());
				wr.write(requestStr);
				wr.flush();
				BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
				String inputLine;
				String wholeDoc = "";

				while ((inputLine = in.readLine()) != null) {
					System.out.println(inputLine);
					wholeDoc += inputLine;
				}
				in.close();

				RESTClient restClient = new RESTClient();
				restClient.pubDat(wholeDoc);
			} else {
				RESTClient restClient = new RESTClient();
				restClient.startPostingData(args[0], new URL(args[1]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pubDat(String wholeDoc){
		System.out.println(wholeDoc);

		JSONObject resp = (JSONObject) JSONSerializer.toJSON(wholeDoc);
		try{
			if (resp.getString("status").equalsIgnoreCase("success")){
				System.out.println("success!");
				startPostingData(resp.getString("ident"), new URL("http://localhost:8081"));
			} else {
				System.out.println("fail!");
				tryAgain();
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}

	public void startPostingData(String id, URL sfsloc){
		try{
			/*URL pub = new URL("http://smote.cs.berkeley.edu:8080/is4/pub");
			URL pub = new URL("http://localhost:8080/is4/pub?schema=hello");*/
			URL pub = new URL(sfsloc.toString() + "?type=generic&pubid="+id);
		
			while (true){
				System.out.println("Posting data to " + pub.toString());
				try{Thread.sleep(1000*2);} catch(Exception e){e.printStackTrace();}
				JSONObject fakeData = new JSONObject();
				fakeData.put("name", "data_stream");
				fakeData.put("PubId", id);
				
				JSONObject dataObj = new JSONObject();
				Random random = new Random();
				int faketemp=60 + Math.abs(random.nextInt(26));
				dataObj.put("val",new Integer(faketemp));

				fakeData.put("Data", dataObj);

				System.out.println(fakeData.toString());

				URLConnection urlConn = pub.openConnection();
				urlConn.setRequestProperty("Content-Type", "application/json");
				urlConn.setDoOutput(true);
				OutputStreamWriter wr = new OutputStreamWriter(urlConn.getOutputStream());
				wr.write(fakeData.toString());
				wr.flush();

				BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
				/*String inputLine;
				String wholeDoc = "";
				while((inputLine = in.readLine()) != null) {
					wholeDoc += inputLine;
				}
				System.out.println(wholeDoc);
				in.close();*/
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void tryAgain(){
		try {
			System.out.println("Trying again");
			tryCount+=1;
			JSONObject joinReq = JSONSchemaValidator.fetchJSONObj("http://jortiz81.homelinux.com/schemas/protocols/join_request.json");
			JSONObject objStream = joinReq.getJSONObject("object_stream");
			objStream.put("device_name", fakeName+(new Integer(tryCount)).toString());
			
			String requestStr = joinReq.toString();//jsonData; 

			System.out.println(requestStr);

			//URL yahoo = new URL("http://smote.cs.berkeley.edu:8080/is4/join");
			URL yahoo = new URL("http://localhost:8080/is4/join");
			//URL yahoo = new URL("http://localhost:3429/stream_repos");
			URLConnection yc = yahoo.openConnection();
			yc.setRequestProperty("Content-Type", "application/json");
			yc.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(yc.getOutputStream());
			wr.write(requestStr);
			wr.flush();
			BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			String inputLine;
			String wholeDoc = "";

			while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
				wholeDoc += inputLine;
			}
			in.close();

			RESTClient restClient = new RESTClient();
			restClient.pubDat(wholeDoc);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}


}
