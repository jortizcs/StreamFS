package sfs.lib;

import java.net.*;
import java.io.*;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;

public class HttpOps {
	
	private static transient final Logger logger = Logger.getLogger(HttpOps.class.getName());
	
	public HttpOps(){
		
	}
	
	/*
	 *	The first element of the array is the response code, the second element is the body of the response.
	 */
	public static Vector<Object> get(URL url){
		Vector<Object> respvec = new Vector<Object>(2);
		try{
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(5000);
			conn.connect();

			//GET reply
			respvec.add(new Integer(conn.getResponseCode()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer lineBuffer = new StringBuffer();
			String line = null;
			while((line = reader.readLine()) != null)
				lineBuffer.append(line);
			line = lineBuffer.toString();
			reader.close();
			
			respvec.add(line);
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			return null;
		}
		return respvec;
	}
	
	public static Vector<Object> put(URL url, String data) {
		Vector<Object> respvec = new Vector<Object>(2);
		try{
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("PUT");
			conn.setConnectTimeout(5000);
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
			out.write(data);
			out.close();
			
			//GET reply
			respvec.add(new Integer(conn.getResponseCode()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer lineBuffer = new StringBuffer();
			String line = null;
			while((line = reader.readLine()) != null)
				lineBuffer.append(line);
			line = lineBuffer.toString();
			reader.close();
			
			respvec.add(line);
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			return null;
		}
		return respvec;
	}
	
	public static Vector<Object> post(URL url, String data){
		Vector<Object> respvec = new Vector<Object>(2);
		try{
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(5000);
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
			out.write(data);
			out.close();
			
			//GET reply
			respvec.add(new Integer(conn.getResponseCode()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer lineBuffer = new StringBuffer();
			String line = null;
			while((line = reader.readLine()) != null)
				lineBuffer.append(line);
			line = lineBuffer.toString();
			reader.close();
			
			respvec.add(line);
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			return null;
		}
		return respvec;
	}
	
	public static Vector<Object> delete(URL url){
		Vector<Object> respvec = new Vector<Object>(2);
		try{
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setRequestMethod("DELETE");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.connect();

			//GET reply
			respvec.add(new Integer(conn.getResponseCode()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer lineBuffer = new StringBuffer();
			String line = null;
			while((line = reader.readLine()) != null)
				lineBuffer.append(line);
			line = lineBuffer.toString();
			reader.close();
			
			respvec.add(line);
			
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
			return null;
		}
		return respvec;
	}
}

