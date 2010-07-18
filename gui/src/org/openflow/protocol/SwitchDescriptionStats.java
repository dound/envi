package org.openflow.protocol;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;
import org.openflow.gui.net.protocol.StatsHeader;

/**
 * OFGMessage containing information about a switch.  It is the response to
 * OFPST_DESC statistics request.
 * 
 * @author David Underhill
 */
public class SwitchDescriptionStats extends StatsHeader {
    // -1 => leave room for null terminating char
    public static final int MAX_DSEC_STR_LEN = 256;
    public static final int SERIAL_NUM_LEN = 32;
    
    public final String manufacturer, hw_desc, sw_desc, serial_num, desc;
    
    /** 
     * Extract the descriptions from the specified input stream.
     */
    public SwitchDescriptionStats(long dpid, StatsFlag flags, final DataInput in) throws IOException {
        super(StatsHeader.REPLY,
              dpid,
              StatsType.DESC,
              flags);
        
        this.manufacturer = SocketConnection.readString(in, MAX_DSEC_STR_LEN);
        this.hw_desc      = SocketConnection.readString(in, MAX_DSEC_STR_LEN);
        this.sw_desc      = SocketConnection.readString(in, MAX_DSEC_STR_LEN);
        this.serial_num   = SocketConnection.readString(in, SERIAL_NUM_LEN);
        this.desc      = SocketConnection.readString(in, MAX_DSEC_STR_LEN);
    }
    
    public int length() {
        return super.length() + 4*MAX_DSEC_STR_LEN + SERIAL_NUM_LEN;
    }
    
    public String toString() {
        return super.toString() + TSSEP + "manf=" + manufacturer
                                        + " hw=" + hw_desc
                                        + " sw=" + sw_desc
                                        + " sid=" + serial_num
                                        + " desc=" + desc;
    }
}
