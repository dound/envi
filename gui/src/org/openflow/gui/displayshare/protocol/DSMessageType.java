package org.openflow.gui.displayshare.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Enumerates what types of messages are in the DisplayShare protocol.
 * 
 * @author David Underhill
 */
public enum DSMessageType {
    /** parameters message */
    PARAMS((byte)0x00),
    
    /** a single frame (image of a display) */
    FRAME((byte)0x01),
    
    ;

    /** the special value used to identify messages of this type */
    private final byte typeID;

    private DSMessageType(byte typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public byte getTypeID() {
        return typeID;
    }

    /** Returns the OFGMessageType constant associated with typeID, if any */
    public static DSMessageType typeValToMessageType(byte typeID) {
        for(DSMessageType t : DSMessageType.values())
            if(t.getTypeID() == typeID)
                return t;

        return null;
    }
    
    /** 
     * Constructs the object representing the received message.  The message is 
     * known to be of length len and len - 4 bytes representing the rest of the 
     * message should be extracted from buf.
     */
    public static DSMessage decode(int len, DataInput in) throws IOException {
        // parse the message header (except length which was already done)
        byte typeByte = in.readByte();
        DSMessageType t = DSMessageType.typeValToMessageType(typeByte);
        if(t == null)
            throw new IOException("Unknown type ID: " + typeByte);
        
        DSMessage msg = decode(len, t, in);
        return msg;
    }
     
    /** 
     * Constructs the object representing the received message.  The message is 
     * known to be of length len.  The header of the message (length and type)
     * have been read, but the remainder is still on the input stream.
     */
    private static DSMessage decode(int len, DSMessageType t, DataInput in) throws IOException {
        // parse the rest of the message
        switch(t) {
            case PARAMS:
                return new DSParams(in);
            
            case FRAME:
                return new DSFrame(len, in);

            default:
                throw new IOException("Unhandled type received: " + t.toString() + " (len=" + len + "B)");
        }
    }
}
