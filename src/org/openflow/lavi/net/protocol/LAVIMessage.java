package org.openflow.lavi.net.protocol;

import java.io.IOException;
import org.openflow.lavi.net.util.ByteBuffer;

/**
 * Header for LAVI protocol messages.
 * 
 * @author David Underhill
 */
public class LAVIMessage {
    public static final int SIZEOF = 9;
    
    /** total length of this message in bytes */
    public int length() {
        return SIZEOF;
    }
    
    /** type of this message */
    public LAVIMessageType type;
    
    /** transaction id of this message, if any */
    public int xid;
    
    /** used to construct a LAVI message */
    public LAVIMessage(final LAVIMessageType t, final int xid) {
        this.type = t;
        this.xid = xid;
    }
    
    /** used to construct a message being received */
    public LAVIMessage(final LAVIMessageType t, final ByteBuffer buf) throws IOException {
        this(t, buf.nextInt());
    }
}
