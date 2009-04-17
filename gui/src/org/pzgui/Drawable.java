package org.pzgui;

import java.awt.Graphics2D;

/**
 * Indicates an object which can be drawn in a PZWindow.
 * 
 * @author David Underhill
 */
public interface Drawable {
    /** Graphics to be drawn before drawObject() is called */
    public void drawBeforeObject(Graphics2D gfx);

    /** Draw this object */
    public abstract void drawObject(Graphics2D gfx);

     /** Whether these x, y coordinates fall are contained by the object */
    public boolean contains(int x, int y);

    /** Whether this object has been drawn this redraw cycle */
    public boolean isDrawn();

    /** Flag this object as undrawn for the current redraw cycle */
    public void unsetDrawn();

    /** Whether this object is currently selected */
    public boolean isSelected();

    /** Set whether this object is currently selected */
    public void setSelected(boolean selected);

    /** Whether this object currently has the mouse over it */
    public boolean isHovered();

    /** Set whether this object currently has the mouse over it */
    public void setHovered(boolean hovering);

    /** Called when the object is dragged to position x, y */
    public void drag(int x, int y);
}
