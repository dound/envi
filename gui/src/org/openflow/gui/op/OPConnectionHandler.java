package org.openflow.gui.op;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.pzgui.Constants;
import org.pzgui.Drawable;
import org.pzgui.DrawableEventListener;
import org.pzgui.DrawableFilter;
import org.pzgui.icon.GeometricIcon;
import org.pzgui.icon.Icon;
import org.pzgui.icon.ShapeIcon;

import org.openflow.gui.ConnectionHandler;
import org.openflow.gui.Topology;
import org.openflow.gui.op.OPLayoutManager;
import org.openflow.gui.drawables.Node;
import org.openflow.gui.drawables.OPModule;
import org.openflow.gui.drawables.OPNodeWithNameAndPorts;
import org.openflow.gui.net.protocol.NodeType;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.op.OPModuleStatusReply;
import org.openflow.gui.net.protocol.op.OPModuleStatusRequest;
import org.openflow.gui.net.protocol.op.OPModulesAdd;
import org.openflow.gui.net.protocol.op.OPMoveModule;
import org.openflow.gui.net.protocol.op.OPTestInfo;
import org.openflow.util.Pair;

public class OPConnectionHandler extends ConnectionHandler
                                 implements DrawableEventListener {

    /** the manager for our single topology */
    private final OPLayoutManager manager;
    
    /** shape which will hold test input */
    private final OPNodeWithNameAndPorts testInput;
    
    /** shape which will hold test output */
    private final OPNodeWithNameAndPorts testOutput;
    
    /** 
     * Configuration of the network.  This map tracks which nodes have which 
     * modules (maps type-ID pairs to node-list_of_modules pairs) 
     */
    private HashMap<Pair<NodeType, Long>, Pair<OPNodeWithNameAndPorts, ArrayList<OPModule>>> config
        = new HashMap<Pair<NodeType, Long>, Pair<OPNodeWithNameAndPorts, ArrayList<OPModule>>> ();
    
    /** get the key for node n in the config data structure */
    private Pair<NodeType, Long> key(OPNodeWithNameAndPorts n) {
        return new Pair<NodeType, Long>(n.getType(), n.getID());
    }
    
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
    
    public void drawableEvent(Drawable d, AWTEvent e, String event) {
        if(event.equals("mouse_released")) {
            MouseEvent me = (MouseEvent)e;
            if(d instanceof OPModule) {
                OPModule m = (OPModule)d;
                m.dragDone();
                
                if(me.getButton() == MouseEvent.BUTTON1)
                    handleModulePlaced(m, me);
                else
                    handleModuleStatusRequested(m);
            }
                
        }
    }
    
    /** define a filter which accepts anything but OPModule objects */
    private static final DrawableFilter filterIgnoreModules = new DrawableFilter() {
        public boolean consider(Drawable d) {
            return !(d instanceof OPModule);
        }
    };
    
    /** handles the dragging (i.e., install, move, or uninstall) of a module */
    private void handleModulePlaced(OPModule m, MouseEvent e) {
        Drawable d = manager.selectFrom(e.getX(), e.getY(), filterIgnoreModules);
        if(d instanceof OPNodeWithNameAndPorts) {
            // dragged m to a new place: install request
            OPNodeWithNameAndPorts n = (OPNodeWithNameAndPorts)d;
            if(m.isCompatibleWith(n)) {
                // duplicate if m is an original
                if(m.isOriginal()) {
                    m = new OPModule(m);
                    manager.addDrawable(m);
                }
                
                moveModule(m, n);
                config.get(key(n)).b.add(m);
                m.setPos(e.getX(), e.getY());
                return;
            }
        }
        
        // dragged to empty space or an incompatible node
        if(m.getNodeInstalledOn() != null && !m.isOriginal()) {
            
            // dragged m to nowhere: uninstall request
            moveModule(m, null);
            manager.removeDrawable(m);
        }
    }
    
    /** moves a module m to the specified node */
    private void moveModule(OPModule m, OPNodeWithNameAndPorts to) {
        OPNodeWithNameAndPorts from = m.getNodeInstalledOn();
        if(from != null)
            config.get(key(from)).b.remove(m);
        
        try {
            OPMoveModule mvMsg = new OPMoveModule(m, from, to);
            getConnection().sendMessage(mvMsg);
            m.installOnNode(to);
        }
        catch(IOException e) {
            System.err.println("Error: failed to " + (to==null ? "un" : "") + "install module due to network error: " + e.getMessage());
        }
    }
    
    /** handles requesting the status of the specified module */
    private void handleModuleStatusRequested(OPModule m) {
        // ignore the request if the module is not installed 
        OPNodeWithNameAndPorts n = m.getNodeInstalledOn();
        if(n == null)
            return;
        
        // send a request for the module's status
        org.openflow.gui.net.protocol.Node msgN = new org.openflow.gui.net.protocol.Node(n.getType(), n.getID());
        org.openflow.gui.net.protocol.Node msgM = new org.openflow.gui.net.protocol.Node(m.getType(), m.getID());
        OPModuleStatusRequest req = new OPModuleStatusRequest(msgN, msgM);
        try {
            this.getConnection().sendMessage(req);
        } catch (IOException ex) {
            System.err.println("Error: unable to send status request: " + req);
        }
    }
    
    /** 
     * Calls super.connectionStateChange() and then does some custom processing.
     */
    public void connectionStateChange() {
        super.connectionStateChange();
        
        if(!getConnection().isConnected())
            System.exit(-1);
    }
    
    /** 
     * Directly handles ElasticTreeConnectionManager-specific messages received from the  
     * backend and delegates handling of other messages to super.process().
     */
    public void process(final OFGMessage msg) {
        switch(msg.type) {
        case OP_MODULES_ADD:
            for(org.openflow.gui.net.protocol.op.OPModule m : ((OPModulesAdd)msg).modules)
                super.processDrawableNodeAdd(processNodeAdd(m));
            break;
        
        case OP_MODULE_STATUS_REPLY:
            processModuleStatusReply((OPModuleStatusReply)msg);
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
            name = ((org.openflow.gui.net.protocol.op.OPModule)n).name;
            return new OPModule(true, name, n.id, icon);
        
        case TYPE_MODULE_SW:
            icon = new ShapeIcon(new Ellipse2D.Double(0, 0, MODULE_SIZE, MODULE_SIZE), DARK_BLUE, Color.BLACK);
            name = ((org.openflow.gui.net.protocol.op.OPModule)n).name;
            return new OPModule(false, name, n.id, icon);
            
        default:
            return super.processNodeAdd(n);
        }
        
        OPNodeWithNameAndPorts ret = new OPNodeWithNameAndPorts(n.nodeType, name, n.id, icon);
        if(ret.getType() == NodeType.TYPE_NETFPGA || ret.getType() == NodeType.TYPE_LAPTOP) {
            config.put(key(ret), new Pair<OPNodeWithNameAndPorts, ArrayList<OPModule>>(ret, new ArrayList<OPModule>()));
        }
        return ret;
    }

    private void processModuleStatusReply(OPModuleStatusReply msg) {
        Pair<NodeType, Long> key = new Pair<NodeType, Long>(msg.node.nodeType, msg.node.id);
        Pair<OPNodeWithNameAndPorts, ArrayList<OPModule>> value = config.get(key);
        if(value == null) {
            System.err.println("Got module status for a module on an unknown node: " + msg);
            return;
        }
        else {
            for(OPModule m : value.b) {
                if(msg.module.id==m.getID() && msg.module.nodeType==m.getType()) {
                    m.setStatus(msg.status + " (" + new java.util.Date().toString() + ")");
                    manager.displayIcon(msg.status, 5000, 18, m.getX(), m.getY());
                    return;
                }
            }
        }
        
        System.err.println("Got module status for an unknown module on node " + value.a + ": " + msg);
    }
    
    /** handles displaying test info */
    private void processTestInfo(OPTestInfo msg) {
        testInput.setName(msg.input);
        testOutput.setName(msg.output);
    }
}