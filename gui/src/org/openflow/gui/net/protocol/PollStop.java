package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Tells the backend to stop polling for a given message.
 * 
 * @author David Underhill
 *
 */
public class PollStop extends OFGMessage {
    /** the message transaction ID to stop polling */
    public final int xid_to_stop_polling;
    
    public PollStop(int xid_to_stop_polling) {
        super(OFGMessageType.POLL_STOP, 0);
        this.xid_to_stop_polling = xid_to_stop_polling;
    }
    
    /** This returns the maximum length of PollStop */
    public int length() {
        return super.length() + 4;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(xid_to_stop_polling);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "stop polling xid=" + xid_to_stop_polling;
    }
}
