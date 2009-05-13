package org.openflow.util;

import org.openflow.gui.drawables.NodeWithPorts;

/**
 * A hop for some path through the network.  It specifies a node on that path 
 * and the port on which the path enters the node and the port on which it 
 * leaves.
 * 
 * @author David Underhill
 */
public class FlowHop {
    /** the incoming port */
    public final short inport;
    
    /** the node */
    public final NodeWithPorts node;
    
    /** the outgoing port */
    public final short outport;
    
    /** create a new NodePortPair */
    public FlowHop(short inport, NodeWithPorts n, short outport) {
        this.inport = inport;
        this.node = n;
        this.outport = outport;
    }
    
    public int hashCode() {
        return node.hashCode() + inport + 15*outport;
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof FlowHop)) return false;
        FlowHop npp = (FlowHop)o;
        return npp.inport==inport && npp.node.equals(node) && npp.outport==outport;
    }
}
