package org.pzgui.math;

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
    
    /** Returns an insersection point of l and the specified box or null if they do not intersection */
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
}
