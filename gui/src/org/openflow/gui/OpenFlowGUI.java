package org.openflow.gui;

import java.io.DataInput;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.pzgui.DialogHelper;
import org.pzgui.Drawable;
import org.pzgui.DrawableEventListener;
import org.pzgui.PZClosing;
import org.pzgui.PZManager;
import org.pzgui.layout.Edge;
import org.pzgui.layout.PZLayoutManager;
import org.pzgui.layout.Vertex;

import org.openflow.gui.drawables.*;
import org.openflow.gui.drawables.Link;
import org.openflow.gui.drawables.Link.LinkExistsException;
import org.openflow.gui.net.*;
import org.openflow.gui.net.protocol.*;
import org.openflow.gui.net.protocol.auth.*;
import org.openflow.protocol.*;
import org.openflow.util.string.DPIDUtil;

/**
 * The core of an OpenFlow GUI (OFG).  It initializes the GUI (including the GUI 
 * manager) and handles the connection to the backend (including the processing
 * of received messages and maintaining the network topology data structures).
 * 
 * This class provides only a simple, extensible OpenFlow GUI.  It can be 
 * extended to provide support for more specific GUI applications.
 * 
 * @author David Underhill
 *
 * @param <MANAGER>  The kind of PZLayoutManager to use to manage the GUI itself.
 */
public class OpenFlowGUI<MANAGER extends PZLayoutManager> implements MessageProcessor<OFGMessage>, PZClosing, DrawableEventListener {
    /** run the GUI front-end */
    public static void main(String args[]) {
        String server = null;
        if(args.length > 0)
            server = args[0];
        
        Short port = null;
        
        PZLayoutManager manager = new PZLayoutManager();
        new OpenFlowGUI<PZLayoutManager>(manager, server, port, true).startConnection();
        
        // layout the nodes with the spring algorithm by default
        manager.setLayout(new edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge>(manager.getGraph()));
    }
    
    /** connection to the backend */
    protected final BackendConnection<OFGMessage> conn;
    
    /** the GUI window manager */
    protected final MANAGER manager;
    
    /** whether to automatically request link info for all new switches */
    private boolean autoRequestLinkInfoForNewSwitch;
    
    /** whether to automatically request that link stats be periodically sent for all new links */
    private boolean autoTrackStatsForNewLink;
    
    /** how often to refresh basic port statistics */
    private int statsRefreshRate_msec;
    
    /** whether the GUI is shutting down */
    private boolean disconnecting = false;

    /**
     * Start the GUI front-end.
     * 
     * @param manager  the object for managing this GUI 
     * @param server   the IP or hostname where the back-end is located
     * @param port     the port the back-end is listening on
     * @param auto     whether subscribeSwitches/Links and auto requests should 
     *                 be on or off (like calling the full GUI constructor with
     *                 auto for all boolean arguments)
     */
    public OpenFlowGUI(MANAGER manager, String server, Short port, boolean auto) {
        this(manager, server, port, auto, auto, auto, auto, 2000);
    }
    
    /** 
     * Start the GUI front-end.
     * 
     * @param manager                          the object for managing this GUI 
     * @param server                           the IP or hostname where the back-end is located
     * @param port                             the port the back-end is listening on
     * @param subscribeSwitches                whether to subscribe to switch changes
     * @param subscribeLinks                   whether to subscribe to link changes
     * @param autoRequestLinkInfoForNewSwitch  whether to automatically request link info for all new switches
     * @param autoTrackStatsForNewLink         whether to automatically request that link stats be periodically sent for all new links
     * @param statsRefreshRate_msec            how often to refresh basic port statistics (irrelevant if autoTrackStatsForNewLink is false)
     */
    public OpenFlowGUI(MANAGER manager,
                String server, Short port,
                boolean subscribeSwitches, boolean subscribeLinks,
                boolean autoRequestLinkInfoForNewSwitch,
                boolean autoTrackStatsForNewLink,
                int statsRefreshRate_msec) {
        // ask the user for the NOX controller's IP if it wasn't already given
        if(server == null || server.length()==0)
            server = DialogHelper.getInput("What is the IP or hostname of the NOX server?", "127.0.0.1");

        if(server == null) {
            System.out.println("Goodbye");
            System.exit(0);
        }
        
        if(port == null)
            port = BackendConnection.DEFAULT_PORT;
        conn = new BackendConnection<OFGMessage>(this, server, port, subscribeSwitches, subscribeLinks);

        // set defaults
        this.autoRequestLinkInfoForNewSwitch = autoRequestLinkInfoForNewSwitch;
        this.autoTrackStatsForNewLink = autoTrackStatsForNewLink;
        this.statsRefreshRate_msec = statsRefreshRate_msec;
        
        // fire up the GUI
        this.manager = manager;
        manager.start();
    }
   
    /** start the connection - should only be called once */
    public void startConnection() {
        // try to connect to the backend
        conn.start();
    }
    
    /** shutdown the connection */
    public void pzClosing(PZManager manager) {
        disconnecting = true;
        long start = System.currentTimeMillis();
        conn.shutdown();
        Thread.yield();

        // wait until the connection has been torn down or 1sec has passed
        while(!conn.isShutdown() && System.currentTimeMillis()-start<1000) {}
    }
    
    /** a drawable has fired an event (this method does nothing by default) */
    public void drawableEvent(Drawable d, String event) {}
    
    /** Called when the backend has been disconnected or reconnected */
    public void connectionStateChange() {
        if(!conn.isConnected()) {
            cleanup();
        }
    }
    
    /**
     * Cleanup state.  Called by the OpenFlowGUI object whenever it becomes disconnected
     * from the backend.  Will remove all switches.
     */
    protected void cleanup() {
        // remove all switches when we get disconnected
        for(Long d : switchesList)
            disconnectSwitch(d);
    }

    /** 
     * Constructs the object representing the received message.  The message is 
     * known to be of length len and len - 4 bytes representing the rest of the 
     * message should be extracted from buf.
     */
    public OFGMessage decode(int len, DataInput in) throws IOException {
        return OFGMessageType.decode(len, in);
    }

    /** Handles messages received from the backend */
    public void process(final OFGMessage msg) {
        if(BackendConnection.PRINT_MESSAGES)
            System.out.println("recv: " + msg.toString());
        
        switch(msg.type) {
        case AUTH_REQUEST:
            processAuthRequest((AuthHeader)msg);
            break;
            
        case SWITCHES_ADD:
            processSwitchesAdd((SwitchesAdd)msg);
            break;
            
        case SWITCHES_DELETE:
            processSwitchesDel((SwitchesDel)msg);
            break;
            
        case LINKS_ADD:
            processLinksAdd((LinksAdd)msg);
            break;
            
        case LINKS_DELETE:
            processLinksDel((LinksDel)msg);
            break;
            
        case STAT_REPLY:
            processStatReply((StatsHeader)msg);
            break;
            
        case AUTH_REPLY:
        case SWITCHES_REQUEST:
        case LINKS_REQUEST:
        case STAT_REQUEST:
            System.err.println("Received unexpected message type: " + msg.type.toString());
            
        default:
            System.err.println("Unhandled type received: " + msg.type.toString());
        }
    }

    private void processAuthRequest(AuthHeader msg) {
        switch(msg.authType) {
        case PLAIN_TEXT:
            handleAuthRequestPlainText();
            break;
        
        default:
            System.err.println("Unhandled authentication type received: " + msg.authType.toString());
        }
    }

    /** query the user for login credentials and send them to the backend */
    private void handleAuthRequestPlainText() {
        String username = DialogHelper.getInput("What is your username?");
        if(username == null) username = "";
        String pw = DialogHelper.getInput("What is your password, " + username + "?");
        if(pw == null) pw = "";
        
        try {
            conn.sendMessage(new AuthPlainText(username, pw));
        }
        catch(IOException e) {
            System.err.println("Failed to send plain-text authentication reply");
            conn.reconnect();
        }
    }

    /** switches in the topology */
    protected final ConcurrentHashMap<Long, OpenFlowSwitch> switchesMap = new ConcurrentHashMap<Long, OpenFlowSwitch>();
    protected final CopyOnWriteArrayList<Long> switchesList = new CopyOnWriteArrayList<Long>();
    
    /** list of switches which should be displayed as one or more virtual switches */
    protected final ConcurrentHashMap<Long, VirtualSwitchSpecification> virtualSwitches = new ConcurrentHashMap<Long, VirtualSwitchSpecification>();
    
    /** add a new display virtualization scheme for a switch */
    public void addVirtualizedSwitchDisplay(VirtualSwitchSpecification v) {
        OpenFlowSwitch s = switchesMap.get(v.getParentDPID());
        if(s != null)
            removeSwitchDrawable(s);
        
        virtualSwitches.put(v.getParentDPID(), v);
        if(s != null)
            addSwitchDrawable(s);
    }
    
    /** remove an existing display virtualization scheme for a switch; returns true if such a scheme existed */
    public boolean removeVirtualizedSwitchDisplay(Long dpid) {
        OpenFlowSwitch s = switchesMap.get(dpid);
        if(s != null)
            removeSwitchDrawable(s);
        
        return virtualSwitches.remove(dpid) != null;
    }
    
    /** Tells the manager to draw a switch (or its virtualized switches if it is virtualized). */
    private void addSwitchDrawable(OpenFlowSwitch s) {
        VirtualSwitchSpecification v = virtualSwitches.get(s.getID());
        if(v == null) {
            manager.addDrawable(s);
        }
        else {
            for(int i=0; i<v.getNumVirtualSwitches(); i++)
                manager.addDrawable(v.getVirtualSwitch(i).v);
        }
    }
    
    /** Tells the manager to stop drawing a switch (or its virtualized switches if it is virtualized). */
    private void removeSwitchDrawable(OpenFlowSwitch s) {
        VirtualSwitchSpecification v = virtualSwitches.get(s.getID());
        if(v == null) {
            manager.removeDrawable(s);
        }
        else {
            for(int i=0; i<v.getNumVirtualSwitches(); i++)
                manager.removeDrawable(v.getVirtualSwitch(i).v);
        }
    }
    
    private OpenFlowSwitch addSwitch(long dpid) {
        OpenFlowSwitch s = new OpenFlowSwitch(dpid);
        switchesMap.put(dpid, s);
        switchesList.add(dpid);
        s.setPos((int)Math.random()*500, (int)Math.random()*500);
        addSwitchDrawable(s);
        
        if(!autoRequestLinkInfoForNewSwitch)
            return s;
        
        // get the links associated with this switch
        try {
            conn.sendMessage(new LinksRequest(dpid));
        } catch (IOException e) {
            System.err.println("Warning: unable to get switches for switch + " + DPIDUtil.toString(dpid));
        }
        
        try {
            conn.sendMessage(new SwitchDescriptionRequest(dpid));
        } catch (IOException e) {
            System.err.println("Warning: unable to get switch desc for switch + " + DPIDUtil.toString(dpid));
        }
        
        return s;
    }
    
    /** add new switches to the topology */
    private void processSwitchesAdd(SwitchesAdd msg) {
        for(long dpid : msg.dpids)
            if(!switchesMap.containsKey(dpid))
                addSwitch(dpid);
    }

    /** remove former switches from the topology */
    private void processSwitchesDel(SwitchesDel msg) {
        for(long dpid : msg.dpids)
            if(!disconnectSwitch(dpid))
                System.err.println("Ignoring switch delete message for non-existant switch: " + DPIDUtil.toString(dpid));
    }
    
    /** remove a switch from the topology */
    private boolean disconnectSwitch(Long dpid) {
        OpenFlowSwitch s = switchesMap.get(dpid);
        if(s != null) {
            removeSwitchDrawable(switchesMap.remove(dpid)); 
            switchesList.remove(dpid);
            
            // disconnect all links associated with the switch too
            for(Link l : s.getLinks()) {
                try {
                    l.disconnect(conn);
                } 
                catch(IOException e) {
                    // ignore: connection down => polling messages cleared on the backend already
                }
            }
            
            return true;
        }
        return false;
    }
    
    /** 
     * Makes sure a switch with the specified exists and creates one if not.  
     * 
     * @param dpid  DPID of the switch which should exist for a link
     * @return the switch associated with dpid
     */
    private OpenFlowSwitch handleLinkToSwitch(long dpid) {
        OpenFlowSwitch s = switchesMap.get(dpid);
        if(s != null)
            return s;
        
        System.err.println("Warning: received link to switch DPID we didn't previously have (" + 
                           DPIDUtil.dpidToHex(dpid) + ")");
            
        // create the missing switch
        return addSwitch(dpid);
    }
    
    public Link addLink(NodeWithPorts dst, short dstPort, NodeWithPorts src, short srcPort) throws LinkExistsException {
        VirtualSwitchSpecification vDst = virtualSwitches.get(dst.getID());
        if(vDst != null) {
            dst = vDst.getVirtualSwitchByPort(dstPort);
            if(dst == null)
                return null; /* ignore unvirtualized ports on a display virtualized switch */
        }
        
        VirtualSwitchSpecification vSrc = virtualSwitches.get(src.getID());
        if(vSrc != null) {
            src = vSrc.getVirtualSwitchByPort(srcPort);
            if(src == null)
                return null; /* ignore unvirtualized ports on a display virtualized switch */
        }
        
        Link l = new Link(dst, dstPort, src, srcPort);
        return l;
    }
    
    private void processLinksAdd(LinksAdd msg) {
        for(org.openflow.gui.net.protocol.Link x : msg.links) {
            OpenFlowSwitch dstSwitch = handleLinkToSwitch(x.dstDPID);
            OpenFlowSwitch srcSwitch = handleLinkToSwitch(x.srcDPID);
            try {
                Link l = addLink(dstSwitch, x.dstPort, srcSwitch, x.srcPort);
                if(l == null)
                    continue;
                
                if(!autoTrackStatsForNewLink)
                    continue;
                
                // tell the backend to keep us updated on the link's utilization
                try {
                    l.trackStats(statsRefreshRate_msec, Match.MATCH_ALL, conn);
                }
                catch (IOException e) {
                    System.err.println("Warning: unable to setup link utilization polling for switch " + 
                            DPIDUtil.toString(x.dstDPID) + " port " + l.getMyPort(dstSwitch));
                }
            }
            catch(LinkExistsException e) {
                // ignore 
            }
        }
    }
    
    private void processLinksDel(LinksDel msg) {
        for(org.openflow.gui.net.protocol.Link x : msg.links)
            disconnectLink(x.dstDPID, x.dstPort, x.srcDPID, x.srcPort);
    }
    
    /** remove a link from the topology */
    private void disconnectLink(long dstDPID, short dstPort, long srcDPID, short srcPort) {
        OpenFlowSwitch srcSwitch = switchesMap.get(srcDPID);
        if(srcSwitch == null) {
            logLinkMissing("delete", "src switch", dstDPID, dstPort, srcDPID, srcPort);
            return;
        }
        
        OpenFlowSwitch dstSwitch = switchesMap.get(dstDPID);
        if(dstSwitch == null) {
            logLinkMissing("delete", "dst switch", dstDPID, dstPort, srcDPID, srcPort);
            return;
        }
        
        Link existingLink = dstSwitch.getLinkTo(dstPort, srcSwitch, srcPort);
        if(existingLink != null) {
            try {
                existingLink.disconnect(conn);
            } 
            catch(IOException e) {
                // ignore: connection down => polling messages cleared on the backend already
            }
        }
        else
            logLinkMissing("delete", "link", dstDPID, dstPort, srcDPID, srcPort);
    }
    
    /** Prints an error message about a missing link. */
    protected void logLinkMissing(String msg, String why, long dstDPID, short dstPort, long srcDPID, short srcPort) {
        if(disconnecting) return;
        System.err.println("Ignoring link " + msg + " message for non-existant " + why + ": " + 
                DPIDUtil.toString(srcDPID) + ", port " + srcPort + " to " +
                DPIDUtil.toString(dstDPID) + ", port " + dstPort);
    }

    private void processStatReply(StatsHeader msg) {
        switch(msg.statsType) {
        case DESC:
            processStatReplyDesc((SwitchDescriptionStats)msg);
            break;
            
        case AGGREGATE:
            processStatReplyAggregate((AggregateStatsReply)msg);
            break;
        
        default:
            System.err.println("Unhandled stats type received: " + msg.statsType.toString());
        }
    }

    private void processStatReplyAggregate(AggregateStatsReply reply) {
        // get the request which solicited this reply
        OFGMessage msg = conn.popAssociatedStatefulRequest(reply.xid);
        AggregateStatsRequest req;
        if(msg==null || !(msg instanceof AggregateStatsRequest)) {
            System.err.println("Warning: matching stateful request for " +
            		"AggregateStatsReply is not an AggregateStatsRequest (got " + msg + ")");
            return;
        }
        req = (AggregateStatsRequest)msg;
        
        // get the switch associated with these stats
        OpenFlowSwitch s = switchesMap.get(req.dpid);
        if(s == null) {
            System.err.println("Warning: received aggregate stats reply for unknown switch " + DPIDUtil.toString(req.dpid));
            return;
        }
        
        Link l = s.getLinkFrom(req.outPort);
        if(l == null) {
            System.err.println("Warning: received aggregate stats reply for disconnect port " + req.outPort + " for switch " + DPIDUtil.toString(req.dpid));
            return;
        }
        
        l.updateStats(req.match, reply);
    }

    private void processStatReplyDesc(SwitchDescriptionStats msg) {
        OpenFlowSwitch s = switchesMap.get(msg.dpid);
        if(s != null)
            s.setSwitchDescription(msg);
        else
            System.err.println("Warning: received switch description for unknown switch " + DPIDUtil.toString(msg.dpid));
    }    
}
