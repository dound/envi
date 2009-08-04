package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;

public class OPModuleAlert extends OFGMessage {
    /** the module being reported upon */
    public final Node module;
    
    /** alert message */
    public final String msg;
    
    /** Construct from a data stream */
    public OPModuleAlert(final int len, final int xid, final DataInput in) throws IOException {
        super(OFGMessageType.OP_MODULE_ALERT, xid);

        module = new Node(in);
        msg = SocketConnection.readNullTerminatedString(in);
    }
}
