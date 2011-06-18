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
package local.metadata;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import net.sf.json.*;
import java.io.*;

public class MetadataMngr implements Serializable
{

	private static Logger logger = Logger.getLogger(MetadataMngr.class.getPackage().getName());
	private static MetadataMngr metadataMngr = null;
	private static HashMap<String, Binding> bindTable = null;
	private static BindStateMngr bindStateMngr = null;

	protected static String IS4HOME=null;
	protected static String bindTableObjPath = "/.state/bindTable.obj";
	private static boolean init = false;

	private MetadataMngr()
	{
		logger.info("Instantiated MetadataMngr");
		if(bindStateMngr == null)
		    bindStateMngr = BindStateMngr.getInstance();

		if(!init) {
			try {
				initGlobalVars();
				File f = new File(bindTableObjPath);

				if(!f.exists() && bindTable == null) {
					bindTable = new HashMap<String, Binding>();
				} else if (f.exists()){
					FileInputStream fileIn = new FileInputStream(f);
					ObjectInputStream in = new ObjectInputStream(fileIn);
					bindTable = (HashMap<String, Binding>)in.readObject();
				}
				init = true;
			} catch (Exception e) {
				logger.log(Level.WARNING, "Error loading previous state", e);
			}
		}
	}

	public static MetadataMngr getInstance()
	{
		logger.info("Fetching local static instance of MetadataMngr");
		if(metadataMngr == null)
		    metadataMngr = new MetadataMngr();
		return metadataMngr;
	}

	protected static void initGlobalVars(){
		String home = System.getenv().get("IS4HOME");
		if(IS4HOME == null && home !=null && !home.equals("")){
			IS4HOME = home;
			bindTableObjPath = IS4HOME + bindTableObjPath;
		} else if(IS4HOME == null && home ==null || home.equals("")){
			logger.severe("Environment variable not set");
			System.exit(1);
		}
	}

	public boolean bind(String id, JSONObject metadata)
	{
		if(bindTable.containsKey(id))
			return false;
		if(!metadata.containsKey("object_stream") || 
			!metadata.containsKey("context_stream") || 
			!metadata.containsKey("logic_stream"))
			return false;

		logger.info((new StringBuilder()).append("Binding: (\n\t").append(id).append(" --> \t\n").append(metadata.toString()).append("\n)").toString());
		Binding binding = new Binding(id, metadata);
		bindStateMngr.manageBinding(binding);
		bindTable.put(id, binding);
		saveState();
		return true;
	}

	public void touch(String id){
		if(bindTable.containsKey(id)) {
			Binding b = bindTable.get(id);
			BindStateMngr.getInstance().updateBindingTimestamp(b);
		}
	}

	public boolean unbind(String id)
	{
		if(!bindTable.containsKey(id))
			return false;
		logger.info((new StringBuilder()).append("unbinding: ").append(id).toString());
		bindStateMngr.expireBinding(bindTable.get(id));
		bindTable.remove(id);
		saveState();
		return true;
	}

	public boolean rebind(String id, String name)
	{
		return false;
	}

	public boolean isBound(String id)
	{
		logger.info((new StringBuilder()).append("checking is ").append(id).append(" is bound?").toString());
		return bindTable.containsKey(id);
	}

	public JSONArray getAllBoundIds(){
		if(bindTable == null)
			logger.warning("bindTable is NULL");
		Set<String> idsSet = bindTable.keySet();
		JSONArray idsJArray = new JSONArray();
		if(idsSet != null) {
			Iterator<String> ids = idsSet.iterator();
			while(ids.hasNext())
				idsJArray.add(ids.next());
		}
		return idsJArray;
	}

	public synchronized void saveState(){
		try {
			FileOutputStream fileOut = new FileOutputStream(bindTableObjPath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(bindTable);
			logger.info("Wrote objectes");
		} catch(Exception e) {
			logger.log(Level.WARNING, "Likely and IOException while writing objects", e);
		}
	}

}
