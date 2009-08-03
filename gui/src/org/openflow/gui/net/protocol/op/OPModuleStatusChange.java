package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.Node;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Indicate a status change in a module
 * 
 * @author grg
 */
public class OPModuleStatusChange extends OFGMessage {
    public static final int STATUS_READY = 1;
    public static final int STATUS_NOT_READY = 2;
    
    /** the module being reported upon */
    public final Node module;
    
    /** The status */
    public final int status;
    
    /** Construct from a data stream */
    public OPModuleStatusChange(final int len, final int xid, final DataInput in) throws IOException {
        super(OFGMessageType.OP_MODULE_STATUS_CHANGE, xid);

        module = new Node(in);
        status = in.readByte();
    }

    /** Check if the module is ready */
    public boolean isReady() {
        return status == STATUS_READY;
    }
}
