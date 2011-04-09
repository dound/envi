package org.openflow.gui;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.drawables.Flow;
import org.openflow.gui.drawables.Host;
import org.openflow.gui.drawables.Link;
import org.openflow.gui.drawables.Node;
import org.openflow.gui.drawables.NodeWithPorts;
import org.openflow.gui.drawables.OpenFlowSwitch;
import org.openflow.gui.net.BackendConnection;
import org.openflow.gui.net.MessageProcessor;
import org.openflow.gui.net.protocol.FlowsAdd;
import org.openflow.gui.net.protocol.FlowsDel;
import org.openflow.gui.net.protocol.LinksAdd;
import org.openflow.gui.net.protocol.LinksDel;
import org.openflow.gui.net.protocol.NodeType;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;
import org.openflow.gui.net.protocol.Request;
import org.openflow.gui.net.protocol.RequestLinks;
import org.openflow.gui.net.protocol.RequestType;
import org.openflow.gui.net.protocol.StatsHeader;
import org.openflow.gui.net.protocol.SwitchDescriptionRequest;
import org.openflow.gui.net.protocol.NodesAdd;
import org.openflow.gui.net.protocol.NodesDel;
import org.openflow.gui.net.protocol.auth.AuthReply;
import org.openflow.gui.net.protocol.auth.AuthRequest;
import org.openflow.gui.net.protocol.auth.AuthStatus;
import org.openflow.protocol.AggregateStatsReply;
import org.openflow.protocol.AggregateStatsRequest;
import org.openflow.protocol.Match;
import org.openflow.protocol.SwitchDescriptionStats;
import org.openflow.util.FlowHop;
import org.openflow.util.string.DPIDUtil;
import org.pzgui.DialogHelper;
import org.pzgui.PZClosing;
import org.pzgui.PZManager;

/**
 * Processes messages from a connection and updates the associated topology 
 * accordingly.  It also maintains the network topology data structure 
 * associated with it.
 * 
 * This class should be extended to process new messages in extended OFG 
 * versions which add new message types. 
 * 
 * @author David Underhill
 */
public class ConnectionHandler implements MessageProcessor<OFGMessage>,
                                          PZClosing {
    /** the connection being managed */
    private final BackendConnection<OFGMessage> connection;
    
    /** the topology this connection populates */
    private final Topology topology;
    
    /** whether the connection is being shut down */
    private boolean shutting_down = false;
    
    /** whether to subscribe to switch updates */
    private boolean subscribeToSwitchChanges;
    
    /** whether to subscribe to link updates */
    private boolean subscribeToLinkChanges;
    
    /**
     * Create a connection bound to the server at the specified address and port
     * which will be used to populate the specified topology.
     * 
     * @param topo               the topology this connection will interact with
     * @param ip                 the IP where the server lives
     * @param port               the port the server listens on
     * @param subscribeSwitches  whether to subscribe to switch changes
     * @param subscribeLinks     whether to subscribe to link changes
     */
    public ConnectionHandler(Topology topo, String ip, int port, 
                             boolean subscribeSwitches, boolean subscribeLinks) {
        topology = topo;
        connection = new BackendConnection<OFGMessage>(this, ip, port);
        subscribeToSwitchChanges = subscribeSwitches;
        subscribeToLinkChanges = subscribeLinks;
    }
    
    public BackendConnection<OFGMessage> getConnection() {
        return connection;
    }
    
    public Topology getTopology() {
        return topology;
    }
    
    /**
     * @deprecated  this method will be removed soon; it has been replaced by
     *              connectedStateChange(boolean)  
     */
    public final void connectionStateChange() {
        throw new Error("using old connectionStateChange() method");
    }
    
    /** Called when the backend has been disconnected or reconnected */
    public void connectionStateChange(boolean connected) {
        if(!connection.isConnected()) {
            topology.removeAll(connection);
        }
        else {
            // ask the backend for a list of switches and links
            try {
                if(isSubscribeToSwitchChanges()) {
                    connection.sendMessage(new Request(OFGMessageType.NODES_REQUEST, RequestType.ONETIME));
                    connection.sendMessage(new Request(OFGMessageType.NODES_REQUEST, RequestType.SUBSCRIBE));
                }
                
                if(subscribeToLinkChanges) {
                    connection.sendMessage(new RequestLinks(RequestType.ONETIME));
                    connection.sendMessage(new RequestLinks(RequestType.SUBSCRIBE));
                }
            }
            catch(IOException e) {
                System.err.println("Error: unable to setup subscriptions");
            }
        }
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
            processAuthRequest((AuthRequest)msg);
            break;
            
        case AUTH_STATUS:
            processAuthStatus((AuthStatus)msg);
            break;
            
        case ECHO_REQUEST:
            processEchoRequest(msg.xid);
            break;

        case ECHO_REPLY:
            processEchoReply(msg.xid);
	    break;
            
        case NODES_ADD:
            processNodesAdd((NodesAdd)msg);
            break;
            
        case NODES_DELETE:
            processNodesDel((NodesDel)msg);
            break;
            
        case LINKS_ADD:
            processLinksAdd((LinksAdd)msg);
            break;
            
        case LINKS_DELETE:
            processLinksDel((LinksDel)msg);
            break;
            
        case FLOWS_ADD:
            processFlowsAdd((FlowsAdd)msg);
            break;
            
        case FLOWS_DELETE:
            processFlowsDel((FlowsDel)msg);
            break;
            
        case STAT_REPLY:
            processStatReply((StatsHeader)msg);
            break;
            
        case AUTH_REPLY:
        case NODES_REQUEST:
        case LINKS_REQUEST:
        case STAT_REQUEST:
            System.err.println("Received unexpected message type: " + msg.type.toString());
            
        default:
            System.err.println("Unhandled type received: " + msg.type.toString());
        }
    }

    /** 
     * Query the user for login credentials and send them to the backend.  If
     * msg does not supply a salt of at least 20B then a message is instead
     * printed to stderr and the request is ignored.
     */
    protected void processAuthRequest(AuthRequest req) {
        if(req.salt.length < 20) {
            System.err.println("Ignoring an authentication request with an insufficiently long salt (salt length: " + req.salt.length + "B)");
            return;
        }
        
        String username = DialogHelper.getInput("What is your username?");
        if(username == null) username = "";
        String pw = DialogHelper.getInput("What is your password, " + username + "?");
        if(pw == null) pw = "";
        
        try {
            getConnection().sendMessage(new AuthReply(req.xid, username, pw, req.salt));
        }
        catch(IOException e) {
            System.err.println("Failed to send an authentication reply: " + e.getMessage());
        }
    }
    
    /**
     * Let the user know about the authentication status contained in msg.  A 
     * message is printed to stdout if authentication succeeded.  Otherwise,
     * a dialog box pops up to notify the user of the authentication failure.
     */
    protected void processAuthStatus(AuthStatus status) {
        String m = (status.length() > 0) ? ": " + status.msg : "";
        if(status.authenticated)
            System.out.println("The server has accepted your session"+m);
        else
            DialogHelper.displayError("The server has refused to authenticate your session" + m);
    }
    
    /** 
     * Handles the echo request by replying.
     */
    protected void processEchoRequest(int xid) {
        try {
            getConnection().sendMessage(new OFGMessage(OFGMessageType.ECHO_REPLY, xid));
        } catch (IOException e) {
            System.err.println("Failed to send echo reply: " + e.getMessage());
        }
    }
    
    /** 
     * Handles the echo reply by simply printing a message to stdout.
     */
    protected void processEchoReply(int xid) {
        System.out.println("received echo reply (xid=" + xid + ")");
    }
    
    /**
     * Handles a new switch by requesting its links and description if 
     * Options.AUTO_REQUEST_LINK_INFO_FOR_NEW_SWITCH is true.
     * 
     * @param s  the new switch
     */
    protected void handleNewSwitch(OpenFlowSwitch s) {
        handleNewSwitch(s, true);
    }
    
    /**
     * Handles a new switch by requesting its links and description if 
     * Options.AUTO_REQUEST_LINK_INFO_FOR_NEW_SWITCH is true.
     * 
     * @param s          the new switch
     * @param linksOnly  only request new links (don't send a switch description request)
     */
    protected void handleNewSwitch(OpenFlowSwitch s, boolean linksOnly) {
        if(!Options.AUTO_REQUEST_LINK_INFO_FOR_NEW_SWITCH)
            return;
        
        // get the links associated with this switch
        long dpid = s.getID();
        try {
            org.openflow.gui.net.protocol.Node n;
            n = new org.openflow.gui.net.protocol.Node(NodeType.OPENFLOW_SWITCH, dpid);
            getConnection().sendMessage(new RequestLinks(RequestType.ONETIME, n));
        } catch (IOException e) {
            System.err.println("Warning: unable to get switches for switch + " + DPIDUtil.toString(dpid));
        }

        if(linksOnly)
            return;
            
        try {
            getConnection().sendMessage(new SwitchDescriptionRequest(dpid));
        } catch (IOException e) {
            System.err.println("Warning: unable to get switch desc for switch + " + DPIDUtil.toString(dpid));
        }
    }
    
    /** add new nodes to the topology */
    private void processNodesAdd(NodesAdd msg) {
        for(org.openflow.gui.net.protocol.Node msgNode : msg.nodes)
            processDrawableNodeAdd(processNodeAdd(msgNode));
    }
    
    /** add new node to the topology */
    protected void processDrawableNodeAdd(Node n) {
        if(n instanceof NodeWithPorts) {
            int ret = topology.addNode(connection, (NodeWithPorts)n);
            
            if(ret>=0 && n instanceof OpenFlowSwitch)
                handleNewSwitch((OpenFlowSwitch)n, ret!=0 /* if locally new, only request links */);
        }
    }
    
    /** 
     * Add a new node to the topology.  The default implementation assumes the
     * new node is a OpenFlowSwitch (unless its type is host).   The caller will
     * add the returned node to the topology if it is a subclass of 
     * NodeWithPorts.  The default caller will also call handleNewSwitch() if 
     * an OpenFlowSwitch is returned.
     */
    protected Node processNodeAdd(org.openflow.gui.net.protocol.Node n) {
        if(n.nodeType == NodeType.HOST)
            return new Host(n.id);
        else
            return new OpenFlowSwitch(this.getConnection(), n.id, n.nodeType);
    }

    /** remove nodes from the topology */
    private void processNodesDel(NodesDel msg) {
        for(org.openflow.gui.net.protocol.Node n : msg.nodes)
            if(topology.removeNode(connection, n.id) < 0)
                System.err.println("Ignoring switch delete message for non-existant switch: " + DPIDUtil.toString(n.id));
    }
    
    private void processLinksAdd(LinksAdd msg) {
        for(org.openflow.gui.net.protocol.LinkSpec x : msg.links) {
            NodeWithPorts dst = topology.getNode(x.dstNode.id);
            if(dst == null) {
                logNodeMissing("LinkAdd", "dst", x.dstNode.id);
                continue;
            }
            
            NodeWithPorts src = topology.getNode(x.srcNode.id);
            if(src == null) {
                logNodeMissing("LinkAdd", "src", x.srcNode.id);
                continue;
            }
            
                Link l = topology.addLink(x.linkType, dst, x.dstPort, src, x.srcPort);
                if(l == null)
                    continue;
                l.setMaximumDataRate(x.capacity_bps);
                
                if(!Options.AUTO_TRACK_STATS_FOR_NEW_LINK)
                    continue;
                
                // tell the backend to keep us updated on the link's utilization
                try {
                    l.trackStats(Options.STATS_REFRESH_RATE_MSEC, Match.MATCH_ALL, getConnection());
                }
                catch (IOException e) {
                    System.err.println("Warning: unable to setup link utilization polling for switch " + 
                            DPIDUtil.toString(x.dstNode.id) + " port " + l.getMyPort(dst));
                }
        }
    }
    
    private void processLinksDel(LinksDel msg) {
        for(org.openflow.gui.net.protocol.Link x : msg.links) {
            int ret = topology.disconnectLink(connection, x.dstNode.id, x.dstPort, x.srcNode.id, x.srcPort);
            switch(ret) {
            case  0: /* success */ break;
            case -1: logLinkMissing("delete", "src node", x.dstNode.id, x.dstPort, x.srcNode.id, x.srcPort); break;
            case -2: logLinkMissing("delete", "dst node", x.dstNode.id, x.dstPort, x.srcNode.id, x.srcPort); break;
            case -3: logLinkMissing("delete", "link",     x.dstNode.id, x.dstPort, x.srcNode.id, x.srcPort); break;
            }
        }
    }
    
    private void processFlowsAdd(FlowsAdd msg) {
        for(org.openflow.gui.net.protocol.Flow x : msg.flows) {
            NodeWithPorts src = topology.getNode(x.srcNode.id);
            if(src == null) {
                logNodeMissing("FlowAdd", "src", x.srcNode.id);
                continue;
            }
            
            NodeWithPorts dst = topology.getNode(x.dstNode.id);
            if(dst == null) {
                logNodeMissing("FlowAdd", "dst", x.dstNode.id);
                continue;
            }
            
            FlowHop[] hops = new FlowHop[x.path.length + 2];
            hops[0] = new FlowHop((short)-1, src, x.srcPort);
            hops[hops.length-1] = new FlowHop(x.dstPort, dst, (short)-1);
            
            int i = 1;
            for(org.openflow.gui.net.protocol.FlowHop fh : x.path) {
                NodeWithPorts hop = topology.getNode(fh.node.id);
                if(hop == null) {
                    logNodeMissing("FlowAdd", "hop" + i, fh.node.id);
                    continue;
                }
                hops[i++] = new FlowHop(fh.inport, hop, fh.outport);
            }
            
            Flow flow = new Flow(x.type, x.id, hops);
            topology.addFlow(flow);
        }
    }
    
    private void processFlowsDel(FlowsDel msg) {
        for(org.openflow.gui.net.protocol.Flow x : msg.flows)
            topology.removeFlowByID(x.id);
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
        OFGMessage msg = getConnection().popAssociatedStatefulRequest(reply.xid);
        AggregateStatsRequest req;
        if(msg==null || !(msg instanceof AggregateStatsRequest)) {
            System.err.println("Warning: matching stateful request for " +
                    "AggregateStatsReply is not an AggregateStatsRequest (got " + msg + ")");
            return;
        }
        req = (AggregateStatsRequest)msg;
        
        // get the switch associated with these stats
        NodeWithPorts n = topology.getNode(req.dpid);
        if(n == null) {
            System.err.println("Warning: received aggregate stats reply for unknown switch " + DPIDUtil.toString(req.dpid));
            return;
        }
        
        Link l = n.getLinkFrom(req.outPort);
        if(l == null) {
            System.err.println("Warning: received aggregate stats reply for disconnect port " + req.outPort + " for switch " + DPIDUtil.toString(req.dpid));
            return;
        }
        
        l.updateStats(req.match, reply);
    }

    private void processStatReplyDesc(SwitchDescriptionStats msg) {
        NodeWithPorts n = topology.getNode(msg.dpid);
        if(n != null) {
            if(n instanceof OpenFlowSwitch)
                ((OpenFlowSwitch)n).setSwitchDescription(msg);
            else
                System.err.println("Warning: received switch description for non-switch " + n.toString());
        }
        else
            System.err.println("Warning: received switch description for unknown switch " + DPIDUtil.toString(msg.dpid));
    }
    

    /** Prints an error message about a missing node. */
    protected void logNodeMissing(String msg, String why, long id) {
        if(shutting_down) return;
        System.err.println("Ignoring " + msg + " message for non-existant " + why + " node: " + 
                DPIDUtil.toString(id));
    }
    
    /** Prints an error message about a missing link. */
    protected void logLinkMissing(String msg, String why, long dstDPID, short dstPort, long srcDPID, short srcPort) {
        if(shutting_down) return;
        System.err.println("Ignoring link " + msg + " message for non-existant " + why + ": " + 
                DPIDUtil.toString(srcDPID) + ", port " + srcPort + " to " +
                DPIDUtil.toString(dstDPID) + ", port " + dstPort);
    }

    public void shutdown() {
        shutting_down = true;
        connection.shutdown();
    }

    /** Returns whether the connection is subscribed to switch changes */
    public boolean isSubscribeToSwitchChanges() {
        return subscribeToSwitchChanges;
    }
    
    /** 
     * Sets whether the connection is subscribed to switch changes and sends 
     * the appropriate subscription request if this is different. 
     */
    public void setSubscribeToSwitchChanges(boolean b) throws IOException {
        if(b == subscribeToSwitchChanges)
            return;
        
        RequestType rt = b ? RequestType.SUBSCRIBE : RequestType.UNSUBSCRIBE;
        connection.sendMessage(new Request(OFGMessageType.NODES_REQUEST, rt));
        subscribeToSwitchChanges = b;
    }

    /** Returns whether the connection is subscribed to link changes */
    public boolean isSubscribeToLinkChanges() {
        return subscribeToSwitchChanges;
    }
    
    /** 
     * Sets whether the connection is subscribed to link changes and sends 
     * the appropriate subscription request if this is different. 
     */
    public void setSubscribeToLinkChanges(boolean b) throws IOException {
        if(b == subscribeToLinkChanges)
            return;
        
        RequestType rt = b ? RequestType.SUBSCRIBE : RequestType.UNSUBSCRIBE;
        connection.sendMessage(new RequestLinks(rt));
        subscribeToLinkChanges = b;
    }

    /**
     * Sends a goodbye message to the backend and then shutsdown the connection.
     */
    public void pzClosing(PZManager manager) {
        try {
            connection.sendMessage(new OFGMessage(OFGMessageType.DISCONNECT, 0));
            connection.shutdown();
        } catch (IOException e) {
            System.err.println("Unable to send DISCONNECT message: " + e.getMessage());
        }
    }
}
