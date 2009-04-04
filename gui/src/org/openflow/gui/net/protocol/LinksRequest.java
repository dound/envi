package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

import org.openflow.util.string.DPIDUtil;

/**
 * Request for links from a specific switch.
 *
 * @author David Underhill
 */
public class LinksRequest extends OFGMessage {
    /** switch to get the links for */
    public final long srcDPID;
    
    public LinksRequest(long srcDPID) {
        super(OFGMessageType.LINKS_REQUEST, 0);
        this.srcDPID = srcDPID;
    }
    
    /** This returns the maximum length of LinkSubscribe */
    public int length() {
        return super.length() + 8;
    }
    
    /** Writes the header (via super.write()) and a long representing the switch to get link info for */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeLong(srcDPID);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "links from " + DPIDUtil.toString(srcDPID);
    }
}
