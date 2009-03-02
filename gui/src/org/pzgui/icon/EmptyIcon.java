package org.pzgui.icon;

import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * A text-based icon.
 * 
 * @author David Underhill
 */
public class EmptyIcon extends Icon {
    private final Dimension size;
    
    public EmptyIcon(int w, int h) {
        size = new Dimension(w, h);
        super.setSize(size);
    }
    
    public void clearCache() {
        /* no-op */
    }
    
    public void draw( Graphics2D gfx, int x, int y ) {
        /* no-op */
    }
    
    public void draw( Graphics2D gfx, int x, int y, int w, int h ) {
        /* no-op */
    }
    
    public Dimension getSize() {
        return size;
    }
}
