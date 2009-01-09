package org.openflow.lavi.net.protocol;

import java.io.IOException;
import org.openflow.lavi.net.util.ByteBuffer;

/**
 * Switch(es) deleted message.
 * 
 * @author David Underhill
 */
public class LinksDel extends LinksList {
    public LinksDel(final Link[] links) {
        this(0, links);
    }
    
    public LinksDel(int xid, final Link[] links) {
        super(LAVIMessageType.LINKS_DELETE, xid, links);
    }
    
    public LinksDel(final int len, final ByteBuffer buf) throws IOException {
        super(len, LAVIMessageType.LINKS_DELETE, buf);
    }
}
