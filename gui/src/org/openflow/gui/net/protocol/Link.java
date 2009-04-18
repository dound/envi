package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Structure to specify a link.
 * 
 * @author David Underhill
 */
public class Link {
    public static final int SIZEOF = 2 + 2 * (Node.SIZEOF + 2);

    /** type of the link */
    public final LinkType linkType;
    
    /** source node */
    public final Node srcNode;

    /** port number on the source switch */
    public final short srcPort;
    
    /** destination node */
    public final Node dstNode;
    
    /** port number of the link is connected to on the destination switch */
    public final short dstPort;
    
    public Link(DataInput in) throws IOException {
        this(LinkType.typeValToMessageType(in.readShort()), 
             new Node(in), in.readShort(),
             new Node(in), in.readShort());
    }
    
    public Link(LinkType linkType, Node srcNode, short srcPort, Node dstNode, short dstPort) {
        this.linkType = linkType;
        this.srcNode = srcNode;
        this.srcPort = srcPort;
        this.dstNode = dstNode;
        this.dstPort = dstPort;
    }
    
    public void write(DataOutput out) throws IOException {
        out.writeShort(linkType.getTypeID());
        srcNode.write(out);
        out.writeShort(srcPort);
        dstNode.write(out);
        out.writeShort(dstPort);
    }
    
    public String toString() {
        return "Link{" + srcNode + "/" + srcPort  + 
                         " -- " + linkType.toString() + " --> " + 
                         dstNode + "/" + dstPort + "}";
    }
}
