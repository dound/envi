package org.openflow.lavi.drawables;

import java.awt.Graphics2D;
import org.pzgui.icon.ImageIcon;
import org.pzgui.layout.AbstractLayoutable;
import org.pzgui.layout.Vertex;
import org.pzgui.StringDrawer;

/**
 * Information about a node in the topology.
 * 
 * @author David Underhill
 */
public abstract class Node extends AbstractLayoutable implements Vertex<Link> {
    public Node(String name, int x, int y) {
        this.name = name;
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
    
    protected final void drawName( Graphics2D gfx, int x, int yMin, int yMax) {
        int yName;
        yName = yMin - gfx.getFontMetrics().getHeight() / 2;
        StringDrawer.drawCenteredString( getName(), gfx, x, yName);
    }
    
    public void drawNodeWithImage( Graphics2D gfx, java.awt.Image img, java.awt.Dimension sz) {
        drawNodeWithImage( gfx, img, sz, true);
    }
    
    public void drawNodeWithImage( Graphics2D gfx, java.awt.Image img, java.awt.Dimension sz, boolean doDrawName) {
        ImageIcon.draw(gfx, img, getX(), getY(), sz.width, sz.height);
        if( doDrawName)
            drawName( gfx, getX(), getY() - sz.height / 2, getY() + sz.height / 2);
    }
    
    public void drawNodeWithImage( Graphics2D gfx, java.awt.Image img, java.awt.Dimension sz, int textYOffset) {
        ImageIcon.draw(gfx, img, getX(), getY(), sz.width, sz.height);
        drawName( gfx, getX(), getY() - textYOffset, getY() + textYOffset);
    }
    
    public abstract void draw(Graphics2D gfx);
    
    public void drawObject(Graphics2D gfx) {
        draw(gfx);
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
