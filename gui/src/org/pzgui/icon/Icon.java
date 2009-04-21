package org.pzgui.icon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JLabel;

/**
 * Generic icon for drawing.  Caches different sizes of the icon as needed.
 * 
 * @author David Underhill
 */
public abstract class Icon extends Component {
    protected static final JLabel lblForFM = new JLabel();
    public static FontMetrics FONT_METRICS = null;
    
    /** Clears any cached copies of the icon at any size other than its original size. */
    public abstract void clearCache();
    
    /** Draws this icon on the specified graphics object at the specified coordinates. */
    public void draw( Graphics2D gfx, int x, int y ) {
        draw(gfx, x,  y, getSize().width, getSize().height);
    }
    
    /** Draws this icon on the specified graphics object at the specified coordinates at the specified scale. */
    public void draw( Graphics2D gfx, int x, int y, float scale ) {
        Dimension size = getSize();
        int w = (int)(size.width  * scale);
        int h = (int)(size.height * scale);
        draw(gfx, x, y, w, h);
    }
    
    /** Draws this icon on the specified graphics object at the specified coordinates at the specified size. */
    public abstract void draw( Graphics2D gfx, int x, int y, int w, int h );
    
    /** Draws this icon. */
    public void paint(Graphics g) {
        draw((Graphics2D)g, getX(), getY(), (int)(scaleH * getHeight()), (int)(scaleW * getWidth()));
    }
    
    /** whether x,y is contained by this icon */
    public boolean contains(int x, int y, int iconX, int iconY) {
        int left = iconX-getWidth()/2;
        int top  = iconY-getHeight()/2;
        
        return( x>=left && x<left+getWidth() && y>=top && y<top+getHeight());
    }
    
    /** Returns the default size of this icon. */
    public abstract Dimension getSize();
    
    /** gets the default height of this icon */
    public int getHeight() {
        return getSize().height;
    }
    
    /** gets the default width of this icon */
    public int getWidth() {
        return getSize().width;
    }
    
    /** scaling for the paint method */
    private float scaleW = 1.0f, scaleH = 1.0f;
    
    public void setSize(int w, int h) {
        super.setSize(w, h);
        Dimension sz = new Dimension(w, h);
        super.setMaximumSize(sz);
        super.setPreferredSize(sz);
        super.setMinimumSize(sz);
        
        Dimension d = getSize();
        scaleW = w / (float)d.width;
        scaleH = h / (float)d.height;
    }
}
