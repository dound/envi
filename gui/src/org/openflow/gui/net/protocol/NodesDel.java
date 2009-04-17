package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Switch(es) deleted message.
 * 
 * @author David Underhill
 */
public class NodesDel extends NodesList {
    public NodesDel(final Node[] nodes) {
        this(0, nodes);
    }
    
    public NodesDel(int xid, final Node[] nodes) {
        super(OFGMessageType.NODES_DELETE, xid, nodes);
    }
    
    public NodesDel(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.NODES_DELETE, xid, in);
    }
}
