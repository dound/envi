package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Node(s) added message.
 * 
 * @author David Underhill
 */
public class NodesAdd extends NodesList {
    public NodesAdd(final Node[] nodes) {
        this(0, nodes);
    }
    
    public NodesAdd(int xid, final Node[] nodes) {
        super(OFGMessageType.NODES_ADD, xid, nodes);
    }
    
    public NodesAdd(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.NODES_ADD, xid, in);
    }
}
