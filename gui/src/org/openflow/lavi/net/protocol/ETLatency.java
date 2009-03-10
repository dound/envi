package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Tells the GUI about the latency of each layer in the system.
 * 
 * @author David Underhill
 *
 */
public class ETLatency extends LAVIMessage {
    /** latency via just the edge layer */
    public final int latency_ms_edge;
    
    /** latency up through the aggregation layer */
    public final int latency_ms_agg;
    
    /** latency up through the core layer */
    public final int latency_ms_core;
    
    public ETLatency(final int len, final int xid, final DataInput in) throws IOException {
        super(LAVIMessageType.ET_LATENCY, xid);
        if(len != length())
            throw new IOException("ETBandwidth is " + len + "B - expected " + length() + "B");
        
        this.latency_ms_edge = in.readInt();
        this.latency_ms_agg = in.readInt();
        this.latency_ms_core = in.readInt();
    }
    
    /** This returns the maximum length of ETPowerUsage */
    public int length() {
        return super.length() + 12;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(latency_ms_edge);
        out.writeInt(latency_ms_agg);
        out.writeInt(latency_ms_core);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "edge_ms=" + latency_ms_edge  + " agg_ms=" + latency_ms_agg + " core_ms=" + latency_ms_core;
    }
}
