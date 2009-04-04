package org.openflow.gui.net.protocol;

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
        super(OFGMessageType.SWITCHES_ADD, xid, dpids);
    }
    
    public SwitchesAdd(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.SWITCHES_ADD, xid, in);
    }
}
