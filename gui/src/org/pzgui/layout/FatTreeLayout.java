package org.pzgui.layout;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;

public class FatTreeLayout<V, E> extends AbstractLayout<V, E> implements IterativeContext {
    private int k;
    
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
    
    /** Sets the degree of the fat tree redundancy */
    public void setK(int k) {
        if(this.k != k) {
            this.k = k;
            relayout();
        }
    }

    /** Layout the nodes in the graph from scratch */
    public void relayout() {
        
    }
}
