package org.openflow.lavi.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Tells the GUI about the power usage of the system.
 * 
 * @author David Underhill
 *
 */
public class ETPowerUsage extends LAVIMessage {
    /** the number of volts we are currently using */
    public final int volts_current;
    
    /** the number of volts used when the system is fully utilized */
    public final int volts_max;
    
    public ETPowerUsage(int volts_current, int volts_max) {
        super(LAVIMessageType.ET_POWER_USAGE, 0);
        this.volts_current = volts_current;
        this.volts_max = volts_max;
    }
    
    /** This returns the maximum length of ETPowerUsage */
    public int length() {
        return super.length() + 8;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(volts_current);
        out.writeInt(volts_max);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "cur=" + volts_current + " max=" + volts_max;
    }
}
