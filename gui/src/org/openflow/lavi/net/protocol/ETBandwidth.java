package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Tells the GUI about the bandwidth active in the system.
 * 
 * @author David Underhill
 *
 */
public class ETBandwidth extends LAVIMessage {
    /** achieved aggregate system bandwidth */
    public final int bandwidth_achieved_bps;
    
    public ETBandwidth(final int len, final int xid, final DataInput in) throws IOException {
        super(LAVIMessageType.ET_BANDWIDTH, xid);
        if(len != length())
            throw new IOException("ETBandwidth is " + len + "B - expected " + length() + "B");
        
        bandwidth_achieved_bps = in.readInt();
    }
    
    /** This returns the maximum length of ETPowerUsage */
    public int length() {
        return super.length() + 4;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(bandwidth_achieved_bps);
    }
    
    public String toString() {
        return super.toString() + TSSEP + bandwidth_achieved_bps  + " bps";
    }
}
