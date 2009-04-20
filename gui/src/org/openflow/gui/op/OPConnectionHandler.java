package org.openflow.gui.op;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.pzgui.Constants;
import org.pzgui.Drawable;
import org.pzgui.DrawableEventListener;
import org.pzgui.icon.GeometricIcon;
import org.pzgui.icon.Icon;
import org.pzgui.icon.ShapeIcon;

import org.openflow.gui.ConnectionHandler;
import org.openflow.gui.Topology;
import org.openflow.gui.op.OPLayoutManager;
import org.openflow.gui.drawables.Node;
import org.openflow.gui.drawables.SimpleNode;
import org.openflow.gui.drawables.SimpleNodeWithPorts;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.op.OPModule;
import org.openflow.gui.net.protocol.op.OPModulesAdd;
import org.openflow.gui.net.protocol.op.OPTestInfo;

public class OPConnectionHandler extends ConnectionHandler
                                 implements DrawableEventListener {

    /** the manager for our single topology */
    private final OPLayoutManager manager;
    
    /**
     * Construct the front-end for EXConnectionHandler.
     * 
     * @param manager the manager responsible for drawing the GUI
     * @param server  the IP or hostname where the back-end is located
     * @param port    the port the back-end is listening on
     */
    public OPConnectionHandler(OPLayoutManager manager, String server, Short port) {
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
        
        case OP_MODULES_ADD:
            processModulesAdd((OPModulesAdd)msg);
            break;
        
        case OP_TEST_INFO:
            processTestInfo((OPTestInfo)msg);
            break;
        
        default:
            super.process(msg);
        }
    }
    
    // drawing related info for our OpenPipes-specific drawings
    public static final int[] INOUT_XS = new int[] {5, 10,  5, 0, 5};
    public static final int[] INOUT_YS = new int[] {0,  5, 10, 5, 0};
    public static final int[] NETFPGA_XS = new int[]{0, 20, 20, 10, 10,  2, 2, 0, 0};
    public static final int[] NETFPGA_YS = new int[]{0,  0,  8,  8, 10, 10, 8, 8, 0};
    public static final int[] LAPTOP_XS = new int[]{2, 17, 17, 19,  0,  2, 2};
    public static final int[] LAPTOP_YS = new int[]{0,  0, 15, 17, 17, 15, 0};
    public static final Color DARK_GREEN = Color.GREEN.darker();
    public static final Color DARK_BLUE = Color.BLUE.darker();
    
    /**
     * Handles OpenPipes-specific nodes and uses the superclass implementation
     * for all other nodes.
     */
    protected Node processNodeAdd(org.openflow.gui.net.protocol.Node n) {
        Icon icon;
        switch(n.nodeType) {
        
        case TYPE_IN:
        case TYPE_OUT:
            icon = new GeometricIcon(INOUT_XS, INOUT_YS, Color.WHITE, Color.BLACK, Constants.STROKE_DEFAULT);
            break;
            
        case TYPE_NETFPGA:
            icon = new GeometricIcon(NETFPGA_XS, NETFPGA_YS, Color.GREEN, Color.BLACK, Constants.STROKE_DEFAULT);
            break;
            
        case TYPE_LAPTOP:
            icon = new GeometricIcon(LAPTOP_XS, LAPTOP_YS, Color.BLUE, Color.BLACK, Constants.STROKE_DEFAULT);
            break;
            
        case TYPE_MODULE_HW:
        case TYPE_MODULE_SW:
            throw new Error("Error: received module in nodes list: " + n.nodeType.toString());
            
        default:
            return super.processNodeAdd(n);
        }
        
        return new SimpleNodeWithPorts(n.nodeType, n.id, icon);
    }

    private void processModulesAdd(OPModulesAdd msg) {
        for(OPModule m : msg.modules) {
            Icon icon;
            switch(m.nodeType) {
                case TYPE_MODULE_HW:
                    icon = new ShapeIcon(new Rectangle2D.Double(0, 0, 30, 30), DARK_GREEN);
                    break;
                
                case TYPE_MODULE_SW:
                    icon = new ShapeIcon(new Ellipse2D.Double(0, 0, 30, 30), DARK_BLUE);
                    break;
                    
                default:
                    throw new Error("Error: received non-module in modules list: " + m.nodeType.toString());
            }
            
            SimpleNode n = new SimpleNode(m.nodeType, m.id, icon);
            n.setName(m.name);
            manager.addDrawable(n);
        }
    }

    /** handles displaying test info */
    private void processTestInfo(OPTestInfo msg) {
        // TODO: not yet implemented
    }
}
