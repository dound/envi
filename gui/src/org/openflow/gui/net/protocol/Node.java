package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.util.string.DPIDUtil;

/**
 * Structure to specify a node.
 * 
 * @author David Underhill
 */
public class Node {
    public static final int SIZEOF = 10;

    /** type of the node */
    public final NodeType nodeType;
    
    /** unique ID of the node (datapath ID for a switch) */
    public final long id;
    
    public Node(DataInput in) throws IOException {
        this(NodeType.typeValToMessageType(in.readShort()), in.readLong());
    }
    
    public Node(NodeType nodeType, long id) {
        this.nodeType = nodeType;
        this.id = id;
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeShort(nodeType.getTypeID());
        out.writeLong(id);
    }
    
    public String toString() {
        return nodeType + "{" + DPIDUtil.toString(id) + "}";
    }

    public int hashCode() {
        return 7*(int)(id ^ (id >>> 32)) + 15*nodeType.getTypeID();
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof Node)) return false;
        Node n = (Node)o;
        return nodeType.getTypeID()==n.nodeType.getTypeID() && id==n.id;
    }
}
