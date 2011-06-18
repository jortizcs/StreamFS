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

//import java.net.*;
import java.net.URL;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Vector;
//import java.util.*;
import java.io.*;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
* Manages the addition and removal of subscribers
*/
public class SubscriberTable implements Serializable{

	//subscriber name to subscriber object mapping
	protected Hashtable<String, Subscriber> subnameTable = new Hashtable<String, Subscriber>();

	//subscriber ID to subscriber object mapping 
	protected Hashtable<UUID, Subscriber> subTable = new Hashtable<UUID, Subscriber>();
	
	//publisher ID to subscriber list-object mapping
	protected Hashtable<UUID, List<Subscriber>> streamTable = new Hashtable<UUID, List<Subscriber>>();
	
	//hash to keep track of URLs
	protected HashMap<URL, Subscriber> urlMap = new HashMap<URL, Subscriber>();

	//Logging
	protected static transient final Logger localLogger =Logger.getLogger(SubscriberTable.class.getPackage().getName());
	//Path to serialized object
	protected static String thisTableObjPath = "/subscriberTable.obj";
	protected static String subTableObjPath = "/subtable.obj";
	protected static String subnameTableObjPath = "/subnametable.obj";
	protected static String streamTableObjPath = "/streamtable.obj";
	protected static String urlMapObjPath = "/urlmap.obj";
	
	public SubscriberTable(){
	}

	/**
	 * Returns a previously saved SubscriberTable instance or returns 
	 * a new instance of a SubscriberTable object.  The path MUST point to the
	 * folder/directory where the object was previously saved.
	 *
	 * @param path path to folder or directory where the object is saved.
	 * @returns new SubscriberTable instance or the instance saved in the given folder.
	 */
	public static SubscriberTable loadSubscriberTable(String path){
		SubscriberTable sTable = null;
		FileInputStream fileIn = null;
		ObjectInputStream in = null;
		SubscriberTable subscriberTable = new SubscriberTable();
		Logger thisLogger = subscriberTable.localLogger;
		thisLogger.info("Loading subscriber table form " + path);
		try {
			//check for SubscriberTable object existence
			File f1 = new File(path + thisTableObjPath);
			//System.out.println("1: " + path + thisTableObjPath);
			if(f1.exists()){
				fileIn = new FileInputStream(f1);
				in = new ObjectInputStream(fileIn);
				sTable = (SubscriberTable)in.readObject();
				thisLogger.fine("Subscriber table loaded");
			}else {
				sTable = new SubscriberTable();
				thisLogger.fine("Subscriber table instantiated");
			}

			f1 = new File(path + subTableObjPath);
			//System.out.println("2: " + path + subTableObjPath);
			if(f1.exists()){
				fileIn = new FileInputStream(f1);
				in = new ObjectInputStream(fileIn);
				sTable.subTable = (Hashtable<UUID, Subscriber>)in.readObject();
				thisLogger.fine("Subscriber uuid table set and loaded in SubscriberTable Instance");
			}

			f1 = new File(path + subnameTableObjPath);
			//System.out.println("2: " + path + subTableObjPath);
			if(f1.exists()){
				fileIn = new FileInputStream(f1);
				in = new ObjectInputStream(fileIn);
				sTable.subnameTable = (Hashtable<String, Subscriber>)in.readObject();
				thisLogger.fine("Subscriber name table set and loaded in SubscriberTable Instance");
			}
 
			f1 = new File(path + streamTableObjPath);
			//System.out.println("3: " + path + streamTableObjPath);
			if(f1.exists()){
				fileIn = new FileInputStream(f1);
				in = new ObjectInputStream(fileIn);
				sTable.streamTable = (Hashtable<UUID, List<Subscriber>>)in.readObject();
				thisLogger.fine("Stream table set and loaded in SubscriberTable Instance");
			}

			f1 = new File(path + urlMapObjPath);
			//System.out.println("4: " + path + urlMapObjPath);
			if(f1.exists()){
				fileIn = new FileInputStream(f1);
				in = new ObjectInputStream(fileIn);
				sTable.urlMap = (HashMap<URL, Subscriber>)in.readObject();
				thisLogger.fine("Url map set and loaded in SubscriberTable Instance");
			}

		} catch(Exception e) {
			thisLogger.log(Level.SEVERE, "SubscriberTable: Error Loading SubscriberTable; Exiting!", e);
			System.exit(1);
			//System.err.print("SubscriberTable: Error Loading SubscriberTable");
			//e.printStackTrace();
		}

		return sTable;
	}

	/**
	 * Save this object and associated sub-Objects in the given path.
	 */
	public void saveObject(String path) {
		localLogger.info("Saving object in " + path);
		try {
			FileOutputStream fileOut = new FileOutputStream(path + thisTableObjPath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			SubscriberTable thisTable = new SubscriberTable();
			thisTable.subTable = this.subTable;
			thisTable.streamTable = this.streamTable;
			thisTable.urlMap = this.urlMap;
			out.writeObject(thisTable);
			localLogger.fine("Writing Subscriber table to disk");

			fileOut = new FileOutputStream(path + subTableObjPath);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(subTable);
			localLogger.fine("Writing Subscriber UUID hash table to disk");

			fileOut = new FileOutputStream(path + subnameTableObjPath);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(subnameTable);
			localLogger.fine("Writing Subscriber name hash table to disk");

			fileOut = new FileOutputStream(path + streamTableObjPath);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(streamTable);
			localLogger.fine("Writing Stream hash table to disk");

			fileOut = new FileOutputStream(path + urlMapObjPath);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(urlMap);
			localLogger.fine("Writing url map to disk");
			
			//System.out.println("SubscriberTable: Wrote objects");
		} catch(Exception e) {
			localLogger.log(Level.WARNING, "SubscriberTable: Error(s) while saving objects",e);
			//System.err.print("SubscriberTable: Error(s) while saving objects");
			//e.printStackTrace();
		}
	}
	
	//put subscriber in table
	public synchronized void add(Subscriber s){
		try{
			localLogger.fine("Adding a subcriber:\n" + s.toString());
			ArrayList<String> streamIds = (ArrayList<String>)s.getSubStreamIds();
			localLogger.info("s.|StreamdIds| = " + streamIds.size());
			
			//add subscriber to list of subscribers for this publisher
			localLogger.fine("Checking each publisher's subscriber list and adding this subscriber if it's not on the associated list");
			for(int i=0; i<streamIds.size(); i++){
				String pubId = streamIds.get(i);
				localLogger.info("StreamId[" + i + "]= " + pubId);
				if(pubId != null){
					ArrayList<Subscriber> theseSubs = (ArrayList<Subscriber>) streamTable.get(UUID.fromString(pubId));
					
					if(theseSubs != null){
						if(!theseSubs.contains(s)) {
							theseSubs.add(s);
						} else {
							localLogger.warning("Already a subscriber to " + pubId +
									", NOT ADDING");
						}
					}else {
						//System.out.println("No subscribers, adding...");
						localLogger.info("No subscribers, adding a new subscriber");
						ArrayList<Subscriber> subscriberList = new ArrayList<Subscriber>();
						subscriberList.add(s);

						//add to each table/map
						streamTable.put(UUID.fromString(pubId), subscriberList);
					}
				}
				else{
					//System.out.println("PUBLISHER IN LIST IS NULL");
					localLogger.warning("Publisher in list null");
				}
			}
		} catch (IllegalArgumentException e) {
			localLogger.log(Level.WARNING, "Unexpected illegal publisher id in list", e);
		}

		//now add subscriber to table of subscribers
		if(!subTable.containsKey(s.getSubId())){
			//System.out.println("adding subid" + s.getSubId());
			localLogger.fine("Adding subid " + s.getSubId());
			subTable.put(UUID.fromString(s.getSubId()), s);
		}
			
		//register the URL (or URL of the proxy), these have to be unique as well
		if(s.usesProxy() ){//&& !urlMap.containsKey(s.getProxyUrl())){
			String p = s.getProxyUrl().toString() + "?random=" +UUID.randomUUID().toString();
			try{
				URL pUrl = new URL(p);
				localLogger.fine("Registering proxy url: " + pUrl.toString());
				urlMap.put(pUrl,s);
			}
			catch(Exception e){
				localLogger.log(Level.WARNING, "", e);
			}
		} else if(!urlMap.containsKey(s.getSubUrl())){
			//System.out.println("Registering url: "+ s.getSubUrl() + " " + s.usesProxy());
			localLogger.fine("Registering url: "+ s.getSubUrl() + " " + s.usesProxy());
			urlMap.put(s.getSubUrl(), s);
		}

		//saveObjects();
	}

	/**
	 *  Publisher has unregistered (i.e. publisher-to-subscriber_list mapping is removed).
	 */
	public synchronized void publisherRemoved(String pubId){
		try {
			localLogger.info("Removing " + pubId);
			//Long pubIdLong = new Long(pubId);
			UUID id = UUID.fromString(pubId);
			List<Subscriber> associatedSubList = streamTable.remove(id);
			if(associatedSubList != null)
				for(int i=0; i<associatedSubList.size(); i++)
					associatedSubList.get(i).removeStream(pubId);
			localLogger.info("Removed id " + pubId + " from SubscriberTable");
		} catch (IllegalArgumentException e){
			localLogger.log(Level.WARNING, "Cannot remove " + pubId + "; invalid UUID format", e);
		}
	}

	/**
	 *  Remove subscriber from subscriber list.
	 */
	public synchronized void remove(Subscriber s){
		int check=0;
		localLogger.info("Removing subscriber");

		if(s!=null){
			localLogger.fine(s.toString());
		}
		else {
			localLogger.warning("Trying to remove a null subscriber");
			return;
		}


		try {
			if (s != null){
				subTable.remove(s.getSubId());
				ArrayList<String> streamIds = (ArrayList<String>)s.getSubStreamIds();
		
				//remove the url for the subscriber or the proxy
				if(s.usesProxy()){
					urlMap.remove(s.getProxyUrl());
				}
				else{
					urlMap.remove(s.getSubUrl());
				}

				//remove the subscriber from the list of subscribers for all publishers to which
				//this subscriber has a subscription to
				check =1;
				for(int i=0; i<streamIds.size(); ++i){
					localLogger.finer("Getting the subcriber list for publisher " + streamIds.get(i));
					List<Subscriber> subscriberList = streamTable.get(UUID.fromString(streamIds.get(i)));
					if(subscriberList != null){
						ArrayList<Subscriber> thisList = (ArrayList<Subscriber>) subscriberList;
						if(thisList.contains(s)){
							localLogger.finest("Removing " + s.getSubId() + " from Susbcriber list: " + thisList.toString());
							//streamTable.remove(streamIds.get(i));
							thisList.remove(s);
							//streamTable.put(UUID.fromString(streamIds.get(i)), thisList);
						}
					}
				}

				check =2;
				//remove from the other three tables
				subTable.remove(UUID.fromString(s.getSubId()));
				if(s.getName()!=null)
					subnameTable.remove(s.getName());
				urlMap.remove(s.getSubUrl());
			}
		} catch(IllegalArgumentException e) {
			if(check==1){
				localLogger.log(Level.WARNING, "Could not remove " + s.getSubId() + "; Unexpected illegal UUID in stream Ids list", e);
			} else {
				localLogger.log(Level.WARNING, "Could not remove " + s.getSubId() + "; Illegal UUID String", e);
			}
		}
	}

	public synchronized void addPubToSubList(String pid, String sid){
		try {
			Registrar R = Registrar.registrarInstance();
			if(R.isRegisteredId(pid)){
				//add to subscriber list
				Subscriber s = this.getSubscriber(sid);
				s.addStream(pid);

				//add subscriber to sub list for this pub
				UUID PID = UUID.fromString(pid);
				if(streamTable.containsKey(PID)){
					List<Subscriber> subList = streamTable.get(PID);
					//DEBUG
					System.out.print("Trying to add subscriber_id=" + sid);
					for(int g=0; g<subList.size(); g++){
						System.out.println("sublist[" + g + "]: " +
									subList.get(g).getSubId());
					}
					//////////////////////////
					if(!subList.contains(s))
						subList.add(s);
				} else {
					ArrayList<Subscriber> subList = new ArrayList<Subscriber>();
					subList.add(s);
					streamTable.put(PID,subList);
				}
			}
		} catch (IllegalArgumentException e) {
			localLogger.log(Level.WARNING, "Invalid publisher id " + pid + ", or subscriber id " + sid); 
		}
	}

	public synchronized void removePubFromSubList(String pid, String sid){
		try{
			Registrar R = Registrar.registrarInstance();
			UUID PID = UUID.fromString(pid);
			UUID SID = UUID.fromString(sid);

			if(R.isRegisteredId(pid) && streamTable.containsKey(PID)){
				List<Subscriber> subList = (List<Subscriber>) streamTable.get(PID);

				//remove to subscriber list
				Subscriber s = subTable.get(SID);
				if(s!=null){
					subList.remove(s);
					s.removeStream(pid);
				}
			}
		} catch (IllegalArgumentException e) {
			localLogger.log(Level.WARNING, "Could not remove pub " + pid + " from subscriber " + sid + " list", e);
		}
	}
	
	public List<Subscriber> getAllSubs(String publisherId){
		try {
			UUID pid = UUID.fromString(publisherId);
			return streamTable.get(pid);

		} catch(IllegalArgumentException e) {
			localLogger.log(Level.WARNING, "Invalid stream identifier format: " + publisherId + "; invalid UUID", e);
			return (List<Subscriber>) new ArrayList<Subscriber>();
		}
	}
	
	public List<String> getAllStreams(String subId){
		try {
			UUID subid = UUID.fromString(subId);
			Subscriber s = subTable.get(subid);
			if(s!=null){
				localLogger.info("Subscriber " + subId + " found in SubscriberTable");
				return s.getSubStreamIds();
			} else {
				localLogger.info("No subscriber " + subId + " in SubscriberTable");
			}
		} catch (IllegalArgumentException e){
			localLogger.log(Level.WARNING, "Invalid stream identifier format: " + subId + "; invalid UUID", e);
		}
		
		return null;
	}
	
	public List<Subscriber> getRegisteredSubscribers(){
		Collection<Subscriber> sublist_ = (Collection<Subscriber>) subTable.values();
		if(sublist_ != null)
			return new ArrayList<Subscriber>(sublist_);
		
		return null;
	}

	public List<String> getRegisteredSubscriberIds(){
		Collection<UUID> sublist_ = (Collection<UUID>) subTable.keySet();
		Vector<UUID> sublistVec = new Vector<UUID>(sublist_);
		Collection<String> sublistStr = (Collection<String>) new Vector<String>(subTable.size()); 
		if(sublist_ != null){
			for(int i=0; i<sublist_.size(); i++)
				sublistStr.add(sublistVec.get(i).toString());
			return new ArrayList<String>(sublistStr);
		}
		
		return null;
	}

	public Subscriber getSubscriber(String id){
		try {
			UUID thisId = UUID.fromString(id);
			return subTable.get(thisId);
		} catch (IllegalArgumentException e) {
			localLogger.log(Level.WARNING, "Invalid subscriber identifier format: " + id + "; invalid UUID", e);
			return null;
		}
	}


	public Subscriber getSubscriber(URL url){
		return urlMap.get(url);
	}

	//membership
	public boolean containsUrl(URL url){
		return urlMap.containsKey(url);
	}

	public boolean containsSubId(String subId){
		try {
			//System.out.println("sid="+subId);
			//System.out.println("keyset:"+subTable.keySet());
			UUID id = UUID.fromString(subId);
			return subTable.containsKey(id);
		} catch (IllegalArgumentException e) {
			localLogger.log(Level.WARNING, "Invalid subscriber identifier format: " + subId + "; invalid UUID", e);
			return false;
		}
	}

	
}
