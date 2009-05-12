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
    
    /** the flow's path from source to destination (inclusive) */
    public final NodePortPair[] path;
    
    public Flow(final FlowType type, final int id, final NodePortPair... path) {
        this.type = type;
        this.id = id;
        this.path = path;
    }
    
    public Flow(final FlowType type, final int id, final org.openflow.util.NodePortPair... path) {
        this.type = type;
        this.id = id;
        this.path = new NodePortPair[path.length];
        for(int i=0; i<path.length; i++)
            this.path[i] = new NodePortPair(new Node(path[i].node.getType(), path[i].node.getID()), path[i].port);
    }
    
    public Flow(final FlowType type, final int id, final Collection<NodePortPair> path) {
        this.type = type;
        this.id = id;
        this.path = new NodePortPair[path.size()];
        int i = 0;
        for(NodePortPair elt : path) {
            this.path[i] = new NodePortPair(new Node(elt.node.nodeType, elt.node.id), elt.port);
            i += 1;
        }
    }
    
    /** This returns the length of Flow */
    public int length() {
        return 10 + path.length * NodePortPair.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeShort(type.getTypeID());
        out.writeInt(id);
        out.writeInt(path.length);
        for(NodePortPair e : path)
            e.write(out);
    }
    
    public String toString() {
        String ret = "Flow:" + type.toString() + ":" + id + "{";
        for(NodePortPair e : path)
            ret += e.toString() + ", ";
        
        return ret.substring(0, ret.length()-1) + "}";
    }
}
