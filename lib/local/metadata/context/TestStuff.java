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

import local.metadata.context.*;

import local.db.*;
import local.rest.*;
import local.rest.resources.*;
import net.sf.json.*;
import jdsl.graph.api.*;
import jdsl.graph.ref.*;

import com.sun.net.httpserver.*;
import java.util.concurrent.Executors;
import java.util.*;
import java.io.*;
import java.net.*;

public class TestStuff extends Filter implements HttpHandler {
	private String bindAddress = "localhost";
	private int port = 8080;

	//This addresses the HttpContext switch bug
	//For every call to create a context in the httpServer after the root, the HttpContext object changes, and the filter
	//is no longer used in the new object.
	protected HttpContext thisContext = null;
	private String URI=null;

	public TestStuff(String uri, String address, int p){
		bindAddress = address;
		port = p;
		URI = uri;
	}

	public TestStuff(String uri){
		URI = uri;
	}

	public void start(){
		try {
			InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(bindAddress), port);
			HttpServer httpServer = HttpServer.create(addr, 0);
			DBAbstractionLayer dbAbstractionLayer = new DBAbstractionLayer();

			//metadata handler
			String mpath = "/";
			HttpContext metadataContext = httpServer.createContext(mpath, this);
			metadataContext.getFilters().add(this);

			httpServer.setExecutor(Executors.newCachedThreadPool());
			httpServer.start();
		} catch (Exception e ) {
			e.printStackTrace();
		}
	}

	public String description(){
		return "Test filter";		
	}

	public void doFilter(HttpExchange exchange, Filter.Chain chain) throws IOException {
		boolean paramsOk = false;
		if((paramsOk = parseParams(exchange)) && chain==null)
			this.handle(exchange);
		else if (!paramsOk) 
			sendResponse(exchange, 404, null);
		else
			chain.doFilter(exchange);
	}

	protected boolean parseParams(HttpExchange exchange) {
		StringTokenizer tokenizer = new StringTokenizer(exchange.getRequestURI().toString(), "?");
		if(tokenizer != null && tokenizer.hasMoreTokens()){
			String thisResourcePath = tokenizer.nextToken();
			if(URI == null && !thisResourcePath.equals(URI) && !thisResourcePath.equals(URI + "/"))
				return false;
			if(tokenizer.countTokens()>0) {
				StringTokenizer paramStrTokenizer = new StringTokenizer(tokenizer.nextToken(), "&");
				if(paramStrTokenizer !=null && paramStrTokenizer.hasMoreTokens()){
					while (paramStrTokenizer.hasMoreTokens()){
						StringTokenizer paramPairsTokenizer = new StringTokenizer(paramStrTokenizer.nextToken(),"=");
						if(paramPairsTokenizer != null && paramPairsTokenizer.hasMoreTokens()){
							String attr = paramPairsTokenizer.nextToken();
							String val = paramPairsTokenizer.nextToken();
							exchange.setAttribute(attr, val);
						}
					}
				}
			} else{
			}
		}
		return true;
	}

	private void filterCheck(HttpExchange exchange){
		try {
			//This addresses the HttpContext switch bug in the library
			//The filter must be called BEFORE the handler
			if (exchange.getHttpContext() != thisContext && exchange.getHttpContext().getFilters().size()==0) {
				thisContext = exchange.getHttpContext();
				thisContext.getFilters().add(this);
				this.doFilter(exchange, null);
				return;
			}
		} catch (IOException e){
		}
	}

	public static void main(String[] args) {
		System.out.println("=====Starting test server=====");
		System.out.println("http://localhost:8080/");
		TestStuff testStuff = new TestStuff("/");
		testStuff.start();	
		
		System.out.println("=====Testing Context Graph Fetch=====");
		
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
				labelToPath.put(((JSONObject)nodes.get(i)).getString("label"), thisPath.toString());
			}

			JSONObject allpaths = new JSONObject();
			allpaths.accumulateAll(labelToPath);
			return allpaths;
		} else {
			return null;
		}

	}

	public boolean isTree(JSONObject contextGraph, JSONArray errors){
		//boolean istree = true;
		JSONArray nodes = contextGraph.getJSONArray("graph_nodes");
		int rootCount= 0;
		for(int i=0; i<nodes.size(); i++){
			JSONObject thisNode = (JSONObject) nodes.get(i);
			JSONArray children = thisNode.getJSONArray("children");
			JSONArray parents = thisNode.getJSONArray("parents");
			if(parents.size() > 1){
				errors.add("node_" +  thisNode.getString("cnid") + " has more than one parent");
				return false;
			} 
			
			if(parents.size() == 0)
				rootCount+=1;

			if(rootCount > 1){
				errors.add("Multuple root nodes");
				return false;
			}

			for(int j=0; j<children.size(); j++){
				if(parents.contains(children.get(i))){
					errors.add("node_" + thisNode.getString("cnid") + 
								" share the same parent and child: " + (String)children.get(i));
					return false;
				}
			}
		}
		return true;
	}

	public void handle(HttpExchange exchange) throws IOException{
			//check if the filter was hit up
			filterCheck(exchange);
			String contextGraphId = UUID.randomUUID().toString().substring(0,8);
			String requestMethod = exchange.getRequestMethod();
			if (requestMethod.equalsIgnoreCase("GET")) {
				System.out.println("GET heard");
			} else if (requestMethod.equalsIgnoreCase("PUT") || requestMethod.equalsIgnoreCase("POST")){
				System.out.println("PUT Heard");
				JSONObject jsonRequest = getJSONRequestBody(exchange);
				String type = jsonRequest.getString("type");

				if(type.equalsIgnoreCase("context_node")){
					handleContextNode(contextGraphId, jsonRequest);
				} else if (type.equalsIgnoreCase("context_edge")){
					handleContextEdge(contextGraphId, jsonRequest);
				} else if (type.equalsIgnoreCase("context_graph")){
					System.out.println("Processing context_graph");
					handleContextGraph(contextGraphId, jsonRequest);
				}
			}
	}

	private void handleContextNode(String cid, JSONObject contextNodeJson){
		try {
			String contextGraphId = cid;
			String thisNodeId = contextGraphId + "n-" + contextNodeJson.getString("name");
			System.out.println("Node id: " + thisNodeId);

			Integer tempNodeId = new Integer(contextNodeJson.getInt("cnid"));
			System.out.println("Temp node id: " + tempNodeId);

			//clean up associatedDevices list
			JSONArray devices = contextNodeJson.optJSONArray("AssociatedDevices");
			JSONArray validDevices = processDeviceList(devices);
			contextNodeJson.put("AssociatedDevices", validDevices);

			//clean up parents node list
			JSONArray nodeList = contextNodeJson.optJSONArray("parents");
			JSONArray validParents = processNodeList(nodeList);
			contextNodeJson.put("parents", validParents);

			//clean up children node list
			nodeList = contextNodeJson.optJSONArray("children");
			JSONArray validChildren = processNodeList(nodeList);
			contextNodeJson.put("children", validChildren);

			//check context node id
			Integer cnid = new Integer(contextNodeJson.getInt("cnid"));

			contextNodeJson.put("cnid", thisNodeId);
			System.out.println("cnid: " + thisNodeId);
			ContextGraphNode thisNode = new ContextGraphNode(contextNodeJson);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private JSONArray processDeviceList(JSONArray devices){
		JSONArray validDevices = new JSONArray();
		if(devices != null){
			//check devices and only include the valid ones in the array that's returned
		}

		return validDevices;
	}

	private JSONArray processNodeList(JSONArray nodeList){
		JSONArray validNodeList = new JSONArray();
		if(nodeList != null){
			//filter nodeList
		}

		return validNodeList;
	}

	private void handleContextEdge(String cid, JSONObject contextEdgeJson){
		try{
			//check the source node -- replace internal id refernce with cnid of associated node
			String source = "blah1";
			contextEdgeJson.put("sourceNode", source);

			//check destination node
			String dest = "blah2";
			contextEdgeJson.put("destinationNode", dest);

			String ceid = cid + "e-" + contextEdgeJson.getString("name");
			contextEdgeJson.put("ceid", ceid);

			System.out.println("ceid: " + ceid);
			ContextGraphEdge edge = new ContextGraphEdge(contextEdgeJson);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	private void handleContextGraph(String cid, JSONObject cmap){
		try {
			JSONArray jsonNodes = cmap.getJSONArray("graph_nodes");
			JSONArray jsonEdges = cmap.optJSONArray("graph_edges");
			JSONArray errors = new JSONArray();
			if(!checkNames(jsonNodes, jsonEdges, errors))
				System.out.println("checkNames errors: " + errors.toString());
			if(!checkInternalIds(jsonNodes, jsonEdges, errors))
				System.out.println("checkInternalIds errors: " + errors.toString());
			
			addMissingEdges(jsonNodes, jsonEdges);
			addMissingFields(jsonNodes, jsonEdges);
			JSONObject o = new JSONObject();o.put("graph_nodes", jsonNodes);o.put("graph_edges", jsonEdges);
			System.out.println(o.toString());
			createInternalGraph(jsonNodes, jsonEdges);
			
			cmap.put("cid", cid);
			cmap.put("graph_nodes", jsonNodes);
			cmap.put("graph_edges", jsonEdges);
			String dotOutput = dotConversion(cmap);
			System.out.println(dotOutput);
			sendDot(cid, dotOutput, "jortiz@jortiz81.homelinux.com", "/var/www/is4/metadata/context/dot");
			/*JSONObject b = genTreeResourceList(cmap, errors);
			if(b==null)
				System.out.println(errors.toString());
			else
				System.out.println(b.toString());*/
			//System.out.println(cmap.toString());

			//generate resource tree
			String prefix = "/is4/Cory/lt/";
			JSONObject treeResourceList = genTreeResourceList(cmap, errors);
			if(treeResourceList != null){
				Set<String> nodeNameSet = treeResourceList.keySet();
				Iterator<String> nodeNameIter = nodeNameSet.iterator();
				while(nodeNameIter.hasNext()){
					String thisPath = prefix + nodeNameIter.next();
					thisPath = thisPath.replace(" ", "_");
					LoadTreeResource thisLTR = new LoadTreeResource(thisPath, LoadTreeResource.PANEL_ELEMENT);
					System.out.println("Adding resource: " + thisLTR.getURI());
					//RESTServer.addResource(thisLTR);
				}
			} else {
				System.out.println("errors" + errors.toString());
			}
		} catch(Exception e){
			e.printStackTrace();
		}
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
			//logger.log(Level.WARNING, "", e);
			e.printStackTrace();
			return false;
		}
		return true;
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
				Integer thisNodeId = new Integer(thisNode.getInt("cnid"));
				System.out.println("Adding node_id: " + thisNodeId.toString());
				internalNodeIds.put(thisNodeId, thisNode);
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
					else 
						System.out.println("parent id: " + thisParentId + " is NOT valid");
				}
				thisNode.put("parents", filteredParents);

				stage = "Checking child ids for node " + i + " in the array";
				JSONArray children = thisNode.getJSONArray("children");
				JSONArray filteredChildren = new JSONArray();
				for(j=0; j<children.size(); j++){
					Integer thisChildId = new Integer(children.getInt(j));
					if(internalNodeIds.containsKey(thisChildId))
						filteredChildren.add(thisChildId);
					else
						System.out.println("child id: " + thisChildId + " is NOT valud");
				}
				thisNode.put("children", filteredChildren);
				stage = "";

				//replace the internal node references
				internalNodeIds.put(new Integer(thisNode.getInt("cnid")), thisNode);
			}

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
					System.out.println("Could not fined one of the node for edge " + thisEdge.getString("name"));
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
			//logger.log(Level.WARNING, "JSONException caught", e);
			errors.add(stage);
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
			e.printStackTrace();
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
			//System.out.println("adding: (" + internalId.toString() + ", " + newCnid + ")");
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

	public String dotConversion(JSONObject cmap){
		try {
			StringBuffer dotBuf = new StringBuffer().append("digraph ContextMap_").append(cmap.getString("cid")).append("{");
			Hashtable<String, JSONObject> nodeLookupTable = new Hashtable<String, JSONObject>();
			JSONArray nodes = cmap.getJSONArray("graph_nodes");
			for(int i=0; i<nodes.size(); i++)
				nodeLookupTable.put(((JSONObject)nodes.get(i)).getString("cnid"), (JSONObject)nodes.get(i));

			JSONArray edges = cmap.getJSONArray("graph_edges");
			for(int i=0; i<edges.size(); i++){
				JSONObject thisEdge = (JSONObject) edges.get(i);
				String sourceCnid = thisEdge.getString("sourceNode");
				String destCnid = thisEdge.getString("destinationNode");
				String sourceName = nodeLookupTable.get(sourceCnid).getString("label");
				String destName =  nodeLookupTable.get(destCnid).getString("label");
				String dotEdge =  "\"" + sourceName + "\"->\"" + destName + "\";";
				dotBuf.append(dotEdge);
			}
			dotBuf.append("}");
			return dotBuf.toString();
		} catch(Exception e){
			//logger.log(Level.WARNING, "Error during conversion of graph to DOT", e);
			e.printStackTrace();
			return "";
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

			System.out.println(makePngCommand);
			Process p = Runtime.getRuntime().exec(makePngCommand);
			System.out.println(command);
			p = Runtime.getRuntime().exec(command);
			p = Runtime.getRuntime().exec(deleteCmd1);
			p = Runtime.getRuntime().exec(deleteCmd2);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private JSONObject getJSONRequestBody(HttpExchange exchange) {
		try{
			BufferedReader is = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
			String line="";
			StringBuffer bodyBuf = new StringBuffer();
			while((line=is.readLine())!=null){
				bodyBuf.append(line).append(" ");
			}
			
			return (JSONObject) JSONSerializer.toJSON(bodyBuf.toString());
		} catch (JSONException e){
			e.printStackTrace();
			JSONObject r = new JSONObject();
			JSONArray a = new JSONArray();
			a.add("Request Error: Invalid JSON format");
			r.put("operation", "add_metadata");
			r.put("status", "fail");
			r.put("errors", a);
			sendResponse(exchange, 200, r.toString());
		} catch (IOException ioe){
			ioe.printStackTrace();
		}
		return new JSONObject();
	}

	protected void sendResponse(HttpExchange exchange, int errorCode, String response){
		try{
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "application/json");
			exchange.sendResponseHeaders(errorCode, 0);

			OutputStream responseBody = exchange.getResponseBody();
			if(response!=null)
				responseBody.write(response.getBytes());
			responseBody.close();
		}catch(Exception e){
		}
	}

}
