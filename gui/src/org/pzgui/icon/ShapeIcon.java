package org.pzgui.icon;

import org.pzgui.Constants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.HashMap;

/**
 * A java.awt.Shape-based icon.
 * 
 * @author David Underhill
 */
public class ShapeIcon extends Icon {
    private Shape shape;
    private Color fillColor;
    private Color outlineColor;
    private final Dimension size;
    private final HashMap<Dimension, Shape> resampledShapes = new HashMap<Dimension, Shape>();
    
    public ShapeIcon(Shape s, Color fill) {
        this(s, fill, null);
    }
    
    public ShapeIcon(Shape s, Color fill, Color outline) {
        this.shape = s;
        size = s.getBounds().getSize();
        this.fillColor = fill;
        this.outlineColor = outline;
    }
    
    /** has no cache, so this method is a no-op */
    public void clearCache() {
        resampledShapes.clear();
    }
    
    public void draw(Graphics2D gfx, int x, int y) {
        draw(gfx, shape, fillColor, outlineColor, x, y);
    }
    
    public void draw(Graphics2D gfx, int x, int y, int w, int h) {
        // get an appropriately sized shape if we don't have it already
        if(size.getWidth()!=w || size.getHeight()!=h) {
            Dimension d = new Dimension(w, h);
            Shape s = resampledShapes.get(d);
            if(s == null) {
                // have to make a new shape of the requested size
                AffineTransform af = new AffineTransform();
                af.setToScale(w / (double)size.getWidth(), h / (double)size.getHeight());
                s = af.createTransformedShape(shape);
            }
            shape = s;
        }
        
        draw(gfx, x, y);
    }
    
    public static void draw( Graphics2D gfx, Shape s, Color fill, Color outline, int x, int y) {
        gfx.setPaint(fill);
        gfx.fill(s);
        
        if( outline != null ) {
            gfx.setPaint(outline);
            gfx.draw(s);
        }
        
        gfx.setPaint(Constants.PAINT_DEFAULT);
    }
    
    public Dimension getSize() {
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
}
