package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

/**
 * Structure to describe a flow.
 * 
 * @author David Underhill
 */
public class Flow {
    /** type of flow */
    public final FlowType type;
    
    /** flow identifier */
    public final int id;
    
    public final Node srcNode;
    public final short srcPort;
    
    public final Node dstNode;
    public final short dstPort;
    
    /** the flow's path from source to destination (inclusive) */
    public final FlowHop[] path;
    
    public Flow(final FlowType type, final int id, Node srcNode, short srcPort, Node dstNode, short dstPort, final FlowHop... path) {
        this.type = type;
        this.id = id;
        this.srcNode = srcNode;
        this.srcPort = srcPort;
        this.dstNode = dstNode;
        this.dstPort = dstPort;
        this.path = path;
    }
    
    public Flow(final FlowType type, final int id, Node srcNode, short srcPort, Node dstNode, short dstPort, final Collection<FlowHop> path) {
        this.type = type;
        this.id = id;
        this.srcNode = srcNode;
        this.srcPort = srcPort;
        this.dstNode = dstNode;
        this.dstPort = dstPort;
        this.path = new FlowHop[path.size()];
        int i = 0;
        for(FlowHop elt : path) {
            this.path[i] = new FlowHop(elt.inport, new Node(elt.node.nodeType, elt.node.id), elt.outport);
            i += 1;
        }
    }
    
    /** This returns the length of Flow */
    public int length() {
        return 10 + path.length * FlowHop.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeShort(type.getTypeID());
        out.writeInt(id);
        srcNode.write(out);
        out.writeShort(srcPort);
        dstNode.write(out);
        out.writeShort(dstPort);
        out.writeShort(path.length);
        for(FlowHop e : path)
            e.write(out);
    }
    
    public String toString() {
        String ret = "Flow:" + type.toString() + ":" + id + ":" + srcNode + ":" + srcPort + "{";
        for(FlowHop e : path)
            ret += e.toString() + ", ";
        
        return ret.substring(0, ret.length()-1) + "}:" + dstNode + ":" + dstPort;
    }
}
