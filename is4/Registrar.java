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

import local.db.*;

import is4.*;
import is4.exceptions.*;
import net.sf.json.*;

import java.util.*;
import java.lang.String;
import java.lang.NullPointerException;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Registrar implements Serializable{

	protected static Registrar registrar = null;
	protected static Hashtable<String,JoinInfoTuple> regTable = new Hashtable<String,JoinInfoTuple>();
	protected static Hashtable<UUID,String> keyTable = new Hashtable<UUID,String>();

	protected static String IS4HOME=null;
	protected static String regObjPath = "/.state/Registrar.obj";
	protected static String regTableObjPath = "/.state/rt.obj";
	protected static String keyTableObjPath = "/.state/kt.obj";

	protected static transient final Logger logger = Logger.getLogger(Registrar.class.getPackage().getName());
	private static boolean initialized = false;

	private Registrar(){
	}

	/*
	 * Returns the single instance of the registrar object.
	 */
	public static Registrar registrarInstance() {
		//loadIs4State();
		/*File f1 = new File(regObjPath);
		File f2 = new File(regTableObjPath);
		File f3 = new File(keyTableObjPath);

		if(registrar == null && !f1.exists()) {
			registrar = new Registrar();
			//System.out.println("New Registrar");
			logger.info("New Registrar");
		} else if (f1.exists() && registrar == null) {
			try{
				FileInputStream fileIn = new FileInputStream(f1);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				registrar = (Registrar)in.readObject();
				logger.info("Previous Registrar instance saved, loading...");

				fileIn = new FileInputStream(f2);
				in = new ObjectInputStream(fileIn);
				regTable = (Hashtable<String, JoinInfoTuple>)in.readObject();

				fileIn = new FileInputStream(f3);
				in = new ObjectInputStream(fileIn);
				keyTable = (Hashtable<UUID,String>)in.readObject();

				//System.out.println("Loaded registrar");
				logger.info("Loaded registrar");
			}catch(Exception e){
				e.printStackTrace();
				logger.log(Level.SEVERE, "Exception while loading previous Registrar state", e);
				System.exit(1);
			
		}

		//System.out.println("regTable size: " + registrar.regTable.size());
		//System.out.println("keyTable size: " + registrar.keyTable.size());

		*/
		if(!initialized){
			loadIs4State();
			registrar = new Registrar();
			databaseSync();
		}

		return registrar;
	}

	protected static void loadIs4State() throws NullPointerException{
		String home = System.getenv().get("IS4HOME");
		if(IS4HOME == null && home !=null && !home.equals("")){
			IS4HOME = home;
			regObjPath = IS4HOME + regObjPath;
			regTableObjPath = IS4HOME + regTableObjPath;
			keyTableObjPath = IS4HOME + keyTableObjPath;
		} else if(IS4HOME == null && home ==null || home.equals("")){
			NullPointerException e = new NullPointerException("Registar: IS4HOME environment variable must be set!");
			logger.log(Level.SEVERE, "Environment variable not set", e);
			throw e;
		}
	}

	protected static void databaseSync(){
		try{
			JSONObject publishersJson = ((MySqlDriver)DBAbstractionLayer.database).getPublishersTable();
			Set<String> allPubIdsSet = publishersJson.keySet();
			Iterator<String> allPubIdIterator = allPubIdsSet.iterator();
			while(allPubIdIterator.hasNext()){
				String thisPubId = allPubIdIterator.next();
				UUID pubuuid = UUID.fromString(thisPubId);

				//put in keyTable
				keyTable.put(pubuuid, publishersJson.getString(thisPubId));

				//put in regTable
				Date date = new Date();
				JoinInfoTuple jit = new JoinInfoTuple(pubuuid, date.getTime()/1000);
				regTable.put(publishersJson.getString(thisPubId), jit);
			}
			//save object state 
			saveState();
			initialized = true;
			return;
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
		initialized = false;
	}

	/*
	 *  Checks if the given object is registered in the system.a
	 *
	 *  @param name the assocaite unique id received by the object or device after
	 *  		registration.
	 *
	 *  @returns true if registered, false if not.
	 */
	public boolean isRegisteredName(String name){
		logger.info("Checking if " + name + " is registered");
		checkDatabaseName(name);
		return regTable.containsKey(name);
	}

	/*
	 *  Checks if the publisher with the given registration/publisher id
	 *  is currently registered with the system.
	 */
	/*public boolean isRegistered(UUID pubId){
		//System.out.println("Checking registration");
		logger.info("Checking if publisher with pudId: " + pubId.toString() + " is registered");
		return keyTable.containsKey(pubId);
	}*/
	public boolean isRegisteredId(String pubId){
		try{
			logger.info("Checking if publisher with pudId: " + pubId.toString() + " is registered");
			checkDatabaseId(pubId);
			UUID pubIdUUID = UUID.fromString(pubId);
			return keyTable.containsKey(pubIdUUID);
		} catch(IllegalArgumentException e){
			logger.log(Level.WARNING, pubId + " is an invalid UUID", e);
			return false;
		}
	}

	/*
	 *  Unregisters the publisher with the given publisher/registration id.
	 */
	public synchronized void unregisterDevice(String regId){
		logger.info("Removing publisher: " + regId);
		try {
			UUID regIdUUID = UUID.fromString(regId);
			String name = keyTable.get(regIdUUID);
			String removedKTV = keyTable.remove(regIdUUID);
			JoinInfoTuple removedRTV  = null;
			if(name!=null){
				removedRTV=regTable.remove(name);
				if(removedRTV==null)
					logger.warning("Associated value for publisher " + regId + " is null");
				else
					logger.fine("Removing publisher " + regId + ": [" + name + ": " + removedRTV.toString() + "]");
			} else {
				logger.warning("The \"name\" associated with " + regId + " is null");
			}

			

			//save object state
			saveState();
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, "Invalid UUID format: " + regId + "\nCould not remove device", e);
			saveState();
		}
	}

	/**
 	 *  Unregisters the give publisher with the given id.
	 */
	/*public synchronized void unregisterDevice(UUID regIdUUID){
		this.unregisterDevice(regIdUUID.toString());
	}*/

	public void checkDatabaseId(String pid){
		try {
			String name = null;
			if((name = ((MySqlDriver)DBAbstractionLayer.database).getName(pid)) != null){

				//register the device
				Date date = new Date();
				UUID Pid = UUID.fromString(pid);
				JoinInfoTuple jit = new JoinInfoTuple(Pid, date.getTime()/1000);
				regTable.put(name, jit);
				keyTable.put(Pid, name);

				logger.fine("adding " + name + ":" + jit.toString());

				//save object state 
				saveState();
			}
		} catch(Exception e) {
			logger.log(Level.WARNING, "",e);
		}
	}

	public void checkDatabaseName(String name){
		try {
			String pid= null;
			if((pid= ((MySqlDriver)DBAbstractionLayer.database).getPid(name)) != null){
				UUID Pid = UUID.fromString(pid);

				//register the device
				Date date = new Date();
				JoinInfoTuple jit = new JoinInfoTuple(Pid, date.getTime()/1000);
				regTable.put(name, jit);
				keyTable.put(Pid, name);

				logger.fine("adding " + name + ":" + jit.toString());

				//save object state 
				saveState();
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "", e);
		}
	}

	/*
	 *  Register the device by name given by String parameter.  Returns the associated
	 *  unique registration id to be used for future communication.
	 */
	public synchronized String registerDevice(String name) throws NameRegisteredException, NoMoreRegistrantsException{

		/*if(regTable.containsKey(name))
			throw new NameRegisteredException();*/

		UUID newId = UUID.randomUUID();

		//make sure there's no collisions -- chances are there will be 0 collisions
		int loopMax = 10000; int loopCount=0;
		while(keyTable.containsKey(newId) && loopCount < 10000)
			newId = UUID.randomUUID();
		if(loopCount >= loopMax)
			throw new NoMoreRegistrantsException();
		//keyTable.put(newId, name);
		/////////////////////////////////////////////////////////////////////////////

		//register the device
		Date date = new Date();
		JoinInfoTuple jit = new JoinInfoTuple(newId, date.getTime()/1000);
		regTable.put(name, jit);
		keyTable.put(newId, name);

		//

		logger.fine("adding " + name + ":" + jit.toString());

		//save object state 
		//saveState();

		return newId.toString();
	}

	/*public synchronized void unregisterDevice(String name){
		JoinInfoTuple jit = regTable.remove(name);

		logger.fine("removing " + name + ": " + jit.toString() + " in Registrar");

		//save object state
		saveState();
	}*/

	/*public List<UUID> getPubIds(){
		Set<UUID> keySet = keyTable.keySet();

		if(keySet != null){
			ArrayList<UUID> keyList = new ArrayList<UUID>(keySet);
			logger.fine("Getting PubId list; size="+ keyList.size());
			return (List<UUID>) keyList;
		}

		return (List<UUID>)keySet;
	}*/

	public List<String> getPubIds() {//Str() {
		ArrayList<UUID> uuidList = null;
		Set<UUID> keySet = keyTable.keySet();
		ArrayList<UUID> keyList = new ArrayList<UUID>(keySet);
		
		if(keySet != null)
			uuidList=(ArrayList<UUID>) keyList;
		else
			return new ArrayList<String>();

		ArrayList<String> uuidListStr = new ArrayList<String>();
		for(int i=0; i<uuidList.size(); i++)
			uuidListStr.add(uuidList.get(i).toString());

		logger.fine("Getting PubId list; size="+ uuidListStr.size());
		return (List<String>)uuidListStr;
		
	}

	public synchronized static void saveState(){
		/*try {
			FileOutputStream fileOut = new FileOutputStream(regObjPath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(registrar);


			fileOut = new FileOutputStream(regTableObjPath);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(regTable);

			fileOut = new FileOutputStream(keyTableObjPath);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(keyTable);
			
			//System.out.println("Wrote objects");
			logger.info("Wrote objectes");
		} catch(Exception e) {
			//e.printStackTrace();
			logger.log(Level.SEVERE, "Likely and IOException while writing objects", e);
		}*/
	}

}
