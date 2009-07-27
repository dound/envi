package org.openflow.gui;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openflow.gui.drawables.Flow;
import org.openflow.gui.drawables.Link;
import org.openflow.gui.drawables.NodeWithPorts;
import org.openflow.gui.drawables.Link.LinkExistsException;
import org.openflow.gui.net.BackendConnection;
import org.openflow.gui.net.protocol.LinkType;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.util.FlowHop;
import org.openflow.util.Pair;
import org.openflow.util.RefTrack;
import org.pzgui.PZManager;

/**
 * Encapsulates the nodes in a topology.  May be updated with data from multiple
 * connections.
 * 
 * @author David Underhill
 */
public class Topology {
    /** Construct a new, empty Topology. */
    public Topology(final PZManager manager) {
        nodesMap = new ConcurrentHashMap<Long, NodeRefTrack>();
        nodesList = new CopyOnWriteArrayList<Long>();
        virtualNodes = new ConcurrentHashMap<Long, VirtualSwitchSpecification>();
        this.manager = manager;
    }
    
    
    // -------------- Global Node Tracking ------------- //
    
    /** 
     * Simple extension of RefTrack for the particular set of parameters used
     * in Topology to make he code more readable.
     */
    private static class NodeRefTrack extends RefTrack<NodeWithPorts, BackendConnection<OFGMessage>> {
        public NodeRefTrack(NodeWithPorts objToTrack, BackendConnection<OFGMessage> ref) {
            super(objToTrack, ref);
        }
    }
    
    /** 
     * A global list of nodes in all topologies.  The keys are datapath IDs.
     * The values are DrawableRefTrack objects which is simply a node 
     * and the list of connections which supply information about it.
     */
    private static final ConcurrentHashMap<Long, NodeRefTrack> globalNodes;
    static { globalNodes = new ConcurrentHashMap<Long, NodeRefTrack>(); }
    
    /** 
     * A lock to prevent a race condition between remove old NodeRefTrack and
     * adding new ones.  
     */
    private static final Object globalNodesWriterLock = new Object();
    
    
    // ---------------- Node Tracking --------------- //
    
    /** nodes in this topology */
    private final ConcurrentHashMap<Long, NodeRefTrack> nodesMap;
    
    /** IDs of nodes in this topology */
    private final CopyOnWriteArrayList<Long> nodesList;
    
    /**
     * Adds a node to this topology.
     * 
     * @param owner  the connection which supplies information about this node 
     * @param n      the node to add
     * @return  true if the node was added, false it was already present in some
     *          topology (not necessarily this one)
     */
    public boolean addNode(BackendConnection<OFGMessage> owner, NodeWithPorts n) {
        boolean ret = false;
        Long id = n.getID();
        NodeRefTrack localR = nodesMap.get(id);
        if(localR == null) {
            synchronized(globalNodesWriterLock) {
                NodeRefTrack r = globalNodes.get(id);
                if(r == null) {
                    globalNodes.put(id, new NodeRefTrack(n, owner));
                    ret = true;
                }
                else {
                    r.addRef(owner);
                    n = r.obj; // use the existing node
                }
            }
            
            nodesMap.put(id, new NodeRefTrack(n, owner));
            nodesList.add(id);
            addNodeToManager(n);
        }
        else
            localR.addRef(owner);
        
        return ret;
    }
    
    /**
     * Gets the node with the specified ID, if any such node exists in this
     * topology.
     * 
     * @return the NodeWithPorts with the requested ID, or null if no such node exists
     */
    public NodeWithPorts getNode(Long id) {
        NodeRefTrack r = nodesMap.get(id);
        return r==null ? null : r.obj;
    }
    
    /** Gets the set of IDs currently in the topology */
    public Set<Long> getNodeIDs() {
        return nodesMap.keySet();
    }

    /**
     * Gets whether this topology has a node with the specified ID.
     */
    public boolean hasNode(Long id) {
        return getNode(id) != null;
    }
    
    /**
     * Gets the node with the specified ID, if any such node exists in this
     * topology.
     * 
     * @return the NodeWithPorts with the requested ID, or null if no such node exists
     */
    public static NodeWithPorts globalGetNode(Long id) {
        NodeRefTrack r = globalNodes.get(id);
        return r==null ? null : r.obj;
    }
    
    /** Gets the set of IDs from all nodes in all topologies */
    public Set<Long> globalGetNodeIDs() {
        return globalNodes.keySet();
    }
    
    /**
     * Cleanup state associated only with the specified owner (presumably 
     * because that connection is no longer connected).
     * 
     * @param owner  the owning connection
     */
    public void removeAllNodes(BackendConnection<OFGMessage> owner) {
        // remove all switches when we get disconnected
        for(Long d : nodesList)
            removeNode(owner, d);
    }
    
    /**
     * Remove the reference from owner to the node with the specified id.  If 
     * that was the last references to the node, then the node is cleaned up.
     * 
     * @param owner  the owning connection
     * @param id     ID of the node being removed
     * 
     * @return -1 if id is not in this topology
     *          0 other connections still refer to it (so it remains in the topology)
     *          1 if it has been removed from this topology (but still exists globally)
     *          2 if it has been removed from all topologies
     */
    public int removeNode(BackendConnection<OFGMessage> owner, Long id) {
        // determine whether this is the last reference and remove it safely if so
        NodeRefTrack localR = nodesMap.get(id);
        if(localR == null)
            return -1; // not in this topology
        
        // remove it from this topology
        int ret = 0; // remains in local topologies (others refer to it)
        if(localR.removeRef(owner)) {
            nodesList.remove(id);
            nodesMap.remove(id);
            removeNodeFromManager(localR.obj);
            ret = 1; // no referants remain in the local topology
        }
        
        // remove the reference from the global topology list too
        synchronized(globalNodesWriterLock) {
            NodeRefTrack r = globalNodes.get(id);
            if(r.removeRef(owner)) {
                globalNodes.remove(id);
                
                // disconnect all links associated with the switch too
                for(Link l : r.obj.getLinks()) {
                    try {
                        l.disconnect(owner);
                    } 
                    catch(IOException e) {
                        // ignore: connection down => polling messages cleared on the backend already
                    }
                }
                
                return 2; // removed from all topologies
            }
            else
                return ret;
        }
    }
    
    
    // ---------------- Link Tracking --------------- //
    
    public Link addLink(LinkType linkType, NodeWithPorts dst, short dstPort, NodeWithPorts src, short srcPort) throws LinkExistsException {
        VirtualSwitchSpecification vDst = virtualNodes.get(dst.getID());
        if(vDst != null) {
            dst = vDst.getVirtualSwitchByPort(dstPort);
            if(dst == null)
                return null; /* ignore unvirtualized ports on a display virtualized switch */
        }
        
        VirtualSwitchSpecification vSrc = virtualNodes.get(src.getID());
        if(vSrc != null) {
            src = vSrc.getVirtualSwitchByPort(srcPort);
            if(src == null)
                return null; /* ignore unvirtualized ports on a display virtualized switch */
        }
        
        Link l = new Link(linkType, dst, dstPort, src, srcPort);
        return l;
    }
    
    /** 
     * Remove a link from the topology.
     * 
     * @return   0 on success
     *          -1 if the source node does not exist
     *          -2 if the destination node does not exist
     *          -3 if the nodes are found but the link does not exist
     */
    public int disconnectLink(BackendConnection<OFGMessage> conn,
                              long dstDPID, short dstPort, long srcDPID, short srcPort) {
        NodeWithPorts srcNode = getNode(srcDPID);
        if(srcNode == null)
            return -1; // missing src node
        
        NodeWithPorts dstNode = getNode(dstDPID);
        if(dstNode == null)
            return -2; // missing dst node
        
        Link existingLink = dstNode.getLinkTo(dstPort, srcNode, srcPort);
        if(existingLink != null) {
            try {
                existingLink.disconnect(conn);
            } 
            catch(IOException e) {
                // ignore: connection down => polling messages cleared on the backend already
            }
            
            return 0;
        }
        else
            return -3;
    }
    
    
    // ------------- Graphics Management ------------ //
    
    /** the manager which is responsible for drawing nodes in this topology */
    private final PZManager manager;
    
    /** Tells the manager to draw a noed (or its virtualized switches if it is virtualized). */
    private void addNodeToManager(NodeWithPorts s) {
        VirtualSwitchSpecification v = virtualNodes.get(s.getID());
        if(v == null) {
            manager.addDrawable(s);
        }
        else {
            for(int i=0; i<v.getNumVirtualSwitches(); i++)
                manager.addDrawable(v.getVirtualSwitch(i).v);
        }
    }
    
    /** Tells the manager to stop drawing a node (or its virtualized switches if it is virtualized). */
    private void removeNodeFromManager(NodeWithPorts s) {
        VirtualSwitchSpecification v = virtualNodes.get(s.getID());
        if(v == null) {
            manager.removeDrawable(s);
        }
        else {
            for(int i=0; i<v.getNumVirtualSwitches(); i++)
                manager.removeDrawable(v.getVirtualSwitch(i).v);
        }
    }

    
    //  Gfx Virtualization  (draw many nodes as one)  //
    
    /** list of switches which should be displayed as one or more virtual switches */
    private final ConcurrentHashMap<Long, VirtualSwitchSpecification> virtualNodes;
    
    /** add a new display virtualization scheme for a node */
    public void addVirtualizedDisplay(VirtualSwitchSpecification v) {
        NodeRefTrack r = nodesMap.get(v.getParentDPID());
        if(r != null)
            removeNodeFromManager(r.obj);
        
        virtualNodes.put(v.getParentDPID(), v);
        if(r != null)
            addNodeToManager(r.obj);
    }
    
    /** remove an existing display virtualization scheme for a node; returns true if such a scheme existed */
    public boolean removeVirtualizedDisplay(Long dpid) {
        NodeRefTrack r = nodesMap.get(dpid);
        if(r != null)
            removeNodeFromManager(r.obj);
        
        return virtualNodes.remove(dpid) != null;
    }

    
    /** flows in the topology */
    private final ConcurrentHashMap<Integer, Flow[]> flowsMap = new ConcurrentHashMap<Integer, Flow[]>();
    
    /** add a flow to the topology */
    public void addFlow(Flow newFlow) {
        Flow[] flows = flowsMap.get(newFlow.getID());
        if(flows == null)
            flowsMap.put(newFlow.getID(), new Flow[]{newFlow});
        else {
            // flow(s) with this ID already exist; add it to the list
            Flow[] newFlows = new Flow[flows.length + 1];
            System.arraycopy(flows, 0, newFlows, 0, flows.length);
            newFlows[flows.length] =  newFlow;
            
            // ignore new flow segments which overlap with others that share its ID
            for(Flow f : flows) {
                for(int i=0; i<f.getPath().length-1; i++) {
                    Pair<FlowHop, FlowHop> segment = new Pair<FlowHop, FlowHop>(f.getPath()[i], f.getPath()[i+1]);
                    if(newFlow.hasSegment(segment))
                        newFlow.ignoreSegment(segment);
                }
            }
        }
        manager.addDrawable(newFlow);
    }
    
    /**
     * Gets the set of flow(s) with the specified ID, if any such flow(s) exist
     * in this topology.
     * 
     * @return the Flows with the requested ID, or null if no such flows exist
     */
    public Flow[] getFlow(Integer id) {
        return flowsMap.get(id);
    }
    
    /** Gets the set of flow IDs currently in the topology */
    public Set<Integer> getFlowIDs() {
        return flowsMap.keySet();
    }

    /**
     * Gets whether this topology has a flow with the specified ID.
     */
    public boolean hasFlow(Integer id) {
        return getFlow(id) != null;
    }

    /** remove a flow from the topology */
    public void removeFlowByID(int id) {
        Flow[] flows = flowsMap.remove(id);
        if(flows != null)
            for(Flow f : flows)
                manager.removeDrawable(f);
    }
}
