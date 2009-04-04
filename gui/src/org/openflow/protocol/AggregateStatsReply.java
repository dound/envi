package org.openflow.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.StatsHeader;

/**
 * A reply with aggregate statistics.
 * 
 * @author David Underhill
 */
public class AggregateStatsReply extends StatsHeader {
    /** Number of packets in flows. */
    public long packet_count;

    /** Number of bytes in flows. */
    public long byte_count;

    /** Number of flows. */
    public int flow_count;

    /** Create an aggregate request for stats from the switch with the specified DPID. */
    public AggregateStatsReply(long dpid) {
        super(StatsHeader.REPLY,
              dpid,
              StatsType.AGGREGATE,
              StatsFlag.NONE);
    }
    
    /** 
     * Create an aggregate stats reply from the switch with the specified 
     * DPID and flags and read the reply from the receive buffer.
     */
    public AggregateStatsReply(long dpid, StatsFlag flags, DataInput in) throws IOException {
        super(StatsHeader.REPLY,
              dpid,
              StatsType.AGGREGATE,
              flags);
        
        packet_count = in.readLong();
        byte_count = in.readLong();
        flow_count = in.readInt();
        in.readInt(); /* 4B pad */
    }
    
    /** returns true because this message is part of a stateful exchange */
    public boolean isStatefulReply() {
        return true;
    }
    
    /** total length of this message in bytes */
    public int length() {
        return super.length() + 24;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeLong(packet_count);
        out.writeLong(byte_count);
        out.writeInt(flow_count);
        out.writeInt(0); // pad
    }
    
    public String toString() {
        return super.toString() + TSSEP + "#packets=" + packet_count 
                                        + " #bytes=" + byte_count
                                        + " #flows=" + flow_count;
    }
}
