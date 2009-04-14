package org.openflow.gui.net.protocol.ex;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Tells the GUI about the power usage of the system.
 * 
 * @author David Underhill
 *
 */
public class EXPowerUsage extends OFGMessage {
    /** the number of watts we are currently using */
    public final int watts_current;
    
    /** the number of amps we are currently pulling */
    public final int amps_current;
    
    
    public EXPowerUsage(final int len, final int xid, final DataInput in) throws IOException {
        super(OFGMessageType.EX_POWER_USAGE, xid);
        if(len != length())
            throw new IOException("got " + len + "B for " + getClass().getName() + " but expected " + length() + "B");
        
        this.watts_current = in.readInt();
        this.amps_current = in.readInt();
    }
    
    /** This returns the maximum length of EXPowerUsage */
    public int length() {
        return super.length() + 8;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(watts_current);
        out.writeInt(amps_current);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "Watts=" + watts_current  + " Amps=" + amps_current;
    }
}
