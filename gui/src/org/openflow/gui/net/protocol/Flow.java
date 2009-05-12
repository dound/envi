package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

import org.openflow.util.IDPortPair;
import org.openflow.util.NodePortPair;
import org.openflow.util.string.DPIDUtil;

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
    public final IDPortPair[] path;
    
    public Flow(final FlowType type, final int id, final IDPortPair... path) {
        this.type = type;
        this.id = id;
        this.path = path;
    }
    
    public Flow(final FlowType type, final int id, final NodePortPair... path) {
        this.type = type;
        this.id = id;
        this.path = new IDPortPair[path.length];
        for(int i=0; i<path.length; i++)
            this.path[i] = new IDPortPair(path[i].node.getID(), path[i].port);
    }
    
    public Flow(final FlowType type, final int id, final Collection<NodePortPair> path) {
        this.type = type;
        this.id = id;
        this.path = new IDPortPair[path.size()];
        int i = 0;
        for(NodePortPair elt : path) {
            this.path[i] = new IDPortPair(elt.node.getID(), elt.port);
            i += 1;
        }
    }
    
    /** This returns the length of Flow */
    public int length() {
        return 10 + path.length * (8 + 2);
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeShort(type.getTypeID());
        out.writeInt(id);
        out.writeInt(path.length);
        for(IDPortPair e : path) {
            out.writeLong(e.id);
            out.writeShort(e.port);
        }
    }
    
    public String toString() {
        String ret = "Flow:" + type.toString() + ":" + id + "{";
        for(IDPortPair e : path)
            ret += DPIDUtil.toString(e.id) + "/" + e.port + ", ";
        
        return ret.substring(0, ret.length()-1) + "}";
    }
}
