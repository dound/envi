package org.pzgui.math;

import java.awt.geom.Point2D;

/**
 * A 2-element vector that is represented by float-precision floating point 
 * x,y coordinates.
 *
 * @author David Underhill
 */
public class Vector2f extends Point2D implements java.io.Serializable, Cloneable {

    static final long serialVersionUID = 100L;
    
    public static final float EPS = 0.00001f;
        
    public float x, y;

    //<editor-fold defaultstate="collapsed" desc="constructor methods">
    
    /**
     * Constructs and initializes a Vector2 from the specified xy coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }


    /**
     * Constructs and initializes a Vector2 from the array of length 3.
     * @param v the array of length 2 containing xyz in order
     */
    public Vector2f(float[] v) {
        this.x = v[0];
        this.y = v[1];
    }


    /**
     * Constructs and initializes a Vector2 from the specified Vector3.
     * @param v1 the Vector2 containing the initialization x y data
     */
    public Vector2f(Vector2f v1) {
         this.x = v1.x;
         this.y = v1.y;
    }
    
    /**
     * Constructs and initializes a Vector2 from one point to another
     */
    public Vector2f(Vector2f from, Vector2f to) {
         this.x = to.x - from.x;
         this.y = to.y - from.y;
    }
    
    /**
     * Constructs and initializes a Vector3 to (0,0,0).
     */
    public Vector2f() {
       this.x = 0;
       this.y = 0;
    }

    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="mutator methods">
    
    public double getX() { return x; }
    public double getY() { return y; }
    
    public void setX  ( float x ) { this.x = x; }
    public void setY  ( float y ) { this.y = y; }
    
    /** sets the components of this vector */
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void setLocation(double x, double y) {
        set((float)x, (float)y);
    }
    
    //</editor-fold>

    /** clamps the values to the specified ranges (minimum value = 0) */
    public void clamp( float xMax, float yMax ) {
        x = Math.min( x, xMax );
        y = Math.min( y, yMax );
    }
    
    /**
     * Returns the length of this vector.
     * @return the length of this vector
     */
    public final float length() {
        return (float)Math.sqrt(this.x*this.x + this.y*this.y);
    }
    

    /**
     * Returns the length squared of this vector.
     * @return the length squared of this vector
     */
    public final float lengthSq() {
        return this.x*this.x + this.y*this.y;
    }
    
    /** adds v to this vector */
    public Vector2f add( Vector2f v ) {
        this.x += v.x;
        this.y += v.y;
        
        return this;
    }
    
    /** subtracts v from this vector */
    public Vector2f subtract( Vector2f v ) {
        this.x -= v.x;
        this.y -= v.y;
        
        return this;
    }
    
    /** multiplies this by s and returns this */
    public Vector2f multiply( float s ) {
        this.x *= s;
        this.y *= s;
        
        return this;
    }
    
    /** divides this by s and returns this */
    public Vector2f divide( float s ) {
        this.x /= s;
        this.y /= s;
        
        return this;
    }
    
    /** returns the product of a vector and a scalar */
    public static Vector2f multiply( Vector2f v1, float s  ) {
        return new Vector2f(v1).multiply(s);
    }
    
    /** returns the quotient of a vector and a scalar */
    public static Vector2f divide( Vector2f v1, float s  ) {
        return new Vector2f(v1).divide(s);
    }
    
    /** adds v to this vector */
    public static Vector2f add( Vector2f v1, Vector2f v2 ) {
        return new Vector2f(v1.x+v2.x, v1.y+v2.y);
    }
    
    /** adds v to this vector */
    public static Vector2f subtract( Vector2f v1, Vector2f v2 ) {
        return new Vector2f(v1.x-v2.x, v1.y-v2.y);
    }
    
    /** adds v to this vector */
    public static Vector2f makeUnit(Vector2f v) {
        float len = v.length();
        return new Vector2f(v.x/len, v.y/len);
    }
    
    public Vector2f clone() {
       return new Vector2f(this);
    }

    public String toString() {
       return "(" + x + ", " + y + ")";
    }

    public String toStringPlain() {
       return x + " " + y;
    }
}
