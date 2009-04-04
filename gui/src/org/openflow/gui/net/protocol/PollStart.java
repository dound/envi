package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * OFGMessage which tells the backend to periodically send a message for us.
 * 
 * @author David Underhill
 */
public class PollStart extends OFGMessage {
    /** 
     * time between copies of this message being sent out (in units of 100ms, 
     * e.g. pollInterval=5 => poll every 500ms) 
     * */
    public final short pollInterval;
    
    /** the message to poll */
    public final OFGMessage msg;
    
    /**
     * Construct a PollStart message.
     * 
     * @param pollInterval  how often (in 100ms units) for the backend to send 
     *                      this message; if 0, then it will be sent only once 
     * @param msg           the message to send
     */
    public PollStart(short pollInterval, OFGMessage msg) {
        super(OFGMessageType.POLL_START, 0);
        this.pollInterval = pollInterval;
        this.msg = msg;
    }
    
    /** This returns the maximum length of LinkSubscribe */
    public int length() {
        return super.length() + 2 + msg.length();
    }
    
    /** Writes the header (via super.write()), the poll interval, and poll message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeShort(pollInterval);
        msg.write(out);
    }
    
    public String toString() {
        return super.toString() + TSSEP +  
                "Start Polling every " + (pollInterval*100) + "ms: " + msg;
    }
}
