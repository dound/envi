package org.openflow.lavi;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openflow.lavi.drawables.*;
import org.openflow.lavi.drawables.Link;
import org.openflow.lavi.drawables.Link.LinkExistsException;
import org.openflow.lavi.drawables.NodeWithPorts.PortUsedException;
import org.openflow.lavi.net.*;
import org.openflow.lavi.net.protocol.*;
import org.openflow.lavi.net.protocol.auth.*;
import org.openflow.protocol.*;
import org.openflow.util.string.DPIDUtil;
import org.pzgui.DialogHelper;
import org.pzgui.PZClosing;
import org.pzgui.PZManager;
import org.pzgui.layout.Edge;
import org.pzgui.layout.PZLayoutManager;
import org.pzgui.layout.Vertex;

public class LAVI implements LAVIMessageProcessor, PZClosing {
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
    private final PZLayoutManager manager;
    
    /** how often to refresh basic port statistics */
    private long statsRefreshRate_msec = 2000;
    
    /** start the LAVI front-end */
    public LAVI(String server, Short port) {
        // ask the user for the NOX controller's IP if it wasn't already given
        if(server == null || server.length()==0)
            server = DialogHelper.getInput("What is the IP or hostname of the NOX server?", "mvm-10g2.stanford.edu");

        if(port == null)
            conn = new LAVIConnection(this, server);
        else
            conn = new LAVIConnection(this, server, port);

        // fire up the GUI
        manager = new PZLayoutManager();
        manager.addClosingListener(this);
        manager.start();
        
        // try to connect to the backend
        conn.start();
        
        // layout the nodes with the spring algorithm by default
        manager.setLayout(new edu.uci.ics.jung.algorithms.layout.SpringLayout2<Vertex, Edge>(manager.getGraph()));
        
        // TODO: add some sort of Event thread like swing has to process timed reqs? 
        new Thread() {
            public void run() {
                while(true) {
                    // only run periodically
                    try {
                        Thread.sleep(statsRefreshRate_msec);
                    }
                    catch(InterruptedException e) {}
                
                    try {
                        if(conn.isConnected())
                            for(Long dpid : switchesList)
                                updateStatsForSwitch(dpid);
                    }
                    catch(IOException e) {
                        System.err.println("Unable to request stats: " + e.getMessage());
                    }
                }
            }
        }.start();
    }

    /** sends an AggregateStatsRequest for each port on the switch at this dpid */
    public void updateStatsForSwitch(Long dpid) throws IOException { 
        OpenFlowSwitch o = switchesMap.get(dpid);
        if(o == null) return;
        
        for(Link l : o.getLinks())
            conn.sendLAVIMessage(new AggregateStatsRequest(dpid, l.getMyPort(o)));
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
    
    private OpenFlowSwitch addSwitch(long dpid) {
        OpenFlowSwitch s = new OpenFlowSwitch(dpid);
        switchesMap.put(dpid, s);
        switchesList.add(dpid);
        s.setPos((int)Math.random()*500, (int)Math.random()*500);
        manager.addDrawable(s);
        
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
            manager.removeDrawable(switchesMap.remove(dpid)); 
            switchesList.remove(dpid);
            
            // disconnect all links associated with the switch too
            for(Link l : s.getLinks())
                l.disconnect();
            
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
    
    private void processLinksAdd(LinksAdd msg) {
        for(org.openflow.lavi.net.protocol.Link x : msg.links) {
            OpenFlowSwitch dstSwitch = handleLinkToSwitch(x.dstDPID);
            OpenFlowSwitch srcSwitch = handleLinkToSwitch(x.srcDPID);
            try {
                new Link(dstSwitch, x.dstPort, srcSwitch, x.srcPort);
            }
            catch(LinkExistsException e) {
                // ignore 
            }
            catch(PortUsedException e) {
                System.err.println("Could not add link: " + e.getMessage());
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
        if(existingLink != null)
            existingLink.disconnect();
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
            System.err.println("Warning: matching stateful request for AggregateStatsReply is not an AggregateStatsRequest");
            return;
        }
        req = (AggregateStatsRequest)msg;
        
        // get the switch associated with these stats
        OpenFlowSwitch s = switchesMap.get(req.dpid);
        if(s == null)
            System.err.println("Warning: received aggregate stats reply for unknown switch " + DPIDUtil.toString(req.dpid));
        
        Link l = s.getLinkFrom(req.outPort);
        if(l == null)
            System.err.println("Warning: received aggregate stats reply for disconnect port " + req.outPort + " for switch " + DPIDUtil.toString(req.dpid));
        
        l.updateStats(reply);
    }

    private void processStatReplyDesc(SwitchDescriptionStats msg) {
        OpenFlowSwitch s = switchesMap.get(msg.dpid);
        if(s != null)
            s.setSwitchDescription(msg);
        else
            System.err.println("Warning: received switch description for unknown switch " + DPIDUtil.toString(msg.dpid));
    }
}
