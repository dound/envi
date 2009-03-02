package org.pzgui;

import org.pzgui.math.Vector2i;

/**
 * How a window is connected (docked) to another.
 * 
 * @author David Underhill
 */
public enum DockLocation {
    NOT_DOCKED(),
    RIGHT_OF(),
    LEFT_OF(),
    ABOVE(),
    BELOW();
    
    /**
     * Returns the position for the top-left corner of a child window based on 
     * the current docking scheme.
     * 
     * @param parentPos   Location of the parent's top-left corner.  If null, childPos is returned.
     * @param parentSize  Size of the parent.
     * @param childPos    Location of the child's top-left corner.
     * @param childSize   Size of the child.
     * 
     * @return position of the child
     */
    public Vector2i getChildPosition(Vector2i parentPos, Vector2i parentSize, Vector2i childPos, Vector2i childSize) {
        if(parentPos==null)
            return childPos;
        
        switch(this) {
            case NOT_DOCKED: return childPos;
            case RIGHT_OF:   return new Vector2i(parentPos.x + parentSize.x, parentPos.y);
            case LEFT_OF:    return new Vector2i(parentPos.x - childSize.x,  parentPos.y);
            case ABOVE:      return new Vector2i(parentPos.x,  parentPos.y - childSize.y);
            case BELOW:      return new Vector2i(parentPos.x,  parentPos.y + parentSize.y);
        }
        throw(new Error("missing handler for case " + this.name() + " in DockLocation::getPosition()"));
    }
}
