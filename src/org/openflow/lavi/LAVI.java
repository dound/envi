package org.openflow.lavi;

import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import org.openflow.lavi.drawables.*;
import org.openflow.lavi.drawables.Link;
import org.openflow.lavi.net.*;
import org.openflow.lavi.net.protocol.*;
import org.openflow.lavi.net.protocol.auth.*;
import org.openflow.protocol.SwitchDescriptionStats;
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
        Short port = null;
        
        new LAVI(server, port);
    }
    
    /** connection to the backend */
    private final LAVIConnection conn;
    
    /** the GUI window manager */
    private final PZLayoutManager manager;
    
    /** start the LAVI front-end */
    public LAVI(String server, Short port) {
        // ask the user for the NOX controller's IP if it wasn't already given
        if(server == null || server.length()==0)
            server = DialogHelper.getInput("What is the IP or hostname of the NOX server?", "noxtrial.stanford.edu");

        if(port == null)
            conn = new LAVIConnection(this, server);
        else
            conn = new LAVIConnection(this, server, port);

        // try to connect to the backend
        conn.start();

        // fire up the GUI
        manager = new PZLayoutManager();
        manager.addClosingListener(this);
        manager.setLayout(new FRLayout2<Vertex, Edge>(manager.getGraph()));
        manager.start();
    }

    /** shutdown the connection */
    public void pzClosing(PZManager manager) {
        long start = System.currentTimeMillis();
        conn.shutdown();
        Thread.yield();

        // wait until the connection has been torn down or 1sec has passed
        while(!conn.isShutdown() && System.currentTimeMillis()-start<1000) {}
    }

    /** Handles messages received from the LAVI backend */
    public void process(final LAVIMessage msg) {
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
            new AuthPlainText(username, pw).write(conn.getStream());
        }
        catch(IOException e) {
            System.err.println("Failed to send plain-text authentication reply");
            conn.reconnect();
        }
    }

    /** switches in the topology */
    private HashMap<Long, OpenFlowSwitch> switches = new HashMap<Long, OpenFlowSwitch>(); 
    
    private OpenFlowSwitch addSwitch(long dpid) {
        OpenFlowSwitch s = new OpenFlowSwitch(dpid);
        switches.put(dpid, s);
        manager.addDrawable(s);
        return s;
    }
    
    /** add new switches to the topology */
    private void processSwitchesAdd(SwitchesAdd msg) {
        for(long dpid : msg.dpids)
            if(!switches.containsKey(dpid))
                addSwitch(dpid);
        
    }

    /** remove former switches from the topology */
    private void processSwitchesDel(SwitchesDel msg) {
        for(long dpid : msg.dpids)
            if(switches.containsKey(dpid))
                manager.removeDrawable(switches.remove(dpid));
    }
    
    /** links in the topology */
    private HashSet<Link> links = new HashSet<Link>();
    
    /** 
     * Makes sure a switch with the specified exists and creates one if not.  
     * 
     * @param dpid  DPID of the switch which should exist for a link
     * @return the switch associated with dpid
     */
    private OpenFlowSwitch handleLinkToSwitch(long dpid) {
        OpenFlowSwitch s = switches.get(dpid);
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
            Link newLink = new Link(dstSwitch, x.dstPort, srcSwitch, x.srcPort);
            if(!links.contains(newLink))
                links.add(newLink);
            else
                newLink.disconnect(); // already existed
        }
    }
    
    private void processLinksDel(LinksDel msg) {
        for(org.openflow.lavi.net.protocol.Link x : msg.links) {
            OpenFlowSwitch dstSwitch = handleLinkToSwitch(x.dstDPID);
            OpenFlowSwitch srcSwitch = handleLinkToSwitch(x.srcDPID);
            
            Link existingLink = dstSwitch.getLinkTo(x.dstPort, srcSwitch, x.srcPort);
            if(existingLink != null) {
                existingLink.disconnect();
                links.remove(existingLink);
            }
        }
    }

    private void processStatReply(StatsHeader msg) {
        switch(msg.statsType) {
        case DESC:
            processStatReplyDesc((SwitchDescriptionStats)msg);
            break;
            
        case AGGREGATE:
            System.err.println("Warning: aggregate stats reply not yet handled");
            break;
        
        default:
            System.err.println("Unhandled stats type received: " + msg.statsType.toString());
        }
    }

    private void processStatReplyDesc(SwitchDescriptionStats msg) {
        OpenFlowSwitch s = switches.get(msg.dpid);
        if(s != null)
            s.setSwitchDescription(msg);
        else
            System.err.println("Warning: received switch description for unknown switch " + DPIDUtil.toString(msg.dpid));
    }
}
