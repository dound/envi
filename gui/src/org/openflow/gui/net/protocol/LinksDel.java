package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Link(s) deleted message.
 * 
 * @author David Underhill
 */
public class LinksDel extends LinksList {
    public LinksDel(final Link[] links) {
        this(0, links);
    }
    
    public LinksDel(int xid, final Link[] links) {
        super(OFGMessageType.LINKS_DELETE, xid, links);
    }
    
    public LinksDel(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.LINKS_DELETE, xid, in);
    }
}
