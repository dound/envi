package org.openflow.gui;

/**
 * This class defines global options to the program.  Some of these options are
 * final as they cannot be changed by running but may differ from one class to
 * the next.
 * 
 * @author David Underhill
 */
public final class Options {
    /** default server IP, if any */
    public static final String DEFAULT_SERVER_IP = "openflowvisor.stanford.edu";
    
    /** default port to connect to the server on */
    public static final short DEFAULT_PORT = 2503;
    
    /** whether to automatically request link info for all new switches */
    public static final boolean AUTO_REQUEST_LINK_INFO_FOR_NEW_SWITCH = true;
    
    /** whether to automatically request that link stats be periodically sent for all new links */
    public static final boolean AUTO_TRACK_STATS_FOR_NEW_LINK = true;
    
    /** how often to refresh basic port statistics */
    public static final int STATS_REFRESH_RATE_MSEC = 2000;
    
    /**
     * Whether links between nodes should be represented using one undirected
     * or two directed links.
     */
    public static final boolean USE_DIRECTED_LINKS = false;
    
    /** whether to use a light or dark color scheme */
    public static final boolean USE_LIGHT_COLOR_SCHEME = true;
    
    /* prevents this class from being instantiated */
    private Options() {}
}
