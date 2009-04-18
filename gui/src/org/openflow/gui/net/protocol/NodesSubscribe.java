package org.openflow.gui.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Request to (un)subscribe from switch additions or removals.
 *
 * @author David Underhill
 */
public class NodesSubscribe extends OFGMessage {
    public static final short SUBSCRIBE_ALL_NODES = 0;
    
    /** the type to subscribe to */
    public final short type;
    
    /** what the state of the subscription should be set to */
    public final boolean subscribe;
    
    public NodesSubscribe(boolean newSubscriptionState) {
        this(newSubscriptionState, SUBSCRIBE_ALL_NODES);
    }
    
    public NodesSubscribe(boolean newSubscriptionState, short type) {
        super(OFGMessageType.NODES_SUBSCRIBE, 0);
        subscribe = newSubscriptionState;
        this.type = type;
    }
    
    /** This returns the maximum length of SwitchSubscribe */
    public int length() {
        return super.length() + 3;
    }
    
    /** Writes the header (via super.write()) and a byte representing the subscription state */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeShort(type);
        out.writeBoolean(subscribe);
    }
    
    public String toString() {
        String strType;
        if(type == SUBSCRIBE_ALL_NODES)
            strType = "all types";
        else
            strType = "type " + NodeType.typeValToMessageType(type);
        
        String how = subscribe ? "subscribe" : "unsubscribe";
        return super.toString() + TSSEP + how + " to nodes of " + strType;
    }
}
