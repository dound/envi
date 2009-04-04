package org.openflow.gui.net.protocol.auth;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Type of authentication.
 * 
 * @author David Underhill
 */
public enum AuthType {
    PLAIN_TEXT((byte)0x00),
    ROT13((byte)0x01),
    PUBLIC_KEY((byte)0x02),
    DNA_SAMPLE((byte)0x03),
    ;
    
    /** the special value used to identify authentication methods */
    private final byte authTypeID;

    private AuthType(byte authTypeID) {
        this.authTypeID = authTypeID;
    }

    /** returns the special value used to identify this type */
    public byte getAuthTypeID() {
        return authTypeID;
    }

    /** Returns the AuthType constant associated with authTypeID, if any */
    public static AuthType typeValToAuthType(byte authTypeID) {
        for(AuthType t : AuthType.values())
            if(t.getAuthTypeID() == authTypeID)
                return t;

        return null;
    }
    
    /** 
     * Constructs the object representing the received message.  The message is 
     * known to be of length len and len - OFGMessage.SIZEOF bytes representing
     * the rest of the message should be extracted from buf.
     */
    public static AuthHeader decode(int len, OFGMessageType t, int xid, DataInput in) throws IOException {
        if(t != OFGMessageType.AUTH_REQUEST)
            throw new IOException("AuthType.decode was unexpectedly asked to decode type " + t.toString());
        
        // parse the authentication header
        byte authTypeByte = in.readByte();
        AuthType authType = AuthType.typeValToAuthType(authTypeByte);
        if(authType == null)
            throw new IOException("Unknown authentication type ID: " + authTypeByte);
        
        // parse the rest of the message
        switch(authType) {
            case PLAIN_TEXT:
                return new AuthPlainText(in);
            
            default:
                throw new IOException("Unhandled authentication type received: " + authType.toString());
        }
    }
}
