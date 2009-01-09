package org.openflow.lavi.net.protocol;

import java.io.IOException;
import org.openflow.lavi.net.util.ByteBuffer;

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
    
    public SwitchesAdd(final int len, final ByteBuffer buf) throws IOException {
        super(len, LAVIMessageType.SWITCHES_ADD, buf);
    }
}
