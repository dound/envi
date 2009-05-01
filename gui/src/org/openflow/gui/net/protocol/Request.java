package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * A generic request.
 *
 * @author David Underhill
 */
public class Request extends OFGMessage {
    public static final short ANY_TYPE = 0;
    
    /** what kind of request this is */
    public final RequestType requestType;
    
    /** the type of Node, Link, or Flow (etc) this is for */
    public final short type;
    
    public Request(OFGMessageType msgType, RequestType requestType) {
        this(msgType, requestType, ANY_TYPE);
    }
    
    public Request(OFGMessageType msgType, RequestType requestType, short type) {
        super(msgType, 0);
        this.requestType = requestType;
        this.type = type;
        if(!(this instanceof RequestLinks) && msgType==OFGMessageType.LINKS_REQUEST)
            throw(new Error("use RequestLinks for LINKS_REQUEST messages"));
    }
    
    /** This returns the message length */
    public int length() {
        return super.length() + 3;
    }
    
    /** Writes the header (via super.write()) and a byte representing the subscription state */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeByte(requestType.getTypeID());
        out.writeShort(type);
    }
    
    public String toString() {
        return super.toString() + TSSEP + requestType.toString();
    }
}
