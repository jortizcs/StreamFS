/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 */

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.algorithms.layout.util.VisRunner;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.event.GraphEvent;
import edu.uci.ics.jung.graph.event.GraphEventListener;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.util.Animator;

import org.apache.commons.collections15.functors.ConstantTransformer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRootPane;

import java.util.*;
import java.net.*;
import net.sf.json.*;
import java.io.*;
import java.util.logging.*;

/**
 * A variation of AddNodeDemo that animates transitions between graph states.
 *
 * @author Tom Nelson
 */
public class AnimatedMetadataGraph extends javax.swing.JApplet {
	private static Logger logger = Logger.getLogger(Object.class.getPackage().getName());
	private static HashMap<String, JSONObject> rmap = new HashMap<String, JSONObject>();

    /**
	 *
	 */
	private static final long serialVersionUID = -5345319851341875800L;

	private Graph<String,String> g = null;

    private VisualizationViewer<String,String> vv = null;

    private AbstractLayout<String,String> layout = null;

    Timer timer;

    boolean done;

    protected JButton switchLayout;

    public static final int EDGE_LENGTH = 100;

    @Override
    public void init() {

        //create a graph
    	Graph<String,String> ig = Graphs.<String,String>synchronizedDirectedGraph(new DirectedSparseMultigraph<String,String>());

        ObservableGraph<String,String> og = new ObservableGraph<String,String>(ig);
        og.addGraphEventListener(new GraphEventListener<String,String>() {

			public void handleGraphEvent(GraphEvent<String, String> evt) {
				System.err.println("got "+evt);

			}});
        this.g = og;
        //create a graphdraw
        layout = new FRLayout<String,String>(g);
        layout.setSize(new Dimension(600,600));
		Relaxer relaxer = new VisRunner((IterativeContext)layout);
		relaxer.stop();
		relaxer.prerelax();

		Layout<String,String> staticLayout =
			new StaticLayout<String,String>(g, layout);

        vv = new VisualizationViewer<String,String>(staticLayout, new Dimension(600,600));

        JRootPane rp = this.getRootPane();
        rp.putClientProperty("defeatSystemEventQueueCheck", Boolean.TRUE);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(java.awt.Color.lightGray);
        getContentPane().setFont(new Font("Serif", Font.PLAIN, 12));

        vv.setGraphMouse(new DefaultModalGraphMouse<Number,Number>());

        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
        vv.setForeground(Color.black);

        vv.addComponentListener(new ComponentAdapter() {

			/**
			 * @see java.awt.event.ComponentAdapter#componentResized(java.awt.event.ComponentEvent)
			 */
			@Override
			public void componentResized(ComponentEvent arg0) {
				super.componentResized(arg0);
				System.err.println("resized");
				layout.setSize(arg0.getComponent().getSize());
			}});

        getContentPane().add(vv);
        switchLayout = new JButton("Switch to SpringLayout");
        switchLayout.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            public void actionPerformed(ActionEvent ae) {
            	Dimension d = vv.getSize();//new Dimension(600,600);
                if (switchLayout.getText().indexOf("Spring") > 0) {
                    switchLayout.setText("Switch to FRLayout");
                    layout =
                    	new SpringLayout<String,String>(g, new ConstantTransformer(EDGE_LENGTH));
                    layout.setSize(d);
            		Relaxer relaxer = new VisRunner((IterativeContext)layout);
            		relaxer.stop();
            		relaxer.prerelax();
            		StaticLayout<String,String> staticLayout =
            			new StaticLayout<String,String>(g, layout);
    				LayoutTransition<String,String> lt =
    					new LayoutTransition<String,String>(vv, vv.getGraphLayout(),
    							staticLayout);
    				Animator animator = new Animator(lt);
    				animator.start();
    			//	vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
    				vv.repaint();

                } else {
                    switchLayout.setText("Switch to SpringLayout");
                    layout = new FRLayout<String,String>(g, d);
                    layout.setSize(d);
            		Relaxer relaxer = new VisRunner((IterativeContext)layout);
            		relaxer.stop();
            		relaxer.prerelax();
            		StaticLayout<String,String> staticLayout =
            			new StaticLayout<String,String>(g, layout);
    				LayoutTransition<String,String> lt =
    					new LayoutTransition<String,String>(vv, vv.getGraphLayout(),
    							staticLayout);
    				Animator animator = new Animator(lt);
    				animator.start();
    			//	vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
    				vv.repaint();

                }
            }
        });

        getContentPane().add(switchLayout, BorderLayout.SOUTH);

        timer = new Timer();
    }

    @Override
    public void start() {
        validate();
        //set timer so applet will change
        timer.schedule(new RemindTask(), 5000, 5000); //subsequent rate
        vv.repaint();
    }

    Integer v_prev = null;

    public void process() {

    	vv.getRenderContext().getPickedVertexState().clear();
    	vv.getRenderContext().getPickedEdgeState().clear();
        try {

            if (g.getVertexCount() < 100) {
                //add a vertex
                Integer v1 = new Integer(g.getVertexCount());

                g.addVertex(v1.toString());
                vv.getRenderContext().getPickedVertexState().pick(v1.toString(), true);

                // wire it to some edges
                if (v_prev != null) {
                	Integer edge = g.getEdgeCount();
                	vv.getRenderContext().getPickedEdgeState().pick((new Integer(edge)).toString(), true);
                    g.addEdge((new Integer(edge)).toString(), v_prev.toString(), v1.toString());
                    // let's connect to a random vertex, too!
                    int rand = (int) (Math.random() * g.getVertexCount());
                    edge = g.getEdgeCount();
                	vv.getRenderContext().getPickedEdgeState().pick((new Integer(edge)).toString(), true);
                   g.addEdge((new Integer(edge)).toString(), v1.toString(), (new Integer(rand)).toString());
                }

                v_prev = v1;

                layout.initialize();

        		Relaxer relaxer = new VisRunner((IterativeContext)layout);
        		relaxer.stop();
        		relaxer.prerelax();
        		StaticLayout<String,String> staticLayout =
        			new StaticLayout<String,String>(g, layout);
				LayoutTransition<String,String> lt =
					new LayoutTransition<String,String>(vv, vv.getGraphLayout(),
							staticLayout);
				Animator animator = new Animator(lt);
				animator.start();
//				vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
				vv.repaint();

            } else {
            	done = true;
            }

        } catch (Exception e) {
            System.out.println(e);

        }
    }

	JSONObject lastResourceMap = null;

	public void process2(){
		try {
			//URL local = new URL("http://localhost:8081");
			URL local = new URL("http://192.168.1.104:8081");
			JSONObject resources = AnimatedMetadataGraph.getAllResources(local);
			//System.out.println(resources.toString());
			
			//add all the vertices first 
			//since we don't know the order in which vertices will appear on the list
			//we want to make sure we can add the necessary edges
			Iterator keys = resources.keys();
			while(keys.hasNext()){
				String rName = (String) keys.next();
				String rType = resources.getJSONObject(rName).getString("type");
				if(!g.containsVertex(rName)){
					g.addVertex(rName);
					//vv.getRenderContext().getPickedVertexState().pick(rType.toString(), true);
				}
			}
			
			//add all the edges between them
			keys = resources.keys();
			while(keys.hasNext()){
				String rName = (String) keys.next();
				String rType = resources.getJSONObject(rName).getString("type");
				String parent= getParentPath(rName);
				String parent2 = null;
				if(parent != null){
					if(parent.endsWith("/"))
						parent2 = parent.substring(0,parent.length()-1);
					else
						parent2 = parent + "/";
				}
				if(g.containsVertex(rName) && (g.containsVertex(parent) || g.containsVertex(parent2)) && !g.isNeighbor(parent, rName)){
					Integer edge = g.getEdgeCount();
					System.out.println("Adding Edge: (" + parent + ", " + rName + ")");
                	vv.getRenderContext().getPickedEdgeState().pick(rType, true);
					if(g.containsVertex(parent))
                    	g.addEdge(edge.toString(), parent, rName);
					else
						g.addEdge(edge.toString(), parent2, rName);
				}
			}
			
			//remove any vertices that are no longer part of the resource graph
			if(lastResourceMap != null){
				keys = lastResourceMap.keys();
				while(keys.hasNext()){
					String thisVertexName = (String) keys.next();
					if(!resources.containsKey(thisVertexName) && g.containsVertex(thisVertexName))
						g.removeVertex(thisVertexName);
				}
			}
			
			lastResourceMap = resources;
			
			
			layout.initialize();

    		Relaxer relaxer = new VisRunner((IterativeContext)layout);
    		relaxer.stop();
    		relaxer.prerelax();
    		StaticLayout<String,String> staticLayout =
    			new StaticLayout<String,String>(g, layout);
			LayoutTransition<String,String> lt =
				new LayoutTransition<String,String>(vv, vv.getGraphLayout(),
						staticLayout);
			Animator animator = new Animator(lt);
			animator.start();
//				vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
			vv.repaint();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

    class RemindTask extends TimerTask {

        @Override
        public void run() {
			process2();
            if(done) cancel();

        }
    }

    public static void main(String[] args) {
		
    	AnimatedMetadataGraph and = new AnimatedMetadataGraph();
    	JFrame frame = new JFrame();
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	frame.getContentPane().add(and);

    	and.init();
    	and.start();
    	frame.pack();
    	frame.setVisible(true);
    }

	public static JSONObject getAllResources(URL sfsUrl){
		if(sfsUrl != null){
			try{
				URL sfsGetRsrcsUrl = new URL(sfsUrl.toString() + "/admin/listrsrcs/");
				URLConnection smapConn = sfsGetRsrcsUrl.openConnection();
				smapConn.setConnectTimeout(5000);
				smapConn.connect();

				//GET reply
				BufferedReader reader = new BufferedReader(new InputStreamReader(smapConn.
											getInputStream()));
				StringBuffer lineBuffer = new StringBuffer();
				String line = null;
				while((line = reader.readLine()) != null)
					lineBuffer.append(line);
				line = lineBuffer.toString();
				reader.close();

				return (JSONObject) JSONSerializer.toJSON(line);
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
				return null;
			}
		}
		return null;
	}
	
	public static JSONArray getChildren(URL sfsUrl, String path){
		if(sfsUrl != null){
			try{
				URL sfsGetRsrcsUrl = new URL(sfsUrl.toString() + path );
				URLConnection smapConn = sfsGetRsrcsUrl.openConnection();
				smapConn.setConnectTimeout(5000);
				smapConn.connect();

				//GET reply
				BufferedReader reader = new BufferedReader(new InputStreamReader(smapConn.
											getInputStream()));
				StringBuffer lineBuffer = new StringBuffer();
				String line = null;
				while((line = reader.readLine()) != null)
					lineBuffer.append(line);
				line = lineBuffer.toString();
				reader.close();

				JSONObject resp = (JSONObject) JSONSerializer.toJSON(line);
				return resp.optJSONArray("children");
			} catch(Exception e){
				logger.log(Level.WARNING, "", e);
				return null;
			}
		}
		return null;
	}
	
	public static String getParentPath(String childPath){
		if(!childPath.equals("/")){
			Vector<String> pathElements = new Vector<String>();
			StringTokenizer tokenizer = new StringTokenizer(childPath, "/");
			while(tokenizer.hasMoreTokens())
				pathElements.add(tokenizer.nextToken());
		
			StringBuffer parentPath = new StringBuffer().append("/");
			for(int i=0; i<pathElements.size()-1; i++)
				parentPath.append(pathElements.get(i)).append("/");
			return parentPath.toString();
		}
		return null;
		
	}
}







