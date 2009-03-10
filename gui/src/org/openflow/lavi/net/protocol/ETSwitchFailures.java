package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Switch(es) failed message.
 * 
 * @author David Underhill
 */
public class ETSwitchFailures extends SwitchList {
    public ETSwitchFailures(final long[] dpids) {
        this(0, dpids);
    }
    
    public ETSwitchFailures(int xid, final long[] dpids) {
        super(LAVIMessageType.ET_SWITCH_FAILURES, xid, dpids);
    }
    
    public ETSwitchFailures(final int len, final int xid, final DataInput in) throws IOException {
        super(len, LAVIMessageType.ET_SWITCH_FAILURES, xid, in);
    }
}
