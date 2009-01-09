package org.openflow.lavi.net.protocol;

/**
 * Structure to specify a link.
 * 
 * @author David Underhill
 */
public class Link {
    public static final int SIZEOF = 12;
    
    /** port number on the source switch */
    public final short srcPort;
    
    /** datapath ID of the destination switch */
    public final long dstDPID;
    
    /** port number of the link is connected to on the destination switch */
    public final short dstPort;
    
    public Link(short srcPort, long dstDPID, short dstPort) {
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.dstDPID = dstDPID;
    }
}
