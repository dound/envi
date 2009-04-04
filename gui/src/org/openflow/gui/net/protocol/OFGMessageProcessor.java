package org.openflow.gui.net.protocol;


/**
 * Interface for a class which can process OpenFlow GUI protocol messages.
 * 
 * @author David Underhill
 */
public interface OFGMessageProcessor {
    /** Process a change in the connection status */
    public void connectionStateChange();
    
    /** Process an OpenFlow GUI protocol message */
    public void process(OFGMessage msg);
}
