package org.openflow.gui.fv;

import java.util.ArrayList;

import org.openflow.gui.OpenFlowGUI;
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
public final class FlowVisorGUI {
    /** cannot instantiate this class */
    private FlowVisorGUI() {}
    
    /** run the front-end */
    public static void main(String args[]) {
        // get the server(s) to connect to
        ArrayList<Pair<String, Short>> servers = OpenFlowGUI.getServers(args);
        
        // create the data structure to track multiple connections ad topologies
        FVMultipleConnectionAndTopologyHandler mch = new FVMultipleConnectionAndTopologyHandler();
        
        // create a manager to handle drawing the topology info received by the connection
        FVLayoutManager gm = new FVLayoutManager(mch);
        
        // layout the nodes with the spring algorithm by default
        edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge> sl;
        sl = new edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge>(gm.getGraph());
        sl.setRepulsionRange(0); // don't repel
        gm.setLayout(sl);
        
        // create the initial connection(s)
        for(Pair<String, Short> server : servers) {
            FVConnectionHandler ch = new FVConnectionHandler(gm, server.a, server.b);
            mch.addConnectionManager(ch);
        }
        
        // start our managers
        gm.start();
        for(int i=0; i<mch.getNumConnectionManagers(); i++)
            mch.getConnectionManager(i).getConnection().start();
    }
}
