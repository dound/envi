package org.openflow.gui.drawables;

import java.awt.Graphics2D;

import org.pzgui.AbstractDrawable;
import org.pzgui.icon.Icon;

/**
 * Wraps an Icon object so that it can drawn on a Pan-Zoom GUI. 
 * 
 * @author David Underhill
 */
public class DrawableIcon extends AbstractDrawable {
    private Icon icon;
    private int x, y;
    private int w, h;
    
    /** whether the icon can be hovered over or selected */
    private boolean selectable;
    
    public DrawableIcon(Icon icon, int x, int y) {
        this(icon, x, y, icon.getWidth(), icon.getHeight());
    }

    public DrawableIcon(Icon icon, int x, int y, int w, int h) {
        this(icon, x, y, w, h, true);
    }

    public DrawableIcon(Icon icon, int x, int y, int w, int h, boolean s) {
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.selectable = s;
    }
    
    public void drawObject(Graphics2D gfx) {
        icon.draw(gfx, x, y, w, h);
    }
    
    public boolean contains(int x, int y) {
        return selectable && icon.contains(x, y, x, y);
    }

    public boolean isSelectable() {
        return selectable;
    }
    
    public void setSelectable(boolean b) {
        selectable = b;
    }
}
