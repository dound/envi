package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Flow(s) added message.
 * 
 * @author David Underhill
 */
public class FlowsAdd extends FlowsList {
    public FlowsAdd(final Flow[] flows) {
        this(0, flows);
    }
    
    public FlowsAdd(int xid, final Flow[] flows) {
        super(OFGMessageType.FLOWS_ADD, xid, flows);
    }
    
    public FlowsAdd(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.FLOWS_ADD, xid, in);
    }
}
