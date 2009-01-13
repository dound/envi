package org.openflow.lavi.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Structure to specify a link.
 * 
 * @author David Underhill
 */
public class Link {
    public static final int SIZEOF = 20;

    /** datapath ID of the source switch */
    public final long srcDPID;

    /** port number on the source switch */
    public final short srcPort;
    
    /** datapath ID of the destination switch */
    public final long dstDPID;
    
    /** port number of the link is connected to on the destination switch */
    public final short dstPort;
    
    public Link(long srcDPID, short srcPort, long dstDPID, short dstPort) {
        this.srcDPID = srcDPID;
        this.srcPort = srcPort;
        this.dstDPID = dstDPID;
        this.dstPort = dstPort;
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeLong(srcDPID);
        out.writeShort(srcPort);
        out.writeLong(dstDPID);
        out.writeShort(dstPort);
    }
}
