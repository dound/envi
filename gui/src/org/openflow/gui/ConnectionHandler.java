package org.openflow.gui;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.drawables.Host;
import org.openflow.gui.drawables.Link;
import org.openflow.gui.drawables.Node;
import org.openflow.gui.drawables.NodeWithPorts;
import org.openflow.gui.drawables.OpenFlowSwitch;
import org.openflow.gui.drawables.Link.LinkExistsException;
import org.openflow.gui.net.BackendConnection;
import org.openflow.gui.net.MessageProcessor;
import org.openflow.gui.net.protocol.LinksAdd;
import org.openflow.gui.net.protocol.LinksDel;
import org.openflow.gui.net.protocol.LinksRequest;
import org.openflow.gui.net.protocol.NodeType;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;
import org.openflow.gui.net.protocol.StatsHeader;
import org.openflow.gui.net.protocol.SwitchDescriptionRequest;
import org.openflow.gui.net.protocol.NodesAdd;
import org.openflow.gui.net.protocol.NodesDel;
import org.openflow.gui.net.protocol.auth.AuthHeader;
import org.openflow.gui.net.protocol.auth.AuthPlainText;
import org.openflow.protocol.AggregateStatsReply;
import org.openflow.protocol.AggregateStatsRequest;
import org.openflow.protocol.Match;
import org.openflow.protocol.SwitchDescriptionStats;
import org.openflow.util.string.DPIDUtil;
import org.pzgui.DialogHelper;

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
public class ConnectionHandler implements MessageProcessor<OFGMessage> {
    /** the connection being managed */
    private final BackendConnection<OFGMessage> connection;
    
    /** the topology this connection populates */
    private final Topology topology;
    
    /** whether the connection is being shut down */
    private boolean shutting_down = false;
    
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
        connection = new BackendConnection<OFGMessage>(this, ip, port, subscribeSwitches, subscribeLinks);
    }
    
    public BackendConnection<OFGMessage> getConnection() {
        return connection;
    }
    
    public Topology getTopology() {
        return topology;
    }
    
    /** Called when the backend has been disconnected or reconnected */
    public void connectionStateChange() {
        if(!connection.isConnected())
            topology.removeAllNodes(connection);
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
            getConnection().sendMessage(new AuthPlainText(username, pw));
        }
        catch(IOException e) {
            System.err.println("Failed to send plain-text authentication reply");
            getConnection().reconnect();
        }
    }
    
    /**
     * Handles a new switch by requesting its links and description if 
     * Options.AUTO_REQUEST_LINK_INFO_FOR_NEW_SWITCH is true.
     * 
     * @param s  the new switch
     */
    protected void handleNewSwitch(OpenFlowSwitch s) {
        if(!Options.AUTO_REQUEST_LINK_INFO_FOR_NEW_SWITCH)
            return;
        
        // get the links associated with this switch
        long dpid = s.getID();
        try {
            getConnection().sendMessage(new LinksRequest(dpid));
        } catch (IOException e) {
            System.err.println("Warning: unable to get switches for switch + " + DPIDUtil.toString(dpid));
        }
        
        try {
            getConnection().sendMessage(new SwitchDescriptionRequest(dpid));
        } catch (IOException e) {
            System.err.println("Warning: unable to get switch desc for switch + " + DPIDUtil.toString(dpid));
        }
    }
    
    /** add new nodes to the topology */
    private void processNodesAdd(NodesAdd msg) {
        for(org.openflow.gui.net.protocol.Node msgNode : msg.nodes)
            processNodesAdd(processNodeAdd(msgNode));
    }
    
    /** add new node to the topology */
    protected void processNodesAdd(Node n) {
        if(n instanceof NodeWithPorts) {
            if(!topology.addNode(connection, (NodeWithPorts)n))
                return;
                
            if(n instanceof OpenFlowSwitch)
                handleNewSwitch((OpenFlowSwitch)n);
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
            return new OpenFlowSwitch(n.id);
    }

    /** remove nodes from the topology */
    private void processNodesDel(NodesDel msg) {
        for(org.openflow.gui.net.protocol.Node n : msg.nodes)
            if(topology.removeNode(connection, n.id) < 0)
                System.err.println("Ignoring switch delete message for non-existant switch: " + DPIDUtil.toString(n.id));
    }
    
    private void processLinksAdd(LinksAdd msg) {
        for(org.openflow.gui.net.protocol.Link x : msg.links) {
            NodeWithPorts dst = topology.getNode(x.dstNode.id);
            if(dst == null) {
                logNodeMissing("LinkAdd", "dst", x.dstNode.id);
                return;
            }
            
            NodeWithPorts src = topology.getNode(x.srcNode.id);
            if(src == null) {
                logNodeMissing("LinkAdd", "src", x.srcNode.id);
                return;
            }
            
            try {
                Link l = topology.addLink(x.linkType, dst, x.dstPort, src, x.srcPort);
                if(l == null)
                    continue;
                
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
            catch(LinkExistsException e) {
                // ignore 
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
}
