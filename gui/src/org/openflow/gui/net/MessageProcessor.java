package org.openflow.gui.net;

import java.io.DataInput;
import java.io.IOException;

/**
 * Interface for a class which can process protocol messages.
 * 
 * @author David Underhill
 */
public interface MessageProcessor<MSG_TYPE extends Message> {
    /** Process a change in the connection status 
     * @param connected TODO*/
    public void connectionStateChange(boolean connected);
    
    /** 
     * Constructs the object representing the received message.  The message is 
     * known to be of length len and len - 4 bytes representing the rest of the 
     * message should be extracted from buf.
     */
    public MSG_TYPE decode(int len, DataInput in) throws IOException;
    
    /** Process a protocol message */
    public void process(MSG_TYPE msg);
    
}
