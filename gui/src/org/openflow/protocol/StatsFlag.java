package org.openflow.protocol;

/**
 * Enumerates what stats flags are in the OpenFlow protocol.  Equivalent 
 * to the OFPSF_REQ_* constants.
 * 
 * @author David Underhill
 */
public enum StatsFlag {
    /* none exist yet */
    NONE((short)0);
    
    /** the special value used to identify stats flags */
    private final short typeID;

    private StatsFlag(short typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public short getTypeID() {
        return typeID;
    }

    /** Returns the OFGMessageType constant associated with typeID, if any */
    public static StatsFlag typeValToStatsFlag(short typeID) {
        for(StatsFlag t : StatsFlag.values())
            if(t.getTypeID() == typeID)
                return t;

        return null;
    }
}
