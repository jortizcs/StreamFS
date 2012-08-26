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
 * StreamFS release version 2.0
 */

package local.rest.resources;

import is4.*;
import local.db.*;
import local.rest.*;
import local.rest.smap.*;

import net.sf.json.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import javax.naming.InvalidNameException;
import java.util.zip.GZIPOutputStream;

import org.simpleframework.http.core.Container;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;

public class InfoBusResource extends GenericPublisherResource {
	private static transient final Logger logger = Logger.getLogger(InfoBusResource.class.getPackage().getName());
	private static InfoBusResource ibusRsrc = null;

    private static ConcurrentHashMap<Request, Response> eavesdroppers = new ConcurrentHashMap<Request, Response>();

	private InfoBusResource(String path) throws Exception, InvalidNameException{
		super(path, UUID.fromString("00000000-0000-0000-0000-000000000000"));
		UUID thisPubIdUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
			
		//register the publisher
		/*Registrar registrar = Registrar.registrarInstance();
		String newPubId = registrar.registerDevice(path);
		try{ 
			thisPubIdUUID = UUID.fromString(newPubId);
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}*/

		//add to publishers table
		if(database.getIs4RRPath(thisPubIdUUID) ==null)
			database.addPublisher(thisPubIdUUID, null, null, path, null);
	}
	
	public static InfoBusResource getInstance(String path){
		try {
			if(ibusRsrc == null){
				ibusRsrc = new InfoBusResource(path);
				RESTServer.addResource(ibusRsrc);
			}
		} catch (Exception e){
			logger.log(Level.WARNING, "", e);
			return null;
		}
		return ibusRsrc;
	}

	public void get(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
		/*JSONObject response = new JSONObject();
		response.put("status", "success");
		response.put("stream_type_cnt", database.publisherCount());
		sendResponse(m_request, m_response, 200, response.toString(), internalCall, internalResp);*/
        boolean error = false;
        try {
            if(sendHeaderOnly(m_response))
                eavesdroppers.put(m_request, m_response);
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            error=true;
        } finally{
            try {
                if(error && m_response!=null)
                    m_response.close();
            } catch(Exception e2){
            }
        }
	}

	public void put(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		sendResponse(m_request, m_response, 400, null, internalCall, internalResp);
	}

	public void post(Request m_request, Response m_response, String path, String data, boolean internalCall, JSONObject internalResp){
		put(m_request, m_response, path, data, internalCall, internalResp);
	}

	public void delete(Request m_request, Response m_response, String path, boolean internalCall, JSONObject internalResp){
		sendResponse(m_request, m_response, 400, null, internalCall, internalResp);
	}

    public boolean sendHeaderOnly(Response m_response){
        try {
            long time = System.currentTimeMillis();
            
            m_response.set("Content-Type", "application/json");
            m_response.set("Server", "StreamFS/2.0 (Simple 4.0)");
            m_response.set("Connection", "close");
            m_response.setDate("Date", time);
            m_response.setDate("Last-Modified", time);
            m_response.setCode(200);
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
            return false;
        }
        return true;
    }

    public static void dataReceived(JSONObject data){
        Iterator<Request> keys = eavesdroppers.keySet().iterator();
        boolean error = false;
        Request m_request = null;
        Response m_response = null;
        while(keys.hasNext()){
            m_request = keys.next();
            m_response = eavesdroppers.get(m_request);
            if(m_response!=null){
                error = false;
                
                GZIPOutputStream gzipos = null; 
                String enc = m_request.getValue("Accept-encoding");
                boolean gzipResp = false;
                if(enc!=null && enc.indexOf("gzip")>-1)
                    gzipResp = true;

                PrintStream body = null;
                try{
                    body = m_response.getPrintStream();
                    if(data!=null && !gzipResp)
                        body.println(data);
                    else if(data!=null && gzipResp){
                        m_response.set("Content-Encoding", "gzip");
                        gzipos = new GZIPOutputStream((OutputStream)body);
                        gzipos.write(data.toString().getBytes());
                        gzipos.close();
                    }
                } catch(Exception e) {
                    error = true;
                    logger.log(Level.WARNING, "",e);
                } finally {
                    if(body!=null && error==true)
                        body.close();
                    else if(body!=null)
                        body.flush();
                }
            }
        }
    }

}
