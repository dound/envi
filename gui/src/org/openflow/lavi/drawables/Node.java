package org.openflow.lavi.drawables;

import java.awt.Graphics2D;

import org.pzgui.icon.Icon;
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
        icon.draw(gfx, getX(), getY());
        drawName(gfx, getX(), getY() + icon.getHeight());
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
