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

import local.rest.*;
import local.db.*;
import local.rest.resources.*;
import net.sf.json.*;
import jdsl.graph.api.*;
import jdsl.graph.ref.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;

public class ContextMngr {
	private static transient Logger logger = Logger.getLogger(ContextMngr.class.getPackage().getName());
	private static MySqlDriver database = (MySqlDriver) DBAbstractionLayer.database;

	private static ContextMngr contextMngr = null;

	//management modes
	private static final int NEW_GRAPH_MODE = 0;

	private static final int REPLACE_GRAPH_MODE = 1;
	private static final int REPLACE_NODE_MODE = 2;
	private static final int REPLACE_EDGE_MODE = 3;

	private static final int UPDATE_NODE_MODE = 4;
	private static final int UPDATE_EDGE_MODE = 5;

	private static final int REMOVE_GRAPH_MODE = 6;
	private static final int REMOVE_NODE_MODE = 7;
	private static final int REMOVE_EDGE_MODE = 8;

	private ContextMngr(){}

	public static ContextMngr getInstance(){
		if(contextMngr == null)
			contextMngr = new ContextMngr();
		return contextMngr;
	}

	public boolean addNewContextMap(JSONObject cmap, JSONArray errors){
		try {
			int mode = NEW_GRAPH_MODE;
			if(cmap.getString("type").equalsIgnoreCase("context_graph")){
				JSONArray jsonNodes = cmap.getJSONArray("graph_nodes");
				JSONArray jsonEdges = cmap.optJSONArray("graph_edges");

				if(!checkNames(jsonNodes, jsonEdges, errors)){
					logger.warning("checkNames errors: " + errors.toString());
					return false;
				}
				if(!checkInternalIds(jsonNodes, jsonEdges, errors)){
					logger.warning("checkInternalIds errors: " + errors.toString());
					return false;
				}
				
				addMissingEdges(jsonNodes, jsonEdges);
				addMissingFields(jsonNodes, jsonEdges);

				//save the graph
				createInternalGraph(jsonNodes, jsonEdges);
				String cid = jsonNodes.getJSONObject(0).getString("cnid").substring(0,8);
				cmap.put("cid", cid);
				cmap.put("graph_nodes", jsonNodes);
				cmap.put("graph_edges", jsonEdges);
				cmap.put("name", "context_graph");
				DBAbstractionLayer.database.putEntry(cmap);
				//JSONObject resourceList = genResourceList(contextGraph);
				

				//generate resource tree
				String prefix = "/is4/Cory/lt/";
				JSONObject treeResourceList = genTreeResourceList(cmap, errors);
				//logger.fine("PP_RESOURCE LIST:" + treeResourceList.toString());
				if(treeResourceList != null){
					Set<String> nodeNameSet = treeResourceList.keySet();
					Iterator<String> nodeNameIter = nodeNameSet.iterator();
					while(nodeNameIter.hasNext()){
						String thisPath = prefix + treeResourceList.getString(nodeNameIter.next());
						thisPath = thisPath.replace(" ", "_");
						Resource resource =null;
						if(thisPath.endsWith("devices") || thisPath.endsWith("devices/")){
							resource = (Resource) new DevicesResource(thisPath);
						}
						else if(thisPath.contains("devices")){
							String subPath = thisPath.substring(thisPath.indexOf("devices"), thisPath.length());
							StringTokenizer tokenizer = new StringTokenizer(subPath, "/");
							if(tokenizer.countTokens()==2){
								resource  = (Resource) new DeviceInstanceResource(thisPath);
							}
							else if(tokenizer.countTokens()==3) {
								UUID pubid = database.isRRPublisher(thisPath);
								if(pubid != null) {
									resource = (Resource) new PublisherResource(thisPath, pubid);
								}else{
									resource= new Resource(thisPath);
								}
							}else{
									resource= new Resource(thisPath);
							}
						}	
						else {
							resource = (Resource) new LoadTreeResource(thisPath, LoadTreeResource.PANEL_ELEMENT);
						}
						RESTServer.addResource(resource);
					}
				} else{
					logger.fine("COULD NOT ADD LOAD TREE RESOURCES\n\tERRORS: " + errors.toString()); 
				}
			} else {
				String e ="Can only add type: context_graph; Cannot add: " + cmap.getString("type"); 
				errors.add(e);
				return false;
			}
		} catch(Exception e){
			logger.log(Level.WARNING,"Error while add new context map", e);
			String msg = "Map may not adhere to context_map schema, check that all required fields are present";
			errors.add(msg);
			return false;
		}
		return true;
	}

	public JSONObject genTreeResourceList(JSONObject contextGraph, JSONArray errors){
		JSONArray nodes = contextGraph.getJSONArray("graph_nodes");

		Hashtable<JSONObject, String> nodeToPath = new Hashtable<JSONObject, String>();
		Hashtable<String, JSONObject> idToNode = new Hashtable<String, JSONObject>();
		for(int i=0; i<nodes.size(); i++){
			JSONObject thisNode = (JSONObject) nodes.get(i);
			idToNode.put(thisNode.getString("cnid"), thisNode);
		}

		Hashtable<String, String> labelToPath = new Hashtable<String, String>();
		if(isTree(contextGraph, errors)){
			for(int i=0; i<nodes.size(); i++){
				Stack<String> pathElements = new Stack<String>();
				JSONObject thisNode = (JSONObject) nodes.get(i);
				pathElements.push(thisNode.getString("label"));
				JSONArray parents = thisNode.getJSONArray("parents");
				while(parents.size()>0){
					thisNode = idToNode.get(parents.get(0));
					pathElements.push(thisNode.getString("label"));
					parents = thisNode.getJSONArray("parents");
				}

				StringBuffer thisPath = new StringBuffer();
				while(!pathElements.empty())
					thisPath.append(pathElements.pop()).append("/");
				labelToPath.put(((JSONObject)nodes.get(i)).getString("name"), thisPath.toString());
			}

			JSONObject allpaths = new JSONObject();
			allpaths.accumulateAll(labelToPath);
			return allpaths;
		} else {
			logger.fine("NOT A TREE!");
			return null;
		}

	}

	public boolean isTree(JSONObject contextGraph, JSONArray errors){
		//boolean istree = true;
		JSONArray nodes = contextGraph.getJSONArray("graph_nodes");
		int rootCount= 0;
		Vector<String> rootNames = new Vector<String>();
		for(int i=0; i<nodes.size(); i++){
			JSONObject thisNode = (JSONObject) nodes.get(i);
			JSONArray children = thisNode.getJSONArray("children");
			JSONArray parents = thisNode.getJSONArray("parents");
			if(parents.size() > 1){
				errors.add("node_" +  thisNode.getString("cnid") + " has more than one parent");
				return false;
			} 
			
			if(parents.size() == 0) {
				rootNames.addElement(thisNode.getString("name"));
				rootCount+=1;
			}

			if(rootCount > 1){
				errors.add("Multiple root nodes");
				errors.add(rootNames.elementAt(0));
				errors.add(rootNames.elementAt(1));
				return false;
			}

			for(int j=0; j<children.size(); j++){
				if(parents.contains(children.get(j))){
					errors.add("node_" + thisNode.getString("cnid") + 
								" share the same parent and child: " + (String)children.get(j));
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkNodeSchema(int mode, JSONObject node){
		return true;
	}

	private boolean checkEdgeSchema(int mode, JSONObject edge){
		return true;
	}

	private void addMissingFields(JSONArray nodes, JSONArray edges) {
		int i;
		for(i=0; i<nodes.size(); i++){
			JSONObject thisNode = (JSONObject) nodes.get(i);
			if(!thisNode.containsKey("label"))
				thisNode.put("label", thisNode.getString("name"));
			if(!thisNode.containsKey("AssociatedDevices"))
				thisNode.put("AssociatedDevices", new JSONArray());
			if(!thisNode.containsKey("description"))
				thisNode.put("desciption", "");
			if(!thisNode.containsKey("location"))
				thisNode.put("location", "");
			if(!thisNode.containsKey("$schema"))
				thisNode.put("$schema", "http://jortiz81.homelinux.com/is4/schemas/context_node_schema.json");
		}

		for(i=0; i<edges.size(); i++){
			JSONObject thisEdge = (JSONObject) edges.get(i);
			if(!thisEdge.containsKey("label"))
				thisEdge.put("label", thisEdge.getString("name"));
		}
	}

	private boolean checkInternalIds(JSONArray nodes, JSONArray edges, JSONArray errors) {

		String stage = "";
		try {
			int i;
			//load all internal ids and the node it references
			Hashtable<Integer, JSONObject> internalNodeIds = new Hashtable<Integer, JSONObject>();
			for(i=0; i<nodes.size(); ++i){
				JSONObject thisNode = (JSONObject) nodes.get(i);
				stage = "Checking cnid for node " + i + " in graph_nodes array; must be an integer";
				internalNodeIds.put((new Integer(thisNode.getInt("cnid"))), thisNode);
				stage="";
			}

			//check all the references and fix them if needed
			for(i=0; i<nodes.size(); ++i){
				JSONObject thisNode = (JSONObject) nodes.get(i);

				stage = "Checking parent ids for node " + i + " in the array";
				JSONArray parents = thisNode.getJSONArray("parents");
				JSONArray filteredParents = new JSONArray();
				int j;
				for(j=0; j<parents.size(); j++){
					Integer thisParentId = new Integer(parents.getInt(j));
					if(internalNodeIds.containsKey(thisParentId))
						filteredParents.add(thisParentId);
				}
				thisNode.put("parents", filteredParents);

				stage = "Checking child ids for node " + i + " in the array";
				JSONArray children = thisNode.getJSONArray("children");
				JSONArray filteredChildren = new JSONArray();
				for(j=0; j<children.size(); j++){
					Integer thisChildId = new Integer(children.getInt(j));
					if(internalNodeIds.containsKey(thisChildId))
						filteredChildren.add(thisChildId);
				}
				thisNode.put("children", filteredChildren);
				stage = "";

				//replace the internal node references
				internalNodeIds.put(new Integer(thisNode.getInt("cnid")), thisNode);

				//replace the nodes list to point to a cleaned up, valid, consistent node list
				Iterator<JSONObject> nodeJSONIterator = internalNodeIds.values().iterator();
				JSONArray validNodes = new JSONArray();
				while(nodeJSONIterator.hasNext())
					validNodes.add(nodeJSONIterator.next());

				//replace the elements in the nodes array if necessary, leaving only valid nodes
				if(nodes.size() != validNodes.size()){
					while(nodes.size()>0)
						nodes.remove(0);
					for(int k=0; k<validNodes.size(); k++)
						nodes.add(validNodes.get(i));
				}
			}

			//check all edge references and fix them if needed
			JSONArray validEdges = new JSONArray();
			for(i=0; i<edges.size(); ++i){
				JSONObject thisEdge = (JSONObject) edges.get(i);
				stage = "checking edge " + i;
				Integer sourceId = new Integer(thisEdge.getInt("sourceNode"));
				Integer destinationId = new Integer(thisEdge.getInt("destinationNode"));
				if(internalNodeIds.containsKey(sourceId) && internalNodeIds.containsKey(destinationId))
					validEdges.add(thisEdge);
				else 
					logger.info("Could not fined one of the node for edge " + thisEdge.getString("name"));
				stage = "";
			}

			//replace the elements in the edges array, leaving only valid edges
			if(edges.size() != validEdges.size()){
				while(edges.size()>0)
					edges.remove(0);
				for(i=0; i<validEdges.size(); i++)
					edges.add(validEdges.get(i));
			}

		} catch (JSONException e){
			logger.log(Level.WARNING, "JSONException caught", e);
			errors.add(stage);
			return false;
		}

		return true;
	}

	private boolean checkNames(JSONArray nodes, JSONArray edges, JSONArray errors){
		try {
			Hashtable<String, JSONObject> internalNodeNames = new Hashtable<String, JSONObject>();
			int i;
			for(i=0; i<nodes.size(); i++){
				JSONObject thisNode = (JSONObject) nodes.get(i);
				if(!internalNodeNames.containsKey(thisNode.getString("name"))) {
					internalNodeNames.put(thisNode.getString("name"), thisNode);
				} else {
					errors.add("Duplicate node name " + thisNode.getString("name") + "; all node name must be unique");
					return false;
				}
			}

			Hashtable<String, JSONObject> internalEdgeNames = new Hashtable<String, JSONObject>();
			for(i=0; i<edges.size(); ++i){
				JSONObject thisEdge = (JSONObject) edges.get(i);
				if(!internalEdgeNames.containsKey(thisEdge.getString("name"))) {
					internalEdgeNames.put(thisEdge.getString("name"), thisEdge);
				} else {
					errors.add("Duplicate edge name " + thisEdge.getString("name") + "; all node name must be unique");
					return false;
				}
				
			}
		} catch(JSONException e){
			logger.log(Level.WARNING, "", e);
			return false;
		}
		return true;
	}

	
	private void addMissingEdges(JSONArray nodes, JSONArray edges){
		int edgeCounter = 0;
		String edgeNamePrefix = "edge_";
		int i;

		//add missing edge objects
		Hashtable<String, JSONObject> edgeNames = new Hashtable<String, JSONObject>();
		for(i=0; i<edges.size(); i++){
			JSONObject thisEdge = (JSONObject) edges.get(i);
			edgeNames.put(thisEdge.getString("name"), thisEdge);
		}

		//Gather all edges introduced by node references
		Hashtable<String, JSONObject> nodeEdges = new Hashtable<String, JSONObject>();

		for(i=0; i<nodes.size(); i++){
			JSONObject thisNode = (JSONObject) nodes.get(i);
			JSONArray parents = thisNode.getJSONArray("parents");
			int j;
			for(j=0; j<parents.size(); j++){
				Integer sourceId = new Integer(parents.getInt(j));
				Integer destId = new Integer(thisNode.getInt("cnid"));
				String newNodeEdgeStr = sourceId.toString() + "->" + destId.toString();

				if(!nodeEdges.containsKey(newNodeEdgeStr)){
					//new edge
					JSONObject newEdge = new JSONObject();
					String newEdgeName = edgeNamePrefix + edgeCounter;
					while(edgeNames.containsKey(newEdgeName)){
						edgeCounter += 1;
						newEdgeName = edgeNamePrefix + edgeCounter;
					}

					newEdge.put("label", newEdgeName);
					newEdge.put("name", newEdgeName);
					newEdge.put("type", "context_edge");
					newEdge.put("sourceNode", sourceId);
					newEdge.put("destinationNode", destId);
					edgeNames.put(newEdgeName, newEdge);
					edges.add(newEdge);

					nodeEdges.put(newNodeEdgeStr, newEdge);
				}
			}

			JSONArray children = thisNode.getJSONArray("children");
			for(j=0; j<children.size(); j++){
				Integer sourceId = new Integer(thisNode.getInt("cnid"));
				Integer destId = new Integer(children.getInt(j));
				String newNodeEdgeStr = sourceId + "->" + destId;

				if(!nodeEdges.containsKey(newNodeEdgeStr)){
					//new edge
					JSONObject newEdge = new JSONObject();
					String newEdgeName = edgeNamePrefix + edgeCounter;
					while(edgeNames.containsKey(newEdgeName)){
						edgeCounter += 1;
						newEdgeName = edgeNamePrefix + edgeCounter;
					}
					newEdge.put("label", newEdgeName);
					newEdge.put("name", newEdgeName);
					newEdge.put("type", "context_edge");
					newEdge.put("sourceNode", sourceId);
					newEdge.put("destinationNode", destId);
					edgeNames.put(newEdgeName, newEdge);
					edges.add(newEdge);

					nodeEdges.put(newNodeEdgeStr, newEdge);
				}
			}
		}

		//populate a lookupTable from internalId to jsonNode and dump all parent/children references
		Hashtable<Integer, JSONObject> internalIdLookupTable = new Hashtable<Integer, JSONObject>();
		for(i=0; i<nodes.size(); i++){
			JSONObject thisNode = (JSONObject)nodes.get(i);
			internalIdLookupTable.put(new Integer(thisNode.getInt("cnid")), thisNode);

			//dump all parent/children references
			JSONArray parents = thisNode.getJSONArray("parents");
			JSONArray children = thisNode.getJSONArray("children");
			while(parents.size()>0)
				parents.remove(0);
			while(children.size()>0)
				children.remove(0);
		}
	

		//re-populate all children/parent references	
		for(i=0; i<edges.size(); i++){
			JSONObject thisEdge = (JSONObject) edges.get(i);
			Integer sourceId = new Integer(thisEdge.getInt("sourceNode"));
			Integer destId = new Integer(thisEdge.getInt("destinationNode"));
			JSONObject sourceNode = internalIdLookupTable.get(sourceId);
			JSONObject destNode = internalIdLookupTable.get(destId);

			JSONArray sChildren  = sourceNode.getJSONArray("children");
			JSONArray dParents = destNode.getJSONArray("parents");

			sChildren.add(destId);
			dParents.add(sourceId);
		}
		
	}

	public void sendDot(String cid, String dotOutput, String host, String uri){
		String filename = "ContextMap_" + cid + ".dot";
		String filename2 = "ContextMap_" + cid + ".svg";
		try {
			String makePngCommand = "dot -Tsvg " + filename + " -o ContextMap_" + cid + ".svg";
			String command  = "scp " + filename2 + " " + host + ":" + uri;
			String deleteCmd1 = "rm -f " + filename2;
			String deleteCmd2 = "rm -f " + filename;

			File dotFile = new File(filename);
			FileOutputStream dotFileOstream = new FileOutputStream(dotFile);
			dotFileOstream.write(dotOutput.getBytes());
			dotFileOstream.close();

			Process p = Runtime.getRuntime().exec(makePngCommand);
			p = Runtime.getRuntime().exec(command);
			p = Runtime.getRuntime().exec(deleteCmd1);
			p = Runtime.getRuntime().exec(deleteCmd2);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private IncidenceListGraph createInternalGraph(JSONArray nodes, JSONArray edges){
		String cid = fixReferences(nodes, edges);
		Hashtable<String, ContextGraphNode> nodeNameLookupTable = new Hashtable<String, ContextGraphNode>();

		IncidenceListGraph graph = new IncidenceListGraph();
		int i;
		try{
			for(i=0; i<nodes.size(); i++){
				ContextGraphNode cgraphNode = new ContextGraphNode((JSONObject) nodes.get(i));
				//Vertex v = graph.insertVertex(cgraphNode);
				nodeNameLookupTable.put(((JSONObject)nodes.get(i)).getString("cnid"), cgraphNode);
			}

			for(i=0; i<edges.size(); i++){
				ContextGraphEdge cgraphEdge = new ContextGraphEdge((JSONObject) edges.get(i));
				ContextGraphNode src = nodeNameLookupTable.get(((JSONObject)edges.get(i)).getString("sourceNode"));
				ContextGraphNode dst = nodeNameLookupTable.get(((JSONObject)edges.get(i)).getString("destinationNode"));
				//Edge e = graph.insertEdge(src, dst, cgraphEdge);
			}
		} catch(Exception e){
			logger.log(Level.WARNING, "",e);
		}

		return graph;
	}

	private String fixReferences(JSONArray nodes, JSONArray edges){
		//generate context graph prefix
		String contextGraphId = UUID.randomUUID().toString().substring(0,8);

		//fix nodes
		Hashtable<Integer, JSONObject> internalRefs = new Hashtable<Integer, JSONObject>();
		Hashtable<Integer, String> internalToGlobalMap = new Hashtable<Integer, String>();
		int i;
		for(i=0; i<nodes.size(); i++){
			JSONObject thisNode = (JSONObject) nodes.get(i);
			Integer internalId = new Integer(thisNode.getInt("cnid"));
			internalRefs.put(internalId, thisNode);
			String newCnid = contextGraphId + "n-" + thisNode.getString("name");
			thisNode.put("cnid", newCnid);
			internalToGlobalMap.put(internalId, newCnid);
		}

		for(i=0; i<nodes.size(); i++){
			JSONObject thisNode = (JSONObject) nodes.get(i);
			JSONArray newParentIds = new JSONArray();
			JSONArray oldParentIds = thisNode.getJSONArray("parents");
			int j;
			for(j=0; j<oldParentIds.size(); j++){
				Integer internalId  = new Integer(oldParentIds.getInt(j));
				if(internalToGlobalMap.containsKey(internalId))
					newParentIds.add(internalToGlobalMap.get(internalId));
			}
			thisNode.put("parents", newParentIds);

			JSONArray newChildrenIds = new JSONArray();
			JSONArray oldChildrenIds = thisNode.getJSONArray("children");
			for(j=0; j<oldChildrenIds.size(); j++){
				Integer internalId  = new Integer(oldChildrenIds.getInt(j));
				if(internalToGlobalMap.containsKey(internalId))
					newChildrenIds.add(internalToGlobalMap.get(internalId));
			}
			thisNode.put("children", newChildrenIds);	
		}

		//fix edges
		for(i=0; i<edges.size(); i++){
			JSONObject thisEdge = (JSONObject) edges.get(i);
			String newSourceId = "";
			Integer internalSourceId = new Integer(thisEdge.getInt("sourceNode"));
			if(internalToGlobalMap.containsKey(internalSourceId))
				newSourceId = internalToGlobalMap.get(internalSourceId);
			thisEdge.put("sourceNode", newSourceId);

			String newDestId = "";
			Integer internalDestId = new Integer(thisEdge.getInt("destinationNode"));
			if(internalToGlobalMap.containsKey(internalDestId))
				newDestId = internalToGlobalMap.get(internalDestId);
			thisEdge.put("destinationNode", newDestId);

			String ceid = contextGraphId + "e-" + thisEdge.getString("name");
			thisEdge.put("ceid", ceid);
			
		}

		return contextGraphId;
	}

	public boolean deleteContextMap(String cid, JSONArray errors){
		return true;
	}

	public boolean updateContextMap(String cid, JSONObject cnode, JSONArray errors){
		return true;
	}

	public boolean addNodeToContextMap (String cid, JSONObject cnode, JSONArray errors){
		return true;
	}

	public boolean removeNodeFromContextMap(String cid, String nodeId){
		return true;
	}

	public boolean replaceContextMap(String cid, JSONObject cmap){
		return true;
	}

	public String dotConversion(JSONObject cmap){
		try {
			StringBuffer dotBuf = new StringBuffer().append("digraph ").append(cmap.getString("cid")).append("{");
			JSONArray edges = cmap.getJSONArray("graph_edges");
			for(int i=0; i<edges.size(); i++){
				JSONObject thisEdge = (JSONObject) edges.get(i);
				String sourceCnid = thisEdge.getString("sourceNode");
				String destCnid = thisEdge.getString("destinationNode");
				String dotEdge = sourceCnid + "->" + destCnid + ";";
				dotBuf.append(dotEdge);
			}
			dotBuf.append("}");
			return dotBuf.toString();
		} catch(Exception e){
			logger.log(Level.WARNING, "Error during conversion of graph to DOT", e);
			return "";
		}
	}

}
