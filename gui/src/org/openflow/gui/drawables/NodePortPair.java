package org.openflow.gui.drawables;

/**
 * A pair of a NodeWithPorts and port number.
 * 
 * @author David Underhill
 */
public class NodePortPair {
    /** the node */
    public final NodeWithPorts node;
    
    /** the port */
    public final short port;
    
    /** create a new NodePortPair */
    public NodePortPair(NodeWithPorts n, short p) { 
        node = n;
        port = p;
    }
    
    public int hashCode() {
        return node.hashCode() + port;
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof NodePortPair)) return false;
        NodePortPair npp = (NodePortPair)o;
        return npp.port==port && npp.node.equals(node);
    }
}