package org.openflow.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.openflow.util.Pair;
import org.pzgui.DialogHelper;
import org.pzgui.PZManager;
import org.pzgui.PZWindow;
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
        PZWindow.BASE_TITLE = "ENVI: Network Monitor";
        
        Pair<String, Short> serverPort = getServer(args);
        String server = serverPort.a;
        short port = serverPort.b;
        
        // create a manager to handle drawing the topology info received by the connection
        PZLayoutManager gm = new PZLayoutManager();
        createGatesYaml();
        gm.loadDrawablePositionsFromFile("gates.yaml");
        
        // layout the nodes with the spring algorithm by default
        gm.setLayout(new edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge>(gm.getGraph()));
        
        // create a manager to handle the connection itself
        ConnectionHandler cm = makeDefaultConnection(gm, server, port, true, true);
        
        // start our managers
        gm.start();
        cm.getConnection().start();
    }
    
    /** ensures that gates.yaml exists (easier distribution than adding a resource to the JNLP file) */
    private static void createGatesYaml() {
        String fn = "gates.yaml";
        File f = new File(fn);
        if(!f.exists()) {
            java.io.FileOutputStream out;
            try {
                out = new java.io.FileOutputStream(fn);
                out.write("--- \n- \n    id: 00:00:00:0d:b9:16:f4:20\n    x: 613\n    y: 202\n- \n    id: 00:00:00:23:20:5c:df:e1\n    x: 609\n    y: 659\n- \n    id: 00:00:02:0d:b9:16:f3:c4\n    x: 535\n    y: 271\n- \n    id: 00:00:00:12:e2:b8:f3:ce\n    x: 665\n    y: 481\n- \n    id: 00:00:02:0d:b9:16:f2:dc\n    x: 189\n    y: 676\n- \n    id: 00:00:02:0d:b9:16:f2:50\n    x: 414\n    y: 493\n- \n    id: 00:00:02:0d:b9:16:f3:f8\n    x: 345\n    y: 573\n- \n    id: 00:00:02:0d:b9:16:ef:f0\n    x: 617\n    y: 92\n- \n    id: 00:00:02:0d:b9:16:f0:20\n    x: 106\n    y: 230\n- \n    id: 00:00:02:0d:b9:16:ef:c4\n    x: 78\n    y: 343\n- \n    id: 00:00:02:0d:b9:16:f2:58\n    x: 313\n    y: 478\n- \n    id: 00:00:02:0d:b9:16:f2:88\n    x: 360\n    y: 0\n- \n    id: 00:00:02:0d:b9:16:ef:98\n    x: 574\n    y: 0\n- \n    id: 00:00:02:0d:b9:16:f3:44\n    x: 0\n    y: 278\n- \n    id: 00:00:02:0d:b9:16:f4:30\n    x: 679\n    y: 0\n- \n    id: 00:00:02:0d:b9:16:f3:18\n    x: 0\n    y: 176\n- \n    id: 00:00:00:0d:b9:16:f3:a8\n    x: 469\n    y: 579\n- \n    id: 00:00:02:0d:b9:16:f4:64\n    x: 0\n    y: 0\n- \n    id: 00:00:02:0d:b9:16:f2:34\n    x: 800\n    y: 101\n- \n    id: 00:00:02:0d:b9:16:f2:f0\n    x: 316\n    y: 137\n- \n    id: 00:00:02:0d:b9:16:f4:0c\n    x: 269\n    y: 41\n- \n    id: 00:00:00:0d:b9:16:f4:3c\n    x: 729\n    y: 579\n- \n    id: 00:00:00:0d:b9:16:f3:e0\n    x: 49\n    y: 88\n- \n    id: 00:00:00:12:e2:78:31:f3\n    x: 771\n    y: 455\n- \n    id: 00:00:02:0d:b9:16:ef:a8\n    x: 414\n    y: 88\n- \n    id: 00:00:00:0d:b9:16:f4:40\n    x: 709\n    y: 305\n- \n    id: 00:00:02:0d:b9:16:f3:84\n    x: 443\n    y: 333\n- \n    id: 00:00:00:0d:b9:16:f0:08\n    x: 507\n    y: 431\n- \n    id: 00:00:00:12:e2:78:67:63\n    x: 591\n    y: 359\n- \n    id: 00:00:02:0d:b9:16:f3:e4\n    x: 95\n    y: 497\n- \n    id: 00:00:02:0d:b9:16:f4:44\n    x: 101\n    y: 0\n- \n    id: 00:00:02:0d:b9:16:f4:48\n    x: 6\n    y: 417\n- \n    id: 00:00:00:1b:3f:c5:47:00\n    x: 845\n    y: 528\n- \n    id: 00:00:00:0d:b9:16:f3:ec\n    x: 934\n    y: 577\n- \n    id: 00:00:02:0d:b9:16:f2:a4\n    x: 461\n    y: 0\n- \n    id: 00:00:02:0d:b9:16:ef:b4\n    x: 101\n    y: 607\n- \n    id: 00:00:00:aa:aa:aa:aa:aa\n    x: 402\n    y: 676\n- \n    id: 00:00:00:12:e2:98:a5:ce\n    x: 371\n    y: 248\n".getBytes());
                out.close();
            } 
            catch (FileNotFoundException e) {}
            catch (IOException e) {}
        }
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
