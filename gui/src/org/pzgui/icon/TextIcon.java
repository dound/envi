package org.pzgui.icon;

import org.pzgui.Constants;
import org.pzgui.StringDrawer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * A text-based icon.
 * 
 * @author David Underhill
 */
public class TextIcon extends Icon {
    private final String text;
    private int fontSize;
    private Color fillColor;
    private Color outlineColor;
    private final Dimension size;
    
    private final Font font;
    
    public TextIcon(String text, Font f, int fillFontSize, Color fill) {
        this(text, f, fillFontSize, fill, null);
    }
    
    public TextIcon(String text, Font f, int fontSize, Color fill, Color outline) {
        this.text = text;
        this.font = f;
        this.fontSize = fontSize;
        this.fillColor = fill;
        this.outlineColor = outline;
        this.size = new Dimension(0, fontSize);
    }
    
    /** TextIcon has no cache, so this method is a no-op. */
    public void clearCache() {
        /* no-op */
    }
    
    public void draw( Graphics2D gfx, int x, int y ) {
        draw(gfx, text, font, fontSize, fillColor, outlineColor, x, y);
    }
    
    public void draw( Graphics2D gfx, int x, int y, int w, int h ) {
        float r = (h / (float)size.height);
        draw(gfx, text, font, (int)(fontSize * r), fillColor, outlineColor, x, y);
    }
    
    public static void draw( Graphics2D gfx, String text, Font f, int fontSize, Color fill, Color outline, int x, int y) {
        Font fillFont = (f.getSize()==fontSize ? f : new Font(f.getName(), f.getStyle(), fontSize));
        gfx.setFont(fillFont);
        gfx.setPaint(fill);
        
        if( outline != null )
            StringDrawer.drawCenteredStringOutlined(text, gfx, x, y, outline);
        else
            StringDrawer.drawCenteredString(text, gfx, x, y);
        
        gfx.setPaint(Constants.PAINT_DEFAULT);
        gfx.setFont(Constants.FONT_DEFAULT);
    }
    
    public Dimension getSize() {
        if( FONT_METRICS == null )
            return size;
        
        this.size.width = FONT_METRICS.stringWidth(text);
        
        return size;
    }
    
    public Color getFillColor() {
        return fillColor;
    }
    
    public void setFillColor(Color c) {
        fillColor = c;
    }
    
    public Color getOutlineColor() {
        return outlineColor;
    }
    
    public void setOutlineColor(Color c) {
        outlineColor = c;
    }

    public int getFillFontSize() {
        return fontSize;
    }

    public void setFontSize(int fillFontSize) {
        this.fontSize = fillFontSize;
    }
}
