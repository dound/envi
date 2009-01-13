package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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
    
    public SwitchList(final int len, final LAVIMessageType t, final DataInput in) throws IOException {
        super(t, in);
        
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
            dpids[index++] = in.readLong();
        }
    }
    
    public int length() {
        return super.length() + dpids.length * 8;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        for(long dpid : dpids)
            out.writeLong(dpid);
    }
}
