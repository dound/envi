package org.openflow.protocol;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessageType;
import org.openflow.gui.net.protocol.StatsHeader;

/**
 * Enumerates what types of stats are in the OpenFlow protocol.  Equivalent 
 * to the OFPST_* constants.
 * 
 * @author David Underhill
 */
public enum StatsType {
    /** Description of this OpenFlow switch */
    DESC((short)0x0000),

    /** Individual flow stats */
    FLOW((short)0x0001),

    /** Aggregate flow statistics */
    AGGREGATE((short)0x0002),

    /** Flow table statistics */
    TABLE((short)0x0003),

    /** Physical port statistics */
    PORT((short)0x0004),

    /** Vendor extension */
    VENDOR((short)0xFFFF);

    /** the special value used to identify stats of this type */
    private final short typeID;

    private StatsType(short typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public short getTypeID() {
        return typeID;
    }

    /** Returns the OFGMessageType constant associated with typeID, if any */
    public static StatsType typeValToStatsType(short typeID) {
        for(StatsType t : StatsType.values())
            if(t.getTypeID() == typeID)
                return t;

        return null;
    }
    
    /**
     * Constructs the object representing the received message.  The message is 
     * known to be of length len and len - OFGMessage.SIZEOF bytes representing
     * the rest of the message should be extracted from buf.
     */
    public static StatsHeader decode(int len, OFGMessageType t, int xid, DataInput in) throws IOException {
        if(t != OFGMessageType.STAT_REPLY)
            throw new IOException("StatsType.decode was unexpectedly asked to decode type " + t.toString());
        
        // parse the stats header
        long dpid = in.readLong();
        
        short statsTypeVal = in.readShort();
        StatsType type = StatsType.typeValToStatsType(statsTypeVal);
        if(type == null)
            throw new IOException("Unknown stats type ID: " + statsTypeVal);
        
        StatsFlag flags = StatsFlag.typeValToStatsFlag(in.readShort());
        
        // parse the rest of the message
        switch(type) {
            case DESC:
                return new SwitchDescriptionStats(dpid, flags, in);

            case AGGREGATE:
                return new AggregateStatsReply(dpid, flags, in);
            
            default:
                throw new IOException("Unhandled stats type received: " + type.toString());
        }
    }
}
