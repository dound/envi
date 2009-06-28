package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * OPModule(s) deleted message.
 * 
 * @author Glen Gibb
 */
public class OPModulesDel extends OPModulesList {
    public OPModulesDel(final OPModule[] modules) {
        this(0, modules);
    }
    
    public OPModulesDel(int xid, final OPModule[] modules) {
        super(OFGMessageType.OP_MODULES_DEL, xid, modules);
    }
    
    public OPModulesDel(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.OP_MODULES_DEL, xid, in);
    }
}
