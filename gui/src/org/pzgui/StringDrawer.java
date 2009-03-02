package org.pzgui;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.font.TextLayout;

/**
 * Provides some generic GUI helper functions for drawing strings.
 * 
 * @author David Underhill
 */
public abstract class StringDrawer {
    /** Draw the string s horizontally centered at position x, y */
    public static final void drawCenteredString(String s, Graphics2D gfx, int x, int y) {
        // center the string horizontally
        x -= gfx.getFontMetrics().stringWidth(s) / 2;
        gfx.drawString(s, x, y);
    }

    /** Draw the string s outlined and horizontally centered at position x, y */
    public static final void drawCenteredStringOutlined(String s, Graphics2D gfx, int x, int y, Paint outlinePaint) {
        // center the string horizontally
        x -= gfx.getFontMetrics().stringWidth(s) / 2;
        
        // draw the string itself
        gfx.drawString(s, x, y);
        
        // set the paint color for the outline
        Paint origPaint = gfx.getPaint();
        gfx.setPaint(outlinePaint);
        
        // draw the outline
        TextLayout textTl = new TextLayout(s, gfx.getFont(), gfx.getFontRenderContext());
        Shape outline = textTl.getOutline(null);
        gfx.translate(x, y);
        gfx.draw(outline);
        gfx.translate(-x, -y);
        
        // restore the original paint color
        gfx.setPaint(origPaint);
    }

    /** Draw the string s right-aligned at position x, y */
    public static final void drawRightAlignedString(String s, Graphics2D gfx, int x, int y) {
        // right-align the string horizontally
        x -= gfx.getFontMetrics().stringWidth(s);
        gfx.drawString(s, x, y);
    }
}
