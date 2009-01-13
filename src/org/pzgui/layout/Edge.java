package org.pzgui.layout;

/**
 * Interface which Edges must implement in order to be laid out.
 * 
 * @author David Underhill
 *
 * @param <V>  Vertex object type
 */
public interface Edge<V extends Vertex> {
    /** Get the source of the edge */
    public V getSource();
    
    /** Get the destination of the edge */
    public V getDestination();
    
    /** Get the endpoint which is not vertex v */
    public V getOther(V v);
}
