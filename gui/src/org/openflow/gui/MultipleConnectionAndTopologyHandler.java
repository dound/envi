package org.openflow.gui;

import java.util.concurrent.CopyOnWriteArrayList;

import org.pzgui.PZManager;

/**
 * This class tracks the connection(s) to the backend(s) and their associated 
 * topology(ies).  It may be helpful when populating more than one topology or 
 * receiving topology information from multiple connections.
 * 
 * @author David Underhill
 */
public class MultipleConnectionAndTopologyHandler<CH extends ConnectionHandler> {
    /** topology(ies) being tracked */
    private final CopyOnWriteArrayList<Topology> topologies;
    
    /** connection(s) to the backend */
    private final CopyOnWriteArrayList<CH> connections;
    
    
    /**
     * Initialize the multiple connection and topology manager with the initial
     * connection.
     */
    public MultipleConnectionAndTopologyHandler() {
        this(null);
    }
    
    /**
     * Initialize the multiple connection and topology manager with the initial
     * connection.
     * 
     * @param ch  the initial connection manager (if null then it starts with no
     *            connections)
     */
    public MultipleConnectionAndTopologyHandler(CH ch) {
        topologies = new CopyOnWriteArrayList<Topology>();
        connections = new CopyOnWriteArrayList<CH>();
        
        if(ch != null)
            addConnectionManager(ch);
    }
    
    
    // ------------- Connection Managers ------------ //
    
    /** 
     * Add a connection manager.  That manager's topology is registered in the
     * topology list if it is not already present.
     */
    public void addConnectionManager(CH conn) {
        connections.add(conn);
        if(!topologies.contains(conn.getTopology()))
            topologies.add(conn.getTopology());
    }
    
    /** 
     * Returns the requested connection to the backend.
     * 
     *  @param index  the index of the connection to get
     */
    public CH getConnectionManager(int index) {
        return connections.get(index);
    }
    
    /** Returns the number of connections being maintained */
    public int getNumConnectionManagers() {
        return connections.size();
    }
    
    /** 
     * Removes and shuts down the specified connection.  If it was the only 
     * connection populating a topology, then that topology is discarded from 
     * the list.
     * 
     * @param index  the index of the connection to remove
     */
    public void removeConnection(int index) {
        CH conn = connections.remove(index);
        conn.shutdown();
        
        // remove the topology if nobody references it now
        for(CH cm : connections)
            if(cm.getTopology()== conn.getTopology())
                return;
        topologies.remove(conn.getTopology());
    }
    
    /** 
     * Shuts down all connections in response to the GUI being shutdown.
     */
    public void pzClosing(PZManager manager) {
        long start = System.currentTimeMillis();
        
        // shutdown all connections
        CH conn = getConnectionManager(0);
        while(getNumConnectionManagers() > 0)
            removeConnection(0);
        
        Thread.yield();

        // wait until the connection has been torn down or 1sec has passed
        if(conn != null)
            while(!conn.getConnection().isShutdown() && System.currentTimeMillis()-start<1000) {}
    }
    
    
    // ----------------- Topologies ----------------- //

    /** 
     * Returns the requested connection to the backend.
     * 
     *  @param index  the index of the connection to get
     */
    public Topology getTopology(int index) {
        return topologies.get(index);
    }
    
    /** Returns the number of topologies being maintained */
    public int getNumTopologies() {
        return topologies.size();
    }
}
