package org.openflow.gui.fv;

import java.awt.Graphics2D;
import java.util.ArrayList;

import org.openflow.gui.drawables.Flow;
import org.openflow.gui.drawables.NodeWithPorts;
import org.pzgui.Drawable;
import org.pzgui.DrawableFilter;
import org.pzgui.PZWindow;
import org.pzgui.PZWindowEventListener;
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
    private final ArrayList<DisplaySlice> displaySlices = new ArrayList<DisplaySlice>();
    
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
    
    /**
     * Augments the superclass implementation by replacing the default event
     * listener with one which properly handles dragging nodes in slices.
     */
    public void attachWindow(final PZWindow w, boolean addDefaultEventListener) {
        super.attachWindow(w, false);
        
        if(addDefaultEventListener) {
            w.addEventListener(new PZWindowEventListener() {
                public void dragNode(Drawable selNode, int x, int y) {
                    // take into account slice offsets
                    if(sliceDrawableSelectedIn != null) {
                        x -= sliceDrawableSelectedIn.getXOffset();
                        y -= sliceDrawableSelectedIn.getYOffset();
                    }
                    
                    super.dragNode(selNode, x, y);
                }
            });
        }
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
     * Augments the collision detection algorithm to work with slices.
     */
    public Drawable selectFrom(int x, int y, DrawableFilter filter) {
        for(int i=0; i<displaySlices.size(); i++) {
            DisplaySlice ds = displaySlices.get(i);
            Drawable d = super.selectFrom(x-ds.getXOffset(), y-ds.getYOffset(), filter);
            if(d != null) {
                // make sure the drawable we got is actually being shown in this plane
                if(d instanceof NodeWithPorts) {
                    if(!ds.hasNode(((NodeWithPorts)d).getID()))
                        continue;
                }
                else if(d instanceof Flow) {
                    Flow f = (Flow)d;
                    if(f.getPath().length==0 || !ds.hasNode(f.getPath()[0].node.getID()))
                        continue;
                }
                else if(i != 0) {
                    // for Drawables which do not appear in planes, only select 
                    // them if the yOffset is 0 (since they are not translated)
                    continue;
                }
                
                sliceDrawableSelectedIn = ds; 
                return d;
            }
        }
        
        return null;
    }
    
    /** the slice in which the currently selected Drawable is in, if any */
    private DisplaySlice sliceDrawableSelectedIn = null;
    
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
