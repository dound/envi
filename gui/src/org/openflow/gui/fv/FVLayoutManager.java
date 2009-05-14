package org.openflow.gui.fv;

import java.awt.Graphics2D;
import java.util.LinkedList;

import org.pzgui.Drawable;
import org.pzgui.PZWindow;
import org.pzgui.layout.Layoutable;
import org.pzgui.layout.PZLayoutManager;

/**
 * Draws multiple topologies each as a 2D slices of a 3D area.
 * 
 * @author David Underhill
 */
public class FVLayoutManager extends PZLayoutManager {
    /** tracks the active connections and topologies */
    private final FVMultipleConnectionAndTopologyHandler mch;
    
    /** the slices being displayed */
    private final LinkedList<DisplaySlice> displaySlices = new LinkedList<DisplaySlice>();
    
    /**
     * Construct the FlowVisor GUI layout manager.
     * 
     * @param mch        will contain all connections and topologies
     */
    public FVLayoutManager(FVMultipleConnectionAndTopologyHandler mch) {
        super();
        this.mch = mch;
    }
    
    /** adds a slice to be displayed */
    public void addDisplaySlice(FVTopology topology) {
        DisplaySlice ds = new DisplaySlice();
        ds.addTopology(topology);
        displaySlices.add(ds);
    }
    
    /** Augment the superclass implementation by drawing the slice planes */
    public void preRedraw(PZWindow window) {
        super.preRedraw(window);
        Graphics2D gfx = window.getDisplayGfx();
        if(gfx == null)
            return;
        
        for(DisplaySlice ds : displaySlices)
            ds.draw(gfx);
    }
    
    /** 
     * Draws the items associated with d which need to be drawn before it in the
     * appropriate plane(s) based on the slice(s) it lives in. 
     */
    protected void drawBeforeObject(Graphics2D gfx, Drawable d) {
        drawWrap(gfx, d, true);
    }
    
    /** 
     * Draws d in the appropriate plane(s) based on the slice(s) it lives in.
     */
    protected void drawObject(Graphics2D gfx, Drawable d) {
        drawWrap(gfx, d, false);
    }
    
    /**
     * Applies the necessary slice transformation and then draws d.
     * 
     *  @param gfx     where to draw
     *  @param d       what to draw
     *  @param before  if true, d.drawBeforeObject(gfx) is called, else d.drawObject(gfx)
     */
    private void drawWrap(Graphics2D gfx, Drawable d, boolean before) {
        if(!(d instanceof Layoutable)) {
            draw(gfx, d, before);
            return;
        }
        
        Layoutable l = (Layoutable)d;
        for(DisplaySlice ds : displaySlices) {
            if(ds.hasNode(l.getID())) {
                ds.apply(gfx, d);
                draw(gfx, d, before);
                ds.unapply(gfx, d);
            }
        }
    }
    
    /**
     * Draws d.
     * 
     *  @param gfx     where to draw
     *  @param d       what to draw
     *  @param before  if true, d.drawBeforeObject(gfx) is called, else d.drawObject(gfx)
     */
    private void draw(Graphics2D gfx, Drawable d, boolean before) {
        if(before)
            d.drawBeforeObject(gfx);
        else
            d.drawObject(gfx);
    }
    
    private int numVisibleSlices() {
        int ret = 0;
        for(DisplaySlice ds : displaySlices)
            if(ds.isVisible())
                ret += 1;
        
        return ret;
    }
    
    /**
     * Augments the superclass implementation so that it computes layout 
     * positions based on slice size instead of window size.  It causes slices
     * to update their transforms according to the new layout area.
     */
    public void setLayoutSize(int width, int height) {
        int sliceHeight = height/Math.max(1, numVisibleSlices());
        super.setLayoutSize(width, sliceHeight);
        
        int i = 0;
        for(DisplaySlice ds : displaySlices) {
            ds.updateDisplayTransform(0, sliceHeight * i, width, sliceHeight);
            i += 1;
        }
    }
}
