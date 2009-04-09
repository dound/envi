package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Link(s) added message.
 * 
 * @author David Underhill
 */
public class LinksAdd extends LinksList {
    public LinksAdd(final Link[] links) {
        this(0, links);
    }
    
    public LinksAdd(int xid, final Link[] links) {
        super(OFGMessageType.LINKS_ADD, xid, links);
    }
    
    public LinksAdd(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.LINKS_ADD, xid, in);
    }
}
