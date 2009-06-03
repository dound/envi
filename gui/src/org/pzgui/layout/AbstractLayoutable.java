package org.pzgui.layout;

import java.awt.geom.Point2D;
import org.pzgui.AbstractDrawable;

/**
 * Designates an object which may be drawn and laid out.
 * 
 * @author David Underhill
 */
public abstract class AbstractLayoutable extends AbstractDrawable 
                                         implements Layoutable {
    private int x = 0;
    private int y = 0;
    private boolean positionChanged = false;
    private boolean canPositionChange = true;
    
    public Point2D getPos() {
        return new Point2D.Double(x, y);
    }
    
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public final void setPos(int x, int y) {
        setXPos(x);
        setYPos(y);
    }
    
    public void setPos(int x, int y, boolean notePositionChanged) {
        if(notePositionChanged || positionChanged)
            setPos(x, y);
        else {
            setPos(x, y);
            positionChanged = false;
        }
    }
    
    public void setXPos( int x) {
        if(canPositionChange()) {
            if(this.x != x)
                positionChanged = true;
            
            this.x = x;
        }
    }
    
    public void setYPos( int y) {
        if(canPositionChange()) {
            if(this.y != y)
                positionChanged = true;
            
            this.y = y;
        }
    }
    
    public boolean hasPositionChanged() {
        return positionChanged;
    }
    
    public void unsetPositionChanged() {
        positionChanged = false;
    }
    
    public boolean canPositionChange() {
        return canPositionChange;
    }
    
    public void setCanPositionChange(boolean can) {
        canPositionChange = can;
    }
    
    /** returns true if the area described by getHeight()/getWidth() contains x, y */
    public boolean contains(int x, int y) {
        int left = getX()-getWidth()/2;
        int top  = getY()-getHeight()/2;
        
        return( x>=left && x<left+getWidth() && y>=top && y<top+getHeight());
    }
    
    /** Move the node when it is dragged */
    public void drag(int x, int y) {
        setPos(x, y);
    }
}
