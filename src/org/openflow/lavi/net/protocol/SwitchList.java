package org.openflow.lavi.net.protocol;

import java.io.IOException;
import org.openflow.lavi.net.util.ByteBuffer;

/**
 * A list of switches.
 * 
 * @author David Underhill
 */
public abstract class SwitchList extends LAVIMessage {
    public final long[] dpids;
    
    public SwitchList(LAVIMessageType t, final long[] dpids) {
        this(t, 0, dpids);
    }
    
    public SwitchList(LAVIMessageType t, int xid, final long[] dpids) {
        super(t, xid);
        this.dpids = dpids;
    }
    
    public SwitchList(final int len, final LAVIMessageType t, final ByteBuffer buf) throws IOException {
        super(t, buf);
        
        // make sure the number of bytes leftover makes sense
        int left = len - super.length();
        if(left % 8 != 0) {
            throw new IOException("Body of switch list is not a multiple of 8 (length of body is " + left + " bytes)");
        }
        
        // read in the DPIDs
        int index = 0;
        dpids = new long[left / 8];
        while(left > 8) {
            left -= 8;
            dpids[index++] = buf.nextLong();
        }
    }
    
    public int length() {
        return super.length() + dpids.length * 8;
    }
}
