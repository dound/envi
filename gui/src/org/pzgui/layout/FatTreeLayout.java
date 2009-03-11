package org.pzgui.layout;

import java.util.HashMap;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;

public class FatTreeLayout<V extends Vertex, E> extends AbstractLayout<V, E> implements IterativeContext {
    private int k;
    
    private HashMap<Integer, V> nodeIDs = new HashMap<Integer, V>();
    private int nextID = 0;
    private boolean done = false;
    
    /**
     * Constructs a new Fat Tree layout engine.
     */
    public FatTreeLayout(Graph<V,E> graph, int k) {
        super(graph);
        this.k = k;
    }

    /** This method always returns whether the fat tree has been laid out. */
    public synchronized boolean done() {
        return done;
    }
    
    public void initialize() {}

    public synchronized void reset() {
        done = false;
    }
    
    /** This method is a no-op as the FatTree layout is immediate. */
    public void step() {}

    /** Gets the degree of the fat tree redundancy */
    public int getK() {
        return k;
    }
    
    /**
     * Notes when vertex was added to determine where in the fat tree it goes.
     * @param v  the vertex to tag
     * @return  whether v is a host or not
     */
    public synchronized boolean noteVertex(V v) {
        boolean isHost = nextID >= size_core() + size_agg() + size_edge(); 
        nodeIDs.put(nextID++, v);
        reset();
        return isHost;
    }
    
    /** removes all vertex tags */
    public synchronized void clear() {
        nodeIDs.clear();
        nextID = 0;
        reset();
    }
    
    /** Sets the degree of the fat tree redundancy */
    public void setK(int k) {
        if(this.k != k) {
            this.k = k;
            relayout();
        }
    }
    
    /** returns the number of switches in the core layer */
    public int size_core() {
        return k * k / 4;
    }
    
    /** returns the number of switches in the aggregation layer */
    public int size_agg() {
        return k * k / 2;
    }
    
    /** returns the number of switches in the edge layer */
    public int size_edge() {
        return k * k / 2;
    }
    
    /** returns the number of hosts in the fat tree */
    public int size_host() {
        return k * k * k / 4;
    }

    /** returns the number of switches and hosts in the fat tree */
    public int size() {
        return size_core() + size_agg() + size_edge() + size_host();
    }

    /** returns the number of total links */
    public int size_links() {
        return size_agg() * k + size_edge() * k / 2;
    }
    
    /** generated layout settings */
    int core_y = 0, agg_y = 0, edge_y = 0, host_y = 0, pod_sz = 0;
    
    /** Layout the nodes in the graph from scratch */
    public synchronized void relayout() {
        if(done || size() > nextID)
            return;
        
        int core_size = size_core();
        int agg_size = size_agg();
        int edge_size = size_edge();
        int host_size = size_host();
        
        int h = getSize().height - 45;
        int w = getSize().width;
        
        int margin_y = 40;
        int hAvail = h - margin_y * 5;
        core_y = margin_y;
        agg_y  = 2 * hAvail / 3;
        host_y = h - margin_y;
        edge_y = host_y - 3 * margin_y;
        
        int core_x_sep = w / core_size;
        int agg_x_sep  = w / agg_size;
        int edge_x_sep = w / edge_size;
        pod_sz = agg_x_sep * k / 2;
        
        int edgesPerPod = edge_size / k;
        int hostsPerPod = host_size / k;
        int hostsPerEdge = hostsPerPod / edgesPerPod;
        int host_x_offset = edge_x_sep / 3;
        
        for(int i=0; i<nodeIDs.size(); i++) {
            V v = nodeIDs.get(i);
            int core_id = i;
            int agg_id = i - core_size;
            int edge_id = i - core_size - agg_size;
            int host_id = i - core_size - agg_size - edge_size;
            
            if(core_id < core_size) {
                v.setPos(getVertexX(core_x_sep, core_id), core_y);
            }
            else if(agg_id < agg_size) {
                v.setPos(getVertexX(agg_x_sep,  agg_id),  agg_y);
            }
            else if(edge_id < edge_size) {
                v.setPos(getVertexX(edge_x_sep, edge_id), edge_y);
            }
            else if(host_id < host_size) {
                int pod = host_id / hostsPerPod;
                int hostNumInPod = host_id - pod * hostsPerPod;
                int parentEdge = pod*edgesPerPod + hostNumInPod / hostsPerEdge;
                int x = getVertexX(edge_x_sep, parentEdge) + (hostNumInPod % hostsPerEdge - 1) * host_x_offset;
                v.setPos(x, host_y);
            }
        }
        
        done = true;
    }
    
    /** computes the position of the i'th node when they are separated by sep */
    private int getVertexX(int sep, int i) {
        return sep / 2 + i * sep;
    }
}
