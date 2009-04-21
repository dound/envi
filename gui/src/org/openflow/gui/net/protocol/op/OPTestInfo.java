package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Tells the GUI about a test of the system.
 * 
 * @author David Underhill
 *
 */
public class OPTestInfo extends OFGMessage {
    /** the test input */
    public final String input;
    
    /** the test output */
    public final String output;
    
    public OPTestInfo(final int len, final int xid, final DataInput in) throws IOException {
        super(OFGMessageType.OP_TEST_INFO, xid);
        
        this.input  = SocketConnection.readNullTerminatedString(in);
        this.output = SocketConnection.readNullTerminatedString(in);
        
        if(len != this.length())
            throw new IOException("OPTestInfo got message of length " + 
                    length() + " but only " + len + "B were expected");
    }
    
    /** This returns the length of this message */
    public int length() {
        return super.length() + input.length() + 1 + output.length() + 1;
    }
    
    public String toString() {
        return super.toString() + TSSEP + input + " ==> " + output;
    }
}
