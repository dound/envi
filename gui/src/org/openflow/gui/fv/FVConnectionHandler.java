package org.openflow.gui.fv;

import org.openflow.gui.ConnectionHandler;
import org.openflow.gui.Topology;

/**
 * The FlowVisor GUI individual connection handler.  Just a thin wrapper at the
 * moment.
 * 
 * @author David Underhill
 */
public class FVConnectionHandler extends ConnectionHandler {
    /**
     * Construct the front-end for EXConnectionHandler.
     * 
     * @param manager the manager responsible for drawing the GUI
     * @param server  the IP or hostname where the back-end is located
     * @param port    the port the back-end is listening on
     */
    public FVConnectionHandler(FVLayoutManager manager, String server, Short port) {
        super(new Topology(manager), server, port, true, true);
    }
    
    /** 
     * Calls super.connectionStateChange() and then does some custom processing.
     */
    public void connectionStateChange() {
        super.connectionStateChange();
        
        if(!getConnection().isConnected()) {
            // TODO: we just got disconnected - maybe need to do some cleanup
        }
    }
}
