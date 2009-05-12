package org.openflow.gui.net.protocol.et;

/**
 * Enumerates the types of solvers.
 * 
 * @author David Underhill
 */
public enum SolverType {
    UNKNOWN((byte)0),
    SPREAD((byte)1),
    SQUISH((byte)2),
    MODEL_GLPK((byte)3),
    MODEL_GAMS((byte)4),
    HASH((byte)5),
    ;

    /** the special value used to identify messages of this type */
    private final byte typeID;

    private SolverType(byte typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public byte getTypeID() {
        return typeID;
    }

    /** 
     * Returns the SolverType constant associated with typeID, if any or
     * UNKOWN if no type is matched.
     */
    public static SolverType typeValToMessageType(byte typeID) {
        for(SolverType t : SolverType.values())
            if(t.getTypeID() == typeID)
                return t;

        return UNKNOWN;
    }
}
