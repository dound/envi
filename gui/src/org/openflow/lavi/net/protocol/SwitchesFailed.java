package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Switch(es) added message.
 * 
 * @author David Underhill
 */
public class SwitchesFailed extends SwitchList {
    public SwitchesFailed(final long[] dpids) {
        this(0, dpids);
    }
    
    public SwitchesFailed(int xid, final long[] dpids) {
        super(LAVIMessageType.ET_SWITCHES_OFF, xid, dpids);
    }
    
    public SwitchesFailed(final int len, final int xid, final DataInput in) throws IOException {
        super(len, LAVIMessageType.ET_SWITCHES_OFF, xid, in);
    }
}
