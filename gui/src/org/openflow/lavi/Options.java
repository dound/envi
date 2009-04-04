package org.openflow.lavi;

/**
 * This class defines global options to the program.  Some of these options are
 * final as they cannot be changed by running but may differ from one class to
 * the next.
 * 
 * @author David Underhill
 */
public final class Options {
    /**
     * Whether links between nodes should be represented using one undirected
     * or two directed links.
     */
    public static final boolean USE_DIRECTED_LINKS = true;
    
    /* prevents this class from being instantiated */
    private Options() {}
}
