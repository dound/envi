package org.openflow.gui.net.protocol.et;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Tells the GUI about the latency of each layer in the system.
 * 
 * @author David Underhill
 *
 */
public class ETLatency extends OFGMessage {
    /** latency over all paths */
    public final int latency;
    
    public ETLatency(final int len, final int xid, final DataInput in) throws IOException {
        super(OFGMessageType.ET_LATENCY, xid);
        if(len != length())
            throw new IOException("ETBandwidth is " + len + "B - expected " + length() + "B");
        
        this.latency = in.readInt();
    }
    
    /** This returns the maximum length of ETPowerUsage */
    public int length() {
        return super.length() + 4;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(latency);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "=" + latency;
    }
}
