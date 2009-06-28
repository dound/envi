package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Structure to specify a node.
 * 
 * @author Glen Gibb
 */
public class OPModuleInstance {
    public static final int SIZEOF = 16;

    /** id of the module */
    public final long module_id;
    
    /** id of the node */
    public final long node_id;
    
    public OPModuleInstance(DataInput in) throws IOException {
        module_id = in.readLong();
        node_id = in.readLong();
    }

    public void write(DataOutput out) throws IOException {
        out.writeLong(module_id);
        out.writeLong(node_id);
    }
    
   public String toString() {
        return "MODULE_INSTANCE: module_id=" + module_id + " node_id=" + node_id; 
    }
}
