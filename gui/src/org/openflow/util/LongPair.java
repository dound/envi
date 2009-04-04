package org.openflow.util;

/**
 * A pair of long values.
 * 
 * @author David Underhill
 */
public class LongPair {
    /** the smaller of the two values */
    public final long a;
    
    /** the larger of the two values */
    public final long b;
    
    /** the hash code */
    public final int hash;
    
    /** create a new LongPair */
    public LongPair(long l1, long l2) { 
        if(l1 < l2) {
            a = l1;
            b = l2;
        }
        else {
            a = l2;
            b = l1;
        }
        hash = 7 * new Long(a).hashCode() + new Long(b).hashCode();
    }
    
    public int hashCode() {
        return hash;
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof LongPair)) return false;
        LongPair l = (LongPair)o;
        return l.a==a && l.b==b;
    }
}
