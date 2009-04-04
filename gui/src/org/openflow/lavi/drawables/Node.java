package org.openflow.lavi.drawables;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.pzgui.icon.GeometricIcon;
import org.pzgui.icon.Icon;
import org.pzgui.icon.ShapeIcon;
import org.pzgui.layout.AbstractLayoutable;
import org.pzgui.layout.Vertex;
import org.pzgui.Constants;
import org.pzgui.StringDrawer;

/**
 * Information about a node in the topology.
 * 
 * @author David Underhill
 */
public abstract class Node extends AbstractLayoutable implements Vertex<Link> {
    public Node(String name, int x, int y, Icon icon) {
        this.name = name;
        this.icon = icon;
        setPos(x, y);
    }

    // ------------------- Naming ------------------- //
    
    /** name of the Node */
    private String name;

    /** gets the name of the node */
    public String getName() {
        return name;
    }
    
    /** gets a debug version of the node's name */
    public abstract String getDebugName();
    
    /** sets the node's name */
    public void setName(String name) {
        this.name = name;
    }
    
    
    // ------------------- Drawing ------------------ //
    
    /** whether to draw names on nodes */
    public static boolean SHOW_NAMES = false;
    
    /** ratio to use when outlining the shape */
    public static final double OUTLINE_RATIO = 1.25;
    
    /** how to visually represent the Node itself */
    private Icon icon;
    
    /** draws the name of the object centered at the specified x coordinate */
    protected final void drawName( Graphics2D gfx, int x, int y) {
        StringDrawer.drawCenteredString(getName(), gfx, x, y);
    }
    
    /** 
     * Draws this object using the Icon specified by getIcon() at its current
     * location as specified by getX() and getY().  The name is drawn below the
     * object.
     */
    public void drawObject(Graphics2D gfx) {
        // make the object less prominent if it is off (unless it has failed)
        Composite c = null;
        if(isOff() && !isFailed()) {
            c = gfx.getComposite();
            gfx.setComposite(Constants.COMPOSITE_QUARTER);
        }
        
        // outline the object if it is selected or being hovered over
        if(isSelected())
            drawOutline(gfx, Constants.COLOR_SELECTED, OUTLINE_RATIO);
        else if(isHovered())
            drawOutline(gfx, Constants.COLOR_HOVERING, OUTLINE_RATIO);

        // draw the object
        icon.draw(gfx, getX(), getY());
                
        // draw its name
        if(SHOW_NAMES)
            drawName(gfx, getX(), getY() + icon.getHeight());
        
        // draw an "X" if it failed
        if(isFailed())
            drawFailed(gfx);
        
        if(c != null)
            gfx.setComposite(c);
        gfx.setPaint(Constants.PAINT_DEFAULT);
    }
    
    /** draw an "X" over the node to indicate failure */
    protected void drawFailed(Graphics2D gfx) {
        int w=icon.getWidth(), dx=0;
        if(GeometricIcon.X.getWidth() > w) {
            dx = (GeometricIcon.X.getWidth() - w) / 2;
            w = GeometricIcon.X.getWidth();
        }
        
        int h=icon.getHeight(), dy=0;
        if(GeometricIcon.X.getWidth() > h) {
            dy = (GeometricIcon.X.getWidth() - h) / 2;
            h = GeometricIcon.X.getWidth();
        }
        
        GeometricIcon.X.draw(gfx, getX()-dx, getY()-dy, w, h);
    }
    
    /**
     * Draw an outline around the object.
     * 
     * @param gfx           where to draw
     * @param outlineColor  color of the outline
     * @param ratio         how big to make the outline
     */
    public void drawOutline(Graphics2D gfx, Paint outlineColor, double ratio) {
        // size of the outlined shape
        double w = icon.getWidth() * ratio;
        double h = icon.getHeight() * ratio;
        
        Shape outline;
        if(icon instanceof ShapeIcon) {
            // wrap the shape precisely
            ShapeIcon si = (ShapeIcon)icon; 
            si.draw(gfx, getX(), getY(), (int)w, (int)h, outlineColor, Constants.PAINT_DEFAULT);
        }
        else {
            // draw a rectangle around the icon
            outline = new Rectangle2D.Double(getX()-w/2, getY()-h/2, w, h);
            gfx.draw(outline);
            gfx.setPaint(outlineColor);
            gfx.fill(outline);
        }

        gfx.setPaint(Constants.PAINT_DEFAULT);
    }
    
    /** get how this node is visually represented */
    public Icon getIcon() {
        return icon;
    }
    
    /** set how this node is visually represented */
    public void setIcon(Icon icon) {
        this.icon = icon;
    }
    
    
    // ----------------- Node Status ---------------- //
    
    /** whether the node is off because it is not needed */
    private boolean off = false;
    
    /** whether the node is off because it "failed" */
    private boolean failed = false;
    
    public boolean isOff() {
        return off;
    }
    
    public void setOff(boolean b) {
        off = b;
    }

    public boolean isFailed() {
        return failed;
    }
    
    public void setFailed(boolean b) {
        failed = b;
    }
    
    
    // -------------------- Other ------------------- //

    /** Move the node when it is dragged */
    public void drag(int x, int y) {
        setPos(x, y);
    }
    
    /** Returns true if the object of size sz contains the location x, y */
    protected boolean isWithin( int x, int y, java.awt.Dimension sz) {
        int left = getX()-sz.width/2;
        int top  = getY()-sz.height/2;
        
        return( x>=left && x<left+sz.width && y>=top && y<top+sz.height);
    }
    
    /** returns the name of the node */
    public String toString() {
        return getName();
    }
}
