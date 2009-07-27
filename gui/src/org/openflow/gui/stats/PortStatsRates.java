package org.openflow.gui.stats;

import org.openflow.protocol.Match;

/**
 * Augments PortStats by keeping track of the rates of packets, bits, and flows.
 * 
 * @author David Underhill
 */
public class PortStatsRates extends PortStats {    
    /** rate of packets per second */
    protected double packetsPerSec = 0;
    
    /** rate of bits per second */
    protected double bitsPerSec = 0;

    /** rate of flows per second */
    protected double flowsPerSec = 0;
    
    /** weight of new rates relative to previous rates */
    protected double weightOfNew;
    
    public PortStatsRates(Match m) {
        this(m, 1.0);
    }
    
    public PortStatsRates(Match m, double weightOfNew) {
        super(m);
        setWeightOfNew(weightOfNew);
    }
    
    /** Gets the number of packets per second for these stats */
    public double getPacketsPerSec() {
        return packetsPerSec;
    }
    
    /** Gets the number of bits per second for these stats */
    public double getBitsPerSec() {
        return bitsPerSec;
    }
    
    /** Gets the number of flows per second for these stats */
    public double getFlowsPerSec() {
        return flowsPerSec;
    }
    
    /** Gets how strongly new rates are weighted over the previous weight */
    public double getWeightOfNew() {
        return this.weightOfNew;
    }
    
    /** 
     * Sets how strongly new rates are weighted over the previous weight.
     * 
     * @param w  will be clamped to the range of 0.0 to 1.0 inclusive
     */
    public void setWeightOfNew(double w) {
        this.weightOfNew = Math.max(0.0, Math.min(1.0, w));
    }
    
    /** update the rates directly */
    public void setRates(double packetsPerSec, double bitsPerSec, double flowsPerSec, long when) {
        double tDiff = when - updateTime;
        long packetCount = (long)(packetsPerSec * tDiff);
        long byteCount = (long)(bitsPerSec * tDiff / 8);
        int flowCount = (int)(flowsPerSec * tDiff);
        super.update(packetCount, byteCount, flowCount, when);
        
        this.packetsPerSec = packetsPerSec;
        this.bitsPerSec = bitsPerSec;
        this.flowsPerSec = flowsPerSec;
    }

    /** update the statistics with the specified values and recompute rates */
    public void update(long packetCount, long byteCount, int flowCount, long when) {
        double pDiff = packetCount - numPackets;
        double bDiff = 8 * (byteCount - numBytes);
        double fDiff = flowCount - numFlows;
        double tDiff = (when - updateTime) / 1000.0; // tDiff is in seconds
        
        if(tDiff > 0) {
            double weightOfOld = 1.0 - weightOfNew;
            packetsPerSec = weightOfNew*(pDiff / tDiff) + weightOfOld*packetsPerSec;
            bitsPerSec    = weightOfNew*(bDiff / tDiff) + weightOfOld*bitsPerSec;
            flowsPerSec   = weightOfNew*(fDiff / tDiff) + weightOfOld*flowsPerSec;
        }
        
        super.update(packetCount, byteCount, flowCount, when);
    }
    
    /** includes the last update time and the current rates */
    public String toString() {
        return new java.util.Date(updateTime).toString() + " =>" + 
            " pps=" + (int)packetsPerSec +
            " bps=" + (int)bitsPerSec +
            " fps=" + (int)flowsPerSec;
    }
}
