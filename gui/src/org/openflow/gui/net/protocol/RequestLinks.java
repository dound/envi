package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * A request regarding links.
 *
 * @author David Underhill
 */
public class RequestLinks extends Request {
    public static final Node ANY_NODE = new Node(NodeType.typeValToMessageType(Request.ANY_TYPE), 0);
    
    /** which node the links should be from, if any */
    public final Node srcNode;
    
    public RequestLinks(RequestType requestType) {
        this(requestType, ANY_TYPE, ANY_NODE);
    }

    public RequestLinks(RequestType requestType, Node srcNode) {
        this(requestType, ANY_TYPE, srcNode);
    }
    
    public RequestLinks(RequestType requestType, short type, Node srcNode) {
        super(OFGMessageType.LINKS_REQUEST, requestType, type);
        this.srcNode = srcNode;
    }
    
    /** This returns the message length */
    public int length() {
        return super.length() + Node.SIZEOF;
    }
    
    /** Writes the header (via super.write()) and a byte representing the subscription state */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        srcNode.write(out);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "links" + (ANY_NODE.equals(srcNode) ? "" : " from " + srcNode);
    }
}
