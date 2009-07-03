package org.openflow.gui.drawables;

import java.awt.Color;
import java.awt.Graphics2D;

import org.openflow.gui.net.protocol.NodeType;
import org.pzgui.icon.Icon;

public class OPModule extends OPNodeWithNameAndPorts {
    /** List of ports associated with the module */
    private OPModulePort ports[];

    /**
     * Creates a new "original" module (isOriginal() will return true).
     */
    public OPModule(boolean hw, String name, long id, Icon icon, OPModulePort ports[]) {
        super(hw ? NodeType.TYPE_MODULE_HW : NodeType.TYPE_MODULE_SW, name, id, icon);
        this.ports = ports;
        if(getCopyID() != 0)
            throw new Error("Error: copy ID should be 0 for original modules!  Got: " + getCopyID());
        setNameColor(Color.WHITE);
        original = true;
    }
    
    /**
     * Returns a copy of mToCopy whose isOriginal() method will return false.
     */
    public OPModule(OPModule mToCopy) {
        this(mToCopy, org.openflow.gui.net.protocol.op.OPModule.createNodeID(mToCopy.getID(), NEXT_COPY_ID++));
    }

    /**
     * Returns a copy of mToCopy whose isOriginal() method will return false.
     */
    public OPModule(OPModule mToCopy, long newID) {
        super(mToCopy.getType(), mToCopy.getName(), newID, mToCopy.getIcon());
        ports = mToCopy.ports;
        original = false;
        setNameColor(Color.YELLOW);
    }
    
    /** returns true if this module is a hardware module */
    public boolean isHardwareModule() {
        return getType() == NodeType.TYPE_MODULE_HW;
    }
    
    /** node on which this module is installed, if any */
    private OPNodeWithNameAndPorts nodeInstalledOn = null;
    
    /** gets the node the module is installed on */
    public OPNodeWithNameAndPorts getNodeInstalledOn() {
        return nodeInstalledOn;
    }
    
    /** global counter */
    private static int NEXT_COPY_ID = 1;
    
    /**
     * Returns the unique ID associated with this module type.
     */
    public int getModuleID() {
        return org.openflow.gui.net.protocol.op.OPModule.extractModuleID(getID());
    }
    
    /**
     * Returns the unique copy ID for this module if it is not an original.
     * All originals have copy ID 0.
     * 
     * @return  the copy ID
     */
    public int getCopyID() {
        return org.openflow.gui.net.protocol.op.OPModule.extractCopyID(getID());
    }
    
    /** 
     * Tries to install the module on a node - returns false if n is not 
     * compatible with this module as per isCompatibleWith().
     * 
     * @param n  the module to install on (null indicates that it is uninstalled)
     * 
     * @return true on success, false if incompatible
     */
    public boolean installOnNode(OPNodeWithNameAndPorts n) {
        if(n==null || isCompatibleWith(n)) {
            nodeInstalledOn = n;
            return true;
        }
        else
            return false;
    }
    
    /** uninstalls a module */
    public void uninstall() {
        installOnNode(null);
    }
    
    /**
     * Returns whether this module is compatible with n (i.e., it can be 
     * installed on n).
     */
    public boolean isCompatibleWith(OPNodeWithNameAndPorts n) {
        if(getType()==NodeType.TYPE_MODULE_HW && n.getType()==NodeType.TYPE_NETFPGA)
            return true;
        
        if(getType()==NodeType.TYPE_MODULE_SW && n.getType()==NodeType.TYPE_PC)
            return true;
        
        return false;
    }
    
    /** status of the module, if known */
    private String status = "Unknown: not yet queried.";
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String s) {
        status = s;
    }
    
    /** whether the module is an original (i.e., do not remove) */
    private boolean original;
    
    /** whether the module is an original (else it is a dragged copy) */
    public boolean isOriginal() {
        return original;
    }
    
    /** whether the module is being dragged (only used with original modules) */
    private boolean dragging = false;
    
    /** where the module is being dragged */
    private int dragX, dragY;
    
    public int getDragX() {
        return dragX;
    }
    
    public int getDragY() {
        return dragY;
    }
    
    /** Draw the object using super.drawObject() and then add the name in the middle */
    public void drawObject(Graphics2D gfx) {
        super.drawObject(gfx);

        int x = getX();
        int y = getY();
        
        // no more to do if this isn't an original
        if(!isOriginal())
            return;
        
        if(dragX!=x || dragY!=y) {
            super.setPos(dragX, dragY);
            super.drawObject(gfx);
            super.setPos(x, y);
        }
    }
    
    public void setXPos(int x) {
        super.setXPos(x);
        if(!dragging)
            dragX = x;
    }
    
    public void setYPos(int y) {
        super.setYPos(y);
        if(!dragging)
            dragY = y;
    }
    
    /**
     * Originals stay in place while a copy of their image is dragged.  
     * Non-originals are dragged as usual.
     */
    public void drag(int x, int y) {
        if(isOriginal()) {
            dragX = x;
            dragY = y;
            dragging = true;
        }
        else
            super.drag(x, y);
    }
    
    public void dragDone() {
        dragging = false;
    }

    public OPModulePort[] getPorts() {
        return ports;
    }
}
