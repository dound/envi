package org.openflow.lavi.net.protocol.et;

import java.io.DataOutput;
import java.io.IOException;

import org.openflow.lavi.net.protocol.LAVIMessage;
import org.openflow.lavi.net.protocol.LAVIMessageType;

/**
 * Request for switches for a particular k value.
 *
 * @author David Underhill
 */
public class ETSwitchesRequest extends LAVIMessage {
    /** k to get the nodes for */
    public final int k;
    
    public ETSwitchesRequest(int k) {
        super(LAVIMessageType.ET_SWITCHES_REQUEST, 0);
        this.k = k;
    }
    
    /** This returns the maximum length of LinkSubscribe */
    public int length() {
        return super.length() + 4;
    }
    
    /** Writes the header (via super.write()) and a long representing the switch to get link info for */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(k);
    }
    
    public String toString() {
        return super.toString() + TSSEP + " k=" + k;
    }
}
