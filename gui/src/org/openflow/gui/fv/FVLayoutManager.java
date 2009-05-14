package org.openflow.gui.fv;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;

import org.openflow.gui.drawables.Node;
import org.pzgui.Drawable;
import org.pzgui.icon.Icon;
import org.pzgui.icon.ShapeIcon;
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
    
    /**
     * Construct the FlowVisor GUI layout manager.
     * 
     * @param mch        will contain all connections and topologies
     * @param numSlices  maximum number of topologies
     */
    public FVLayoutManager(FVMultipleConnectionAndTopologyHandler mch,
                           int numSlices) {
        super();
        this.mch = mch;
        this.numSlices = numSlices;
        
        // pre-compute transformations for each slice
        ArrayList<SliceTransform> xforms = new ArrayList<SliceTransform>();
        Paint slicePaints[] = new Paint[]{Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.PINK, Color.ORANGE};
        for(int i=0; i<numSlices; i++) {
            AffineTransform t = computeTransform(i, 800);
            Paint p = slicePaints[i % slicePaints.length];
            
            try {
                xforms.add(new SliceTransform(t, p));
            } 
            catch (NoninvertibleTransformException e) {
                throw new Error("bad transform: " + e.getMessage() + ": " + t.toString());
            }
        }
        sliceTransforms = new SliceTransform[xforms.size()];
        for(int i=0; i<xforms.size(); i++)
            sliceTransforms[i] = xforms.get(i);
    }
    
    /** number of slices being shown */
    private int numSlices;
    
    /** computes the transform for a particular slice */
    private AffineTransform computeTransform(int slice, int windowHeight) {
        return AffineTransform.getTranslateInstance(0, windowHeight*slice/(double)numSlices);
    }
    
    /**
     * Specifies how to transform the current drawing matrix for a particular 
     * slice of the network.
     */
    private class SliceTransform {
        private final AffineTransform transform;
        private final AffineTransform transformInverse;
        private final Paint nodePaint;
        
        public SliceTransform(AffineTransform t, Paint p) throws NoninvertibleTransformException {
            transform = t;
            transformInverse = t.createInverse();
            nodePaint = p;
        }
        
        /** applies the transformation of this slice */
        public void apply(Graphics2D gfx, Drawable d) {
            AffineTransform t = gfx.getTransform();
            t.concatenate(transform);
            gfx.setTransform(t);
            
            if(d instanceof Node) {
                Icon icon = ((Node)d).getIcon();
                if(icon instanceof ShapeIcon)
                    ((ShapeIcon)icon).setFillColor(nodePaint);
            }
        }

        /** unapplies the transformation of this slice */
        public void unapply(Graphics2D gfx) {
            AffineTransform t = gfx.getTransform();
            t.concatenate(transformInverse);
            gfx.setTransform(t);
            
            // note: color is not changed; we assume that apply always sets the
            //       color so there is no need to reset it
        }
    }
    
    /** pre-computed information about how to display each slice */
    private final SliceTransform[] sliceTransforms;
    
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
     * Applies the necessary slic transformation and then draws d.
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
        for(int i=0; i<mch.getNumTopologies(); i++) {
            if(mch.getTopology(i).hasNode(l.getID())) {
                sliceTransforms[i].apply(gfx, d);
                draw(gfx, d, before);
                sliceTransforms[i].unapply(gfx);
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
}
