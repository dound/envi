package org.openflow.util;

/**
 * A pair of two objects.
 * 
 * @author David Underhill
 */
public class Pair<A, B> {
    public final A a;
    public final B b;
    
    /** create a new Pair */
    public Pair(A a, B b) { 
        this.a = a;
        this.b = b;
    }
    
    public int hashCode() {
        int aPart = (a==null ? 0 : a.hashCode());
        return aPart * 31 + (b==null ? 0 : b.hashCode());
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof Pair)) return false;
        Pair p = (Pair)o;
        
        if(a != p.a) {
            if(a==null || !a.equals(p.a))
                return false;
        }
        
        if(b != p.b) {
            if(b==null || !b.equals(p.b))
                return false;
        }
        
        return true;
    }
}
