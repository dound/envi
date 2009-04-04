package org.openflow.gui.net.protocol.auth;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Header of authentication packets.
 * 
 * @author David Underhill
 */
public abstract class AuthHeader extends OFGMessage {
    public static final boolean REQUEST = true;
    public static final boolean REPLY = false;
    
    /** the type of authentication to use */
    public final AuthType authType;
    
    public AuthHeader(boolean request, AuthType t) {
        super(request ? OFGMessageType.AUTH_REQUEST : OFGMessageType.AUTH_REPLY, 0);
        authType = t;
    }
    
    public AuthHeader(boolean request, final int xid, final DataInput in) throws IOException {
        super(request ? OFGMessageType.AUTH_REQUEST : OFGMessageType.AUTH_REPLY, xid);
        authType = AuthType.typeValToAuthType(in.readByte());
    }
    
    public int length() {
        return super.length() + 1;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeByte(authType.getAuthTypeID());
    }
    
    public String toString() {
        return super.toString() + TSSEP + authType.toString();
    }
}
