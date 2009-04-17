package org.openflow.gui.net.protocol.et;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.Node;
import org.openflow.gui.net.protocol.OFGMessageType;
import org.openflow.gui.net.protocol.NodesList;

/**
 * Switch(es) off message.
 * 
 * @author David Underhill
 */
public class ETSwitchesOff extends NodesList {
    public ETSwitchesOff(final Node[] nodes) {
        this(0, nodes);
    }
    
    public ETSwitchesOff(int xid, final Node[] nodes) {
        super(OFGMessageType.ET_SWITCHES_OFF, xid, nodes);
    }
    
    public ETSwitchesOff(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.ET_SWITCHES_OFF, xid, in);
    }
}
