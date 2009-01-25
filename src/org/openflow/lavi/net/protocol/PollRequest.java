package org.openflow.lavi.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Message which sets up or stops the polling of a particular message.
 * 
 * @author David Underhill
 */
public class PollRequest extends LAVIMessage {
    /** 
     * time between copies of this message being sent out (in units of 100ms, 
     * e.g. pollInterval=5 => poll every 500ms) 
     * */
    public final short pollInterval;
    
    /** the message to poll */
    public final LAVIMessage msg;
    
    public PollRequest(short pollInterval, LAVIMessage msg) {
        super(LAVIMessageType.POLL_REQUEST, 0);
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
        return super.toString() + TSSEP + (pollInterval==0 ? "Stop Polling" 
                : "Start Polling every " + (pollInterval*100) + "ms: " + msg);
    }
}
