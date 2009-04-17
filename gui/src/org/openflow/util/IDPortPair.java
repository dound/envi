package org.openflow.util;

/**
 * A pair of an ID and port number.
 * 
 * @author David Underhill
 */
public class IDPortPair {
    /** the node */
    public final long id;
    
    /** the port */
    public final short port;
    
    /** create a new IDPortPair */
    public IDPortPair(long id, short p) { 
        this.id = id;
        port = p;
    }
    
    public int hashCode() {
        return new Long(id).hashCode() + port;
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof IDPortPair)) return false;
        IDPortPair npp = (IDPortPair)o;
        return npp.port==port && npp.id==id;
    }
}
