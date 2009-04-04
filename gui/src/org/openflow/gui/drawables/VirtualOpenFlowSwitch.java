package org.openflow.gui.drawables;

/**
 * A part of an OpenFlowSwitch which is displayed independently of the rest of
 * the switch.
 * 
 * @author David Underhill
 */
public class VirtualOpenFlowSwitch extends OpenFlowSwitch {
    private final int virtualSwitchIndex;
    
    public VirtualOpenFlowSwitch(long parentDPID, int virtualSwitchIndex) {
        super(parentDPID);
        this.virtualSwitchIndex = virtualSwitchIndex;
    }
    
    public VirtualOpenFlowSwitch(String name, int x, int y, long parentDPID, int virtualSwitchIndex) {
        super(name, x, y, parentDPID);
        this.virtualSwitchIndex = virtualSwitchIndex;
    }
    
    public int getVirtualSwitchIndex() {
        return virtualSwitchIndex;
    }
}
