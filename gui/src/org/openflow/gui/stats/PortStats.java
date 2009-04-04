package org.openflow.gui.stats;

import org.openflow.protocol.AggregateStatsReply;
import org.openflow.protocol.Match;

/** 
 * Specifies statistics being gathered and their latest values.
 * 
 * @author David Underhill
 */
public class PortStats {
    /** fields to match */
    final Match match;
    
    /** Number of packets in flows. */
    protected long numPackets = 0;

    /** Number of bytes in flows. */
    protected long numBytes = 0;

    /** Number of flows. */
    protected int numFlows = 0;
    
    /** when these stats were last updated */
    protected long updateTime = System.currentTimeMillis();
    
    /** 
     * Constructs a LinkStats to track stats for a specified match.
     */
    public PortStats(final Match m) {
        this.match = m;
    }
    
    /** returns the current packet count */
    public long getPacketCount() {
        return numPackets;
    }

    /** returns the current byte count */
    public long getByteCount() {
        return numBytes;
    }

    /** returns the current flow count */
    public int getFlowCount() {
        return numFlows;
    }
    
    /** returns the last update time */
    public long getLastUpdateTime() {
        return updateTime;
    }
    
    /** update the statistics with those contained in r */
    public final void update(AggregateStatsReply r) {
        update(r.packet_count, r.byte_count, r.flow_count, r.timeCreated);
    }
    
    /** 
     * Update the statistics with the specified values (the statistics are 
     * assumed to have been collected at the current time). 
     * */
    public final void update(long packetCount, long byteCount, int flowCount) {
        update(packetCount, byteCount, flowCount, System.currentTimeMillis());
    }

    /** update the statistics with the specified values */
    public void update(long packetCount, long byteCount, int flowCount, long when) {
        numPackets = packetCount;
        numBytes = byteCount;
        numFlows = flowCount;
        updateTime = when;
    }
    
    public String toString() {
        return new java.util.Date(updateTime).toString() + " =>" + 
            " #packets=" + numPackets +
            " #bytes=" + numBytes +
            " #flows=" + numFlows;
    }
}
