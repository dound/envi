package org.pzgui.layout;

import java.util.HashMap;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;

public class FatTreeLayout<V extends Vertex, E> extends AbstractLayout<V, E> implements IterativeContext {
    private int k;
    
    private HashMap<Integer, V> nodeIDs = new HashMap<Integer, V>();
    private int nextID = 0;
    
    /**
     * Constructs a new Fat Tree layout engine.
     */
    public FatTreeLayout(Graph<V,E> graph, int k) {
        super(graph);
        this.k = k;
    }

    /** This method always returns true as the FatTree layout is immediate. */
    public boolean done() {
        return true;
    }
    
    public void initialize() {}

    public void reset() {}
    
        /** This method is a no-op as the FatTree layout is immediate. */
    public void step() {}

    /** Gets the degree of the fat tree redundancy */
    public int getK() {
        return k;
    }
    
    /**
     * Notes when vertex was added to determine where in the fat tree it goes.
     * @param v  the vertex to tag
     * @return  whether v is a host or not
     */
    public synchronized boolean noteVertex(V v) {
        boolean isHost = nextID >= size_core() + size_agg() + size_edge(); 
        nodeIDs.put(nextID++, v);
        return isHost;
    }
    
    /** removes all vertex tags */
    public synchronized void clear() {
        nodeIDs.clear();
        nextID = 0;
    }
    
    /** Sets the degree of the fat tree redundancy */
    public void setK(int k) {
        if(this.k != k) {
            this.k = k;
            relayout();
        }
    }
    
    /** returns the number of switches in the core layer */
    public int size_core() {
        return k;
    }
    
    /** returns the number of switches in the aggregation layer */
    public int size_agg() {
        return k * k / 2;
    }
    
    /** returns the number of switches in the edge layer */
    public int size_edge() {
        return k * k / 2;
    }
    
    /** returns the number of hosts in the fat tree */
    public int size_host() {
        return k * k * k / 4;
    }

    /** Layout the nodes in the graph from scratch */
    public synchronized void relayout() {
        int core_size = size_core();
        int agg_size = size_agg();
        int edge_size = size_edge();
        int host_size = size_host();
        
        int h = getSize().height;
        int w = getSize().width;
        
        int margin_y = 30;
        int hAvail = h - margin_y * 2;
        int core_y = margin_y;
        int agg_y  = hAvail / 3;
        int edge_y = 2 * hAvail / 3;
        int host_y = h - margin_y;
        
        int core_x_sep = w / (core_size + 1);
        int agg_x_sep  = w / (agg_size  + 1);
        int edge_x_sep = w / (edge_size + 1);
        int host_x_sep = w / (host_size + 1);
        
        for(int i=0; i<nodeIDs.size(); i++) {
            V v = nodeIDs.get(i);
            int core_id = i;
            int agg_id = i - core_size;
            int edge_id = i - core_size - agg_size;
            int host_id = i - core_size - agg_size - edge_size;
            
            if(core_id < core_size) {
                v.setPos(getVertexX(core_x_sep, core_id), core_y);
            }
            else if(agg_id < agg_size) {
                v.setPos(getVertexX(agg_x_sep,  agg_id),  agg_y);
            }
            else if(edge_id < edge_size) {
                v.setPos(getVertexX(edge_x_sep, edge_id), edge_y);
            }
            else if(host_id < host_size) {
                v.setPos(getVertexX(host_x_sep, host_id), host_y);
            }
        }
    }
    
    /** computes the position of the i'th node when they are separated by sep */
    private int getVertexX(int sep, int i) {
        return sep / 2 + i * sep;
    }
}
