package org.openflow.gui.displayshare.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Header of all DisplayShare protocol messages.
 * 
 * @author David Underhill
 */
public abstract class DSMessage implements org.openflow.gui.net.Message<DSMessageType> {
    /** gets the type of the message */
    public abstract DSMessageType getType();
    
    /** total length of this message in bytes */
    public int length() {
        return 3;
    }
    
    /** sends the message over the specified output stream */
    public void write(DataOutput out) throws IOException {
        out.writeShort(length());
        out.writeByte(getType().getTypeID());
    }
    
    public String toString() {
        DSMessageType type = getType();
        return (type == null ? "null-type" : type.toString());
    }
}
