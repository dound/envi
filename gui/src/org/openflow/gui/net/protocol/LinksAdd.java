package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Link(s) added message.
 * 
 * @author David Underhill
 */
public class LinksAdd extends LinkSpecsList {
    public LinksAdd(final LinkSpec[] links) {
        this(0, links);
    }
    
    public LinksAdd(int xid, final LinkSpec[] links) {
        super(OFGMessageType.LINKS_ADD, xid, links);
    }
    
    public LinksAdd(final int len, final int xid, final DataInput in) throws IOException {
        super(len, OFGMessageType.LINKS_ADD, xid, in);
    }
}
