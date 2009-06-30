package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * OPModule(s) added message.
 * 
 * @author Glen Gibb
 */
public class OPModuleInstancesDel extends OPModuleInstancesList {
    public OPModuleInstancesDel(final OPModuleInstance[] instances) {
        this(0, instances);
    }
    
    public OPModuleInstancesDel(int xid, final OPModuleInstance[] instances) {
        super(OFGMessageType.OP_MODULE_INSTANCES_DEL, xid, instances);
    }
    
    public OPModuleInstancesDel(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.OP_MODULE_INSTANCES_DEL, xid, in);
    }
}