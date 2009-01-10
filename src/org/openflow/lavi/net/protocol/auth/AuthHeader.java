package org.openflow.lavi.net.protocol.auth;

import java.io.IOException;
import org.openflow.lavi.net.util.ByteBuffer;
import org.openflow.lavi.net.protocol.LAVIMessage;
import org.openflow.lavi.net.protocol.LAVIMessageType;

/**
 * Header of authentication packets.
 * 
 * @author David Underhill
 */
public abstract class AuthHeader extends LAVIMessage {
    public static final boolean REQUEST = true;
    public static final boolean REPLY = false;
    
    /** the type of authentication to use */
    public final AuthType authType;
    
    public AuthHeader(boolean request, AuthType t) {
        super(request ? LAVIMessageType.AUTH_REQUEST : LAVIMessageType.AUTH_REPLY, 0);
        authType = t;
    }
    
    public AuthHeader(boolean request, final ByteBuffer buf) throws IOException {
        super(request ? LAVIMessageType.AUTH_REQUEST : LAVIMessageType.AUTH_REPLY, buf);
        authType = AuthType.typeValToAuthType(buf.nextByte());
    }
    
    public int length() {
        return super.length() + 1;
    }
}