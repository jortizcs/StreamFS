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

package is4;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.UUID;
import java.util.ArrayList;

import local.rest.proxy.ProxyInternal;
import java.io.Serializable;

/**
 *  Internally used, subscriber object.
 */
public class Subscriber implements Serializable{
	private UUID subscriberID = null;
	private URL url_ = null;
	private String subname = null;
	private List<UUID> streams_ = (List<UUID>)new ArrayList<UUID>();
	private ProxyInternal proxy = null;

	private transient final Logger logger = Logger.getLogger(Subscriber.class.getPackage().getName());


	public Subscriber(String name, String subId, URL url, List<String> streams){
		subname = name;
		url_ = url;
		setup(subId, streams);
		logger.info("Created Subscriber: (" + name + ", " + subId + ", " + 
				url.toString() + ", " + streams + ")");
	}

	public Subscriber(String subId, URL url, List<String> streams){
		url_ = url;
		setup(subId, streams);
		logger.info("Created Subscriber: (" + subId + ", " + 
				url.toString() + ", " + streams + ")");
	}

	public Subscriber(String subId, URL proxyUrl, List<String> streams, boolean proxyEnabled) throws NullPointerException{
		setup(subId, streams);

		//start the proxy process
		//System.out.println("Starting Proxy");
		logger.info("Starting Proxy for subscriber " + subId);
		try{
			proxy = new ProxyInternal(proxyUrl);
			//proxy.start();
			
			//Runtime runtime = Runtime.getRuntime();
			String javahome = System.getenv("JAVAHOME");
			String HOST = proxyUrl.getHost();
			int PORT = (proxyUrl.getPort()>0)?proxyUrl.getPort():80;
			logger.info(javahome + " local.rest.proxy.Proxy " + HOST + " " + PORT );
			//runtime.exec(javahome + " local.rest.proxy.Proxy " + HOST + " " + PORT );
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	public static void main(String[] args){
		try{
			ArrayList<String> streams = new ArrayList<String>();
			for(int i=0; i<10; i++)
				streams.add(UUID.randomUUID().toString());

			Subscriber testSub1 = new Subscriber("test1", UUID.randomUUID().toString(),new URL("http://localhost:8082"), streams);
			Subscriber testSub2 = new Subscriber(UUID.randomUUID().toString(), new URL("http://localhost:8083"), streams);
			Subscriber testSub3 = new Subscriber(UUID.randomUUID().toString(), new URL("http://localhost:8083"), streams, true);

			System.out.println("testSub1:\n" + testSub1.toString());
			System.out.println("\n\ntestSub2:\n" + testSub2.toString());
			System.out.println("\n\ntestSub3:\n" + testSub3.toString());
			Thread.sleep(1000 * 10);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public String toString(){
		StringBuffer objStrBuf = new StringBuffer();
		objStrBuf.append("[name=");
		if(subname!=null)
			objStrBuf.append(subname);
		else
			objStrBuf.append("none");

		objStrBuf.append(",\nident=").append(subscriberID);

		//URL
		objStrBuf.append(",\nurl=");
		if(url_!=null)
			objStrBuf.append(url_.toString());
		else
			objStrBuf.append("none");

		//publisher/stream information
		objStrBuf.append(",\nstream_ids=[");
		if(streams_.size()>0){
			for(int i=0; i<streams_.size(); i++) {
				objStrBuf.append("\n\t").append(streams_.get(i));
				if(i<streams_.size()-1)
					objStrBuf.append(",");
			}
		} else {
			objStrBuf.append("null");
		}
		objStrBuf.append("\n\t],\n");

		//Proxy Information
		objStrBuf.append("ProxyEnabled=");
		if(proxy != null){
			objStrBuf.append("true");
			objStrBuf.append("\n,Proxy URL=").append(proxy.getProxyUrl().toString());
		}
		else {
			objStrBuf.append("false");
		}

		//close it out and return the string
		objStrBuf.append("\n];");
		return objStrBuf.toString();
	}

	private void setup(String subId, List<String> streams){
		int k=-1;
		try {
			subscriberID = UUID.fromString(subId);
			for(int i=0; i<streams.size(); i++) {
				k=i;
				streams_.add(UUID.fromString(streams.get(i)));
				logger.info("Subscriber: " + subId + ", added stream: " + streams.get(i));
			}
		} catch(IllegalArgumentException e){
			String msg=null;
			if (k<0){
				msg = "Could not create Subscriber object; Subscriber id " + subId + " is an invalid UUID";
				logger.log(Level.WARNING, msg, e);
			} else{
				msg = "Could not create Subscriber object; Stream id " + streams.get(k) + " is an invalid UUID";
				logger.log(Level.WARNING, msg, e);
			}
		}
		catch(Exception e){
			logger.log(Level.SEVERE, "Unknown error has occurred", e);
		}
	}

	public String getName(){
		return subname;
	}

	public void setName(String name){
		subname = name;
	}

	public URL getSubUrl(){
		return url_;
	}

	public void setSubUrl(URL url){
		url_ = url;
	}

	public List<String> getSubStreamIds(){
		ArrayList<String> strms = new ArrayList<String>();
		logger.info("getSubStreamIds: streams_.size()=" + streams_.size());
		for (int i=0; i<streams_.size(); i++)
			strms.add(streams_.get(i).toString());
		logger.info("returning: " + strms);
		return strms;
	}

	public String getSubId(){
		return subscriberID.toString();
	}

	public void addStream(String pid){
		try {
			streams_.add(UUID.fromString(pid));
		} catch (IllegalArgumentException e){
			logger.log(Level.WARNING, "Could not add stream id " + pid + "; Invalid UUID",e);
		}
	}

	public void removeStream(String pid){
		try { 
			streams_.remove(UUID.fromString(pid));
		} catch (IllegalArgumentException e){
			logger.log(Level.WARNING, "Could not remove stream id " + pid + "; Invalid UUID", e);
		}

	}

	//Proxy Functions
	/**
	 * Sets the internal proxy for this subscriber.
	 */
	public void setProxyBuffer(ProxyInternal p){
		proxy=p;
	}

	/**
	 *  Unsets the internal proxy for this subscriber.
	 */
	public void unsetProxyBuffer(){
		proxy.stop();
		proxy=null;
	}

	public boolean usesProxy(){
		if(proxy!=null)
			return true;
		return false;
	}

	public URL getProxyUrl(){
		return proxy.getProxyUrl();
	}


}
