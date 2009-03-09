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
    /** the number of watts we are currently using */
    public final int watts_current;
    
    /** the number of watts used when the system is fully utilized */
    public final int watts_max;
    
    public ETPowerUsage(int watts_current, int watts_max) {
        super(LAVIMessageType.ET_POWER_USAGE, 0);
        this.watts_current = watts_current;
        this.watts_max = watts_max;
    }
    
    /** This returns the maximum length of ETPowerUsage */
    public int length() {
        return super.length() + 8;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(watts_current);
        out.writeInt(watts_max);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "cur=" + watts_current + " max=" + watts_max;
    }
}
