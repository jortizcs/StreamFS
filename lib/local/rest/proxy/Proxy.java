/*
 * "Copyright (c) 2010-11 The Regents of the University  of California. 
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Author:  Jorge Ortiz (jortiz@cs.berkeley.edu)
 * IS4 release version 1.0
 */

/**
 *  Proxy process that is spawn by the subscription handler if the subscriber
 *  prefers a pull data interface.  Serves as a json data object buffere between
 *  IS4 and the pull-oriented subscriber.
 */

package local.rest.proxy;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.net.*;
import com.sun.net.httpserver.*;
import java.util.concurrent.Executors;

import net.sf.json.*;

public class Proxy implements Runnable,Serializable{
	private Vector<JSONObject> buffer = new Vector<JSONObject>();
	private String HOST = null;
	private int PORT = -1;
	protected HttpServer httpServer = null;
	private static boolean waitThreadActive = false;

	public Proxy(String host, int port){
		HOST = host;
		PORT = port;
	}

	public static void main(String[] args) {
		if(args.length ==2){
			Integer port = new Integer(args[1]);
			Proxy s = new Proxy(args[0], port.intValue());
			s.setup(s.HOST, s.PORT);
		} else{
			System.out.println("java local.rest.proxy.Proxy <address> <port>");
		}
	}

	public void run(){
		try{
			Runtime runtime = Runtime.getRuntime();
			String javahome = System.getenv("JAVAHOME");
			if(javahome != null && !javahome.equals(""))
				javahome = "java";
			runtime.exec(javahome + " local.rest.proxy.Proxy " + HOST + " " + PORT);
			System.out.println("java local.rest.proxy.Proxy " + HOST + " " + PORT);
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("Error in run()");
		}
	}

	public void setup(String bindAddress, int port){
		try {
			InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(bindAddress), port);
			System.out.println("Binding to " + addr);
			httpServer = HttpServer.create(addr, 0);

			PutDataHandler putDataHandler = new PutDataHandler("/buffer/put");
			httpServer.createContext("/buffer/put", putDataHandler);

			GetDataHandler getDatahandler = new GetDataHandler("/buffer");
			httpServer.createContext("/buffer", getDatahandler);

			KillHandler killHandler = new KillHandler("/stop");
			httpServer.createContext("/stop", killHandler);
		
			//start proxy server process
			httpServer.setExecutor(Executors.newCachedThreadPool());
			httpServer.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class PutDataHandler implements HttpHandler{
		private String resource = null;

		public PutDataHandler(String resourcePath){
			resource = resourcePath;
		}

		public void handle(HttpExchange exchange) throws IOException{
			if(exchange.getRequestMethod().equalsIgnoreCase("PUT") ||
				exchange.getRequestMethod().equalsIgnoreCase("POST")){
				try{
					BufferedReader input = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
					String line = null;
					StringBuffer docBuf = new StringBuffer();
					while((line=input.readLine()) !=null){
						docBuf.append(line);
					}
					input.close();
					JSONObject dataObj = (JSONObject) JSONSerializer.toJSON(docBuf.toString());
					buffer.addElement(dataObj);

					//get the response headers
					Headers responseHeaders = exchange.getResponseHeaders();
					responseHeaders.set("Content-Type", "text/plain");
					exchange.sendResponseHeaders(200, 0);
					exchange.close();
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	public void sendDataBuffer(HttpExchange exchange){
		try{
			JSONArray data = new JSONArray();
			data.addAll((Collection<JSONObject>) buffer);

			//get the response headers, populate, and send
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0);

			JSONObject dataObjs= new JSONObject();
			dataObjs.put("BufferedData",data);

			//get the response body, populate, and send
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write(dataObjs.toString().getBytes());
			responseBody.close();
			buffer.clear();
			exchange.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public class GetDataHandler implements HttpHandler{
		private String resource = null;

		public GetDataHandler(String resourcePath){
			resource = resourcePath;
		}

		public void handle(HttpExchange exchange) throws IOException{
			if(exchange.getRequestMethod().equalsIgnoreCase("GET")){
				try{
					if(buffer.size()==0){
						if(!waitThreadActive) {
							System.out.println("Buffer Empty; Waiting to send.");
							Timer timer = new Timer();
							TimerTask dumpBufferTask = (TimerTask)(new DumpBufferTimerTask(exchange));
							timer.schedule(dumpBufferTask, (long) 1000*10);
							waitThreadActive = true;
						}
					}else{
						sendDataBuffer(exchange);
					}
				} catch(Exception e){
					e.printStackTrace();
				}
			}
			else{
				System.out.println("Heard something");
			}
		}
	}

	public class KillHandler implements HttpHandler{
		private String resource = null;

		public KillHandler(String resourcePath){
			resource = resourcePath;
		}

		public void handle(HttpExchange exchange) throws IOException{
			try {
				if(exchange.getRequestMethod().equalsIgnoreCase("GET")){ 
					//get the response headers, populate, and send 
					Headers responseHeaders = exchange.getResponseHeaders();
					responseHeaders.set("Content-Type", "text/plain");
					exchange.sendResponseHeaders(200, 0);
					exchange.close();
					httpServer.stop(1);
					Thread.sleep(1500);
					System.exit(0);
				}
			} catch (Exception e){
				e.printStackTrace();
				System.out.println("Error while trying to sleep berfore dying");
			}
		}
	}

	public class DumpBufferTimerTask extends TimerTask{
		private HttpExchange exchange = null;

		public DumpBufferTimerTask(HttpExchange thisExchange){
			exchange = thisExchange;
		}

		public void run(){
			
			if(buffer.size()==0) {
				//Keep reset timerTask
				System.out.println("Buffer Empty; Waiting to send.");
				Timer timer = new Timer();
				//TimerTask dumpBufferTask = (TimerTask)(new DumpBufferTimerTask(exchange));
				//timer.schedule(dumpBufferTask, (long)1000*2);
				timer.schedule(this, (long)1000*2);
			}
			else {
				sendDataBuffer(exchange);
				waitThreadActive = false;
			}
		}
	}


}
