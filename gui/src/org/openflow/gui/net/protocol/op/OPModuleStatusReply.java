package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;
import org.openflow.gui.net.protocol.Node;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Tells the GUI about a status of a module.
 * 
 * @author David Underhill
 */
public class OPModuleStatusReply extends OFGMessage {
    /** the node the module is on */
    public final Node node;
    
    /** the module */
    public final Node module;
    
    /** the status of the module */
    public final String status;
    
    public OPModuleStatusReply(final int len, final int xid, final DataInput in) throws IOException {
        super(OFGMessageType.OP_MODULE_STATUS_REPLY, xid);
        
        node = new Node(in);
        module = new Node(in);
        status = SocketConnection.readNullTerminatedString(in);
        
        if(!module.nodeType.isModule())
            throw new Error("Error: module must be of type OPModule");
        
        if(len != this.length())
            throw new IOException(this.getClass().getName() + " got message of length " + 
                    length() + " but only " + len + "B were expected");
    }
    
    /** This returns the length of this message */
    public int length() {
        return super.length() + Node.SIZEOF + Node.SIZEOF + status.length() + 1;
    }
    
    public String toString() {
        return super.toString() + TSSEP + "Status of module " + module + " on " + node + ": " + status;
    }
}
