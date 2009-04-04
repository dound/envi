package org.openflow.lavi.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.openflow.protocol.SwitchDescriptionStats;
import org.openflow.util.string.DPIDUtil;
import org.pzgui.icon.ShapeIcon;

/**
 * Describes an OpenFlow switch.
 * 
 * @author David Underhill
 */
public class OpenFlowSwitch extends NodeWithPorts {
    public OpenFlowSwitch(long dpid) {
        this("", 0, 0, dpid);
    }
    
    public OpenFlowSwitch(String name, int x, int y, long dpid) {
        super(name, x, y, new ShapeIcon(DEFAULT_SHAPE, DEFAULT_FILL));
        this.datapathID = dpid;
    }
    
    
    // ------------------- Drawing ------------------ //
    
    /** default size of the DEFAULT_SHAPE */
    public static final int DEFAULT_SIZE = 40;
    
    /** default shape used to represent a switch */
    public static final Shape DEFAULT_SHAPE = new Ellipse2D.Double(0, 0, DEFAULT_SIZE, DEFAULT_SIZE);
    
    /** default fill color for DEFAULT_SHAPE */
    public static final Paint DEFAULT_FILL = new Color(128, 128, 255);
    
    /** 
     * Uses super.drawObject() to do most of the work and then draws switch
     * description stats if the switch is being hovered over or is selected.
     */
    public void drawObject(Graphics2D gfx) {
        super.drawObject(gfx);
        
        int x = getX() - getIcon().getWidth() / 2;
        int y = getY() - getIcon().getHeight() / 2;
        if(SHOW_NAMES)
            y += gfx.getFontMetrics().getHeight();
            
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
    }
    
 
    // --------------------- ID --------------------- //
    
    /** datapath ID of this switch */
    private long datapathID;
    
    /** when switch description stats were last updated */
    private long descUpdateTime = 0;
    
    /** returns a string version of the switch's datapath ID */
    public String getDebugName() {
        return DPIDUtil.dpidToHex(datapathID);
    }
    
    /** gets the datapath ID of this switch */
    public long getDatapathID() {
        return datapathID;
    }
    
    /** sets the datapath ID of this switch */
    public void setDatapathID(long dpid) {
        this.datapathID = dpid;
    }
    
    
    // ------------- Description Stats -------------- //
    
    /** switch description stats */
    private String manufacturer="?", hw_desc="?", sw_desc="?", serial_num="?";
    
    /** Returns true if s is not null, non-zero length, and not "None" or "?" */
    private boolean isStringSet(String s) {
        return s!=null && s.length()>0 && !s.equals("None") && !s.equals("?");
    }
    
    /** gets the manufacturer of the switch */
    public String getManufacturer() {
        return manufacturer;
    }
    
    /** gets the hardware description of the switch */
    public String getHWDescription() {
        return hw_desc;
    }
    
    /** gets the software description of the switch */
    public String getSWDescription() {
        return sw_desc;
    }
    
    /** gets the serial n umber of the switch */
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
    }
    
    
    // -------------------- Other ------------------- //
    
    public boolean isWithin(int x, int y) {
        return isWithin(x, y, getIcon().getSize());
    }

    public String toString() {
        return getName() + "; dpid=" + DPIDUtil.dpidToHex(getDatapathID());
    }
}
