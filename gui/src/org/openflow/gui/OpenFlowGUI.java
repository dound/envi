package org.openflow.gui;

import java.util.ArrayList;

import org.openflow.util.Pair;
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
     * Run a simple version of the GUI by starting a single connection which 
     * will populate a single topology drawn by a PZLayoutManager.
     */
    public static void main(String args[]) {
        Pair<String, Short> serverPort = getServer(args);
        String server = serverPort.a;
        short port = serverPort.b;
        
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
     * Gets the IP[:PORT] to connect to.
     * 
     * @param args  the command-line arguments to extract a server from; if
     *              one is not provided then the user will be prompted for one
     * 
     * @return  the server to connect to
     */
    public static Pair<String, Short> getServer(String args[]) {
        ArrayList<Pair<String, Short>> servers = getServers(args, true);
        return servers.get(0);
    }
    
    /**
     * Gets the IP[:PORT](s) to connect to.  If no servers are specified, the 
     * user is prompted to specify one.  If the user does not specify anything
     * when prompted, then the program terminates.
     * 
     * 
     * @param args  the command-line arguments to extract server(s) from; if
     *              none are provided then the user will be prompted for one
     * 
     * @return  the server to connect to
     */
    public static ArrayList<Pair<String, Short>> getServers(String args[]) {
        return getServers(args, false);
    }
    
    private static ArrayList<Pair<String, Short>> getServers(String args[], boolean limitToOne) {
        // get the server(s) to connect to
        ArrayList<Pair<String, Short>> servers = new ArrayList<Pair<String, Short>>();
        
        if(args.length == 0) {
            // if none are specified, prompt the user like the base gui
            servers.add(promptForServer());
        }
        else {
            // each argument is a server to connect to
            for(String arg : args) {
                servers.add(parseServerIdentifier(arg));
                if(limitToOne)
                    break;
            }
        }
        
        if(servers.size() == 0) {
            System.out.println("Goodbye");
            System.exit(0);
        }
        
        return servers;
    }
    
    /**
     * Returns the parse of a IP[:PORT].  If PORT is omitted, the 
     * Options.DEFAULT_PORT is returned for the port value.
     * 
     * @return IP-port pair
     */
    public static Pair<String, Short> parseServerIdentifier(String s) {
        int indexOfColon = s.indexOf(':');
        
        String server;
        Short port = Options.DEFAULT_PORT;
        if(indexOfColon > 0) {
            server = s.substring(0, indexOfColon);
            String strPort = s.substring(indexOfColon + 1);
            try {
                port = Short.valueOf(strPort);
            }
            catch(NumberFormatException e) {
                throw new Error("Error: invalid port number: " + strPort);
            }
        }
        else
            server = s;
        
        return new Pair<String, Short>(server, port);
    }
    
    /**
     * Ask the user for the backend's IP[:PORT] in dialog box.
     */
    public static Pair<String, Short> promptForServer() {
        String server = DialogHelper.getInput("What is the IP or hostname of the backend?", 
                                              Options.DEFAULT_SERVER_IP);
        return parseServerIdentifier(server);
    }
}
