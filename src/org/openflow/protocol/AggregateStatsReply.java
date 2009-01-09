package org.openflow.protocol;

import java.io.IOException;
import org.openflow.lavi.net.protocol.StatsHeader;
import org.openflow.lavi.net.util.ByteBuffer;

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
    public AggregateStatsReply(long dpid, StatsFlag flags, ByteBuffer buf) throws IOException {
        super(StatsHeader.REPLY,
              dpid,
              StatsType.AGGREGATE,
              flags);
        
        packet_count = buf.nextLong();
        byte_count = buf.nextLong();
        flow_count = buf.nextInt();
        buf.nextInt(); /* 4B pad */
    }
    
    /** total length of this message in bytes */
    public int length() {
        return super.length() + 24;
    }
}
