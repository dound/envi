package org.openflow.gui.net.protocol;

/**
 * Enumerates what types of links are in the topology.
 * 
 * @author David Underhill
 */
public enum LinkType {
    /** unknown link type */
    UNKNOWN((short)0),
    
    /** a wired link */
    WIRE((short)1),

    /** a wireless link */
    WIRELESS((short)2),

    /** a tunneled link */
    TUNNEL((short)4),
    
    ;

    /** the special value used to identify messages of this type */
    private final short typeID;

    private LinkType(short typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public short getTypeID() {
        return typeID;
    }

    /** 
     * Returns the LinkType constant associated with typeID, if any or
     * UNKOWN if no type is matched.
     */
    public static LinkType typeValToMessageType(short typeID) {
        for(LinkType t : LinkType.values())
            if(t.getTypeID() == typeID)
                return t;

        return UNKNOWN;
    }
}
