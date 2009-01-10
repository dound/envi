package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.IOException;

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
    
    public LinksDel(final int len, final DataInput in) throws IOException {
        super(len, LAVIMessageType.LINKS_DELETE, in);
    }
}
