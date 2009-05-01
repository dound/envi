package org.openflow.gui.net.protocol;

/**
 * Enumerates the types of requests.
 * 
 * @author David Underhill
 */
public enum RequestType {
    UNKNOWN((byte)0),
    
    ONETIME((byte)1),
    
    SUBSCRIBE((byte)2),
    
    UNSUBSCRIBE((byte)3),
    
    ;

    /** the special value used to identify messages of this type */
    private final byte typeID;

    private RequestType(byte typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public byte getTypeID() {
        return typeID;
    }

    /** 
     * Returns the RequestType constant associated with typeID, if any or
     * UNKOWN if no type is matched.
     */
    public static RequestType typeValToMessageType(byte typeID) {
        for(RequestType t : RequestType.values())
            if(t.getTypeID() == typeID)
                return t;

        return UNKNOWN;
    }
}
