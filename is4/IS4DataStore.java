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
 * IS4 release version 1.1
 */

/**
 * The main class to start the Integrated Sensor-stream Storage System (IS4) 
 * 
 */
package is4;

import local.rest.*;
import local.db.*;
import java.lang.Integer;
import java.util.logging.Logger;

public class IS4DataStore{
	private static Registrar registrar = null;
	private static SubMngr subscriptionManager = null;
	public static String thisHost = null;
	public static int thisPort = -1;
	protected static transient final Logger logger = Logger.getLogger(IS4DataStore.class.getPackage().getName());

	public IS4DataStore(){
		logger.info("Instantiating IS4DataStore");
	}

	public static void main(String[] args) {
		RESTServer restServer=null;
		IS4DataStore is4DataStore = new IS4DataStore();
		DBAbstractionLayer dbLayer = new DBAbstractionLayer();

		//Maintaining a global reference to static classes
		registrar = Registrar.registrarInstance();
		subscriptionManager = SubMngr.getSubMngrInstance();

		if(args.length==2){
			Integer port = new Integer(args[1]);
			thisHost = args[0];
			thisPort = port.intValue();

			IS4DataStore.logger.info ("Starting RESTServer");
			//starting the REST Server
			restServer = new RESTServer(args[0], port.intValue());
		}
		else {
			IS4DataStore.logger.info ("Starting RESTServer");
			restServer = new RESTServer();
		}


		restServer.start();
	}
}
