package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * OPModule(s) added message.
 * 
 * @author David Underhill
 */
public class OPModulesAdd extends OPModulesList {
    public OPModulesAdd(final OPModule[] modules) {
        this(0, modules);
    }
    
    public OPModulesAdd(int xid, final OPModule[] modules) {
        super(OFGMessageType.OP_MODULES_ADD, xid, modules);
    }
    
    public OPModulesAdd(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.OP_MODULES_ADD, xid, in);
    }
}
