package org.openflow.gui.op;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

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
import org.openflow.gui.drawables.LayoutableIcon;
import org.openflow.gui.drawables.Link;
import org.openflow.gui.drawables.Node;
import org.openflow.gui.drawables.OPModule;
import org.openflow.gui.drawables.OPModulePort;
import org.openflow.gui.drawables.OPModuleReg;
import org.openflow.gui.drawables.OPNodeWithNameAndPorts;
import org.openflow.gui.net.protocol.LinkType;
import org.openflow.gui.net.protocol.LinksAdd;
import org.openflow.gui.net.protocol.LinksDel;
import org.openflow.gui.net.protocol.NodeType;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.op.OPModuleStatusReply;
import org.openflow.gui.net.protocol.op.OPModuleStatusRequest;
import org.openflow.gui.net.protocol.op.OPModulesAdd;
import org.openflow.gui.net.protocol.op.OPMoveModule;
import org.openflow.gui.net.protocol.op.OPNodesAdd;
import org.openflow.gui.net.protocol.op.OPNodesDel;
import org.openflow.gui.net.protocol.op.OPTestInfo;
import org.openflow.util.Pair;

public class OPConnectionHandler extends ConnectionHandler
                                 implements DrawableEventListener {
    /** whether multiple modules can be installed on a single node */
    public static final boolean ALLOW_MULTIPLE_MODULES_PER_NODE = false;
    
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
        
        // a background for the pallete
        manager.addDrawable(new LayoutableIcon(5555, new ShapeIcon(new Rectangle2D.Double(0, 0, TEST_BOX_WIDTH, 1500), new Color(230,230,230), Color.BLACK), 0, 0));
        
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
                else {
                    if(OPWindowEventListener.isModuleStatusMode())
                        handleModuleStatusRequested(m);
                    else if(OPWindowEventListener.isLinkAddMode())
                        handleLinkChange(m, true);
                    else
                        handleLinkChange(m, false);
                }
            }
            else if(me.getButton() != MouseEvent.BUTTON1) {
                if(OPWindowEventListener.isLinkAddMode())
                    handleLinkChange(d, true);
                else
                    handleLinkChange(d, false);
            }
        }
        else if(event.equals(OPWindowEventListener.MODE_CHANGED_EVENT)) {
            // clear any partial work done in a different mode
            clearLinkEndpoint();
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
            // ignore the request if m is already installed on d
            // but reset the X/Y position
            if(m.getNodeInstalledOn() == d) {
                OPNodeWithNameAndPorts to = (OPNodeWithNameAndPorts)d;
                m.setPos(to.getX(), to.getY());
                return;
            }
            
            // dragged m to a new place: install request
            OPNodeWithNameAndPorts n = (OPNodeWithNameAndPorts)d;
            if(m.isCompatibleWith(n) && (ALLOW_MULTIPLE_MODULES_PER_NODE || config.get(key(n)).b.size()==0)) {
                
                // only need to send a message if the module is moving to a 
                // different node
                if(!n.equals(m.getNodeInstalledOn())) {
                    moveModule(m, n);
                }
                return;
            }
        }
        
        // dragged to empty space or an incompatible node
        if(m.getNodeInstalledOn() != null && !m.isOriginal()) {
            // dragged m to nowhere: uninstall request
            moveModule(m, null);
        }
    }

    /** moves a module m to the specified node */
    private void moveModule(OPModule m, OPNodeWithNameAndPorts to) {
        OPNodeWithNameAndPorts from = m.getNodeInstalledOn();

        try {
            OPMoveModule mvMsg = new OPMoveModule(m, from, to);
            getConnection().sendMessage(mvMsg);
        }
        catch(IOException e) {
            System.err.println("Error: failed to " + (to==null ? "un" : "") + "install module due to network error: " + e.getMessage());
        }
    }

    /** moves a module m to the specified node */
    private void moveModule(OPMoveModule msg) {
        // Attempt to get the various nodes
        long moduleID = msg.module.id;
        long fromID = msg.from.id;
        long toID = msg.to.id;

        // Basic sanity checking
        if (msg.module.equals(OPMoveModule.NODE_NONE)) {
            System.err.println("Attempt made to move nothing");
            return;
        }

        if (msg.from.equals(OPMoveModule.NODE_NONE) && msg.to.equals(OPMoveModule.NODE_NONE)) {
            System.err.println("No source or destination specified when moving a module");
            return;
        }

        OPNodeWithNameAndPorts module = (OPNodeWithNameAndPorts)getTopology().getNode(moduleID);
        OPNodeWithNameAndPorts from = (OPNodeWithNameAndPorts)getTopology().getNode(fromID);
        OPNodeWithNameAndPorts to = (OPNodeWithNameAndPorts)getTopology().getNode(toID);

        // Create a copy of the module if the source node is none
        // (it means that it's being added to the topology)
        if (msg.from.equals(OPMoveModule.NODE_NONE)) {
            long tmpModuleID = org.openflow.gui.net.protocol.op.OPModule.extractModuleID(moduleID);
            module = (OPNodeWithNameAndPorts)getTopology().getNode(tmpModuleID);
        }

        OPModule m = null;
        if (module instanceof OPModule) {
            m = (OPModule)module;
        }

        // Verify that we have a module and node
        if (m == null || (from == null && to == null)) {
            System.err.println("Error: failed to find module and node(s) when installing module instance. " 
                    + "Module ID: " + moduleID);
            return;
        }

        // Are we dragging it to a node?
        if (to != null) {
            // Duplicate the module (it should be an original)
            if(m.isOriginal()) {
                m = new OPModule(m, moduleID);
                getTopology().addNode(getConnection(), m);
            }

            // Update which node the module is installed on
            if(from != null)
                config.get(key(from)).b.remove(m);
            config.get(key(to)).b.add(m);

            m.setPos(to.getX(), to.getY());
            m.installOnNode(to);
        }
        // Otherwise we're removing it but not placing it somewhere new
        else if(m.getNodeInstalledOn() != null) {
            // Update which node the module is installed on
            if(from != null) {
                config.get(key(from)).b.remove(m);
                getTopology().removeNode(getConnection(), Long.valueOf(moduleID));
            }

            manager.removeDrawable(m);
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
    
    /** the mrker to place on the selected link endpoint */
    private static final Icon LINK_ENDPOINT_MARKER = new ShapeIcon(new Ellipse2D.Double(0, 0, 30, 30), Color.RED, Color.BLACK);
    
    /** endpoint of a link being added/removed, if the process has been started */
    private OPNodeWithNameAndPorts linkEndpoint = null;
    
    /** clears the link endpoint and any marker on it */
    private void clearLinkEndpoint() {
        if(linkEndpoint != null) {
            linkEndpoint.setMarker(null);
            linkEndpoint = null;
        }
    }
    
    /** handles the request for the addition or deletion of a link */
    private void handleLinkChange(Drawable d, boolean add) {
        // check to see if the node requested to be hooked up is a valid one
        final OPNodeWithNameAndPorts n;
        boolean ok = true;
        if(d instanceof OPModule) {
            OPModule m = (OPModule)d;
            n = m;
            
            // cannot link to original modules (they are the "palette")
            if(m.isOriginal())
                ok = false;
        }
        else if(d instanceof OPNodeWithNameAndPorts) {
            n = (OPNodeWithNameAndPorts)d;
            if(n.getType()==NodeType.TYPE_IN || n.getType()==NodeType.TYPE_OUT) {
                if(n.getType() == NodeType.TYPE_IN && linkEndpoint!=null)
                    ok = false; // IN cannot be a destination
                else if(n.getType() == NodeType.TYPE_OUT && linkEndpoint==null)
                    ok = false; // OUT cannot be a source
            }
            else
                ok = false; // only IN, OUT, and OPModule can be connected by links
        }
        else {
            n = null;
            ok = false;
        }
        
        // bail out if an invalid choice was made
        if(!ok) {
            clearLinkEndpoint();
            return;
        }
        
        if(linkEndpoint == null) {
            // remember and mark the first choice
            linkEndpoint = n;
            linkEndpoint.setMarker(LINK_ENDPOINT_MARKER);
        }
        else {
            OPNodeWithNameAndPorts srcN = linkEndpoint;
            clearLinkEndpoint();
            
            // cannot link a module to itself
            if(srcN.equals(n))
                return;
            
            tryLinkAddDel(add, srcN, n);
        }
    }

    /**
     * Try adding/deleting a link. This will pop up a dialog if the source has more than one port.
     *
     * @param add		-- boolean indicating whether we are adding a link
     * @param srcNode	-- src node of the link
     * @param dstNode	-- destination node of the link
     */
    protected void tryLinkAddDel(boolean add,
            OPNodeWithNameAndPorts srcNode,
            OPNodeWithNameAndPorts dstNode) {
        short srcPort = 0;
        short dstPort = 0;

        // Query the source port if the source has multiple ports
        if (srcNode instanceof OPModule) {
            OPModule srcModule = (OPModule)srcNode;
            OPModulePort ports[] = srcModule.getPorts();
            if (srcModule.getPorts().length > 1) {
                if (add)
                    tryLinkAddMultiPort(srcModule, dstNode);
                else
                    tryLinkDelMultiPort(srcModule, dstNode);

                return;
            }
        }
        sendLinkAddDelMsg(add, srcNode, srcPort, dstNode, dstPort);
    }

    /**
     * Try adding a link with multiple ports
     *
     * @param srcNode	-- src node of the link
     * @param dstNode	-- destination node of the link
     */
    protected void tryLinkAddMultiPort(
            OPModule srcModule,
            OPNodeWithNameAndPorts dstNode) {
        short dstPort = 0;

        OPModulePort ports[] = srcModule.getPorts();
        Object items[] = new Object[ports.length + 1];
        items[0] = "Please select which port(s) of " + srcModule.getName() + " to connect to " + dstNode.getName();
        for (int i = 0; i < ports.length; i++)
            items[i + 1] = new JCheckBox(ports[i].getName() + " (" + ports[i].getDesc() + ")");

        int result = JOptionPane.OK_OPTION;
        boolean itemSelected = false;
        while (result == JOptionPane.OK_OPTION && !itemSelected) {
            result = JOptionPane.showConfirmDialog(null, items, "Add links", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION)
                return;

            for (int i = 0; i < ports.length; i++) {
                JCheckBox cb = (JCheckBox)items[i + 1];
                itemSelected |= cb.isSelected();
            }
        }

        // Send the actual link messages
        for (int i = 0; i < ports.length; i++) {
            JCheckBox cb = (JCheckBox)items[i + 1];
            if (cb.isSelected())
                sendLinkAddDelMsg(true, srcModule, ports[i].getId(), dstNode, dstPort);
        }
    }

    /**
     * Try deleting a link with multiple ports
     *
     * @param srcNode	-- src node of the link
     * @param dstNode	-- destination node of the link
     */
    protected void tryLinkDelMultiPort(
            OPModule srcModule,
            OPNodeWithNameAndPorts dstNode) {
        short dstPort = 0;

        // Work out what links go from source to dest
        HashSet<Short> srcPortNums = new HashSet<Short>();
        Collection<Link> links = srcModule.getLinks();
        for (Link l: links) {
            if (l.getDestination() == dstNode) {
               srcPortNums.add(Short.valueOf(l.getMyPort(srcModule)));
            }
        }

        // Do we need to delete multiple ports?
        if (srcPortNums.size() == 1) {
            short srcPort = ((Short)(srcPortNums.toArray()[0])).shortValue();
            sendLinkAddDelMsg(false, srcModule, srcPort, dstNode, dstPort);
            return;
        }

        // Work out which OPModulePort elements correspond to the connected ports
        OPModulePort allSrcPorts[] = srcModule.getPorts();
        ArrayList<OPModulePort> ports = new ArrayList<OPModulePort>();
        for (OPModulePort port : allSrcPorts) {
            if (srcPortNums.contains(Short.valueOf(port.getId())))
                ports.add(port);
        }

        // Create the list of ports to show in the dialog
        Object items[] = new Object[ports.size() + 1];
        items[0] = "Please select which port(s) of " + srcModule.getName() + " to disconnect from " + dstNode.getName();
        for (int i = 0; i < ports.size(); i++)
            items[i + 1] = new JCheckBox(ports.get(i).getName() + " (" + ports.get(i).getDesc() + ")");

        // Display a dialog asking which ports to delete
        int result = JOptionPane.OK_OPTION;
        boolean itemSelected = false;
        while (result == JOptionPane.OK_OPTION && !itemSelected) {
            result = JOptionPane.showConfirmDialog(null, items, "Delete links", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION)
                return;

            // Verify we actually have some ports selected
            for (int i = 0; i < ports.size(); i++) {
                JCheckBox cb = (JCheckBox)items[i + 1];
                itemSelected |= cb.isSelected();
            }
        }

        // Send the actual link messages
        for (int i = 0; i < ports.size(); i++) {
            JCheckBox cb = (JCheckBox)items[i + 1];
            if (cb.isSelected())
                sendLinkAddDelMsg(false, srcModule, ports.get(i).getId(), dstNode, dstPort);
        }
    }
    
    /**
     * Send a link add/delete message to the backed
     *
     * @param add		-- boolean indicating whether we are adding a link
     * @param srcNode	-- src node of the link
     * @param srcPort	-- src port
     * @param dstNode	-- destination node of the link
     * @param dstPort	-- destination port
     */
    protected void sendLinkAddDelMsg(boolean add,
            OPNodeWithNameAndPorts srcNode, short srcPort,
            OPNodeWithNameAndPorts dstNode, short dstPort) {
        // create the link add/del message to the backend
        OFGMessage msg;
        org.openflow.gui.net.protocol.Link[] link;
        org.openflow.gui.net.protocol.Node src = new org.openflow.gui.net.protocol.Node(srcNode.getType(), srcNode.getID());
        org.openflow.gui.net.protocol.Node dst = new org.openflow.gui.net.protocol.Node(dstNode.getType(), dstNode.getID());
        link = new org.openflow.gui.net.protocol.Link[] {new org.openflow.gui.net.protocol.Link(LinkType.WIRE, src, srcPort, dst, dstPort)};
        if(add)
            msg = new LinksAdd(link);
        else
            msg = new LinksDel(link);

        // send it
        try {
            getConnection().sendMessage(msg);
        }
        catch(IOException ex) {
            System.err.println("Error: failed to send (" + ex.getMessage() + "): " + msg);
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
        case OP_NODES_ADD:
            for(org.openflow.gui.net.protocol.op.OPNode n : ((OPNodesAdd)msg).nodes)
                super.processDrawableNodeAdd(processNodeAdd(n));
            break;
            
        case OP_NODES_DEL:
            for(org.openflow.gui.net.protocol.op.OPNode n : ((OPNodesDel)msg).nodes)
                super.processNodeDel(n);
            break;

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

        case OP_MOVE_MODULE:
            moveModule((OPMoveModule)msg);
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
    }
    
    
    /**
     * Handles OpenPipes-specific nodes and uses the superclass implementation
     * for all other nodes.
     */
    protected Node processNodeAdd(org.openflow.gui.net.protocol.Node n) {
        GeometricIcon gicon;
        Icon icon;
        String name = "";
        org.openflow.gui.net.protocol.op.OPModule m;
        OPModulePort ports[];
        OPModuleReg regs[];
        
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
            
        case TYPE_PC:
            icon = new org.pzgui.icon.ImageIcon("images/laptop-blue.png");
            break;
            
        case TYPE_MODULE_HW:
            icon = new ShapeIcon(new RoundRectangle2D.Double(0, 0, MODULE_SIZE, MODULE_SIZE*4/5, 10, 10), DARK_GREEN, Color.BLACK);
            m = (org.openflow.gui.net.protocol.op.OPModule)n;
            name = m.name;
            ports = new OPModulePort[m.ports.length];
            for (int i = 0; i < m.ports.length; i++)
                ports[i] = new OPModulePort(m.ports[i].id, m.ports[i].name, m.ports[i].desc);
            regs = new OPModuleReg[m.regs.length];
            for (int i = 0; i < m.regs.length; i++)
                regs[i] = new OPModuleReg(m.regs[i].addr, m.regs[i].name, m.regs[i].desc, m.regs[i].rdOnly);
            return new OPModule(true, name, n.id, icon, ports, regs);
        
        case TYPE_MODULE_SW:
            icon = new ShapeIcon(new Ellipse2D.Double(0, 0, MODULE_SIZE, MODULE_SIZE), DARK_BLUE, Color.BLACK);
            m = (org.openflow.gui.net.protocol.op.OPModule)n;
            name = m.name;
            ports = new OPModulePort[m.ports.length];
            for (int i = 0; i < m.ports.length; i++)
                ports[i] = new OPModulePort(m.ports[i].id, m.ports[i].name, m.ports[i].desc);
            regs = new OPModuleReg[m.regs.length];
            for (int i = 0; i < m.regs.length; i++)
                regs[i] = new OPModuleReg(m.regs[i].addr, m.regs[i].name, m.regs[i].desc, m.regs[i].rdOnly);
            return new OPModule(false, name, n.id, icon, ports, regs);
            
        default:
            return super.processNodeAdd(n);
        }
        
        OPNodeWithNameAndPorts ret = new OPNodeWithNameAndPorts(n.nodeType, name, n.id, icon);
        if(ret.getType() == NodeType.TYPE_NETFPGA || ret.getType() == NodeType.TYPE_PC) {
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
