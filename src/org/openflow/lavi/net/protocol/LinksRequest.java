package org.openflow.lavi.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Request for links from a specific switch.
 *
 * @author David Underhill
 */
public class LinksRequest extends LAVIMessage {
    /** switch to get the links for */
    public final long srcDPID;
    
    public LinksRequest(long srcDPID) {
        super(LAVIMessageType.LINKS_REQUEST, 0);
        this.srcDPID = srcDPID;
    }
    
    /** This returns the maximum length of LinkSubscribe */
    public int length() {
        return super.length() + 8;
    }
    
    /** Writes the header (via super.write()) and a byte representing the subscription state */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeLong(srcDPID);
    }
}
