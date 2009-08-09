package org.openflow.gui.fv;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;

import org.openflow.gui.Topology;
import org.openflow.gui.drawables.Flow;
import org.openflow.gui.drawables.Node;
import org.openflow.gui.drawables.OpenFlowSwitch;
import org.pzgui.Drawable;
import org.pzgui.icon.Icon;
import org.pzgui.icon.ShapeIcon;

/**
 * Contains a set of topologies to draw in a single display slice and
 * specifies how to transform the current drawing matrix for this 
 * display slice.
 * 
 * @author David Underhill
 */
public class DisplaySlice {
    /** how opaque to make the slice plane */
    public static final AlphaComposite COMPOSITE_SLICE_PLANE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f);
    
    /** whether to try to fake a 3D perspective mode (false => 2D mode) */
    public static final boolean USE_FAKE_PERSPECTIVE = false;
    
    public static final double FAKE_PERSPECTIVE_MAX_SHRINKAGE = 0.25;
    
    /** the topologies included in this slice */
    private final LinkedList<FVTopology> topologies = new LinkedList<FVTopology>();
    
    /** slice name */
    private String name = "empty";
    
    /** how to transform points from the original space to the slice's space */
    private AffineTransform transform;
    private AffineTransform transformInverse;
    
    /** fill for the slice plane itself */
    private Paint slicePaint = null;
    
    /** shape of the slice itself */
    private Shape sliceShape = null;
    
    /** whether this slice is visible */
    private boolean visible = true;
    
    /** x offset of this slice */
    private int xOffset = 0;
    
    /** y offset of this slice */
    private int yOffset = 0;
    
    /** height of this slice */
    private int sliceHeight = -1;
    
    /** width of this slice */
    private int sliceWidth = -1; 
    
    /** temporary for storing the original size of an icon pre-perspective scaling */
    private Dimension origSize;
    
    /** construct a new DisplaySlice containing no topologies */
    public DisplaySlice() {
        transformInverse = transform = new AffineTransform();
    }
    
    /** add a topology to this slice */
    public void addTopology(FVTopology topology) {
        if(topologies.size() == 0)
            name = topology.getName();
        
        if(topologies.contains(topology))
            return;
        
        topologies.add(topology);
        updateFillPatterns();
    }

    /** whether this display slice contains the specified topology */
    public boolean hasTopology(FVTopology t) {
        return topologies.contains(t);
    }
    
    /** remove a topology from this slice */
    public void removeTopology(FVTopology topology) {
        topologies.remove(topology);
        updateFillPatterns();
        
        if(topologies.size() == 0)
            name = "empty";
    }
    
    /** gets the name of the slice */
    public String getName() {
        return name;
    }
    
    /** sets the name of the slice */
    public void setName(String name) {
        this.name = name;
    }
    
    /** draw the slice itself */
    public void draw(Graphics2D gfx) {
        if(slicePaint == null || sliceShape == null || !isVisible())
            return;
        
        Composite c = gfx.getComposite();
        gfx.setComposite(COMPOSITE_SLICE_PLANE);
        gfx.setPaint(slicePaint);
        gfx.fill(sliceShape);
        gfx.setComposite(c);
    }

    /** get the x offset of the slice */
    public int getXOffset() {
        return xOffset;
    }
    
    /** get the y offset of the slice */
    public int getYOffset() {
        return yOffset;
    }
    
    /** get the height of the slice */
    public int getHeight() {
        return sliceHeight;
    }
    
    /** get the width of the slice */
    public int getWidth() {
        return sliceWidth;
    }
    
    /** whether this slice contains the specified flow */
    public boolean hasFlow(int id) {
        for(Topology t : topologies)
            if(t.hasFlow(id))
                return true;
        
        return false;
    }
    
    /** whether this slice contains the specified node */
    public boolean hasNode(long id) {
        for(Topology t : topologies)
            if(t.hasNode(id))
                return true;
        
        return false;
    }

    /** whether this slice is currently visible */
    public boolean isVisible() {
        return visible;
    }

    /** sets whether this slice is currently visible */
    public void setVisible(boolean b) {
        visible = b;
    }
    
    /** applies the transformation of this slice */
    public void apply(Graphics2D gfx, Drawable d) {
        AffineTransform t = gfx.getTransform();
        t.concatenate(transform);
        gfx.setTransform(t);
        
        if(d instanceof Node) {
            Node n = (Node)d;
            Icon icon = n.getIcon();
            if(icon instanceof ShapeIcon) {
                ShapeIcon si = (ShapeIcon)icon;
                si.setFillColor(getFillPatternWrap(n.getID(), OpenFlowSwitch.DEFAULT_SIZE, fillPatterns));
                
                if(USE_FAKE_PERSPECTIVE) {
                    double scale = 1.0 - (1.0 - n.getY() / (double)sliceHeight) * FAKE_PERSPECTIVE_MAX_SHRINKAGE;
                    origSize = si.getSize();
                    if(scale < 1.0)
                        si.setSize((int)(origSize.width*scale), (int)(origSize.height*scale));
                }
            }
        }
        else if(d instanceof Flow) {
            Flow f = (Flow)d;
            f.setPaint(getFillPatternWrap(f.getID(), f.getPointSize(), flowFillPatterns));
        }
    }

    /** un-applies the transformation of this slice */
    public void unapply(Graphics2D gfx, Drawable d) {
        AffineTransform t = gfx.getTransform();
        t.concatenate(transformInverse);
        gfx.setTransform(t);
        
        if(d instanceof Node) {
            Node n = (Node)d;
            Icon icon = n.getIcon();
            if(icon instanceof ShapeIcon) {
                if(USE_FAKE_PERSPECTIVE)
                    ((ShapeIcon)icon).setSize(origSize);
            }
        }
        
        // note: color is not changed; we assume that apply always sets the
        //       color so there is no need to reset it
    }

    /** updates the transform for this slice */
    public void updateDisplayTransform(int xOffset, int yOffset, int sliceWidth, int sliceHeight) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.sliceHeight = sliceHeight;
        this.sliceWidth = sliceWidth;
        
        if(USE_FAKE_PERSPECTIVE) {
            int pinch = sliceWidth / 10;
            int[] xs = new int[]{xOffset+pinch, xOffset+sliceWidth, xOffset+sliceWidth-pinch, xOffset};
            int[] ys = new int[]{yOffset, yOffset, yOffset+sliceHeight-10, yOffset+sliceHeight-10};
            sliceShape = new Polygon(xs, ys, xs.length);
        }
        else
            sliceShape = new Rectangle2D.Double(xOffset, yOffset, sliceWidth, sliceHeight);
        
        transform = AffineTransform.getTranslateInstance(xOffset, yOffset);
        try {
            transformInverse = transform.createInverse();
        }
        catch(NoninvertibleTransformException e) {
            System.err.println("updateDisplayTransform() failed to invert transform: " + e.getMessage());
            transformInverse = transform = new AffineTransform(); // identity
        }
    }
    
    /** updates the slice and topology combination fill patterns */
    public void updateFillPatterns() {
        if(topologies.size() == 0)
            return;
        
        // update the color of the slice
        int r=0, g=0, b=0;
        for(FVTopology t : topologies) {
            r += t.getFillColor().getRed();
            g += t.getFillColor().getGreen();
            b += t.getFillColor().getBlue();
        }
        r /= topologies.size();
        g /= topologies.size();
        b /= topologies.size();
        slicePaint = new Color(r, g, b);
            
        fillPatterns.clear();
    }
    
    /** 
     * A cache of the fill patterns for all combinations of topologies.  The 
     * key is a bitmask of which topology colors the fill includes.  Bit 0 
     * signals that the first topology's color is included, and so on.
     */
    private final HashMap<Integer, Paint> fillPatterns = new HashMap<Integer, Paint>();
    private final HashMap<Integer, Paint> flowFillPatterns = new HashMap<Integer, Paint>();
    private final int[] topology_indices = new int[32];
    
    /** gets the fill pattern for the node associated with the specified ID */
    private Paint getFillPatternWrap(long id, int sz, HashMap<Integer, Paint> cache) {
        int i = 0, j = 0, n = 0;
        for(FVTopology t : topologies) {
            if((cache==fillPatterns && t.hasNode(id)) || (cache==flowFillPatterns && t.hasFlow((int)id))) {
                n += 1;
                topology_indices[i++] = j;
            }
            
            j += 1;
        }
        
        return getFillPattern(n, sz, cache);
    }
    
    /** gets the fill pattern some set of topologies (uses a static array) */
    private Paint getFillPattern(int n, int sz, HashMap<Integer, Paint> cache) {
        int bitmask = 0;
        for(int i=0; i<n; i++)
            bitmask += (1 << topology_indices[i]);
        
        // check to see if we already computed this value
        Paint ret = cache.get(bitmask);
        if(ret != null)
            return ret;
        
        // have to compute it now
        BufferedImage fill = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_RGB);
        Graphics2D gfx = (Graphics2D)fill.getGraphics();
        double degreesPerTopo = 360.0 / n;
        double degreesOn = 0;
        for(int i=0; i<n; i++) {
            Arc2D.Double pie = new Arc2D.Double(-5, -5, sz+10, sz+10, degreesOn, degreesPerTopo, Arc2D.PIE);
            degreesOn += degreesPerTopo;
            gfx.setColor(topologies.get(topology_indices[i]).getFillColor());
            gfx.fill(pie);
        }
        
        // memoize it
        TexturePaint tp = new TexturePaint(fill, new Rectangle2D.Double(0, 0, sz, sz));
        cache.put(bitmask, tp);
        return tp;
    }
    
    /** Slice:<slice name>{<names of topologies in the slice>} */
    public String toString() {
        String ret = "Slice:" + name + "{";
        for(FVTopology t : topologies)
            ret += t.getName() + ",";
        return ret.substring(0, ret.length()-1) + "}";
    }
}
