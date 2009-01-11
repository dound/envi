package org.openflow.protocol;

import java.io.DataInput;
import java.io.IOException;
import org.openflow.lavi.net.protocol.StatsHeader;

/**
 * Message containing information about a switch.  It is the response to
 * OFPST_DESC statistics request.
 * 
 * @author David Underhil
 */
public class SwitchDescriptionStats extends StatsHeader {
    // -1 => leave room for null terminating char
    public static final int MAX_DSEC_STR_LEN = 256;
    public static final int SERIAL_NUM_LEN = 32;
    
    public final String manufacturer, hw_desc, sw_desc, serial_num;
    
    /** 
     * Extract the descriptions from the specified input stream.
     */
    public SwitchDescriptionStats(long dpid, StatsFlag flags, final DataInput in) throws IOException {
        super(StatsHeader.REPLY,
              dpid,
              StatsType.DESC,
              flags);
        
        byte strBuf[] = new byte[Math.max(MAX_DSEC_STR_LEN, SERIAL_NUM_LEN)];
        
        for(int i=0; i<MAX_DSEC_STR_LEN; i++)
            strBuf[i] = in.readByte();
        
        this.manufacturer = new String(strBuf);
                
        for(int i=0; i<MAX_DSEC_STR_LEN; i++)
            strBuf[i] = in.readByte();

        this.hw_desc = new String(strBuf);

        for(int i=0; i<MAX_DSEC_STR_LEN; i++)
            strBuf[i] = in.readByte();

        this.sw_desc = new String(strBuf);

        for(int i=0; i<SERIAL_NUM_LEN; i++)
            strBuf[i] = in.readByte();

        this.serial_num = new String(strBuf);
    }
    
    public int length() {
        return super.length() + 3*MAX_DSEC_STR_LEN + SERIAL_NUM_LEN;
    }
}
