package org.openflow.lavi.drawables;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.openflow.lavi.net.LAVIConnection;
import org.openflow.lavi.net.protocol.PollStart;
import org.openflow.lavi.net.protocol.PollStop;
import org.openflow.lavi.stats.PortStatsRates;
import org.openflow.protocol.AggregateStatsReply;
import org.openflow.protocol.AggregateStatsRequest;
import org.openflow.protocol.Match;
import org.pzgui.Constants;
import org.pzgui.AbstractDrawable;
import org.pzgui.icon.GeometricIcon;
import org.pzgui.layout.Edge;
import org.pzgui.math.Vector2f;

/**
 * Information about a link.
 * @author David Underhill
 */
public class Link extends AbstractDrawable implements Edge<NodeWithPorts> {
    public static final int LINE_WIDTH = 2;
    public static final BasicStroke LINE_DEFAULT_STROKE = new BasicStroke(LINE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static final BasicStroke LINE_OUTLINE_STROKE = new BasicStroke(LINE_WIDTH+1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static boolean DRAW_PORT_NUMBERS = false;
    
    /** link endpoints */
    protected NodeWithPorts src;
    protected NodeWithPorts dst;
    protected short srcPort;
    protected short dstPort;
    
    private int numOtherLinks = 0;
    private Polygon boundingBox = null;
    
    /** whether the link is off because it "failed" */
    private boolean failed = false;
    
    public boolean isFailed() {
        return failed;
    }
    
    public void setFailed(boolean b) {
        failed = b;
    }
    
    /**
     * This exception is thrown if a link which already exists is tried to be 
     * re-created.
     */
    public static class LinkExistsException extends Exception {
        /** default constructor */
        public LinkExistsException() {
            super();
        }
        
        /** set the message associated with the exception */
        public LinkExistsException(String msg) {
            super(msg);
        }
    }
    
    /**
     * Constructs a new uni-directional link between src and dst.
     * 
     * @param src                The source of data on this link.
     * @param dst                The endpoint of this link.
     * 
     * @throws LinkExistsException  thrown if the link already exists
     */
    public Link(NodeWithPorts dst, short dstPort, NodeWithPorts src, short srcPort) throws LinkExistsException {
        // do not re-create existing links
        if(src.getDirectedLinkTo(srcPort, dst, dstPort, true) != null)
            throw new LinkExistsException("Link construction error: link already exists");
        
        this.src = src;
        this.dst = dst;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        
        src.addLink(this);
        dst.addLink(this);
    }
    
    public void drawObject(Graphics2D gfx) {
        Stroke s = gfx.getStroke();
        
        int ocount = ((numOtherLinks+1)/2)*2;
        if(ocount == numOtherLinks)
            ocount = -ocount;
        
        int offsetX = (LINE_WIDTH+2) * ocount;
        int offsetY = (LINE_WIDTH+2) * ocount;
        
        updateBoundingBox(src.getX()+offsetX, src.getY()+offsetY, 
                          dst.getX()+offsetX, dst.getY()+offsetY);
        
        // outline the link if it is being hovered over
        if(isHovered()) {
            gfx.draw(boundingBox);
            gfx.setPaint(Constants.COLOR_HOVERING);
            gfx.fill(boundingBox);
        }
        
        // draw the simple link as a line
        gfx.setStroke(LINE_DEFAULT_STROKE);
        gfx.setPaint(curDrawColor);
        gfx.drawLine(src.getX()+offsetX, src.getY()+offsetY, 
                     dst.getX()+offsetX, dst.getY()+offsetY);
        
        if(failed)
            GeometricIcon.X.draw(gfx, (src.getX()+dst.getX())/2+offsetX, (src.getY()+dst.getY())/2+offsetY);
        
        // draw the port numbers
        if(DRAW_PORT_NUMBERS) {
            double alpha = 0.9;
            gfx.setPaint(Constants.cmap(Color.RED));
            int srcPortX = (int)(alpha*src.getX() + (1.0-alpha)*dst.getX() + offsetX);
            int srcPortY = (int)(alpha*src.getY() + (1.0-alpha)*dst.getY() + offsetY);
            gfx.drawString(Short.toString(this.srcPort), srcPortX, srcPortY);
            gfx.setPaint(Constants.cmap(Color.GREEN.darker()));
            int dstPortX = (int)(alpha*dst.getX() + (1.0-alpha)*src.getX() + offsetX);
            int dstPortY = (int)(alpha*dst.getY() + (1.0-alpha)*src.getY() + offsetY);
            gfx.drawString(Short.toString(this.dstPort), dstPortX, dstPortY);
        }
        
        // restore the defaults
        gfx.setStroke(s);
        gfx.setPaint(Constants.PAINT_DEFAULT);
    }
    
    private void updateBoundingBox(int x1, int y1, int x2, int y2) {
        Vector2f from = new Vector2f(x1, y1);
        Vector2f to = new Vector2f(x2, y2);
        Vector2f dir = Vector2f.subtract            (to, from);
        Vector2f unitDir = Vector2f.makeUnit(dir);
        Vector2f perp = new Vector2f(unitDir.y, -unitDir.x).multiply(LINE_WIDTH / 2.0f + 4.0f);

        // build the bounding box
        float o = LINE_WIDTH / 2.0f;
        int[] bx = new int[]{ (int)(x1 - perp.x + o), (int)(x1 + perp.x + o), (int)(x2 + perp.x + o), (int)(x2 - perp.x + o) };
        int[] by = new int[]{ (int)(y1 - perp.y + o), (int)(y1 + perp.y + o), (int)(y2 + perp.y + o), (int)(y2 - perp.y + o) };
        boundingBox = new Polygon(bx, by, bx.length);
    }

    /** 
     * Disconnects this link from its attached ports and stops tracking all 
     * statistics associated with this link.  stopTrackingAllStats() is called
     * by this method.   
     */
    public void disconnect(LAVIConnection conn) throws IOException {
        src.getLinks().remove(this);
        dst.getLinks().remove(this);
        
        stopTrackingAllStats(conn);
    }
    
    public NodeWithPorts getSource() {
        return src;
    }
    
    public NodeWithPorts getDestination() {
        return dst;
    }
    
    public NodeWithPorts getOther(NodeWithPorts p) {
        // throw an error if n is neither src nor dst
        if(src!=p && dst!=p)
            throw new Error("Link::getOther Error: neither src (" + src
                    + ") nor dst (" + dst + ") is p (" + p + ")");
        
        return dst==p ? src : dst;
    }
    
    public short getMyPort(NodeWithPorts p) {
        if(src == p)
            return srcPort;
        else
            return dstPort;
    }
    
    public short getOtherPort(NodeWithPorts p) {
        if(src == p)
            return dstPort;
        else
            return srcPort;
    }
    
    public int hashCode() {
        int hash = 7;
        
        hash += dst.hashCode();
        hash += 31 * dstPort;
        hash += 31 * src.hashCode();
        hash += 31 * srcPort;
        
        return hash;
    }
    
    public boolean equals(Object o) {
        if(this == o) return true;
        if((o == null) || (o.getClass() != this.getClass())) return false;
        Link l = (Link)o;
        return l.dst.getDatapathID() == dst.getDatapathID() &&
               l.dstPort == dstPort &&
               l.src.getDatapathID() == src.getDatapathID() &&
               l.srcPort == srcPort;
    }
    
    public String toString() {
        return src.toString() + " ===> " + dst.toString();
    }
    
    public boolean isWithin(int x, int y) {
        if(boundingBox == null)
            return false;
        else
            return boundingBox.contains(x, y);
    }

    void setOffset(int numOtherLinks) {
        this.numOtherLinks = numOtherLinks;
    }

    /** maximum capacity of the link */
    private double maxDataRate_bps = 1 * 1000 * 1000 * 1000; 

    /** returns the maximum bandwidth which can be sent through the link in bps */
    public double getMaximumDataRate() {
        return maxDataRate_bps;
    }
    

    /** pairs a message transaction ID with the stats it is collecting */
    private class LinkStatsInfo {
        /** transaction ID which will be used to update these statistics */
        public final int xid;
        
        /** whether these statistics are being polled by the backend for us */
        public final boolean isPolling;
        
        /** the statistics */
        public final PortStatsRates stats;
        
        public LinkStatsInfo(int xid, boolean isPolling, PortStatsRates stats) {
            this.xid = xid;
            this.isPolling = isPolling;
            this.stats = stats;
        }
    }
    
    /** statistics being gathered for this link */
    private final ConcurrentHashMap<Match, LinkStatsInfo> stats = new ConcurrentHashMap<Match, LinkStatsInfo>();
    
    /** Gets the stats associated with the specified Match, if any */
    public PortStatsRates getStats(Match m) {
        LinkStatsInfo lsi = stats.get(m);
        if(lsi != null)
            return lsi.stats;
        else
            return null;
    }
    
    /** 
     * Tells the link to acquire the specified stats (once).
     * 
     * @param m                  what statistics to get
     * @param conn               connection to talk to the backend over
     * @throws IOException       thrown if the connection fails
     */
    public void trackStats(Match m, LAVIConnection conn) throws IOException {
        trackStats(0, m, conn);
    }
    
    /** 
     * Tells the link to acquire the specified stats.
     * 
     * @param pollInterval_msec  how often to refresh the stats (0 = only once)
     * @param m                  what statistics to get
     * @param conn               connection to talk to the backend over
     * @throws IOException       thrown if the connection fails
     */
    public void trackStats(int pollInterval_msec, Match m, LAVIConnection conn) throws IOException {
        short pollInterval = (short)(( pollInterval_msec % 100 == 0)
                                     ? pollInterval_msec / 100
                                     : pollInterval_msec / 100 + 1);
        
        // build and send the message to get the stats
        AggregateStatsRequest req = new AggregateStatsRequest(src.getDatapathID(), srcPort, m);
        boolean isPolling = (pollInterval != 0);
        if(isPolling)
            conn.sendLAVIMessage(new PollStart(pollInterval, req));
        else
            conn.sendLAVIMessage(req);
        
        trackStats(m, req.xid, isPolling);
    }
    
    /**
     * Tells the link to setup stats for specified Match but do not acquire them automatically.
     * @param m  the match to setup stats for
     */
    public PortStatsRates trackStats(Match m) {
        return trackStats(m, 0, false);
    }
    
    /**
     * Tells the link to setup stats for specified Match but do not acquire them automatically.
     * @param m  the match to setup stats for
     * @param xid  the xid of the request which is acquiring stats for m
     * @param isPolling  whether the stats are being polled with xid
     */
    public PortStatsRates trackStats(Match m, int xid, boolean isPolling) {
        // remember that we are interested in these stats
        LinkStatsInfo lsi = new LinkStatsInfo(xid, isPolling, new PortStatsRates(m));
        stats.put(m, lsi);
        return lsi.stats;
    }
    
    /**
     * Tells the link to stop tracking stats for the specified Match m.  If m 
     * was being polled, then a message will be sent to the backend to terminate
     * the polling of the message.
     * 
     * @param m  the match to stop collecting statistics for
     * @param conn  the connection over which to tell the backend to stop polling
     * @throws IOException  thrown if the connection fails
     */
    public void stopTrackingStats(Match m, LAVIConnection conn) throws IOException {
        LinkStatsInfo lsi = stats.remove(m);
        if(lsi != null && lsi.isPolling)
            conn.sendLAVIMessage(new PollStop(lsi.xid));
    }
    
    /**
     * Stop tracking and clear all statistics associated with this link.
     *  
     * @param conn  the connection to send POLL_STOP messages over
     * @throws IOException  thrown if the connection fails
     */
    public void stopTrackingAllStats(LAVIConnection conn) throws IOException {
        for(LinkStatsInfo lsi : stats.values())
            if(lsi.isPolling)
                conn.sendLAVIMessage(new PollStop(lsi.xid));
        
        stats.clear();
    }
    
    /** the color to draw the link */
    private Color curDrawColor = Constants.cmap(Color.BLACK);
    
    /** 
     * Returns the current bandwidth being sent through the link in ps or a 
     * value <0 if those stats are not currently being tracked. 
     */
    public double getCurrentDataRate() {
        PortStatsRates psr = getStats(Match.MATCH_ALL);
        if(psr == null)
            return -1;
        else
            return psr.getBitsPerSec();            
    }
    
    /** 
     * returns the current utilization of the link in the range [0, 1] or -1 if
     * stats are not currently being tracked for this
     */
    public double getCurrentUtilization() {
        double rate = getCurrentDataRate();
        if(rate < 0)
            return -1;
        else
            return rate / maxDataRate_bps;
    }
    
    /** update this links with the latest stats reply about this link */
    public void updateStats(Match m, AggregateStatsReply reply) {
        LinkStatsInfo lsi = stats.get(m);
        if(lsi == null)
            System.err.println(this.toString() + " received stats it is not tracking: " + m.toString());
        else {
            lsi.stats.update(reply);
            
            // update the color whenever the (unfiltered) link utilization stats are updated
            if(m.wildcards.isWildcardAll())
                setColor();
        }
    }
    
    /** sets the color this link will be drawn based on the current utilization */
    public void setColor() {
        float usage = (float)getCurrentUtilization();
        if(usage < 0) {
            this.curDrawColor = Color.BLUE; // indicate that we don't know the util
            return;
        }
        else if (usage > 1)
            usage = 1;
        
        if(usage == 0.0f)
            this.curDrawColor = new Color(0.3f, 0.3f, 0.3f, 0.5f); // faded gray
        else {
            float min_intensity = 0.5f;
            float low = 1.0f / 3.0f;
            float mid = 2.0f / 3.0f;
            
            if(usage < low) {
                float intensity = min_intensity + (usage / low) * (1.0f-min_intensity);
                this.curDrawColor = new Color(0f, intensity, 0f);
            }
            else if(usage < mid) {
                float intensity = min_intensity + ((usage-low) / (mid-low)) * (1.0f-min_intensity);
                this.curDrawColor = new Color(intensity, intensity, 0f);
            }
            else {
                float intensity = min_intensity + ((usage-mid) / (1.0f-mid)) * (1.0f-min_intensity);
                this.curDrawColor = new Color(intensity, 0f, 0f);
            }
        }
    }
}
