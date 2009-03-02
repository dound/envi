package org.openflow.lavi.net;

import org.openflow.lavi.net.protocol.LAVIMessage;

/**
 * Interface for a class which can process LAVI protocol messages.
 * 
 * @author David Underhill
 */
public interface LAVIMessageProcessor {
    /** Process a change in the connection status */
    public void connectionStateChange();
    
    /** Process a LAVI protocol message */
    public void process(LAVIMessage msg);
}
