package org.openflow.lavi;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openflow.lavi.drawables.*;
import org.openflow.lavi.drawables.Link;
import org.openflow.lavi.drawables.Link.LinkExistsException;
import org.openflow.lavi.net.*;
import org.openflow.lavi.net.protocol.*;
import org.openflow.lavi.net.protocol.auth.*;
import org.openflow.lavi.stats.PortStatsRates;
import org.openflow.protocol.*;
import org.openflow.util.string.DPIDUtil;
import org.pzgui.DialogHelper;
import org.pzgui.PZClosing;
import org.pzgui.PZManager;
import org.pzgui.layout.ElasticTreeManager;
import org.pzgui.layout.TrafficMatrixChangeListener;

public class LAVI  implements LAVIMessageProcessor, PZClosing, TrafficMatrixChangeListener {
    /** run the LAVI front-end */
    public static void main(String args[]) {
        String server = null;
        if(args.length > 0)
            server = args[0];
        
        Short port = null;
        new LAVI(server, port);
    }
    
    /** connection to the backend */
    private final LAVIConnection conn;
    
    /** the GUI window manager */
    private final ElasticTreeManager manager;
    
    /** how often to refresh basic port statistics */
    private int statsRefreshRate_msec = 2000;
    
    /** start the LAVI front-end */
    public LAVI(String server, Short port) {
        // ask the user for the NOX controller's IP if it wasn't already given
        if(server == null || server.length()==0)
            server = DialogHelper.getInput("What is the IP or hostname of the NOX server?", "mvm-nox2.stanford.edu");

        if(server == null) {
            System.out.println("Goodbye");
            System.exit(0);
        }
        
        if(port == null)
            conn = new LAVIConnection(this, server);
        else
            conn = new LAVIConnection(this, server, port);

        // fire up the GUI
        manager = new ElasticTreeManager(6);
        manager.addClosingListener(this);
        manager.addTrafficMatrixChangeListener(this);
        manager.start();
        
        // try to connect to the backend
        conn.start();
        
        // start the update sender
        tmSender.start();
    }
    
    /** shutdown the connection */
    public void pzClosing(PZManager manager) {
        long start = System.currentTimeMillis();
        conn.shutdown();
        Thread.yield();

        // wait until the connection has been torn down or 1sec has passed
        while(!conn.isShutdown() && System.currentTimeMillis()-start<1000) {}
    }
    
    /** Called when the LAVI backend has been disconnected or reconnected */
    public void connectionStateChange() {
        if(!conn.isConnected()) {
            // remove all switches when we get disconnected
            for(Long d : switchesList)
                disconnectSwitch(d);
        }
    }

    /** Handles messages received from the LAVI backend */
    public void process(final LAVIMessage msg) {
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
            
        case ET_LINK_UTILS:
            processLinkUtils((ETLinkUtilsList)msg);
            break;
            
        case ET_POWER_USAGE:
            processPowerUsage((ETPowerUsage)msg);
            break;
            
        case AUTH_REPLY:
        case SWITCHES_REQUEST:
        case LINKS_REQUEST:
        case STAT_REQUEST:
        case ET_TRAFFIX_MATRIX:
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
            conn.sendLAVIMessage(new AuthPlainText(username, pw));
        }
        catch(IOException e) {
            System.err.println("Failed to send plain-text authentication reply");
            conn.reconnect();
        }
    }

    /** switches in the topology */
    private final ConcurrentHashMap<Long, OpenFlowSwitch> switchesMap = new ConcurrentHashMap<Long, OpenFlowSwitch>();
    private final CopyOnWriteArrayList<Long> switchesList = new CopyOnWriteArrayList<Long>();
    
    /** list of switches which should be displayed as one or more virtual switches */
    private final ConcurrentHashMap<Long, VirtualSwitchSpecification> virtualSwitches = new ConcurrentHashMap<Long, VirtualSwitchSpecification>();
    
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
        VirtualSwitchSpecification v = virtualSwitches.get(s.getDatapathID());
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
        VirtualSwitchSpecification v = virtualSwitches.get(s.getDatapathID());
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
        
        // get the links associated with this switch
        try {
            conn.sendLAVIMessage(new LinksRequest(dpid));
        } catch (IOException e) {
            System.err.println("Warning: unable to get switches for switch + " + DPIDUtil.toString(dpid));
        }
        
        try {
            conn.sendLAVIMessage(new SwitchDescriptionRequest(dpid));
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
        VirtualSwitchSpecification vDst = virtualSwitches.get(dst.getDatapathID());
        if(vDst != null) {
            dst = vDst.getVirtualSwitchByPort(dstPort);
            if(dst == null)
                return null; /* ignore unvirtualized ports on a display virtualized switch */
        }
        
        VirtualSwitchSpecification vSrc = virtualSwitches.get(src.getDatapathID());
        if(vSrc != null) {
            src = vSrc.getVirtualSwitchByPort(srcPort);
            if(src == null)
                return null; /* ignore unvirtualized ports on a display virtualized switch */
        }
        
        Link l = new Link(dst, dstPort, src, srcPort);
        return l;
    }
    
    private void processLinksAdd(LinksAdd msg) {
        for(org.openflow.lavi.net.protocol.Link x : msg.links) {
            OpenFlowSwitch dstSwitch = handleLinkToSwitch(x.dstDPID);
            OpenFlowSwitch srcSwitch = handleLinkToSwitch(x.srcDPID);
            try {
                Link l = addLink(dstSwitch, x.dstPort, srcSwitch, x.srcPort);
                if(l == null)
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
        for(org.openflow.lavi.net.protocol.Link x : msg.links)
            disconnectLink(x.dstDPID, x.dstPort, x.srcDPID, x.srcPort);
    }
    
    /** remove a link from the topology */
    private void disconnectLink(long dstDPID, short dstPort, long srcDPID, short srcPort) {
        OpenFlowSwitch srcSwitch = switchesMap.get(srcDPID);
        if(srcSwitch == null) {
            logLinkMissing("src switch", dstDPID, dstPort, srcDPID, srcPort);
            return;
        }
        
        OpenFlowSwitch dstSwitch = switchesMap.get(dstDPID);
        if(dstSwitch == null) {
            logLinkMissing("dst switch", dstDPID, dstPort, srcDPID, srcPort);
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
            logLinkMissing("link", dstDPID, dstPort, srcDPID, srcPort);
    }
    
    /** Prints an error message about a missing link. */
    private void logLinkMissing(String why, long dstDPID, short dstPort, long srcDPID, short srcPort) {
        System.err.println("Ignoring link delete message for non-existant " + why + ": " + 
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
        LAVIMessage msg = conn.popAssociatedStatefulRequest(reply.xid);
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

    
    /**
     * Periodically sends any update to the traffic matrix.
     */
    private class TrafficMatrixUpdateSender extends Thread {
        private static final int MIN_DELAY_BTWN_UPDATES_MS = 3000;
        private ETTrafficMatrix tmNext = null;
        private ETTrafficMatrix tmLastSent = null;
        private long lastSentTime = 0;
        
        
        public synchronized void setTrafficMatrix(ETTrafficMatrix tm) {
            this.tmNext = tm;
            manager.setNextTrafficMatrixText(tm);
        }
        
        public void run() {
            while(true) {
                // wait until it is appropriate to send another update
                long now = System.currentTimeMillis();
                while(lastSentTime + MIN_DELAY_BTWN_UPDATES_MS > now) {
                    try {
                        Thread.sleep(lastSentTime + MIN_DELAY_BTWN_UPDATES_MS - now);
                    }
                    catch(InterruptedException e) {}
                    now = System.currentTimeMillis();
                }
                
                if(!send())
                    System.err.println("unable to send the change");
                else
                    lastSentTime = System.currentTimeMillis();
            }
        }
    
        private synchronized boolean send() {
            ETTrafficMatrix tm;
            if(tmNext == null)
                return true;
            else if(tmNext.equals(tmLastSent))
                return true;
            else
                tm = tmNext;
            
            try {
                conn.sendLAVIMessage(tm);
            } catch (IOException e) {
                System.err.println("Warning: unable to send traffix matrix update: " + tm);
                return false;
            }
            manager.setCurrentTrafficMatrixText(tm);
            manager.setNextTrafficMatrixText(null);
            tmNext = null;
            tmLastSent = tm;
            return true;
        }
    }
    
    private final TrafficMatrixUpdateSender tmSender = new TrafficMatrixUpdateSender();
    
    public void trafficMatrixChanged(ETTrafficMatrix tm) {
        tmSender.setTrafficMatrix(tm);
    }

    private void processLinkUtils(ETLinkUtilsList msg) {
        for(org.openflow.lavi.net.protocol.ETLinkUtil x : msg.utils)
            processLinkUtil(x.srcDPID, x.srcPort, x.dstDPID, x.dstPort, x.util, msg.timeCreated);
    }
    
    private void processLinkUtil(long dstDPID, short dstPort, long srcDPID, short srcPort, float util, long when) {
        OpenFlowSwitch srcSwitch = switchesMap.get(srcDPID);
        if(srcSwitch == null) {
            logLinkMissing("src switch", dstDPID, dstPort, srcDPID, srcPort);
            return;
        }
        
        OpenFlowSwitch dstSwitch = switchesMap.get(dstDPID);
        if(dstSwitch == null) {
            logLinkMissing("dst switch", dstDPID, dstPort, srcDPID, srcPort);
            return;
        }
        
        Link existingLink = dstSwitch.getLinkTo(dstPort, srcSwitch, srcPort);
        if(existingLink != null) {
            PortStatsRates psr = existingLink.getStats(Match.MATCH_ALL);
            if(psr == null)
                psr = existingLink.trackStats(Match.MATCH_ALL);
            
            double bps = util * existingLink.getMaximumDataRate();
            double pps = bps / (1500*8); // assumes 1500B packets
            psr.setRates(pps, bps, 0, when);
        }
        else
            logLinkMissing("link", dstDPID, dstPort, srcDPID, srcPort);
    }

    private void processPowerUsage(ETPowerUsage msg) {
        manager.setPowerData(msg.watts_current, msg.watts_traditional, msg.watts_max);
    }
}
