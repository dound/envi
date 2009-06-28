package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * OPModule(s) added message.
 * 
 * @author Glen Gibb
 */
public class OPNodesDel extends OPNodesList {
    public OPNodesDel(final OPNode[] nodes) {
        this(0, nodes);
    }
    
    public OPNodesDel(int xid, final OPNode[] nodes) {
        super(OFGMessageType.OP_NODES_DEL, xid, nodes);
    }
    
    public OPNodesDel(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.OP_NODES_DEL, xid, in);
    }
}
