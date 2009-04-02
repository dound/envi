package org.openflow.lavi.net.protocol.et;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.lavi.net.protocol.LAVIMessageType;
import org.openflow.lavi.net.protocol.SwitchList;

/**
 * Switch(es) off message.
 * 
 * @author David Underhill
 */
public class ETSwitchesOff extends SwitchList {
    public ETSwitchesOff(final long[] dpids) {
        this(0, dpids);
    }
    
    public ETSwitchesOff(int xid, final long[] dpids) {
        super(LAVIMessageType.ET_SWITCHES_OFF, xid, dpids);
    }
    
    public ETSwitchesOff(final int len, final int xid, final DataInput in) throws IOException {
        super(len, LAVIMessageType.ET_SWITCHES_OFF, xid, in);
    }
}
