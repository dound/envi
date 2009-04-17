package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Flow(s) deleted message.
 * 
 * @author David Underhill
 */
public class FlowsDel extends FlowsList {
    public FlowsDel(final Flow[] flows) {
        this(0, flows);
    }
    
    public FlowsDel(int xid, final Flow[] flows) {
        super(OFGMessageType.FLOWS_DELETE, xid, flows);
    }
    
    public FlowsDel(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.FLOWS_DELETE, xid, in);
    }
}
