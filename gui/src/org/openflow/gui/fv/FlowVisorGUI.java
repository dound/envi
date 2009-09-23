package org.openflow.gui.fv;

import java.util.ArrayList;

import org.openflow.gui.OpenFlowGUI;
import org.openflow.gui.Options;
import org.openflow.gui.fv.FVConnectionHandler;
import org.openflow.gui.fv.FVLayoutManager;
import org.openflow.util.Pair;
import org.pzgui.layout.Edge;
import org.pzgui.layout.Vertex;

/**
 * Entry point to the FlowVisor GUI.
 * 
 * @author David Underhill
 */
class Triple<A,B,C>
{
	public A a;
	public B b;
	public C c;
	public Triple(A a, B b, C c)
	{
		this.a = a ; this.b = b; this.c = c;
	}

}
public final class FlowVisorGUI {
    /** cannot instantiate this class */
    private FlowVisorGUI() {}

    static final int MASTER_INDEX 	=  1;
    static final int ALL_INDEX 		=  4;

    
    /** run the front-end */
    public static void main(String args[]) {
        //ArrayList<Pair<String, Short>> servers = OpenFlowGUI.getServers(args);

	ArrayList<Triple<String, Integer, String>> servers = new ArrayList();
	boolean localConnection = false;

	for (int i = 0; i < args.length; i++)
	    if ((args[i].compareTo("lo") == 0))
		localConnection = true;

	if(localConnection) // do localhost
	{
		System.err.println("Connecting via localhost; hope you set up the tunnel");
		servers.add(new Triple("localhost",2504,"Slice: Plug-n-Serve"));
		servers.add(new Triple("localhost",2505,"Physical Network"));
		servers.add(new Triple("localhost",2506,"Slice: OpenPipes"));
		servers.add(new Triple("localhost",2501,"Slice: OpenRoads"));
		// servers.add(new Triple("localhost",2503,"Slice: Flow Dragging"));
		servers.add(new Triple("localhost",2507,"All Slices + Production"));
		servers.add(new Triple("localhost",2502,"Slice: Aggregation"));
	}
	else
	{
		System.err.println("Connecting directly... hope you're in Gates");
		servers.add(new Triple("openflow4.stanford.edu",2503, "Slice: Plug-n-Serve"));
		servers.add(new Triple("openflow5.stanford.edu",2503,"Physical Network"));
		servers.add(new Triple("hpn8.stanford.edu",2503, "Slice: OpenPipes"));
		servers.add(new Triple("openflow3.stanford.edu",2503, "Slice: OpenRoads"));
		servers.add(new Triple("openflow5.stanford.edu",2505,"All Slices + Production"));
		servers.add(new Triple("openflow6.stanford.edu",2503, "Slice: Aggregation"));
		//servers.add(new Triple("openflow5.stanford.edu",2504, "Slice Flow Dragging"));
	}
        
        // create the data structure to track multiple connections ad topologies
        FVMultipleConnectionAndTopologyHandler mch = new FVMultipleConnectionAndTopologyHandler();
        
        // create a manager to handle drawing the topology info received by the connection
        FVLayoutManager gm = new FVLayoutManager(mch);

        //gm.setMinSliceHeight(400);
	String yaml = "demo.yaml";
	for (int i = 0; i < args.length; i++)
	    if ((args[i].compareTo("fs") == 0)) {
		System.out.println("Set full screen...");
		gm.fullScreen = true;
	    } else if (args[i].compareTo("1024x768") == 0)
	    {
		gm.addWindow(0, 0, 1024, 768, 0, 0, 1.0f);
		yaml = "demo-1024x768.yaml";
	    } else if (args[i].compareTo("1280x1024") == 0)
	    {
		gm.addWindow(0, 0, 1280, 1024, 0, 0, 1.0f);
		yaml = "demo-1280x1024.yaml";
	    } else if (args[i].compareTo("800x600") == 0)
	    {
		gm.addWindow(0, 0, 800, 600, 0, 0, 1.0f);
		yaml = "demo-800x600.yaml";
		Options.ImageDir = "images-800x600";
	    }

        // layout the nodes with the spring algorithm by default
        edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge> sl;
        sl = new edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge>(gm.getGraph());
        sl.setRepulsionRange(0); // don't repel
        sl.setForceMultiplier(0);
        gm.setLayout(sl);
	gm.loadDrawablePositionsFromFile(yaml);

        
        // create the initial connection(s)
        for(Triple<String, Integer, String> server : servers) {
            FVConnectionHandler ch = new FVConnectionHandler(gm, 
	    	server.a, 
		server.b.shortValue(),		// stoopid java
		server.c);
            mch.addConnectionManager(ch);	
        }
        
        // start our managers
        gm.start();
        for(int i=0; i<mch.getNumConnectionManagers(); i++)
	{
            mch.getConnectionManager(i).getConnection().start();
	    if( i != FlowVisorGUI.MASTER_INDEX )	
	    {
		    DisplaySlice ds  = gm.getDisplaySlice(FlowVisorGUI.ALL_INDEX);
		    FVTopology t = (FVTopology)mch.getTopology(i);
		    ds.addTopology(t);
	    }

	}
    }
}
