package org.openflow.gui.ex;

import org.openflow.gui.ConnectionHandler;
import org.openflow.gui.Topology;
import org.openflow.gui.ex.EXLayoutManager;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.ex.EXPowerUsage;
import org.pzgui.Drawable;
import org.pzgui.DrawableEventListener;

public class EXConnectionHandler extends ConnectionHandler
                                 implements DrawableEventListener {

    /** the manager for our single topology */
    private final EXLayoutManager manager;
    
    /**
     * Construct the front-end for EXConnectionHandler.
     * 
     * @param manager the manager responsible for drawing the GUI
     * @param server  the IP or hostname where the back-end is located
     * @param port    the port the back-end is listening on
     */
    public EXConnectionHandler(EXLayoutManager manager, String server, Short port) {
        super(new Topology(manager), server, port, false, false);
        
        // We keep a reference to the manager doing the drawing because we may 
        // receive custom messages which require communication with it.  This 
        // is most useful when only a single topology is being drawn; something
        // more complicated will be needed when multiple topologies are being
        // drawn.
        this.manager = manager;
        
        // Tell the manager we'd like to know about events like clicking on an
        // object - useful because this manager is able to send messages over
        // the connection to the backend.
        manager.addDrawableEventListener(this);
    }
    
    public void drawableEvent(Drawable d, String event) {
        if(event.equals("mouse_released")) {
            // TODO: handle a mouse click on a Drawable
        }
    }
    
    /** 
     * Calls super.connectionStateChange() and then does some custom processing.
     */
    public void connectionStateChange() {
        super.connectionStateChange();
        
        if(getConnection().isConnected()) {
            // TODO: we just got connected - maybe send a msg to the backend
        }
        else {
            // TODO: we just got disconnected - maybe need to do some cleanup
        }
    }
    
    /** 
     * Directly handles ElasticTreeConnectionManager-specific messages received from the  
     * backend and delegates handling of other messages to super.process().
     */
    public void process(final OFGMessage msg) {
        switch(msg.type) {
        case SWITCHES_ADD:
            // TODO: could do some custom processing on a base message before
            // having the base implementation do its thing
            super.process(msg);
            break;
            
        case EX_POWER_USAGE:
            processPowerUsage((EXPowerUsage)msg);
            break;
            
        // If there are messages which should only be sent FROM the GUI and 
        // never TO the GUI, the cases for those messages should simply raise an
        // exception like this:
        // System.err.println("Received unexpected message type: " + msg.type.toString());
            
        default:
            super.process(msg);
        }
    }
    
    private void processPowerUsage(EXPowerUsage msg) {
        // TODO: do something with the received message
    }
}
