package org.openflow.protocol;

import java.io.IOException;
import org.openflow.lavi.net.protocol.StatsHeader;
import org.openflow.lavi.net.util.ByteBuffer;

/**
 * A request for aggregate statistics.
 * 
 * @author David Underhill
 */
public class AggregateStatsRequest extends StatsHeader {
    public static final byte ALL_TABLES = (byte)0xFF;
    public static final short OFPP_NONE = (short)0xFFFF;
    
    /** Fields to match */
    public final Match match;
    
    /** ID of table to read or 0xFF for all tables */
    public byte tableID;
    
    /** Require matching entries to include this output port, or OFPP_NONE */
    public short outPort;
    
    /** Create an aggregate stats request from the switch with this DPID. */
    public AggregateStatsRequest(long dpid) {
        this(dpid, new Match());
    }
    
    /** Create an aggregate stats request from the switch with this DPID and match. */
    public AggregateStatsRequest(long dpid, Match m) {
        this(dpid, m, ALL_TABLES);
    }
    
    /** Create an aggregate stats request from the switch with this DPID, match, and table ID. */
    public AggregateStatsRequest(long dpid, Match m, byte tableID) {
        this(dpid, m, tableID, OFPP_NONE);
    }
    
    /** Create an aggregate stats request from the switch with this DPID, match, table ID, and port. */
    public AggregateStatsRequest(long dpid, Match m, byte tableID, short outPort) {
        super(StatsHeader.REQUEST,
              dpid,
              StatsType.AGGREGATE,
              StatsFlag.NONE);
        
        this.match   = m;
        this.tableID = tableID;
        this.outPort = outPort;
    }
    
    /** 
     * Create an aggregate request for stats from the switch with the specified 
     * DPID and flags and read the request from the receive buffer.
     */
    public AggregateStatsRequest(long dpid, StatsFlag flags, ByteBuffer buf) throws IOException {
        super(StatsHeader.REQUEST,
              dpid,
              StatsType.AGGREGATE,
              flags);
        
        match = new Match(buf);
        tableID = buf.nextByte();
        buf.nextByte(); /* 1B of pad */
        outPort = buf.nextShort();
    }
    
    /** total length of this message in bytes */
    public int length() {
        return super.length() + Match.SIZEOF + 4;
    }
}