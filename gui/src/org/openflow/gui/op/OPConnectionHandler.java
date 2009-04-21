package org.openflow.gui.op;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

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
import org.openflow.gui.drawables.OPNodeWithNameAndPorts;
import org.openflow.gui.net.protocol.NodeType;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.op.OPModule;
import org.openflow.gui.net.protocol.op.OPModulesAdd;
import org.openflow.gui.net.protocol.op.OPTestInfo;

public class OPConnectionHandler extends ConnectionHandler
                                 implements DrawableEventListener {

    /** the manager for our single topology */
    private final OPLayoutManager manager;
    
    /** shape which will hold test input */
    private final OPNodeWithNameAndPorts testInput;
    
    /** shape which will hold test output */
    private final OPNodeWithNameAndPorts testOutput;
    
    public static final int TEST_BOX_WIDTH  = 500;
    public static final int TEST_BOX_HEIGHT = 50;
    
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
        
        testInput = new OPNodeWithNameAndPorts(NodeType.UNKNOWN, "", 1111, new ShapeIcon(new Rectangle2D.Double(0, 0, TEST_BOX_WIDTH, TEST_BOX_HEIGHT), LIGHT_YELLOW, Color.BLACK));
        testOutput = new OPNodeWithNameAndPorts(NodeType.UNKNOWN, "", 9999, new ShapeIcon(new Rectangle2D.Double(0, 0, TEST_BOX_WIDTH, TEST_BOX_HEIGHT), Color.WHITE, Color.BLACK));
        
        manager.addDrawable(testInput);
        manager.addDrawable(testOutput);
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
            // TODO: we just got disconnected - remove all modules, test info
            System.exit(-1);
        }
    }
    
    /** 
     * Directly handles ElasticTreeConnectionManager-specific messages received from the  
     * backend and delegates handling of other messages to super.process().
     */
    public void process(final OFGMessage msg) {
        switch(msg.type) {
        case OP_MODULES_ADD:
            for(OPModule m : ((OPModulesAdd)msg).modules)
                super.processDrawableNodeAdd(processNodeAdd(m));
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
    public static final int[] LAPTOP_XS = new int[]{3, 18, 18, 21,  0,  3, 3};
    public static final int[] LAPTOP_YS = new int[]{0,  0, 15, 21, 21, 15, 0};
    public static final Color DARK_GREEN = Color.GREEN.darker();
    public static final Color DARK_BLUE = Color.BLUE.darker();
    public static final Color LIGHT_YELLOW = Color.YELLOW.brighter();
    public static final int MODULE_SIZE = 80;
    
    
    static {
        int INOUT_SCALE = 5;
        for(int i=0; i<INOUT_XS.length; i++) {
            INOUT_XS[i] *= INOUT_SCALE;
            INOUT_YS[i] *= INOUT_SCALE;
        }
        
        int NETFPGA_SCALE = 10;
        for(int i=0; i<NETFPGA_XS.length; i++) {
            NETFPGA_XS[i] *= NETFPGA_SCALE;
            NETFPGA_YS[i] *= NETFPGA_SCALE;
        }
        
        int LAPTOP_SCALE = 5;
        for(int i=0; i<LAPTOP_XS.length; i++) {
            LAPTOP_XS[i] *= LAPTOP_SCALE;
            LAPTOP_YS[i] *= LAPTOP_SCALE;
        }
    }
    
    
    /**
     * Handles OpenPipes-specific nodes and uses the superclass implementation
     * for all other nodes.
     */
    protected Node processNodeAdd(org.openflow.gui.net.protocol.Node n) {
        GeometricIcon gicon;
        Icon icon;
        String name = "";
        
        switch(n.nodeType) {
        
        case TYPE_IN:
            gicon = new GeometricIcon(INOUT_XS, INOUT_YS, LIGHT_YELLOW, Color.BLACK, Constants.STROKE_DEFAULT);
            gicon.setCenter(true);
            icon = gicon;
            name = "In";
            break;
            
        case TYPE_OUT:
            gicon = new GeometricIcon(INOUT_XS, INOUT_YS, Color.WHITE, Color.BLACK, Constants.STROKE_DEFAULT);
            gicon.setCenter(true);
            icon = gicon;
            name = "Out";
            break;
            
        case TYPE_NETFPGA:
            gicon = new GeometricIcon(NETFPGA_XS, NETFPGA_YS, Color.GREEN, Color.BLACK, Constants.STROKE_DEFAULT);
            gicon.setCenter(true);
            icon = gicon;
            break;
            
        case TYPE_LAPTOP:
            gicon = new GeometricIcon(LAPTOP_XS, LAPTOP_YS, Color.BLUE, Color.BLACK, Constants.STROKE_DEFAULT);
            gicon.setCenter(true);
            icon = gicon;
            break;
            
        case TYPE_MODULE_HW:
            icon = new ShapeIcon(new RoundRectangle2D.Double(0, 0, MODULE_SIZE, MODULE_SIZE*4/5, 10, 10), DARK_GREEN, Color.BLACK);
            name = ((OPModule)n).name;
            break;
        
        case TYPE_MODULE_SW:
            icon = new ShapeIcon(new Ellipse2D.Double(0, 0, MODULE_SIZE, MODULE_SIZE), DARK_BLUE, Color.BLACK);
            name = ((OPModule)n).name;
            break;
            
        default:
            return super.processNodeAdd(n);
        }
        
        OPNodeWithNameAndPorts s = new OPNodeWithNameAndPorts(n.nodeType, name, n.id, icon);
        if(n instanceof OPModule)
            s.setNameColor(Color.WHITE);
        return s;
    }

    /** handles displaying test info */
    private void processTestInfo(OPTestInfo msg) {
        testInput.setName(msg.input);
        testOutput.setName(msg.output);
    }
}
