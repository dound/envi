package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.IOException;

/**
 * Link(s) failed message.
 * 
 * @author David Underhill
 */
public class ETLinkFailures extends LinksList {    
    public ETLinkFailures(final Link[] links) {
        this(0, links);
    }
    
    public ETLinkFailures(int xid, final Link[] links) {
        super(LAVIMessageType.ET_LINK_FAILURES, xid, links);
    }
    
    public ETLinkFailures(final int len, final int xid, final DataInput in) throws IOException {
        super(len, LAVIMessageType.ET_LINK_FAILURES, xid, in);
    }
}
