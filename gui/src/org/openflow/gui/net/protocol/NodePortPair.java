package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * A pair of a Node and port number.
 * 
 * @author David Underhill
 */
public class NodePortPair {
    public static final int SIZEOF = Node.SIZEOF + 2;
    
    /** the node */
    public final Node node;
    
    /** the port */
    public final short port;
    
    /** create a new IDPortPair */
    public NodePortPair(Node n, short p) { 
        this.node = n;
        port = p;
    }
    
    public void write(DataOutput out) throws IOException {
        node.write(out);
        out.writeShort(port);
    }
    
    public int hashCode() {
        return node.hashCode() + port;
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof NodePortPair)) return false;
        NodePortPair npp = (NodePortPair)o;
        return npp.port==port && node.equals(npp.node);
    }
    
    public String toString() {
        return node.toString() + ":" + port;
    }
}
