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
    
    public synchronized void noteVertex(V v) {
        nodeIDs.put(nextID++, v);
    }
    
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

    /** Layout the nodes in the graph from scratch */
    public synchronized void relayout() {
        int core_size = k;
        int agg_size = k * k / 2;
        int edge_size = k * k / 2;
        
        int h = getSize().height;
        int w = getSize().width;
        
        int margin_y = 50;
        int core_y = margin_y;
        int agg_y  = h / 2;
        int edge_y = h - margin_y;
        
        int core_x_sep = w / (core_size + 1);
        int agg_x_sep  = w / (agg_size  + 1);
        int edge_x_sep = w / (edge_size + 1);
        
        for(int i=0; i<nodeIDs.size(); i++) {
            V v = nodeIDs.get(i);
            int core_id = i;
            int agg_id = i - core_size;
            int edge_id = i - core_size - agg_size;
            
            if(core_id < core_size) {
                v.setPos(getVertexX(core_x_sep, core_id), core_y);
            }
            else if(agg_id < agg_size) {
                v.setPos(getVertexX(agg_x_sep,  agg_id),  agg_y);
            }
            else if(edge_id < edge_size) {
                v.setPos(getVertexX(edge_x_sep, edge_id), edge_y);
            }
        }
    }
    
    /** computes the position of the i'th node when they are separated by sep */
    private int getVertexX(int sep, int i) {
        return sep / 2 + i * sep;
    }
}
