package org.openflow.gui.net.protocol;

/**
 * Enumerates what types of nodes are in the topology.
 * 
 * @author David Underhill
 */
public enum NodeType {
    /** unknown node type */
    UNKNOWN((short)0),
    
    /** an OpenFlow switch */
    OPENFLOW_SWITCH((short)1),

    /** an OpenFlow-enabled wireless access point */
    OPENFLOW_WIRELESS_ACCESS_POINT((short)2),

    /** a host */
    HOST((short)100),
    
    /** the input node */
    TYPE_IN((short)1000),
    
    /** the output node */
    TYPE_OUT((short)1001),
    
    /** a NetFPGA */
    TYPE_NETFPGA((short)1002),
    
    /** a laptop */
    TYPE_LAPTOP((short)1003),
    
    /** a hardware module */
    TYPE_MODULE_HW((short)2000),
    
    /** a software module */
    TYPE_MODULE_SW((short)2001),
    
    ;

    /** the special value used to identify messages of this type */
    private final short typeID;

    private NodeType(short typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public short getTypeID() {
        return typeID;
    }

    /** 
     * Returns the NodeType constant associated with typeID, if any or
     * UNKOWN if no type is matched.
     */
    public static NodeType typeValToMessageType(short typeID) {
        for(NodeType t : NodeType.values())
            if(t.getTypeID() == typeID)
                return t;

        return UNKNOWN;
    }
}
