package org.openflow.gui.net.protocol.et;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessageType;
import org.openflow.gui.net.protocol.SwitchList;

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
        super(OFGMessageType.ET_SWITCHES_OFF, xid, dpids);
    }
    
    public ETSwitchesOff(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.ET_SWITCHES_OFF, xid, in);
    }
}
