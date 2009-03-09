package org.openflow.lavi.drawables;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;

import org.openflow.protocol.SwitchDescriptionStats;
import org.openflow.util.string.DPIDUtil;
import org.pzgui.Constants;
import org.pzgui.icon.GeometricIcon;

/**
 * Describes an OpenFlow node.
 * @author David Underhill
 */
public class OpenFlowSwitch extends NodeWithPorts {
    public static final Dimension SIZE_SMALL = new Dimension(5, 5);
    public static final Dimension SIZE = new Dimension(15, 15);
    public static final Dimension SIZE_BIG = new Dimension(25, 25);
    public static final Paint NAME_COLOR = new Color(128, 128, 255);
    public static final Paint FILL_COLOR_G = new Color(128, 255, 128);
    public static final Paint FILL_COLOR_B = new Color(128, 128, 255);
    public static final boolean SHOW_NAME = false;
    public static final AlphaComposite ALPHA_OFF = AlphaComposite.SrcOver.derive(0.25f);
    
    private long datapathID;
    private static final double OUTLINE_RATIO = 4.0 / 3.0;
    
    private String manufacturer="?", hw_desc="?", sw_desc="?", serial_num="?";
    private long descUpdateTime = 0;
    
    // drawing-specific
    private Dimension size = SIZE;
    private Paint fillColor = FILL_COLOR_B;
    
    /** whether the switch is off because it is not needed */
    private boolean off = false;
    
    /** whether the switch is off because it "failed" */
    private boolean failed = false;
    
    
    public OpenFlowSwitch(long dpid) {
        this("", 0, 0, dpid);
    }
    
    public OpenFlowSwitch(String name, int x, int y, long dpid) {
        super(name, x, y);
        this.datapathID = dpid;
    }

    public boolean isOff() {
        return off;
    }
    
    public void setOff(boolean b) {
        off = b;
    }

    public boolean isFailed() {
        return failed;
    }
    
    public void setFailed(boolean b) {
        failed = b;
    }
    
    /** Move the switch when it is dragged */
    public void drag(int x, int y) {
        setPos(x, y);
    }

    public void drawBeforeObject(Graphics2D gfx) {
        drawLinks(gfx);
    }

    public void draw(Graphics2D gfx) {
        Composite c = null;
        if(isOff()) {
            c = gfx.getComposite();
            gfx.setComposite(ALPHA_OFF);
        }
        
        Paint outlineColor;
        if(isSelected())
            outlineColor = Constants.COLOR_SELECTED;
        else if(isHovered())
            outlineColor = Constants.COLOR_HOVERING;
        else
            outlineColor = null;
        
        if(outlineColor != null) {
            double w = size.width * OUTLINE_RATIO;
            double h = size.height * OUTLINE_RATIO;
            Ellipse2D.Double outline = new Ellipse2D.Double(getX()-w/2, getY()-h/2, w, h);
            
            gfx.draw(outline);
            gfx.setPaint(outlineColor);
            gfx.fill(outline);
            gfx.setPaint(Constants.PAINT_DEFAULT);
        }
        
        int x = getX() - size.width / 2;
        int y = getY() - size.height / 2;
        gfx.drawOval(x, y, size.width, size.height);
        gfx.setPaint(fillColor);
        gfx.fillOval(x, y, size.width, size.height);
        
        if(failed) {
            int w=size.width, dx=0;
            if(GeometricIcon.X.getWidth() > w) {
                dx = (GeometricIcon.X.getWidth() - w) / 2;
                w = GeometricIcon.X.getWidth();
            }
            int h=size.height, dy=0;
            if(GeometricIcon.X.getWidth() > h) {
                dy = (GeometricIcon.X.getWidth() - h) / 2;
                h = GeometricIcon.X.getWidth();
            }
            GeometricIcon.X.draw(gfx, x-dx, y-dy, w, h);
        }
        
        if(SHOW_NAME) {
            gfx.setPaint(NAME_COLOR);
            int textYOffset = -size.height / 2 + 2;
            if(isStringSet(getName()))
                drawName(gfx, getX(), getY() - textYOffset, getY() + textYOffset);
            else
                gfx.drawString(DPIDUtil.toShortString(datapathID), x, y);
            y += gfx.getFontMetrics().getHeight();
        }
        gfx.setPaint(Constants.PAINT_DEFAULT);
        
        // display switch description stats on mouse over
        if(this.isHovered() || this.isSelected()) {
            if(isStringSet(manufacturer)) {
                gfx.drawString(manufacturer, x, y);
                y += gfx.getFontMetrics().getHeight();
            }
            
            if(isStringSet(hw_desc)) {
                gfx.drawString(hw_desc, x, y);
                y += gfx.getFontMetrics().getHeight();
            }
            
            if(isStringSet(sw_desc)) {
                gfx.drawString(sw_desc, x, y);
                y += gfx.getFontMetrics().getHeight();
            }
            
            if(isStringSet(serial_num))
                gfx.drawString(serial_num, x, y);
        }
        
        if(isOff())
            gfx.setComposite(c);
    }
    
    /** Returns true if s is not null, non-zero length, and not "None" or "?" */
    private boolean isStringSet(String s) {
        return s!=null && s.length()>0 && !s.equals("None") && !s.equals("?");
    }
    
    public Dimension getSize() {
        return size;
    }
    
    public void setSize(Dimension d) {
        size = d;
    }
    
    public String getDebugName() {
        return DPIDUtil.dpidToHex(datapathID);
    }
    
    public long getDatapathID() {
        return datapathID;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public String getHWDescription() {
        return hw_desc;
    }
    
    public String getSWDescription() {
        return sw_desc;
    }
    
    public String getSerialNumber() {
        return serial_num;
    }
    
    /** Returns the time the switch's description was last updated */
    public long getDescriptionUpdateTime() {
        return descUpdateTime;
    }
    
    /** Update info about the switch's description */
    public void setSwitchDescription(SwitchDescriptionStats stats) {
        manufacturer = stats.manufacturer;
        hw_desc = stats.hw_desc;
        sw_desc = stats.sw_desc;
        serial_num = stats.serial_num;
        descUpdateTime = System.currentTimeMillis();
        
        if(manufacturer.contains("Nicira")) {
            fillColor = FILL_COLOR_G;
            size = SIZE;
        }
        else {
            fillColor = FILL_COLOR_B;
            size = SIZE_BIG;
        }
    }
    
    public void setFillColor(Color c) {
        fillColor = c;
    }
    
    public boolean isWithin(int x, int y) {
        return isWithin(x, y, getSize());
    }

    public void setDatapathID(long dpid) {
        this.datapathID = dpid;
    }
    
    public String toString() {
        return getName() + "; dpid=" + DPIDUtil.dpidToHex(getDatapathID());
    }
}
