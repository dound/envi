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
    public final FlowHop[] path;
    
    public Flow(final FlowType type, final int id, final FlowHop... path) {
        this.type = type;
        this.id = id;
        this.path = path;
    }
    
    public Flow(final FlowType type, final int id, final Collection<FlowHop> path) {
        this.type = type;
        this.id = id;
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
        out.writeShort(path.length);
        for(FlowHop e : path)
            e.write(out);
    }
    
    public String toString() {
        String ret = "Flow:" + type.toString() + ":" + id + " {";
        for(FlowHop e : path)
            ret += e.toString() + ", ";
        
        return ret.substring(0, ret.length()-1) + "} ";
    }
}
