package org.openflow.gui;

import org.pzgui.DialogHelper;
import org.pzgui.PZManager;
import org.pzgui.layout.Edge;
import org.pzgui.layout.PZLayoutManager;
import org.pzgui.layout.Vertex;

/**
 * Provides static methods for running the GUI.
 * 
 * @author David Underhill
 */
public final class OpenFlowGUI {
    private OpenFlowGUI() { /* this class may not be instantiated */ }
    
    /**
     * Gets the server to connect to.
     * 
     * @param args  the command-line arguments to extract a server from; if
     *              one is not provided then the user will be prompted for one
     * 
     * @return  the server to connect to
     */
    public static String getServer(String args[]) {
        // use the first argument as our server name, if provided
        String server = null;
        if(args.length > 0)
            server = args[0];
        
        // ask the user for the backend's IP if it wasn't already given
        if(server == null || server.length()==0)
            server = DialogHelper.getInput("What is the IP or hostname of the backend?", Options.DEFAULT_SERVER_IP);

        if(server == null) {
            System.out.println("Goodbye");
            System.exit(0);
        }
        
        return server;
    }
    
    /**
     * Creates a connection which will populate a new topology.
     * 
     * @param manager            the manager of the GUI elements
     * @param server             the IP or hostname where the back-end is located
     * @param port               the port the back-end is listening on
     * @param subscribeSwitches  whether to subscribe to switch changes
     * @param subscribeLinks     whether to subscribe to link changes
     */
    public static ConnectionHandler makeDefaultConnection(PZManager manager,
                                                          String server, Short port,
                                                          boolean subscribeSwitches,
                                                          boolean subscribeLinks) {
        return new ConnectionHandler(new Topology(manager), server, port, subscribeSwitches, subscribeLinks);
    }
    
    /** 
     * Run a simple version of the GUI by starting a single connection which 
     * will populate a single topology drawn by a PZLayoutManager.
     */
    public static void main(String args[]) {
        String server = getServer(args);
        Short port = Options.DEFAULT_PORT;
        
        // create a manager to handle drawing the topology info received by the connection
        PZLayoutManager gm = new PZLayoutManager();
        
        // layout the nodes with the spring algorithm by default
        gm.setLayout(new edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge>(gm.getGraph()));
        
        // create a manager to handle the connection itself
        ConnectionHandler cm = makeDefaultConnection(gm, server, port, true, true);
        
        // start our managers
        gm.start();
        cm.getConnection().start();
    }
}
