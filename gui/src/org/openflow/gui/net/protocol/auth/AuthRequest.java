package org.openflow.gui.net.protocol.auth;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * A request for the client to authenticate.
 * 
 * @author David Underhill
 */
public class AuthRequest extends OFGMessage {
    public final byte[] salt;
    
    /** 
     * Read in the body of an AuthRequest.
     */
    public AuthRequest(final int len, final int xid, final DataInput in) throws IOException {
        super(OFGMessageType.AUTH_REQUEST, xid);
        
        int left = len - super.length();
        salt = new byte[left];
        for(int i=0; i<left; i++)
            salt[i] = in.readByte();
    }
    
    public int length() {
        return super.length() + salt.length;
    }
    
    public String toString() {
        return super.toString() + TSSEP + salt.length + "B of salt";
    }
}
