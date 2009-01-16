package org.openflow.lavi.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Request to (un)subscribe from link additions or removals.
 *
 * @author David Underhill
 */
public class LinkSubscribe extends LAVIMessage {
    /** what the state of the subscription should be set to */
    public final boolean subscribe;
    
    public LinkSubscribe(boolean newSubscriptionState) {
        super(LAVIMessageType.LINKS_SUBSCRIBE, 0);
        subscribe = newSubscriptionState;
    }
    
    /** This returns the maximum length of LinkSubscribe */
    public int length() {
        return super.length() + 1;
    }
    
    /** Writes the header (via super.write()) and a byte representing the subscription state */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeBoolean(subscribe);
    }
}
