package org.pzgui.icon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.HashMap;

import org.pzgui.Constants;

/**
 * An icon specified by geometric shapes.
 * 
 * @author David Underhill
 */
public class GeometricIcon extends Icon {
    /** Returns the maximum value from an array. */
    public static final int max(int[] a) {
        int ret = Integer.MIN_VALUE;
        for( int v : a )
            ret = Math.max(ret, v);
        
        return ret;
    }
    
    /** Returns the minimum value from an array. */
    public static final int min(int[] a) {
        int ret = Integer.MAX_VALUE;
        for( int v : a )
            ret = Math.min(ret, v);
        
        return ret;
    }
    
    /** Returns the difference of the max and min values in an array. */
    public static final int spread(int[] a) {
        return max(a) - min(a);
    }
    
    /** Scales the specified coordinates to the requested size polygon */
    public static final Polygon getScaledPolygon(int[] baseX, int[] baseY, int w, int h) {
        int spreadX = spread(baseX);
        int spreadY = spread(baseY);
        
        float scaleX = w / spreadX;
        float scaleY = h / spreadY;
        
        int[] newX = new int[baseX.length];
        int[] newY = new int[baseX.length];
        for( int i=0; i<baseX.length; i++ ) {
            newX[i] = (int)(baseX[i] * scaleX);
            newY[i] = (int)(baseY[i] * scaleY);
        }
        
        return new Polygon(newX, newY, newX.length);
    }
    
    private final Polygon poly;
    private Color borderColor;
    private Color fillColor;
    private Stroke borderStroke;
    private final Dimension size;
    private boolean center = false;
    private final HashMap<Dimension, Polygon> resampledPolygons = new HashMap<Dimension, Polygon>();
    
    public GeometricIcon(int[] x, int[] y, Color fillColor) {
        this(x, y, fillColor, Constants.COLOR_DEFAULT, null);
    }
    
    public GeometricIcon(int[] x, int[] y, Color fillColor, Color borderColor, Stroke borderStroke) {
        poly = new Polygon(x, y, x.length);
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.borderStroke = borderStroke;
        size = new Dimension(spread(x), spread(y));
        super.setSize(size);
    }
    
    public GeometricIcon(int[] x, int[] y, int width, int height, Color fillColor, Color borderColor, Stroke borderStroke) {
        Dimension origSize = new Dimension(spread(x), spread(y));
        if( origSize.width!=width || origSize.height!=height )
            poly = getScaledPolygon(x, y, width, height);
        else
            poly = new Polygon(x, y, x.length);
        
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.borderStroke = borderStroke;
        size = new Dimension(width, height);
        super.setSize(size);
    }
    
    public void clearCache() {
        resampledPolygons.clear();
    }
    
    public void draw( Graphics2D gfx, int x, int y ) {
        draw(gfx, poly, fillColor, borderColor, borderStroke, x, y, size.width, size.height, center);
    }
    
    public void draw( Graphics2D gfx, int x, int y, int w, int h ) {
        draw(gfx, getPolygon(new Dimension(w, h)), fillColor, borderColor, borderStroke, x, y, w, h, center);
    }
    
    public static void draw( Graphics2D gfx, Polygon poly, Color fillColor, Color borderColor, Stroke borderStroke, int x, int y, int w, int h, boolean center) {
        // center the drawing
        if(center) {
            x -= w / 2;
            y -= h / 2;
        }
        
        // draw the polygon in the appropriate place
        gfx.translate(x, y);
        
        Paint p = gfx.getPaint();
        if( fillColor != null ) {
            gfx.setColor(fillColor);
            gfx.fill(poly);
        }
        gfx.setColor(borderColor);
        if(borderStroke != null) {
            Stroke s = gfx.getStroke();
            gfx.setStroke(borderStroke);
            gfx.draw(poly);
            gfx.setStroke(s);
        }
        else
            gfx.draw(poly);
        gfx.setPaint(p);
        
        gfx.translate(-x, -y);
    }
    
    public Dimension getSize() {
        return size;
    }
    
    public Polygon getPolygon() {
        return poly;
    }

    public Color getBorderColor() {
        return borderColor;
    }
    
    public void setBorderColor(Color c) {
        borderColor = c;
    }

    public Stroke getBorderStroke() {
        return borderStroke;
    }
    
    public void setBorderStroke(Stroke s) {
        borderStroke = s;
    }
    
    public Color getFillColor() {
        return fillColor;
    }
    
    public void setFillColor(Color c) {
        fillColor = c;
    }
    
    public boolean isCenter() {
        return center;
    }
    
    public void setCenter(boolean b) {
        center = b;
    }
    
    public Polygon getPolygon(Dimension sz) {
        // if the needed size matches the original, return the original
        if( sz.equals(size) )
            return poly;
        
        // otherwise see if we alredy built the polygon
        Polygon ret = resampledPolygons.get(sz);
        if( ret == null ) {
            // create the scaled image and save it for later
            ret = getScaledPolygon(poly.xpoints, poly.ypoints, sz.width, sz.height);
            resampledPolygons.put(sz, ret);
            
        }
        return ret;
    }

    // example geometric icons
    /** GeometricIcon which is a green checkmark */
    public static final GeometricIcon CHECKMARK = new GeometricIcon(new int[]{0,   8, 24, 20,  8,  4,  0},
                                                                new int[]{16, 26,  4,  0, 18, 14, 16},
                                                                Color.GREEN);

    /** GeometricIcon which is a red 'X' */
    public static final GeometricIcon X = new GeometricIcon(new int[]{0, 6, 12, 18, 24, 16, 24, 18, 12,  6,  0, 10, 0},
                                                        new int[]{6, 0, 10,  0,  6, 12, 18, 24, 12, 24, 18, 12, 6},
                                                        Color.RED);
}
