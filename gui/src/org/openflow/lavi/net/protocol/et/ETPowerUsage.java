package org.openflow.lavi.net.protocol.et;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.lavi.net.protocol.LAVIMessage;
import org.openflow.lavi.net.protocol.LAVIMessageType;

/**
 * Tells the GUI about the power usage of the system.
 * 
 * @author David Underhill
 *
 */
public class ETPowerUsage extends LAVIMessage {
    /** the number of watts we are currently using */
    public final int watts_current;
    
    /** the number of watts the traditional non-Elastic Tree would use */
    public final int watts_traditional;
    
    /** the number of watts used when the system is fully utilized */
    public final int watts_max;
    
    public ETPowerUsage(final int len, final int xid, final DataInput in) throws IOException {
        super(LAVIMessageType.ET_POWER_USAGE, xid);
        if(len != length())
            throw new IOException("ETBandwidth is " + len + "B - expected " + length() + "B");
        
        this.watts_current = in.readInt();
        this.watts_traditional = in.readInt();
        this.watts_max = in.readInt();
    }
    
    /** This returns the maximum length of ETPowerUsage */
    public int length() {
        return super.length() + 12;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(watts_current);
        out.writeInt(watts_traditional);
        out.writeInt(watts_max);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "cur=" + watts_current  + " trad=" + watts_traditional + " max=" + watts_max;
    }
}
