package org.openflow.lavi.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Structure to specify a link utilization.
 * 
 * @author David Underhill
 */
public class ETLinkUtil extends Link {
    public static final int SIZEOF = Link.SIZEOF + 4;

    /** utilization of the link */
    public final float util;
    
    public ETLinkUtil(long srcDPID, short srcPort, long dstDPID, short dstPort, float util) {
        super(srcDPID, srcPort, dstDPID, dstPort);
        this.util = util;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeFloat(util);
    }
    
    public String toString() {
        return super.toString() + ":util=" + util;
    }
}
