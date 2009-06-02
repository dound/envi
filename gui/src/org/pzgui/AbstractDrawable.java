package org.pzgui;

import java.awt.Graphics2D;

/**
 * Designates an object which may be drawn and/or selected.
 * 
 * @author David Underhill
 */
public abstract class AbstractDrawable implements Drawable {
    /** Whether this entity has been drawn for the current redraw */
    private boolean drawn = false;

    /** Whether this entity is selected */
    private boolean selected = false;

    /** Whether the mouse is currently over this entity */
    private boolean hovering = false;

    
    public void drawBeforeObject(Graphics2D gfx) {}

    public abstract void drawObject(Graphics2D gfx);

    public abstract boolean contains(int x, int y);

    public final boolean isDrawn() {
        return drawn;
    }

    public void setDrawn() {
        drawn = true;
    }
    
    public void unsetDrawn() {
        drawn = false;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isHovered() {
        return hovering;
    }

    public void setHovered(boolean hovering) {
        this.hovering = hovering;
    }

    public void drag(int x, int y) {}
}
