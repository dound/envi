package org.openflow.gui.net.protocol.et;

import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;
import org.openflow.gui.net.protocol.Link;

/**
 * Link failed message.
 * 
 * @author David Underhill
 */
public class ETLinkFailureChange extends OFGMessage {
    public final Link link;
    public final boolean failed;
    
    public ETLinkFailureChange(final Link link, boolean failed) {
        super(OFGMessageType.ET_LINK_FAILURES, 0);
        this.link = link;
        this.failed = failed;
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + Link.SIZEOF + 1;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        link.write(out);
        out.writeBoolean(failed);
    }
    
    public String toString() {
        return super.toString() + TSSEP + link.toString() + " => " + (failed ? "failed" : "brought back up");
    }

}
