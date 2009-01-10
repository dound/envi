package org.pzgui.math;

/**
 * A 2-element vector that is represented by integers
 * x,y coordinates.
 *
 * @author David Underhill
 */
public class Vector2i implements java.io.Serializable, Cloneable {

    static final long serialVersionUID = 100L;
        
    public int x, y;

    //<editor-fold defaultstate="collapsed" desc="constructor methods">
    
    /**
     * Constructs and initializes a Vector2 from the specified xy coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }


    /**
     * Constructs and initializes a Vector2 from the array of length 3.
     * @param v the array of length 2 containing xyz in order
     */
    public Vector2i(int[] v) {
        this.x = v[0];
        this.y = v[1];
    }


    /**
     * Constructs and initializes a Vector2 from the specified Vector3.
     * @param v1 the Vector2 containing the initialization x y data
     */
    public Vector2i(Vector2i v1) {
         this.x = v1.x;
         this.y = v1.y;
    }
    
    /**
     * Constructs and initializes a Vector2 from one point to another
     */
    public Vector2i(Vector2i from, Vector2i to) {
         this.x = to.x - from.x;
         this.y = to.y - from.y;
    }

    /**
     * Constructs and initializes a Vector3 to (0,0,0).
     */
    public Vector2i() {
       this.x = 0;
       this.y = 0;
    }

    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="mutator methods">
    
    public int getX() { return x; }
    public int getY() { return y; }

    public void set(Vector2i pos) {
        x = pos.x;
        y = pos.y;
    }
    
    public void setX  ( int x ) { this.x = x; }
    public void setY  ( int y ) { this.y = y; }
    
    /** sets the components of this vector */
    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    //</editor-fold>

    /** clamps the values to the specified ranges (minimum value = 0) */
    public void clamp( int xMax, int yMax ) {
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
    
    /** adds v to this vector */
    public Vector2i add( Vector2i v  ) {
        this.x += v.x;
        this.y += v.y;
        
        return this;
    }
    
    /** subtracts v from this vector */
    public Vector2i subtract( Vector2i v  ) {
        this.x -= v.x;
        this.y -= v.y;
        
        return this;
    }
    
    /** multiplies this by vec and returns this */
    public Vector2i multiply( int s ) {
        this.x *= s;
        this.y *= s;
        
        return this;
    }
    
    /** returns the product of a vector and a scalar */
    public static Vector2i multiply( Vector2i v1, int s  ) {
        return new Vector2i(v1).multiply(s);
    }
    
    /** adds v to this vector */
    public static Vector2i add( Vector2i v1, Vector2i v2  ) {
        return new Vector2i(v1.x+v2.x, v1.y+v2.y);
    }
    
    /** adds v to this vector */
    public static Vector2i subtract( Vector2i v1, Vector2i v2  ) {
        return new Vector2i(v1.x-v2.x, v1.y-v2.y);
    }
    
    /** adds v to this vector */
    public static Vector2i makeUnit(Vector2i v) {
        double len = v.length();
        return new Vector2i((int)(v.x/len), (int)(v.y/len));
    }
    
    public Vector2i clone() {
       return new Vector2i(this);
    }

    public String toString() {
       return "(" + x + ", " + y + ")";
    }

    public String toStringPlain() {
       return x + " " + y;
    }
   
}
