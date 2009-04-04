package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Request to (un)subscribe from switch additions or removals.
 *
 * @author David Underhill
 */
public class SwitchesSubscribe extends OFGMessage {
    /** what the state of the subscription should be set to */
    public final boolean subscribe;
    
    public SwitchesSubscribe(boolean newSubscriptionState) {
        super(OFGMessageType.SWITCHES_SUBSCRIBE, 0);
        subscribe = newSubscriptionState;
    }
    
    /** This returns the maximum length of SwitchSubscribe */
    public int length() {
        return super.length() + 1;
    }
    
    /** Writes the header (via super.write()) and a byte representing the subscription state */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeBoolean(subscribe);
    }
    
    public String toString() {
        return super.toString() + TSSEP + (subscribe ? "subscribe" : "unsubscribe");
    }
}
