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
    
    public DrawableIcon(Icon icon, int x, int y) {
        this(icon, x, y, icon.getWidth(), icon.getHeight());
    }

    public DrawableIcon(Icon icon, int x, int y, int w, int h) {
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    
    public void drawObject(Graphics2D gfx) {
        icon.draw(gfx, x, y, w, h);
    }
    
    public boolean contains(int x, int y) {
        return icon.contains(x, y, x, y);
    }
}
