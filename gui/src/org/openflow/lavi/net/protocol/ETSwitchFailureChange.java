package org.openflow.lavi.net.protocol;

import java.io.DataOutput;
import java.io.IOException;
import org.openflow.util.string.DPIDUtil;

/**
 * Switch failed message.
 * 
 * @author David Underhill
 */
public class ETSwitchFailureChange extends LAVIMessage {
    public final long dpid;
    public final boolean failed;
    
    public ETSwitchFailureChange(final long dpid, boolean failed) {
        super(LAVIMessageType.ET_SWITCH_FAILURES, 0);
        this.dpid = dpid;
        this.failed = failed;
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + 8 + 1;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeLong(dpid);
        out.writeBoolean(failed);
    }
    
    public String toString() {
        return super.toString() + TSSEP + DPIDUtil.toShortString(dpid) + " => " + (failed ? "failed" : "brought back up");
    }
}
