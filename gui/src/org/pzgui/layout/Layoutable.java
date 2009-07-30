package org.pzgui.layout;

import java.awt.geom.Point2D;

/**
 * Indicates an object which can be laid out.
 * 
 * @author David Underhill
 */
public interface Layoutable {
    /** returns the x and y coordinates of this object */
    public Point2D getPos();
    
    /** returns the x-coordinate of this object */
    public int getX();

    /** returns the y-coordinate of this object */
    public int getY();

    /** sets the x and y coordinates of this object */
    public void setPos(int x, int y);
    
    /** 
     * sets the x and y coordinates of this object and notes whether 
     * hasPositionChanged() should return true if x or y are modified by this
     * call. 
     */
    public void setPos(int x, int y, boolean notePositionChanged);
    
    /** sets the x-coordinate of this object */
    public void setXPos(int x);
    
    /** sets the y-coordinate of this object */
    public void setYPos(int y);

    /** gets the height of this object */
    public int getHeight();

    /** gets the width of this object */
    public int getWidth();
    
    /** whether the position changed since the previous unsetPositionChanged() call */
    public boolean hasPositionChanged();
    
    /** Causes hasPositionChanged() to return false until set*Pos() is called. */
    public void unsetPositionChanged();    
    
    /** whether the position can be changed */
    public boolean canPositionChange();
    
    /** set whether the position can be changed */
    public void setCanPositionChange(boolean can);
    
    /** Returns a unique ID for this node */
    public abstract long getID();
}
