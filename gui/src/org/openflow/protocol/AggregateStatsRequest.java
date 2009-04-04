package org.openflow.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.StatsHeader;

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
        this(dpid, OFPP_NONE);
    }
    
    /** Create an aggregate stats request from the switch with this DPID and port. */
    public AggregateStatsRequest(long dpid, short outPort) {
        this(dpid, outPort, new Match());
    }
    
    /** Create an aggregate stats request from the switch with this DPID, port, and match. */
    public AggregateStatsRequest(long dpid, short outPort, Match m) {
        this(dpid, outPort, m, ALL_TABLES);
    }
    
    /** Create an aggregate stats request from the switch with this DPID, port, match, and table ID. */
    public AggregateStatsRequest(long dpid, short outPort, Match m, byte tableID) {
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
    public AggregateStatsRequest(long dpid, StatsFlag flags, DataInput in) throws IOException {
        super(StatsHeader.REQUEST,
              dpid,
              StatsType.AGGREGATE,
              flags);
        
        match = new Match(in);
        tableID = in.readByte();
        in.readByte(); /* 1B of pad */
        outPort = in.readShort();
    }
    
    /** returns true because this message is part of a stateful exchange */
    public boolean isStatefulRequest() {
        return true;
    }
    
    /** total length of this message in bytes */
    public int length() {
        return super.length() + Match.SIZEOF + 4;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        match.write(out);
        out.writeByte(tableID);
        out.writeByte(0); // pad
        out.writeShort(outPort);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "table=" + tableID
                                        + " port=" + outPort
                                        + " match=" + match.toString();
    }
}
