package org.openflow.gui.drawables;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.openflow.gui.Options;
import org.openflow.gui.net.BackendConnection;
import org.openflow.gui.net.protocol.LinkType;
import org.openflow.gui.net.protocol.PollStart;
import org.openflow.gui.net.protocol.PollStop;
import org.openflow.gui.stats.LinkStats;
import org.openflow.protocol.AggregateStatsReply;
import org.openflow.protocol.AggregateStatsRequest;
import org.openflow.protocol.Match;
import org.pzgui.Constants;
import org.pzgui.AbstractDrawable;
import org.pzgui.StringDrawer;
import org.pzgui.icon.GeometricIcon;
import org.pzgui.layout.Edge;
import org.pzgui.math.IntersectionFinder;
import org.pzgui.math.Line;
import org.pzgui.math.Vector2f;
import org.pzgui.math.Vector2i;

/**
 * Information about a link.
 * 
 * @author David Underhill
 */
public class Link extends AbstractDrawable implements Edge<NodeWithPorts> {
    /** how to color the link when it is negatively utilized (probably a special signal or error) */
    public static Color USAGE_COLOR_NEG = Color.BLACK;
    
    /** how to color the link when it is completely unutilized */
    public static Color USAGE_COLOR_0 = Color.BLACK;

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
     * Used to ensure new links are created sequentially to ensure link exists
     * exceptions can be properly generated.
     */
    private static final Object ONE_AT_A_TIME = new Object();
    
    /**
     * Constructs a new link between src and dst.
     * 
     * @param linkType  the type of this link
     * @param dst       the destination of this link
     * @param dstPort   the destination port of the link (on dst)
     * @param src       the source node of data on this link
     * @param srcPort   the source port of the link (on src)
     * 
     * @throws LinkExistsException  thrown if the link already exists
     */
    public Link(LinkType linkType, NodeWithPorts dst, short dstPort, NodeWithPorts src, short srcPort) throws LinkExistsException {
        synchronized(ONE_AT_A_TIME) {
            // do not re-create existing links
            if(src.getDirectedLinkTo(srcPort, dst, dstPort, true) != null)
                throw new LinkExistsException("Link construction error: link already exists");
            
            this.type = linkType;
            this.src = src;
            this.dst = dst;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
            
            src.addLink(this);
            dst.addLink(this);
        }
    }
    
    
    // --------- Basic Accessors / Mutators --------- //
    
    /** the type of this link */
    protected LinkType type;
    
    /** the source of this link */
    protected NodeWithPorts src;
    
    /** the port to which this link connects on the source node */
    protected short srcPort;
    
    /** the destination of this link */
    protected NodeWithPorts dst;
    
    /** the port to which this link connects on the destination node */
    protected short dstPort;
    
    /** maximum capacity of the link */
    private double maxDataRate_bps = 1 * 1000 * 1000 * 1000; 
    
    /** whether the link is off because it "failed" */
    private boolean failed = false;
    
    /** 
     * Disconnects this link from its attached ports and stops tracking all 
     * statistics associated with this link.  stopTrackingAllStats() is called
     * by this method.   
     */
    public void disconnect(BackendConnection conn) throws IOException {
        src.getLinks().remove(this);
        dst.getLinks().remove(this);
        
        stopTrackingAllStats(conn);
    }
    
    /** get the souce of this link */
    public NodeWithPorts getSource() {
        return src;
    }
    
    /** get the destination of this link */
    public NodeWithPorts getDestination() {
        return dst;
    }
    
    /** 
     * Given one endpoint of the link, return the other endpoint.  Throws an 
     * error if p is neither the source or destination of this link.
     */
    public NodeWithPorts getOther(NodeWithPorts p) {
        // throw an error if n is neither src nor dst
        if(src!=p && dst!=p)
            throw new Error("Link::getOther Error: neither src (" + src
                    + ") nor dst (" + dst + ") is p (" + p + ")");
        
        return dst==p ? src : dst;
    }
    
    /** Gets the port number associated with the specified endpoint. */
    public short getMyPort(NodeWithPorts p) {
        if(src == p)
            return srcPort;
        else
            return dstPort;
    }
    
    /** Gets the port number associated with the endpoint which is not p. */
    public short getOtherPort(NodeWithPorts p) {
        if(src == p)
            return dstPort;
        else
            return srcPort;
    }
    
    /** returns true if the link is a wired link */
    public boolean isWired() {
        return type != LinkType.WIRELESS;
    }
    
    /** returns true if the link is a wireless link */
    public boolean isWireless() {
        return type == LinkType.WIRELESS;
    }

    /** gets the type of this link */
    public LinkType setLinkLink() {
        return type;
    }
    
    /** sets the type of this link */
    public void setLinkLink(LinkType type) {
        this.type = type;
    }

    /** returns the maximum bandwidth which can be sent through the link in bps */
    public double getMaximumDataRate() {
        return maxDataRate_bps;
    }
    
    /** sets the maximum bandwidth which can be sent through the link in bps */
    public void setMaximumDataRate(double bps) {
        this.maxDataRate_bps = bps;
    }
    
    /** Returns true if the link has failed. */
    public boolean isFailed() {
        return failed;
    }
    
    /** Sets whether the link has failed. */
    public void setFailed(boolean b) {
        failed = b;
    }

    
    // ------------------- Drawing ------------------ //
    
    /** thickness of a link */
    public static final int LINE_WIDTH = 2;
    
    /** how the draw a link */
    public static final BasicStroke LINE_DEFAULT_STROKE = new BasicStroke(LINE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    
    /** size of the arrow head */
    public static final int ARROW_HEAD_SIZE = 15;
    
    /** bounding square around the arc */
    public static final int WIRELESS_ARC_SIZE = 40;
    
    /** amount of space between arcs */
    public static final int WIRELESS_ARCS_SPACE_BETWEEN = 20;
    
    /** how many degrees the arc covers */
    public static final int WIRELESS_ARC_DEGREES = 90;
    
    /** how to draw the line for wireless links */
    public static final BasicStroke WIRELESS_LINE_DEFAULT_STROKE = 
        new BasicStroke(LINE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, LINE_WIDTH, new float[]{LINE_WIDTH}, 0.0f);
    
    /** whether to draw port numbers each link is attached to */
    public static boolean DRAW_PORT_NUMBERS = false;
    
    /** distance from the edge of an object to draw port numbers, if enabled */
    public static final int PORT_NUMBERS_OFFSET = ARROW_HEAD_SIZE + 10;
    
    /** alpha channel of port numbers */
    public static float DEFAULT_PORT_NUM_ALPHA = 0.9f;
    
    /** port number font */
    public static final Font PORT_NUMBERS_FONT = new Font("Tahoma", Font.BOLD, 24);
    
    /** thickness of a tunnel */
    private static final int DEFAULT_TUNNEL_WIDTH = LINE_WIDTH * 10;
    
    /** gap between a tunnel and an endpoint */
    private static final int TUNNEL_DIST_FROM_BOX = DEFAULT_TUNNEL_WIDTH * 2;
    
    /** dark tunnel color*/
    public static final Color TUNNEL_PAINT_DARK = Constants.cmap(new Color(180, 180, 180));
    
    /** light tunnel color */
    public static final Color TUNNEL_PAINT_LIGHT = Constants.cmap(new Color(196, 196, 196));
    
    /** the color to draw the link (if null, then this link will not be drawn) */
    private Color curDrawColor = Constants.cmap(Color.BLACK);
    
    /** how much to offset the link drawing in the x axis */
    private int offsetX;
    
    /** how much to offset the link drawing in the y axis */
    private int offsetY;
    
    /** Bounds the area in which a link is drawn. */
    private Polygon boundingBox = null;
    
    /** Draws the link */
    public void drawObject(Graphics2D gfx) {
        if(this.isDrawn())
            return;
        else
            this.setDrawn();
        
        // draw nothing if there is no current draw color for the link
        if(curDrawColor == null)
            return;
        
        Stroke s = gfx.getStroke();
        
        // outline the link if it is being hovered over or is selected
        if(isHovered())
            drawOutline(gfx, Constants.COLOR_HOVERING, 1.25);
        else if(isSelected())
            drawOutline(gfx, Constants.COLOR_SELECTED, 1.25);
        
        // draw the lin based on whether it is wired/wireless
        if(isWired())
            drawWiredLink(gfx);
        else
            drawWirelessLink(gfx);
        
        // add a tunnel if it is tunneled
        if(type == LinkType.TUNNEL)
            drawTunnel(gfx, LINE_WIDTH);
        
        // draw the failure indicator if the link has failed
        if(isFailed())
            drawFailed(gfx);
        
        // draw the port numbers
        if(DRAW_PORT_NUMBERS)
            drawPortNumbers(gfx, DEFAULT_PORT_NUM_ALPHA);
        
        // restore the defaults
        gfx.setStroke(s);
        gfx.setPaint(Constants.PAINT_DEFAULT);
    }
    
    /** draw an "X" over the node to indicate failure */
    protected void drawFailed(Graphics2D gfx) {
        GeometricIcon.X.draw(gfx, 
                             (src.getX() + dst.getX())/2 + offsetX, 
                             (src.getY() + dst.getY())/2 + offsetY);
    }
    
    /**
     * Draw an outline around the link.
     * 
     * @param gfx           where to draw
     * @param outlineColor  color of the outline
     * @param ratio         how big to make the outline (relative to the 
     *                      bounding box of this link)
     */
    public void drawOutline(Graphics2D gfx, Paint outlineColor, double ratio) {
        AffineTransform af = new AffineTransform();
        af.setToScale(ratio, ratio);
        Shape s = af.createTransformedShape(boundingBox);
        
        gfx.draw(s);
        gfx.setPaint(outlineColor);
        gfx.fill(s);
    }
    
    /** draws port numbers by the link drawing's endpoints with the specified alpha */
    public void drawPortNumbers(Graphics2D gfx, float alpha) {
        Composite origC = gfx.getComposite();
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        gfx.setComposite(ac);
        
        Font origF = gfx.getFont();
        gfx.setFont(PORT_NUMBERS_FONT);
        
        Vector2i p1 = new Vector2i(src.getX() + offsetX, src.getY() + offsetY);
        Vector2i p2 = new Vector2i(dst.getX() + offsetX, dst.getY() + offsetY);
        
        gfx.setPaint(Constants.cmap(Color.RED));
        Vector2i s = IntersectionFinder.intersect(p2, p1, PORT_NUMBERS_OFFSET+src.getWidth(), PORT_NUMBERS_OFFSET+src.getHeight());
        gfx.drawString(Short.toString(this.srcPort), s.x, s.y);
        
        gfx.setPaint(Constants.cmap(Color.GREEN.darker()));
        Vector2i d = IntersectionFinder.intersect(p1, p2, PORT_NUMBERS_OFFSET+dst.getWidth(), PORT_NUMBERS_OFFSET+dst.getHeight());
        gfx.drawString(Short.toString(this.dstPort), d.x, d.y);
        
        gfx.setComposite(origC);
        gfx.setFont(origF);
    }
    
    /** sets up the stroke and color information for the link prior to it being drawn */
    private void drawLinkPreparation(Graphics2D gfx) {
        gfx.setStroke(LINE_DEFAULT_STROKE);
        if(curDrawColor != null)
            gfx.setPaint(curDrawColor);
    }
    
    /** draws the link as a wired link between endpoints */
    public void drawWiredLink(Graphics2D gfx) {
        drawLinkPreparation(gfx);
        drawWiredLinkNoPrep(gfx);
        gfx.setPaint(Constants.COLOR_DEFAULT);
    }
    
    private void drawWiredLinkNoPrep(Graphics2D gfx) {
        Vector2i p1 = new Vector2i(src.getX() + offsetX, src.getY() + offsetY);
        Vector2i p2 = new Vector2i(dst.getX() + offsetX, dst.getY() + offsetY);
        
        // draw an arrow head on directed links
        if(Options.USE_DIRECTED_LINKS) {
            p2 = IntersectionFinder.intersect(p1, p2, dst.getWidth(), dst.getHeight());
            gfx.fill(getArrowHead(ARROW_HEAD_SIZE, p1.x, p1.y, p2.x, p2.y));
        }
        
        // draw the link between the objects' center points
        gfx.drawLine(p1.x, p1.y, p2.x, p2.y);
    }
    
    /**
     * Gets a path for an arrow head on an arrow drawn from x1,y1 to x2,y2.
     *  
     * @param sz  Size of the arrow head
     * @param x1  initial x coordinate
     * @param y1  initial y coordinate
     * @param x2  final x coordinate
     * @param y2  final y coordinate
     * 
     * @return  path describing an arrow head
     */
    public static GeneralPath getArrowHead(int sz, int x1, int y1, int x2, int y2) {
        final double PHI = Math.toRadians(35);
        double theta = Math.atan2(y2 - y1, x2 - x1);
        Point2D.Double p1 = new Point2D.Double(x1, y1);
        Point2D.Double p2 = new Point2D.Double(x2, y2);
        GeneralPath path = new GeneralPath(new Line2D.Float(p1, p2));
        double x = p2.x + sz*Math.cos(theta+Math.PI-PHI);
        double y = p2.y + sz*Math.sin(theta+Math.PI-PHI);
        path.moveTo((float)x, (float)y);
        path.lineTo((float)p2.x, (float)p2.y);
        x = p2.x + sz*Math.cos(theta+Math.PI+PHI);
        y = p2.y + sz*Math.sin(theta+Math.PI+PHI);
        path.lineTo((float)x, (float)y);
        return path;
    }
    
    /** draws the link as a wireless link between endpoints */
    public void drawWirelessLink(Graphics2D gfx) {
        drawLinkPreparation(gfx);
        
        Vector2i p1 = new Vector2i(src.getX() + offsetX, src.getY() + offsetY);
        Vector2i p2 = new Vector2i(dst.getX() + offsetX, dst.getY() + offsetY);
        Vector2i pI = IntersectionFinder.intersect(p1, p2, dst.getWidth(), dst.getHeight());
        
        // draw a dashed line 
        gfx.setStroke(WIRELESS_LINE_DEFAULT_STROKE);
        drawWiredLinkNoPrep(gfx);
        
        // compute number of arcs to draw
        int dx = pI.x - p1.x;
        int dy = pI.y - p1.y;
        double dist = Math.sqrt(dx*dx + dy*dy);
        int numArcs = (int)(dist / WIRELESS_ARCS_SPACE_BETWEEN) + 1;
        
        // determine the how to draw the arcs
        int angleBtwnPts = (int)(Math.toDegrees(Math.atan2(pI.y-p1.y, pI.x-p1.x)));
        int startAngle = -angleBtwnPts - WIRELESS_ARC_DEGREES/2;
        
        // draw the arcs
        double step = 1.0 / numArcs;
        double alpha = 0.0;
        for(int i=0; i<numArcs; i++) {
            alpha += step;
            double cx = p1.x*alpha + pI.x*(1.0-alpha);
            double cy = p1.y*alpha + pI.y*(1.0-alpha);
            
            int x = (int)cx - WIRELESS_ARC_SIZE/2;
            int y = (int)cy - WIRELESS_ARC_SIZE/2;
     
            gfx.drawArc(x, y, 
                        WIRELESS_ARC_SIZE, WIRELESS_ARC_SIZE, 
                        startAngle, WIRELESS_ARC_DEGREES);
        }
    }
    
    /** draws a tunnel in the area used by the middle of the link */
    public void drawTunnel(Graphics2D gfx, int linkWidth) {
        // find the endpoints of the link based on the intersection of the link with its surrounding box
        Line linkLine = new Line(src.getX(), src.getY(), dst.getX(), dst.getY());
        Vector2f i1 = IntersectionFinder.intersectBox(linkLine, src);
        Vector2f i2 = IntersectionFinder.intersectBox(linkLine, dst);
        
        if(i1 == null) {
            Dimension dimSrc = src.getIcon().getSize();
            i1 = new Vector2f(src.getX()-dimSrc.width/2, src.getY()-dimSrc.height/2);
        }
         
        if(i2 == null) {
            Dimension dimDst = dst.getIcon().getSize();
            i2 = new Vector2f(dst.getX()-dimDst.width/2, dst.getY()-dimDst.height/2);
        }

        // determine the endpoints of the pipe as a fixed distance from the node/box
        float x1, y1, x2, y2;
        float d = TUNNEL_DIST_FROM_BOX;
        {
            float sx = i1.x;
            float sy = i1.y;
            float dx = i2.x;
            float dy = i2.y;
            float denom = (float)Math.sqrt(Math.pow(dx-sx,2) + Math.pow(sy-dy,2));
            x1 = sx + (dx - sx) * d / denom;
            y1 = sy - (sy - dy) * d / denom;
        }
        {
            float sx = i2.x;
            float sy = i2.y;
            float dx = i1.x;
            float dy = i1.y;
            float denom = (float)Math.sqrt(Math.pow(dx-sx,2) + Math.pow(sy-dy,2));
            x2 = sx + (dx - sx) * d / denom;
            y2 = sy - (sy - dy) * d / denom;
        }
        
        // center the tunnel on the coords
        int tw = DEFAULT_TUNNEL_WIDTH;
        x1 -= tw / 2;
        y1 -= tw / 2;
        x2 -= tw / 2;
        y2 -= tw / 2;
        
        // determine where to orient the corners of the rectangular portion of the cylinder
        Vector2f unitV = Vector2f.makeUnit(new Vector2f(x2-x1, y2-y1)).multiply(-tw/2);
        float t = unitV.x; unitV.x = unitV.y; unitV.y = t;
        int[] xs = new int[]{(int)(x1-unitV.x), (int)(x1+unitV.x), (int)(x2+unitV.x), (int)(x2-unitV.x)};
        int[] ys = new int[]{(int)(y1+unitV.y), (int)(y1-unitV.y), (int)(y2-unitV.y), (int)(y2+unitV.y)};
        for(int i=0; i<4; i++) {
            xs[i] += tw / 2;
            ys[i] += tw / 2;
        }
        
        // create the shapes representing pieces of the pipe
        Ellipse2D.Double circle1 = new Ellipse2D.Double(x1, y1, tw, tw);
        Ellipse2D.Double circle2 = new Ellipse2D.Double(x2, y2, tw, tw);
        Polygon pipe = new Polygon(xs, ys, 4);
        
        // build the paint for the gradient of the pipe
        Paint pipePaint = new GradientPaint((int)x1, (int)y1, TUNNEL_PAINT_DARK, (int)x2, (int)y2, TUNNEL_PAINT_LIGHT );
        
        // draw the pipe endpoints
        gfx.setPaint(pipePaint);
        gfx.fill(circle1);
        gfx.setPaint(Constants.PAINT_DEFAULT);
        gfx.draw(circle1);
        
        // draw the body of the pipe
        gfx.setPaint(pipePaint);
        gfx.fill(pipe);
        gfx.setPaint( Constants.PAINT_DEFAULT );
        gfx.drawLine( xs[0], ys[0], xs[3], ys[3] ); // long sides
        gfx.drawLine( xs[1], ys[1], xs[2], ys[2] );
        gfx.drawLine( xs[0], ys[0], xs[1], ys[1] ); // short sides
        gfx.drawLine( xs[2], ys[2], xs[3], ys[3] );
        
        // draw the pipe endpoints
        gfx.setPaint(pipePaint);
        
        // cover up the inner half of the first circle so the pipe looks 3D
        gfx.setStroke(Constants.STROKE_THICK);
        gfx.drawLine( xs[0], ys[0], xs[1], ys[1] );
        gfx.setStroke(Constants.STROKE_DEFAULT);
        
        gfx.fill(circle2);
        gfx.setPaint( Constants.PAINT_DEFAULT );
        gfx.draw(circle2);
        
        // extend the link into and out of the pipe
        BasicStroke strokeOutline = new BasicStroke( linkWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER );
        gfx.setStroke( strokeOutline );
        gfx.drawLine( (int)i2.x, (int)i2.y, (int)x2 + tw / 2, (int)y2 + tw / 2 );
        gfx.setStroke( Constants.STROKE_DEFAULT );
    }

    /** sets how many other links between the same endpoints have already been drawn */
    void setOffset(int numOtherLinks) {
        int ocount = ((numOtherLinks + 1) / 2) * 2;
        if(ocount == numOtherLinks)
            ocount = -ocount;
        
        offsetX = (LINE_WIDTH+2) * ocount;
        offsetY = (LINE_WIDTH+2) * ocount;
        
        updateBoundingBox(src.getX()+offsetX, src.getY()+offsetY, 
                          dst.getX()+offsetX, dst.getY()+offsetY);
    }
    
    /** updates the bounding box for this link */
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
    

    // -------------------- Stats ------------------- //
    
    /** statistics being gathered for this link */
    private final ConcurrentHashMap<Match, LinkStatsInfo> stats = new ConcurrentHashMap<Match, LinkStatsInfo>();
    
    /** pairs a message transaction ID with the stats it is collecting */
    private class LinkStatsInfo {
        /** transaction ID which will be used to update these statistics */
        public final int xid;
        
        /** whether these statistics are being polled by the backend for us */
        public final boolean isPolling;
        
        /** the statistics on traffic from the source and destination switch over this link */
        public final LinkStats stats;
        
        public LinkStatsInfo(int xid, boolean isPolling, Match m) {
            this.xid = xid;
            this.isPolling = isPolling;
            this.stats = new LinkStats(m);
        }
    }
    
    /** Gets the LinkStats associated with the specified Match, if any */
    public LinkStats getStats(Match m) {
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
    public void trackStats(Match m, BackendConnection conn) throws IOException {
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
    public void trackStats(int pollInterval_msec, Match m, BackendConnection conn) throws IOException {
        short pollInterval = (short)(( pollInterval_msec % 100 == 0)
                                     ? pollInterval_msec / 100
                                     : pollInterval_msec / 100 + 1);
        
        // build and send the message to get the stats
        AggregateStatsRequest req = new AggregateStatsRequest(src.getID(), srcPort, m);
        boolean isPolling = (pollInterval != 0);
        if(isPolling)
            conn.sendMessage(new PollStart(pollInterval, req));
        else
            conn.sendMessage(req);
        
        trackStats(m, req.xid, isPolling);
    }
    
    /**
     * Tells the link to setup stats for specified Match but do not acquire them automatically.
     * @param m  the match to setup stats for
     */
    public LinkStats trackStats(Match m) {
        return trackStats(m, 0, false);
    }
    
    /**
     * Tells the link to setup stats for specified Match but do not acquire them automatically.
     * @param m  the match to setup stats for
     * @param xid  the xid of the request which is acquiring stats for m
     * @param isPolling  whether the stats are being polled with xid
     */
    public LinkStats trackStats(Match m, int xid, boolean isPolling) {
        // remember that we are interested in these stats
        LinkStatsInfo lsi = new LinkStatsInfo(xid, isPolling, m);
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
    public void stopTrackingStats(Match m, BackendConnection conn) throws IOException {
        LinkStatsInfo lsi = stats.remove(m);
        if(lsi != null && lsi.isPolling)
            conn.sendMessage(new PollStop(lsi.xid));
    }
    
    /**
     * Stop tracking and clear all statistics associated with this link.
     *  
     * @param conn  the connection to send POLL_STOP messages over
     * @throws IOException  thrown if the connection fails
     */
    public void stopTrackingAllStats(BackendConnection conn) throws IOException {
        for(LinkStatsInfo lsi : stats.values())
            if(lsi.isPolling)
                conn.sendMessage(new PollStop(lsi.xid));
        
        stats.clear();
    }
    
    /** update this links with the latest stats reply about this link */
    public void updateStats(Match m, AggregateStatsReply reply) {
        LinkStatsInfo lsi = stats.get(m);
        if(lsi == null)
            System.err.println(this.toString() + " received stats it is not tracking: " + m.toString());
        else {
            if(reply.dpid == src.getID())
                lsi.stats.statsSrc.update(reply);
            else if(lsi.stats.statsDst != null && reply.dpid == dst.getID())
                lsi.stats.statsDst.update(reply);
            
            // update the color whenever the (unfiltered) link utilization stats are updated
            if(m.wildcards.isWildcardAll())
                setColorBasedOnCurrentUtilization();
        }
    }
    
    /** 
     * Returns the current bandwidth being sent through the link in ps or a 
     * value <0 if those stats are not currently being tracked. 
     */
    public double getCurrentDataRate() {
        LinkStats ls = getStats(Match.MATCH_ALL);
        if(ls == null)
            return -1;
        else
            return ls.getCurrentAverageDataRate();            
    }
    
    /** 
     * Returns the current utilization of the link in the range [0, 1] or -1 if
     * stats are not currently being tracked for this.
     */
    public double getCurrentUtilization() {
        double rate = getCurrentDataRate();
        if(rate < 0)
            return -1;
        else
            return rate / maxDataRate_bps;
    }
    
    
    // ------------- Usage Color Helpers ------------ //
    
    /** sets the color this link will be drawn based on the current utilization */
    public void setColorBasedOnCurrentUtilization() {
        float usage = (float)getCurrentUtilization();
        this.curDrawColor = getUsageColor(usage);
    }
    
    /**
     * Gets the color associated with a particular usage value.
     */
    public static Color getUsageColor(float usage) {
        if(usage < 0) {
            return USAGE_COLOR_NEG;
        }
        else if(usage == 0)
            return USAGE_COLOR_0;
        else if (usage > 1)
            usage = 1;
        
        return USAGE_COLORS[(int)(usage * (NUM_USAGE_COLORS-1))];
    }
    
    /**
     * Computes the color associated with a particular usage value.
     */
    private static Color computeUsageColor(float usage) {
        if(usage < 0.0f)
            return Link.USAGE_COLOR_NEG;
        else {
            if(usage==0.0f)
                return Link.USAGE_COLOR_0;
            
            float mid = 1.5f / 3.0f;
            
            if(usage < mid) {
                // blend green + yellow
                float alpha = usage / mid;
                return new Color(1.0f*alpha+0.0f*(1.0f-alpha),
                                 1.0f*alpha+1.0f*(1.0f-alpha),
                                 0.0f);
            }
            else {
                // blend red + yellow
                float alpha = (usage - mid) / mid;
                return new Color(1.0f*alpha+1.0f*(1.0f-alpha),
                                 0.0f*alpha+1.0f*(1.0f-alpha),
                                 0.0f);
            }
        }
    }
    
    /** how finely to precompute usage colors (larger => more memory and more preceise) */
    public static final int NUM_USAGE_COLORS = 256;
    
    /** array of precomputed usage colors */
    public static final Color[] USAGE_COLORS;
    
    /** image containing the legend of usage colors from low to high utilization */
    public static final  BufferedImage USAGE_LEGEND;
    
    /** precompute usage colors for performance reasons */
    static {
        USAGE_COLORS = new Color[NUM_USAGE_COLORS];
        int legendHeight = 20;
        USAGE_LEGEND = new BufferedImage(NUM_USAGE_COLORS, legendHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D gfx = (Graphics2D)USAGE_LEGEND.getGraphics();
        for(int i=0; i<NUM_USAGE_COLORS; i++) {
            USAGE_COLORS[i] = computeUsageColor(i / (float)(NUM_USAGE_COLORS-1));
            gfx.setPaint(USAGE_COLORS[i]);
            gfx.drawLine(i, 0, i, legendHeight);
        }
        gfx.setPaint(Constants.PAINT_DEFAULT);
        
        // draw the explanation on top of the legend
        gfx.setFont(Constants.FONT_DEFAULT);
        gfx.setColor(Color.BLACK);
        int lw = USAGE_LEGEND.getWidth();
        int lh = USAGE_LEGEND.getHeight();
        int y = lh / 2 + 5;
        int margin_x = 5;
        gfx.drawString("0%", margin_x, y);
        StringDrawer.drawCenteredString("Link Utilization (%)", gfx, lw / 2, y);
        StringDrawer.drawRightAlignedString("100%", gfx, lw - margin_x, y);
        gfx.setColor(Constants.COLOR_DEFAULT);
    }
    
    
    // -------------------- Other ------------------- //
    
    public boolean contains(int x, int y) {
        if(boundingBox == null)
            return false;
        else
            return boundingBox.contains(x, y);
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
        return l.dst.getID() == dst.getID() &&
               l.dstPort == dstPort &&
               l.src.getID() == src.getID() &&
               l.srcPort == srcPort;
    }
    
    public String toString() {
        return src.toString() + " ===> " + dst.toString();
    }
}
