package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * A hop that a flow goes through.
 * 
 * @author David Underhill
 */
public class FlowHop {
    public static final int SIZEOF = Node.SIZEOF + 2;
    
    /** the incoming port */
    public final short inport;
    
    /** the node the flow goes through for this hop */
    public final Node node;
    
    /** the outgoing port */
    public final short outport;
    
    /** create a new IDPortPair */
    public FlowHop(short inport, Node n, short outport) {
        this.inport = inport;
        this.node = n;
        this.outport = outport;
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeShort(inport);
        node.write(out);
        out.writeShort(outport);
    }
    
    public int hashCode() {
        return node.hashCode() + inport + 15 * outport;
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof FlowHop)) return false;
        FlowHop npp = (FlowHop)o;
        return npp.inport==inport && node.equals(npp.node) && npp.outport==outport;
    }
    
    public String toString() {
        return node.toString() + ":" + inport + ":" + outport;
    }
}
