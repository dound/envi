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
    /** the flow's path from source to destination (inclusive) */
    public final IDPortPair[] path;
    
    public Flow(final IDPortPair... path) {
        this.path = path;
    }
    
    public Flow(final NodePortPair... path) {
        this.path = new IDPortPair[path.length];
        for(int i=0; i<path.length; i++)
            this.path[i] = new IDPortPair(path[i].node.getID(), path[i].port);
    }
    
    public Flow(final Collection<NodePortPair> path) {
        this.path = new IDPortPair[path.size()];
        int i = 0;
        for(NodePortPair elt : path) {
            this.path[i] = new IDPortPair(elt.node.getID(), elt.port);
            i += 1;
        }
    }

    /** This returns the length of Flow */
    public int length() {
        return 4 + path.length * (8 + 2);
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeInt(path.length);
        for(IDPortPair e : path) {
            out.writeLong(e.id);
            out.writeShort(e.port);
        }
    }
    
    public String toString() {
        String ret = "Flow{";
        for(IDPortPair e : path)
            ret += DPIDUtil.toString(e.id) + "/" + e.port + ", ";
        
        return ret.substring(0, ret.length()-1) + "}";
    }
}
