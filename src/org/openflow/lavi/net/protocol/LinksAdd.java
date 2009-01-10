package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Switch(es) added message.
 * 
 * @author David Underhill
 */
public class LinksAdd extends LinksList {
    public LinksAdd(final Link[] links) {
        this(0, links);
    }
    
    public LinksAdd(int xid, final Link[] links) {
        super(LAVIMessageType.LINKS_ADD, xid, links);
    }
    
    public LinksAdd(final int len, final DataInput in) throws IOException {
        super(len, LAVIMessageType.LINKS_ADD, in);
    }
}
