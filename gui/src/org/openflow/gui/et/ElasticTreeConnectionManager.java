package org.openflow.gui.et;

import java.awt.AWTEvent;
import java.io.IOException;

import org.pzgui.Drawable;
import org.pzgui.DrawableEventListener;

import org.openflow.gui.ConnectionHandler;
import org.openflow.gui.OpenFlowGUI;
import org.openflow.gui.Topology;
import org.openflow.gui.drawables.Link;
import org.openflow.gui.drawables.NodeWithPorts;
import org.openflow.gui.drawables.OpenFlowSwitch;
import org.openflow.gui.net.protocol.LinkType;
import org.openflow.gui.net.protocol.NodeType;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.et.ETBandwidth;
import org.openflow.gui.net.protocol.et.ETComputationDone;
import org.openflow.gui.net.protocol.et.ETLatency;
import org.openflow.gui.net.protocol.et.ETLinkFailureChange;
import org.openflow.gui.net.protocol.et.ETLinkUtil;
import org.openflow.gui.net.protocol.et.ETLinkUtilsList;
import org.openflow.gui.net.protocol.et.ETPowerUsage;
import org.openflow.gui.net.protocol.et.ETSwitchFailureChange;
import org.openflow.gui.net.protocol.et.ETSwitchesOff;
import org.openflow.gui.net.protocol.et.ETSwitchesRequest;
import org.openflow.gui.net.protocol.et.ETTrafficMatrix;
import org.openflow.gui.stats.LinkStats;
import org.openflow.protocol.Match;
import org.openflow.util.Pair;

/**
 * Handles Elastic Tree-specific communications.  Also provides the main method
 * for Elastic Tree.
 * 
 * @author David Underhill
 */
public class ElasticTreeConnectionManager extends ConnectionHandler 
                         implements DrawableEventListener, TrafficMatrixChangeListener {
    /** run the front-end */
    public static void main(String args[]) {
        Pair<String, Short> serverPort = OpenFlowGUI.getServer(args);
        String server = serverPort.a;
        short port = serverPort.b;
        
        // create a manager to handle drawing the topology info received by the connection
        ElasticTreeManager gm = new ElasticTreeManager(6);
        
        // create a manager to handle the connection itself
        ConnectionHandler cm = new ElasticTreeConnectionManager(gm, server, port);
        
        // start our managers
        gm.start();
        cm.getConnection().start();
    }
    
    /** the manager for our single topology */
    private final ElasticTreeManager manager;
    
    /** whether we have been connected before */
    private boolean firstConnection = true;
    
    /**
     * Construct the front-end for ElasticTreeConnectionManager.
     * 
     * @param manager the manager responsible for drawing the GUI
     * @param server  the IP or hostname where the back-end is located
     * @param port    the port the back-end is listening on
     */
    public ElasticTreeConnectionManager(ElasticTreeManager manager, String server, Short port) {
        super(new Topology(manager), server, port, false, false);
        
        this.manager = manager;
        manager.addDrawableEventListener(this);
        manager.addTrafficMatrixChangeListener(this);
        
        // setup the object which manages the sending of traffic matrix commands to the server
        tmManager = new TrafficMatrixManager(manager.getCurrentTrafficMatrix());
    }
    
    public void drawableEvent(Drawable d, AWTEvent e, String event) {
        if(event.equals("mouse_released"))
            processFailEvent(d);
    }
    
    /** 
     * Calls super.connectionStateChange() and then, if the connection is now
     * active for the first time, sends a message to the server.
     */
    public void connectionStateChange() {
        super.connectionStateChange();
        
        if(firstConnection && getConnection().isConnected()) {
            try {
                getConnection().sendMessage(new ETSwitchesRequest(manager.getK()));
                firstConnection = false;
            }
            catch(IOException e) {}
        }
        
        // if the manager was waiting for a response, it won't be coming if we got d/c
        if(!getConnection().isConnected())
            tmManager.responseWillNotCome();
    }
    
    /** 
     * Directly handles ElasticTreeConnectionManager-specific messages received from the  
     * backend and delegates handling of other messages to super.process().
     */
    public void process(final OFGMessage msg) {
        switch(msg.type) {
        case NODES_ADD:
            if(!tmManager.isWaitingForResponse())
                tmManager.start();
            super.process(msg);
            break;
            
        case ET_LINK_UTILS:
            processLinkUtils((ETLinkUtilsList)msg);
            break;
            
        case ET_POWER_USAGE:
            processPowerUsage((ETPowerUsage)msg);
            break;
        
        case ET_SWITCHES_OFF:
            processSwitchesOff((ETSwitchesOff)msg);
            break;
        
        case ET_BANDWIDTH:
            processBandwidthData((ETBandwidth)msg);
            break;
            
        case ET_LATENCY:
            processLatencyData((ETLatency)msg);
            break;
            
        case ET_COMPUTATION_DONE:
            processComputationDone((ETComputationDone)msg);
            break;
            
        case ET_TRAFFIX_MATRIX:
        case ET_SWITCHES_REQUEST:
        case ET_SWITCH_FAILURES:
        case ET_LINK_FAILURES:
            System.err.println("Received unexpected message type: " + msg.type.toString());
            
        default:
            super.process(msg);
        }
    }

    // ------- Traffic Matrix Sending ------- //
    // ************************************** //
    
    private class TrafficMatrixManager {
        /** the most recent traffic matrix command relayed to the server */
        private ETTrafficMatrix tmOutstanding  = null;
        
        /** the next traffic matrix to relay to the server */
        private ETTrafficMatrix tmNext;
        
        /** whether this manager is waiting for a response */
        private boolean waiting_for_response = false;
        
        /** Initializes the manager with the initial traffic matrix to use */
        public TrafficMatrixManager(ETTrafficMatrix tm) {
            setNextTrafficMatrix(tm);
        }
        
        /** 
         * Sets the next traffic matrix to use.  Overwrites any enqueued traffic
         * matrix.  The next matrix will be sent to the server as soon as the 
         * previous run is complete.  If we are connected and there are no
         * outstanding requests then this traffic matrix is immediately sent.
         */
        public synchronized void setNextTrafficMatrix(ETTrafficMatrix tm) {
            this.tmNext = new ETTrafficMatrix(tm.use_hw, tm.solver, tm.may_split_flows, tm.k, tm.demand, tm.edge, tm.agg, tm.plen);
            if(getConnection().isConnected() && !waiting_for_response)
                sendNextTrafficMatrix();
        }
        
        /** Calls sendNextTrafficMatrix(false) */
        private synchronized boolean sendNextTrafficMatrix() {
            return sendNextTrafficMatrix(false);
        }
        
        /** 
         * Sends the next traffic matrix to the server.
         * 
         * @param force  whether to send it even if the matrix has not changed
         */
        private synchronized boolean sendNextTrafficMatrix(boolean force) {
            if(tmOutstanding != null) {
                // stop if the "next" matrix is no different than the one we 
                // just got results for
                if(!force && manager.isSendOnChangeOnly() && tmOutstanding.equals(tmNext)) {
                    manager.setNextTrafficMatrixText(null);
                    tmManager.responseWillNotCome();
                    return false;
                }
                
                // trying to change the k value: start by getting the nodes in the new network
                if(tmOutstanding.k != tmNext.k) {
                    try {
                        tmManager.responseWillNotCome();
                        getConnection().sendMessage(new ETSwitchesRequest(tmNext.k));
                    }
                    catch(IOException e) {}
                }
            }
            tmOutstanding = tmNext;
            manager.setNextTrafficMatrixText(tmOutstanding);
            
            try {
                getConnection().sendMessage(tmOutstanding);
                waiting_for_response = true;
            } catch (IOException e) {
                System.err.println("Warning: unable to send traffix matrix update: " + tmOutstanding);
                return false;
            }
            return true;
        }
        
        /**
         * Tells the traffic matrix manager that the outstanding request has
         * been completed so that it can send the next request. 
         */
        public synchronized void completedLastTrafficMatrix() {
            manager.setCurrentTrafficMatrixText(tmOutstanding);
            sendNextTrafficMatrix();
        }

        /**
         * Starts the traffic matrix loop by sending a command to the server.  
         * This should be called whenever the connection to the server is setup.
         */
        public void start() {
            sendNextTrafficMatrix();
        }
        
        /**
         * Whether the manager is currently waiting for a response.
         */
        public boolean isWaitingForResponse() {
            return waiting_for_response;
        }
        
        /** 
         * Tells the manager it won't receive a reply for the last message it 
         * sent (the connection died). 
         */
        public void responseWillNotCome() {
            waiting_for_response = false;
        }
    }
    private TrafficMatrixManager tmManager;

    /**
     * Delegates getting results for the new traffic matrix to the 
     * TrafficMatrixManager.  If the fat tree k value has changed, then it 
     * purges the old topology and requests the new one.
     */
    public void trafficMatrixChanged(ETTrafficMatrix tm) {
        if(manager.getLastK() != tm.k) {
            getTopology().removeAllNodes(getConnection());
            try {
                getConnection().sendMessage(new ETSwitchesRequest(tm.k));
            } catch (IOException e) {
                System.err.println("Failed to send request for new switches: " + e.getMessage());
            }
        }
        
        if(tmManager != null)
            tmManager.setNextTrafficMatrix(tm);
    }
    
    
    // --- Elastic Tree Message Processing -- //
    // ************************************** //    
    
    private void processLinkUtils(ETLinkUtilsList msg) {
        double total_bps = 0;
        for(ETLinkUtil x : msg.utils)
            total_bps += processLinkUtil(x.srcNode.id, x.srcPort, x.dstNode.id, x.dstPort, x.util, msg.timeCreated);
        manager.setExpectedAggregateThroughput(total_bps);
    }
    
    /**
     * Updates the throughput data of the link described by these parameters.
     */
    private double processLinkUtil(long dstDPID, short dstPort, long srcDPID, short srcPort, float util, long when) {
        NodeWithPorts srcNode = getTopology().getNode(srcDPID);
        if(srcNode == null) {
            logLinkMissing("util", "src switch", dstDPID, dstPort, srcDPID, srcPort);
            return 0;
        }
        
        NodeWithPorts dstNode = getTopology().getNode(dstDPID);
        if(dstNode == null) {
            logLinkMissing("util", "dst switch", dstDPID, dstPort, srcDPID, srcPort);
            return 0;
        }
        
        Link existingLink = dstNode.getDirectedLinkTo(dstPort, srcNode, srcPort, false);
        if(existingLink != null) {
            LinkStats ls = existingLink.getStats(Match.MATCH_ALL);
            if(ls == null)
                ls = existingLink.trackStats(Match.MATCH_ALL);
            
            double bps = util * existingLink.getMaximumDataRate();
            double pps = bps / (1500*8); // assumes 1500B packets
            if(srcDPID == existingLink.getSource().getID())
                ls.statsSrc.setRates(pps, bps, 0, when);
            else if(srcDPID == existingLink.getDestination().getID())
                ls.statsDst.setRates(pps, bps, 0, when);
            else
                throw new Error("Error: LinkStats associated with existingLink has different DPIDs than its parent?");
            
            existingLink.setColorBasedOnCurrentUtilization();
            return bps;
        }
        else {
            logLinkMissing("util", "link", dstDPID, dstPort, srcDPID, srcPort);
            return 0;
        }
    }

    private void processPowerUsage(ETPowerUsage msg) {
        manager.setPowerData(msg.watts_current, msg.watts_traditional, msg.watts_traditional);
    }

    private void processSwitchesOff(ETSwitchesOff msg) {
        // turn everything back on
        for(Long id : getTopology().getNodeIDs())
            getTopology().getNode(id).setOff(false);
        
        // turn off the specified switches
        for(org.openflow.gui.net.protocol.Node n : msg.nodes) {
            NodeWithPorts o = getTopology().getNode(n.id);
            if(o != null)
                o.setOff(true);
        }
    }

    private void processBandwidthData(ETBandwidth msg) {
        manager.setAchievedAggregateThroughput(msg.bandwidth_achieved_mbps);
    }

    private void processLatencyData(ETLatency msg) {
        manager.setLatencyData(msg.latency_ms_edge, msg.latency_ms_agg, msg.latency_ms_core);
    }

    private void processComputationDone(ETComputationDone msg) {
        tmManager.completedLastTrafficMatrix();
        manager.noteResult(tmManager.tmOutstanding.demand, msg.num_unplaced_flows);
    }
    
    private void processFailEvent(Drawable d) {
        if(d instanceof OpenFlowSwitch) {
            OpenFlowSwitch o = (OpenFlowSwitch)d;
            o.setFailed(!o.isFailed());
            
            try {
                getConnection().sendMessage(new ETSwitchFailureChange(o.getID(), o.isFailed()));    
            }
            catch(IOException e) {
                System.err.println("failed to send switch failure");
            }
        }
        else if(d instanceof Link) {
            Link l = (Link)d;
            l.setFailed(!l.isFailed());
            try {
                getConnection().sendMessage(new ETLinkFailureChange(new org.openflow.gui.net.protocol.Link(
                        LinkType.WIRE,
                        new org.openflow.gui.net.protocol.Node(NodeType.OPENFLOW_SWITCH, l.getSource().getID()),
                        l.getMyPort(l.getSource()),
                        new org.openflow.gui.net.protocol.Node(NodeType.OPENFLOW_SWITCH, l.getDestination().getID()),
                        l.getMyPort(l.getDestination())), l.isFailed()));    
            }
            catch(IOException e) {
                System.err.println("failed to send link failure");
            }
        }
        else
            return;
        tmManager.sendNextTrafficMatrix(true);
    }
}
