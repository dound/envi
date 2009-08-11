package org.openflow.gui.drawables;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.openflow.gui.net.protocol.NodeType;
import org.openflow.util.string.IPUtil;
import org.pzgui.icon.ShapeIcon;

/**
 * Describes a host.
 * 
 * @author David Underhill
 */
public class Host extends NodeWithPorts {
    public Host(long ip) {
        this("", 0, 0, ip);
    }
    
    public Host(String name, int x, int y, long ip) {
        super(NodeType.HOST, name, x, y, newDefaultHostShape());
        this.ip = ip;
    }
    
    /** creates a new Host icon */
    public static final ShapeIcon newDefaultHostShape() {
        return new ShapeIcon(DEFAULT_SHAPE, DEFAULT_FILL);
    }
    
    
    // ------------------- Drawing ------------------ //
    
    /** default size of the DEFAULT_SHAPE */
    public static final int DEFAULT_SIZE = 20;
    
    /** default shape used to represent a switch */
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(0, 0, DEFAULT_SIZE, DEFAULT_SIZE);
    
    /** default fill color for DEFAULT_SHAPE */
    public static final Paint DEFAULT_FILL = new Color(255, 128, 128);
    
 
    // --------------------- ID --------------------- //
    
    /** IP address of this host */
    private long ip;
    
    /** returns a string version of the switch's datapath ID */
    public String getDebugName() {
        return IPUtil.toString((int)getID());
    }
    
    /** gets the IP address of this host */
    public long getID() {
        return ip;
    }
    
    /** sets the IP address of this host */
    public void setID(long ip) {
        this.ip = ip;
    }
    
    
    // -------------------- Other ------------------- //
    
    public String toString() {
        return getName() + "; ip=" + IPUtil.toString((int)getID());
    }
}
