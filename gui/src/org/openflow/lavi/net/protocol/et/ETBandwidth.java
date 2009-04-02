package org.openflow.lavi.net.protocol.et;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.lavi.net.protocol.LAVIMessage;
import org.openflow.lavi.net.protocol.LAVIMessageType;

/**
 * Tells the GUI about the bandwidth active in the system.
 * 
 * @author David Underhill
 *
 */
public class ETBandwidth extends LAVIMessage {
    /** achieved aggregate system bandwidth */
    public final int bandwidth_achieved_mbps;
    
    public ETBandwidth(final int len, final int xid, final DataInput in) throws IOException {
        super(LAVIMessageType.ET_BANDWIDTH, xid);
        if(len != length())
            throw new IOException("ETBandwidth is " + len + "B - expected " + length() + "B");
        
        bandwidth_achieved_mbps = in.readInt();
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + 4;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(bandwidth_achieved_mbps);
    }
    
    public String toString() {
        return super.toString() + TSSEP + bandwidth_achieved_mbps  + " Mbps";
    }
}
