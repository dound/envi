package org.pzgui.layout;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import org.pzgui.Drawable;
import org.pzgui.PZWindow;

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
    
    /** size of the border area around the layout area where nodes should not be placed */
    private int border = 0;
    
    /** maximum size of the layout area */
    private Dimension maxLayoutSize = new Dimension(500, 500);
    
    /** whether to automatically recompute layout size based on the window size */
    private boolean autoRecomputeLayoutSize = true;
    
    /** whether to relayout when a node is dragged */
    private boolean relayoutAfterManualChange = true;
    
    public synchronized void addDrawable(Drawable d) {
        super.addDrawable(d);
        
        // initially position the node randomly
        if(d instanceof AbstractLayoutable) {
            AbstractLayoutable al = (AbstractLayoutable)d;
            if(!layoutablePositions.containsKey(al.getID()))
                al.setPos((int)(Math.random()*1024), (int)(Math.random()*768));
        }            
        
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
                
                // allow the layout to react to user-induced changes
                if(isRelayoutAfterManualChange())
                    layout.reset();
            }
            else {
                // update each vertex based on the layout's update coordinates
                pt = layout.transform(v);
                
                // if an error occurs in the layout algorithm, try to recover 
                if(Double.isNaN(pt.getX())) {
                    if(Double.isNaN(v.getPos().getX()))
                        v.setPos((int)(Math.random()*maxLayoutSize.width), 
                                 (int)(Math.random()*maxLayoutSize.height));
                    
                    layout.setLocation(v, v.getPos());
                }
                else
                    v.setPos((int)pt.getX() + border, (int)pt.getY() + border, false);
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
        this.layout = layout;
        if(this.layout == null)
            return;
        
        this.layout.setGraph(graph);
        updateLayoutSize();
        this.layout.reset();
        
        for(Vertex v : graph.getVertices()) {
            this.layout.setLocation(v, v.getPos());
            v.unsetPositionChanged();
        }
        
        // prevent vertices from occupying the same space
        for(Vertex v1 : graph.getVertices()) {
            for(Vertex v2 : graph.getVertices()) {
                if(v1 != v2) {
                    if(v1.getPos().equals(v2.getPos())) {
                        v1.setPos((int)(Math.random()*maxLayoutSize.width), 
                                  (int)(Math.random()*maxLayoutSize.height));
                        break;
                    }
                }
            }
        }
    }
    
    /** Get the maximum size the layout engine will use for laying out elements. */
    public Dimension getLayoutSize() {
        return maxLayoutSize;
    }

    /** Get the maximum height the layout engine will use for laying out elements. */
    public int getLayoutHeight() {
        return maxLayoutSize.height;
    }

    /** Get the maximum width the layout engine will use for laying out elements. */
    public int getLayoutWidth() {
        return maxLayoutSize.width;
    }
    
    /** 
     * Gets the size (in pixels) of the border to leave clear on each side of 
     * the layout. 
     */
    public int getBorderSize() {
        return border; 
    }

    /** 
     * Sets the size (in pixels) of the border to leave clear on each side of  
     * the layout. 
     */
    public void setBorderSize(int border) {
        this.border = border;
        updateLayoutSize();
    }
    
    /** Set the maximum size the layout engine will use for laying out elements. */
    public void setLayoutSize(int width, int height) {
        maxLayoutSize.setSize(width, height);
        updateLayoutSize();   
    }
    
    /** updates the size of the current layout after taking border into account */
    private void updateLayoutSize() {
        if(layout != null)
            layout.setSize(new Dimension(maxLayoutSize.width  - 2*border,
                                         maxLayoutSize.height - 2*border));
    }

    /** 
     * Sets the layout size to the size of the visible area.  It determines the
     * area to layout in based on the minimum and maximum x and y coordinates 
     * visible in all windows.  
     */
    public void setLayoutSizeBasedOnVisibleArea() {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        synchronized(windows) {
            for(PZWindow w : windows) {
                minX = Math.min(minX, w.getX());
                minY = Math.min(minY, w.getY());
                
                maxX = Math.max(maxX, w.getX() + w.getWidth() - w.getReservedWidthRight());
                maxY = Math.max(maxY, w.getY() + w.getHeight() - w.getReservedHeightBottom());
            }
        }
        
        this.setLayoutSize(maxX-minX, maxY-minY-25);
    }
    
    public void attachWindow(final PZWindow w) {
        super.attachWindow(w);
        if(isAutoRecomputeLayoutSize())
            setLayoutSizeBasedOnVisibleArea();
    }

    public void closeWindow(PZWindow w) {
        super.closeWindow(w);
        if(isAutoRecomputeLayoutSize())
            setLayoutSizeBasedOnVisibleArea();
    }
    
    /** Recomputes the layout size if isAutoRecomputeLayoutSize() is true */
    public void windowResized(PZWindow window) {
        super.windowResized(window);
        if(isAutoRecomputeLayoutSize())
            setLayoutSizeBasedOnVisibleArea();
    }

    /**
     * If true, then the layout size will be recomputed when a window is added,
     * removed, or resized.
     * 
     * @return whether to automatically recompute layout size based on the window size
     */
    public boolean isAutoRecomputeLayoutSize() {
        return autoRecomputeLayoutSize;
    }
    
    /** Sets whether to automatically recompute layout size based on the window size */
    public void setAutoRecomputeLayoutSize(boolean autoRecomputeLayoutSize) {
        this.autoRecomputeLayoutSize = autoRecomputeLayoutSize;
    }

    /** Gets whether to relayout when a node is dragged */
    public boolean isRelayoutAfterManualChange() {
        return relayoutAfterManualChange;
    }
    
    /** Sets whether to relayout when a node is dragged */
    public void setRelayoutAfterManualChange(boolean b) {
        this.relayoutAfterManualChange = b;
    }
}
