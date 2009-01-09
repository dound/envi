package org.openflow.lavi.net.protocol;

import java.io.IOException;
import org.openflow.lavi.net.util.ByteBuffer;

/**
 * Switch(es) deleted message.
 * 
 * @author David Underhill
 */
public class SwitchesDel extends SwitchList {
    public SwitchesDel(final long[] dpids) {
        this(0, dpids);
    }
    
    public SwitchesDel(int xid, final long[] dpids) {
        super(LAVIMessageType.SWITCHES_DELETE, xid, dpids);
    }
    
    public SwitchesDel(final int len, final ByteBuffer buf) throws IOException {
        super(len, LAVIMessageType.SWITCHES_DELETE, buf);
    }
}
