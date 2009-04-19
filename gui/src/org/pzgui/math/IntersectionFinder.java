package org.pzgui.math;

import java.awt.Dimension;

/**
 * Algorithms for finding the intersection points of lines and other shapes.
 * @author David Underhill
 */
public class IntersectionFinder {
    /** Returns the insersection point of infinite lines l1 and l2, or null if they are parallel */
    public static Vector2f intersectLineHelper(Line l1, Line l2) {
        // parallel lines do not intersect
        if(l1.m == l2.m)
            return null;
        
        // handle vertical lines
        if(l1.isVertical)
            return new Vector2f(l1.x1, l1.x1 * l2.m + l2.b);
        if(l2.isVertical)
            return new Vector2f(l2.x1, l2.x1 * l1.m + l1.b);
        
        // handle non-vertical lines
        float x = (l1.b - l2.b) / (l2.m - l1.m);
        return new Vector2f(x, l1.m * x + l1.b);
    }
    
    /** returns true if i is within the bounds of l */
    public static boolean isWithin(Vector2f i, Line l) {
        if( (i.x>=l.x1 && i.x<=l.x2) || (i.x>=l.x2 && i.x<=l.x1) )
            if( (i.y>=l.y1 && i.y<=l.y2) || (i.y>=l.y2 && i.y<=l.y1) )
                return true;
        
        return false;
    }
    
    /** Returns the insersection point of l1 and l2, or null if they do not intersect */
    public static Vector2f intersectLine(Line l1, Line l2) {
        Vector2f i = intersectLineHelper(l1, l2);
        
        if(i != null && isWithin(i, l1) && isWithin(i, l2))
            return i;
        
        return null;
    }
    
    /** 
     * Returns an intersection point of l and the specified box or null if they
     * do not intersection 
     */
    public static Vector2f intersectBox(Line l, int x, int y, int w, int h) {
        Vector2f i;
        
        i = intersectLine(l, new Line(x,     y,     x + w, y));
        if(i!=null) return i;
        
        i = intersectLine(l, new Line(x + w, y,     x + w, y + h));
        if(i!=null) return i;
        
        i = intersectLine(l, new Line(x,     y + h, x + w, y + h));
        if(i!=null) return i;
        
        i = intersectLine(l, new Line(x,     y,     x,     y + h));
        return i;
    }
    
    /** 
     * Returns an intersection point of l and the area of the specified Icon or
     * null if they do not intersection 
     */
    public static Vector2f intersectBox(Line l, org.openflow.gui.drawables.Node n) {
        Dimension d = n.getIcon().getSize();
        return intersectBox(l, 
                            n.getX() - d.width / 2, 
                            n.getY() - d.height / 2, 
                            d.width, 
                            d.height);
    }
    

    /**
     * Returns the first intersection point of a ray cast from point p to the 
     * edge of a box.  This method assumes the object is centered around x2 and
     * y2.
     */
    public static Vector2i intersect(Vector2i p1, Vector2i p2, int width, int height) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        if(dx == 0)
            return new Vector2i(p2.x, p2.y - height / 2);
        double m = dy / (double)dx;
        
        // how far to back up from p2.getX(), p2.getY() to get to the edge
        double d = (height + width) / 4.0;
        
        // number of slope units to back up
        double i = d / Math.sqrt(m*m + 1*1);
        
        // compute and return the intersection
        double xI = (p2.getX() > p1.getX()) ? p2.getX()-i : p2.getX()+i;
        double mAbs = Math.abs(m); 
        double yI = (p2.getY() > p1.getY()) ? p2.getY()-mAbs*i : p2.getY()+mAbs*i;
        return new Vector2i((int)xI, (int)yI);
    }
}
