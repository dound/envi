package org.openflow.gui.net.protocol.auth;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Authentication status update message.
 * 
 * @author David Underhill
 */
public class AuthStatus extends OFGMessage {
    /** whether the receiver has been authenticated */
    public final boolean authenticated;
    
    /** status message */
    public final String msg;
    
    /** 
     * Read in the body of an AuthRequest.
     */
    public AuthStatus(final int len, final int xid, final DataInput in) throws IOException {
        super(OFGMessageType.AUTH_STATUS, xid);
        
        authenticated = in.readBoolean();
        
        int left = len - super.length() - 1;
        byte strBytes[] = new byte[left];
        for(int i=0; i<left; i++)
            strBytes[i] = in.readByte();
        msg = new String(strBytes);
    }
    
    public int length() {
        return super.length() + 1 + msg.length();
    }
    
    public String toString() {
        String prefix = authenticated ? "" : "not ";
        String m = (msg.length() > 0) ? ": " + msg : "";
        return super.toString() + TSSEP + prefix + " authenticated" + m; 
    }
}
