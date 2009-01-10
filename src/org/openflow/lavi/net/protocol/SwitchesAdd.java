package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Switch(es) added message.
 * 
 * @author David Underhill
 */
public class SwitchesAdd extends SwitchList {
    public SwitchesAdd(final long[] dpids) {
        this(0, dpids);
    }
    
    public SwitchesAdd(int xid, final long[] dpids) {
        super(LAVIMessageType.SWITCHES_ADD, xid, dpids);
    }
    
    public SwitchesAdd(final int len, final DataInput in) throws IOException {
        super(len, LAVIMessageType.SWITCHES_ADD, in);
    }
}
