package org.openflow.lavi.et;

import java.io.IOException;
import org.openflow.lavi.LAVI;
import org.openflow.lavi.drawables.Link;
import org.openflow.lavi.drawables.OpenFlowSwitch;
import org.openflow.lavi.net.protocol.LAVIMessage;
import org.openflow.lavi.net.protocol.et.ETBandwidth;
import org.openflow.lavi.net.protocol.et.ETComputationDone;
import org.openflow.lavi.net.protocol.et.ETLatency;
import org.openflow.lavi.net.protocol.et.ETLinkFailureChange;
import org.openflow.lavi.net.protocol.et.ETLinkUtil;
import org.openflow.lavi.net.protocol.et.ETLinkUtilsList;
import org.openflow.lavi.net.protocol.et.ETPowerUsage;
import org.openflow.lavi.net.protocol.et.ETSwitchFailureChange;
import org.openflow.lavi.net.protocol.et.ETSwitchesOff;
import org.openflow.lavi.net.protocol.et.ETSwitchesRequest;
import org.openflow.lavi.net.protocol.et.ETTrafficMatrix;
import org.openflow.lavi.stats.PortStatsRates;
import org.openflow.protocol.Match;
import org.pzgui.Drawable;

/**
 * Elastic Tree front-end.
 * 
 * @author David Underhill
 */
public class ElasticTree extends LAVI<ElasticTreeManager> 
                         implements TrafficMatrixChangeListener {
    /** run the LAVI front-end */
    public static void main(String args[]) {
        String server = null;
        if(args.length > 0)
            server = args[0];
        
        Short port = null;
        new ElasticTree(server, port).startConnection();
    }
    
    /** whether we have been connected before */
    private boolean firstConnection = true;
    
    /**
     * Construct the LAVI front-end for ElasticTree.
     * 
     * @param server  the IP or hostname where the back-end is located
     * @param port    the port the back-end is listening on
     */
    public ElasticTree(String server, Short port) {
        super(new ElasticTreeManager(6), server, port, false);
        
        manager.addClosingListener(this);
        manager.addDrawableEventListener(this);
        manager.addTrafficMatrixChangeListener(this);
        
        // setup the object which manages the sending of traffic matrix commands to the server
        tmManager = new TrafficMatrixManager(manager.getCurrentTrafficMatrix());
    }
    
    public void drawableEvent(Drawable d, String event) {
        if(event.equals("mouse_released"))
            processFailEvent(d);
    }
    
    /** 
     * Calls super.connectionStateChange() and then, if the connection is now
     * active for the first time, sends a message to the server.
     */
    public void connectionStateChange() {
        super.connectionStateChange();
        
        if(firstConnection && conn.isConnected()) {
            try {
                conn.sendLAVIMessage(new ETSwitchesRequest(manager.getK()));
                firstConnection = false;
            }
            catch(IOException e) {}
        }
    }
    
    /**
     * Calls super.cleanup() and then performs cleanup specific to ElasticTree.
     */
    protected void cleanup() {
        super.cleanup();
        
        // if the manager was waiting for a response, it won't be coming
        tmManager.responseWillNotCome();
    }
    
    /** 
     * Directly handles ElasticTree-specific messages received from the LAVI 
     * backend and delegates handling of other messages to super.process().
     */
    public void process(final LAVIMessage msg) {
        switch(msg.type) {
        case SWITCHES_ADD:
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
         * previous run is complete.
         */
        public synchronized void setNextTrafficMatrix(ETTrafficMatrix tm) {
            this.tmNext = new ETTrafficMatrix(tm.use_hw, tm.may_split_flows, tm.k, tm.demand, tm.edge, tm.agg, tm.plen);;
        }
        
        /** Sends the next traffic matrix to the server. */
        private synchronized boolean sendNextTrafficMatrix() {
            if(tmOutstanding != null && tmOutstanding.k != tmNext.k) {
                try {
                    cleanup();
                    conn.sendLAVIMessage(new ETSwitchesRequest(tmNext.k));
                }
                catch(IOException e) {}
            }
            tmOutstanding = tmNext;
            manager.setNextTrafficMatrixText(tmOutstanding);
            
            try {
                conn.sendLAVIMessage(tmOutstanding);
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

    public void trafficMatrixChanged(ETTrafficMatrix tm) {
        if(tmManager != null)
            tmManager.setNextTrafficMatrix(tm);
    }
    
    
    // --- Elastic Tree Message Processing -- //
    // ************************************** //    
    
    private void processLinkUtils(ETLinkUtilsList msg) {
        double total_bps = 0;
        for(ETLinkUtil x : msg.utils)
            total_bps += processLinkUtil(x.srcDPID, x.srcPort, x.dstDPID, x.dstPort, x.util, msg.timeCreated);
        manager.setExpectedAggregateThroughput(total_bps);
    }
    
    /**
     * Updates the throughput data of the link described by these parameters.
     */
    private double processLinkUtil(long dstDPID, short dstPort, long srcDPID, short srcPort, float util, long when) {
        OpenFlowSwitch srcSwitch = switchesMap.get(srcDPID);
        if(srcSwitch == null) {
            logLinkMissing("util", "src switch", dstDPID, dstPort, srcDPID, srcPort);
            return 0;
        }
        
        OpenFlowSwitch dstSwitch = switchesMap.get(dstDPID);
        if(dstSwitch == null) {
            logLinkMissing("util", "dst switch", dstDPID, dstPort, srcDPID, srcPort);
            return 0;
        }
        
        Link existingLink = dstSwitch.getLinkTo(dstPort, srcSwitch, srcPort);
        if(existingLink != null) {
            PortStatsRates psr = existingLink.getStats(Match.MATCH_ALL);
            if(psr == null)
                psr = existingLink.trackStats(Match.MATCH_ALL);
            
            double bps = util * existingLink.getMaximumDataRate();
            double pps = bps / (1500*8); // assumes 1500B packets
            psr.setRates(pps, bps, 0, when);
            
            existingLink.setColor();
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
        for(OpenFlowSwitch o : switchesMap.values())
            o.setOff(false);
        
        // turn off the specified switches
        for(long dpid : msg.dpids) {
            OpenFlowSwitch o = switchesMap.get(dpid);
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
        manager.noteResult(msg.num_unplaced_flows);
    }
    
    private void processFailEvent(Drawable d) {
        if(d instanceof OpenFlowSwitch) {
            OpenFlowSwitch o = (OpenFlowSwitch)d;
            o.setFailed(!o.isFailed());
            
            try {
                conn.sendLAVIMessage(new ETSwitchFailureChange(o.getDatapathID(), o.isFailed()));    
            }
            catch(IOException e) {
                System.err.println("failed to send switch failure");
            }
        }
        else if(d instanceof Link) {
            Link l = (Link)d;
            l.setFailed(!l.isFailed());
            try {
                conn.sendLAVIMessage(new ETLinkFailureChange(new org.openflow.lavi.net.protocol.Link(
                        l.getSource().getDatapathID(),
                        l.getMyPort(l.getSource()),
                        l.getDestination().getDatapathID(),
                        l.getMyPort(l.getDestination())), l.isFailed()));    
            }
            catch(IOException e) {
                System.err.println("failed to send link failure");
            }
        }
    }
}
