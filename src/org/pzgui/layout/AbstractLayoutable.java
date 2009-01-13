package org.pzgui.layout;

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
    
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public final void setPos( int x, int y) {
        setXPos(x);
        setYPos(y);
    }
    
    public void setXPos( int x) {
        if(this.x != x)
            positionChanged = true;
        
        this.x = x;
    }
    
    public void setYPos( int y) {
        if(this.y != y)
            positionChanged = true;
        
        this.y = y;
    }
    
    public boolean hasPositionChanged() {
        return positionChanged;
    }
    
    public void unsetPositionChanged() {
        positionChanged = false;
    }
}
