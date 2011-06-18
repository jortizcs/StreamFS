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

package local.metadata.context;

import local.metadata.context.exceptions.*;
import net.sf.json.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ContextGraphNode{
	private static transient Logger logger = Logger.getLogger(ContextGraphNode.class.getPackage().getName());

	private String contextNodeId = null;
	private String jsonNode = null;
	private String nodeName = null;

	public ContextGraphNode(JSONObject contextNode) throws InvalidContextNodeFormatException{
		try{
			String label = contextNode.getString("label");
			String name  = contextNode.getString("name");
			String schematype = contextNode.getString("type");
			String description = contextNode.getString("description");

			JSONArray devices = contextNode.getJSONArray("AssociatedDevices");
			JSONArray parents = contextNode.getJSONArray("parents");
			JSONArray children = contextNode.getJSONArray("children");

			contextNodeId = contextNode.getString("cnid");
			jsonNode = contextNode.toString();
			
		} catch (JSONException e){
			logger.log(Level.WARNING, "Could not create Context Node", e);
			throw new InvalidContextNodeFormatException();
		}
	}

	protected void saveInDataBase(){
		//4 columns -- Table: contextGraphs
		//	- cid
		//	- node name
		//	- type = [node|edge]
		//	- json object blob
	}

	protected void updateInDatabase(){
	}

	public JSONObject getJson(){
		return (JSONObject) JSONSerializer.toJSON(jsonNode);
	}
}
