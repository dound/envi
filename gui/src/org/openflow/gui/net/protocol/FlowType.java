package org.openflow.gui.net.protocol;

/**
 * Enumerates what types of flows are in the topology.
 * 
 * @author David Underhill
 */
public enum FlowType {
    /** unknown node type */
    UNKNOWN((short)0)
    
    ;

    /** the special value used to identify messages of this type */
    private final short typeID;

    private FlowType(short typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public short getTypeID() {
        return typeID;
    }

    /** 
     * Returns the FlowType constant associated with typeID, if any or
     * UNKOWN if no type is matched.
     */
    public static FlowType typeValToMessageType(short typeID) {
        for(FlowType t : FlowType.values())
            if(t.getTypeID() == typeID)
                return t;

        return UNKNOWN;
    }
}
