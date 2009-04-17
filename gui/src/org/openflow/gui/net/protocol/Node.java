package org.openflow.gui.net.protocol;

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

    /** unique ID of the node (datapath ID for a switch) */
    public final long id;

    /** type of the node */
    public final NodeType nodeType;
    
    public Node(long id, NodeType nodeType) {
        this.id = id;
        this.nodeType = nodeType;
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeLong(id);
        out.writeShort(nodeType.getTypeID());
    }
    
    public String toString() {
        return nodeType + "{" + DPIDUtil.toString(id) + "}";
    }
}
