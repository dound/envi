package org.openflow.gui.ex;

import org.openflow.gui.ConnectionHandler;
import org.openflow.gui.OpenFlowGUI;
import org.openflow.gui.Options;
import org.openflow.gui.ex.EXConnectionHandler;
import org.openflow.gui.ex.EXLayoutManager;
import org.pzgui.layout.Edge;
import org.pzgui.layout.Vertex;

public final class Example {
    /** cannot instantiate this class */
    private Example() {}
    
    /** run the front-end */
    public static void main(String args[]) {
        String server = OpenFlowGUI.getServer(args);
        Short port = Options.DEFAULT_PORT;
        
        // create a manager to handle drawing the topology info received by the connection
        EXLayoutManager gm = new EXLayoutManager();
        
        // layout the nodes with the spring algorithm by default
        gm.setLayout(new edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge>(gm.getGraph()));
        
        // create a manager to handle the connection itself
        ConnectionHandler cm = new EXConnectionHandler(gm, server, port);
        
        // start our managers
        gm.start();
        cm.getConnection().start();
    }
}
