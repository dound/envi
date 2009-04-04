package org.pzgui.icon;

import org.pzgui.Constants;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
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
    private Paint fillColor;
    private Paint outlineColor;
    private final Dimension size;
    private final HashMap<Dimension, Shape> resampledShapes = new HashMap<Dimension, Shape>();
    
    public ShapeIcon(Shape s, Paint fill) {
        this(s, fill, null);
    }
    
    public ShapeIcon(Shape s, Paint fill, Paint outline) {
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
        draw(gfx, x, y, w, h, fillColor, outlineColor);
    }
    
    public void draw(Graphics2D gfx, int x, int y, int w, int h, Paint fill, Paint outline) {
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
        
        draw(gfx, shape, fill, outline, x, y);
    }
    
    public static void draw( Graphics2D gfx, Shape s, Paint fill, Paint outline, int x, int y) {
        int xC = x - s.getBounds().width / 2;
        int yC = y - s.getBounds().height / 2;
        gfx.translate(xC, yC);
        
        gfx.setPaint(fill);
        gfx.fill(s);
        
        if( outline != null ) {
            gfx.setPaint(outline);
            gfx.draw(s);
        }
        
        gfx.setPaint(Constants.PAINT_DEFAULT);
        gfx.translate(-xC, -yC);
    }
    
    public Dimension getSize() {
        return size;
    }
    
    public Paint getFillColor() {
        return fillColor;
    }
    
    public void setFillColor(Paint c) {
        fillColor = c;
    }
    
    public Paint getOutlineColor() {
        return outlineColor;
    }
    
    public void setOutlineColor(Paint c) {
        outlineColor = c;
    }
}
