package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Structure to specify a link and its capacity.
 * 
 * @author David Underhill
 */
public class LinkSpec extends Link {
    public static final int SIZEOF = Link.SIZEOF + 8;
    
    public final long capacity_bps;
    
    public LinkSpec(DataInput in) throws IOException {
        super(in);
        this.capacity_bps = in.readLong();
    }
    
    public LinkSpec(LinkType linkType, Node srcNode, short srcPort, Node dstNode, short dstPort) {
        super(linkType, srcNode, srcPort, dstNode, dstPort);
        this.capacity_bps = 0;
    }

    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeLong(capacity_bps);
    }
    
    public String toString() {
        return super.toString() + ":" + capacity_bps + "bps";
    }

    public int hashCode() {
        return super.hashCode() + 31*(int)capacity_bps;
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof LinkSpec)) return false;
        LinkSpec l = (LinkSpec)o;
        return super.equals(l) && capacity_bps==l.capacity_bps;
    }
}
