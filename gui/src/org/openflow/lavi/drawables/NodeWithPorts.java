package org.openflow.lavi.drawables;

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openflow.lavi.Options;
import org.openflow.util.LongPair;

/**
 * Information about a node with ports in the topology.
 * @author David Underhill
 */
public abstract class NodeWithPorts extends Node {
    /**
     * These are the links attached to this node.  The CopyOnWriteArrayList 
     * makes accesses and traversals very quick and thread-safe at the expense
     * of making mutations expensive.  Mutations should be rare, so this should
     * be a very efficient data structure for our needs.
     */
    private final CopyOnWriteArrayList<Link> links = new CopyOnWriteArrayList<Link>();
    
    public NodeWithPorts(String name, int x, int y) {
        super(name, x, y);
    }
    
    public void unsetDrawn() {
        super.unsetDrawn();
        for(Link l : links)
            l.unsetDrawn();
        
    }
    
    /** draws the links associated with this node */
    public void drawLinks(Graphics2D gfx) {
        HashMap<LongPair, Integer> dpidToCount = new HashMap<LongPair, Integer>();
        Integer count;
        
        // draw the port's links
        for(Link link : links) {
            LongPair lp = new LongPair(link.getSource().getDatapathID(),
                                       link.getDestination().getDatapathID());
            
            count = dpidToCount.get(lp);
            if(count == null)
                count = 0;
            
            link.setOffset(count);
            link.drawObject(gfx);
            
            dpidToCount.put(lp, count + 1);
        }
    }
    
    /**
     * Adds the link to this node
     * @param l  the link to add
     */
    void addLink(Link l) {
        links.add(l);
    }
    
    /** Returns whether the specified port is currently connected to a link. */
    public boolean isPortUsed(short portNum) {
        // we could use a concurrent hash map to track which ports are used 
        // rather than doing a linear search each time, but the number of ports
        // and times this method is called is likely small enough to not benefit
        // from a ConcurrentHashMap caching the results
        for(Link l : links)
            if(l.getMyPort(this) == portNum)
                return true;
        
        return false;
    }
    
    /** returns a list of all the links on this node */
    public Collection<Link> getEdges() {
        return links;
    }
    
    /** returns a list of all the links on this node */
    public Collection<Link> getLinks() {
        return links;
    }
    
    /** Returns the number of links connected to this node. */
    public int getNumLinks() {
        return links.size();
    }

    /** Gets the link from this node on outPort */
    public Link getLinkFrom(short outPort) {
        for(Link l : links)
            if(l.getMyPort(this) == outPort)
                return l;
        
        return null;
    }
    
    /** Returns a link from this node to the requested node if such a link exists */
    public Link getLinkTo(NodeWithPorts n) {
        for(Link l : links)
            if(l.getOther(this) == n)
                return l;
            
        return null;
    }

    /** Returns a link from this node to the requested node if such a link exists */
    public Link getLinkTo(short myPort, NodeWithPorts n, short nPort) {
        for(Link l : links)
            if(l.getOther(this) == n)
                if(l.getMyPort(this)==myPort && l.getMyPort(n)==nPort)
                    return l;
            
        return null;
    }
    
    /** Returns a link from this node to the requested node if such a link exists */
    public Link getDirectedLinkTo(short myPort, NodeWithPorts n, short nPort, boolean nIsDestination) {
        if(!Options.USE_DIRECTED_LINKS)
            return getLinkTo(myPort, n, nPort);
        
        for(Link l : links)
            if((nIsDestination && l.getDestination()==n) || (!nIsDestination && l.getSource()==n))
                if(l.getMyPort(this)==myPort && l.getMyPort(n)==nPort)
                    return l;
            
        return null;
    }
    
    /** Gets a link to a neighboring OpenFlowNode with the specified datapath ID. */
    public Link getLinkTo(long dpid) {
        for(Link l : links) {
            NodeWithPorts o = l.getOther(this);
            if(o!=null && o.getDatapathID()==dpid)
                return l;
        }
        
        return null;
    }
    
    /** Returns a unique ID for this node */
    public abstract long getDatapathID();
    
    public int hashCode() {
        return (int)(getDatapathID() ^ (getDatapathID() >>> 32));
    }
    
    public boolean equals(Object o) {
        if(this == o) return true;
        if((o == null) || (o.getClass() != this.getClass())) return false;
        NodeWithPorts n = (NodeWithPorts)o;
        return n.getDatapathID() == getDatapathID();
    }
}
