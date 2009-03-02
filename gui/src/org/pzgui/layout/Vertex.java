package org.pzgui.layout;

import org.pzgui.Drawable;

/**
 * Interface which Vertices must implement in order to be laid out.
 * 
 * @author David Underhill
 *
 * @param <E>  Edge object type
 */
public interface Vertex<E extends Edge> extends Drawable, Layoutable {
    /** Return an iterator over the edges in a Vertex */
    public Iterable<E> getEdges();
}
