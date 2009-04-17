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
    
    /** sets the x-coordinate of this object */
    public void setXPos(int x);
    
    /** sets the y-coordinate of this object */
    public void setYPos(int y);
    
    /** whether the position changed since the previous unsetPositionChanged() call */
    public boolean hasPositionChanged();
    
    /** Causes hasPositionChanged() to return false until set*Pos() is called. */
    public void unsetPositionChanged();    
    
    /** whether the position can be changed */
    public boolean canPositionChange();
    
    /** set whether the position can be changed */
    public void setCanPositionChange(boolean can);
}
