package org.pzgui;

import java.awt.Graphics2D;

/**
 * Designates an object which may be drawn and/or selected.
 * @author David Underhill
 */
public abstract class Drawable {
    /** Whether this entity has been drawn for the current redraw */
    private boolean drawn = false;

    /** Whether this entity is selected */
    private boolean selected = false;

    /** Whether the mouse is currently over this entity */
    private boolean hovering = false;

    
    /** Graphics to be drawn before drawObject() is called */
    public void drawBeforeObject(Graphics2D gfx) {}

    /** Draw this object */
    public abstract void drawObject(Graphics2D gfx);

    /** Whether these x, y coordinates fall within the object */
    public abstract boolean isWithin(int x, int y);

    /** Whether this object has been drawn this redraw cycle */
    public final boolean isDrawn() {
        return drawn;
    }

    /** Flag this object as undrawn for the current redraw cycle */
    public void unsetDrawn() {
        drawn = false;
    }

    /** Whether this object is currently selected */
    public boolean isSelected() {
        return selected;
    }

    /** Set whether this object is currently selected */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** Whether this object currently has the mouse over it */
    public boolean isHovered() {
        return hovering;
    }

    /** Set whetehr this object currently has the mouse over it */
    public void setHovered(boolean hovering) {
        this.hovering = hovering;
    }

    /** Called when the object is dragged to position x, y */
    public void drag(int x, int y) {}
}
