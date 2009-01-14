package org.pzgui.layout;

import java.awt.geom.Point2D;
import org.pzgui.Drawable;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;

/**
 * A simple extension of the PZManager which facilitates the automatic layout of
 * objects.
 * 
 * @author David Underhill
 */
public class PZLayoutManager extends org.pzgui.PZManager {
    /** the current layout */
    private Layout<Vertex, Edge> layout = null;
    
    /** the graph to layout */
    private final Graph<Vertex, Edge> graph = new DirectedSparseGraph<Vertex, Edge>();
    
    public synchronized void addDrawable(Drawable d) {
        super.addDrawable(d);
        
        if(d instanceof Vertex) {
            Vertex v = (Vertex)d;
            graph.addVertex(v);
            
            for(Object o : v.getEdges()) {
                Edge e = (Edge)o;
                graph.addEdge(e, e.getSource(), e.getDestination());
            }
        }
    }
    
    public synchronized void removeDrawable(Drawable d) {
        super.removeDrawable(d);
        
        if(d instanceof Vertex) {
            Vertex v = (Vertex)d;
            graph.removeVertex(v);
            
            for(Object o : v.getEdges())
                graph.removeEdge((Edge)o); 
        }
    }
    
    /**
     * Update the position of vertices after each redraw and advance the 
     * layout engine if it is an incremental layout engine.
     */
    protected void postRedraw() {
        // do nothing if there is no special layout engine installed
        if(layout == null)
            return;
        
        // update the layout if it is iterative
        if(layout instanceof IterativeContext) 
            ((IterativeContext)layout).step();
        
        Point2D pt;
        for(Vertex v : graph.getVertices()) {
            // if something external to the manager change a vertex, then
            // update the layout with the external position information
            if(v.hasPositionChanged()) {
                layout.setLocation(v, v.getPos());
                v.unsetPositionChanged();
            }
            else {
                // update each vertex based on the layout's update coordinates
                pt = layout.transform(v);
                v.setPos((int)pt.getX(), (int)pt.getY());
            }
        }
    }

    /** gets the graph backing the layout manager */
    public Graph<Vertex, Edge> getGraph() {
        return graph;
    }
    
    /** gets the current layout, if any */
    public Layout<Vertex, Edge> getLayout() {
        return layout;
    }
    
    /** 
     * sets the current layout
     * @param layout  the new layout, or null to turn off auto-layout
     */
    public synchronized void setLayout(Layout<Vertex, Edge> layout) {
        if(this.layout != null)
            this.layout.setGraph(null);
        
        this.layout = layout;
        this.layout.setGraph(graph);
        this.layout.reset();
        
        for(Vertex v : graph.getVertices()) {
            this.layout.setLocation(v, v.getPos());
            v.unsetPositionChanged();
        }
    }
}
