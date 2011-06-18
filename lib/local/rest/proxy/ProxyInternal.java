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
 *  Encapsulates information for running proxy buffers -- buffers that buffer data
 *  on behalf of pull-only subscribers.
 */

package local.rest.proxy;

import java.net.URL;
import java.lang.NullPointerException;
import java.lang.Runtime;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;

public class ProxyInternal implements Serializable{

	private String urlStr = "";
	private int port = 80;
	private URL purl;

	public static void main(String[] args){
		if(args.length==2){
			try{
				ProxyInternal p = new ProxyInternal(new URL("http://" + args[0] + ":" + args[1]));
				System.out.println("Starting proxy");
				p.start();
				Thread.sleep(1000*20);
				System.out.println("Stopping proxy");
				p.stop();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public ProxyInternal(URL proxyUrl) throws NullPointerException{
		if(proxyUrl != null){
			purl = proxyUrl;
			urlStr = proxyUrl.getHost();
			int portNo = proxyUrl.getPort();
			
			if(portNo<1024){
				port=80;
			}else{
				port=portNo;
			}

		} else {
			throw new NullPointerException();
		}
	}
	
	public boolean start(){
		try{
			Proxy p = new Proxy(urlStr, port);
			(new Thread(p)).start();
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean stop(){
		try{
			Runtime runtime = Runtime.getRuntime();
			runtime.exec("/usr/bin/curl " + "http://" + urlStr + ":" + port + "/stop");
			System.out.println("curl " + "http://" + urlStr + ":" + port + "/stop");
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public URL getProxyUrl(){
		return purl;
	}
}
