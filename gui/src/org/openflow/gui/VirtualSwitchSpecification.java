package org.openflow.gui;

import java.util.Vector;

import org.openflow.gui.drawables.VirtualOpenFlowSwitch;

/**
 * Specifies how a single switch should be displayed (as multiple switches).
 * 
 * @author David Underhill
 */
public class VirtualSwitchSpecification {
    /** datapth ID of the switch to virtualize */
    private final long dpid;
    
    public class VirtualSwitchAndPorts {
        public VirtualOpenFlowSwitch v;
        public Vector<Integer> ports;
        public VirtualSwitchAndPorts() {
            ports = new Vector<Integer>();
        }
    }
    
    /**
     * List of ports belonging to each virtual switch.  As an example the list
     * [ [0,1,2,3], [4,5,6,7] ] makes dpid show up as two 4-port switches: one 
     * with ports 0 to 3 while the other has ports 4 to 7.  A switch should 
     * either be fully virtualized or not at all; ports not virtualized on a 
     * partially virtualized switch will not be displayed.  The first index is 
     * the virtual switch number.  The second index specifies which port on the
     * real switch corresponds to the ith port of the virtual switch.
     */
    private final Vector<VirtualSwitchAndPorts> virtualSwitchesPorts;
    
    public VirtualSwitchSpecification(long dpid) {
        this.dpid = dpid;
        this.virtualSwitchesPorts = new Vector<VirtualSwitchAndPorts>();
    }
    
    /** gets the datapth ID of the switch being virtualized for display purposes */
    public long getParentDPID() {
        return dpid;
    }
    
    public void addVirtualSwitchPort(int virtualSwitchIndex, int port) {
        while(virtualSwitchesPorts.size() <= virtualSwitchIndex)
            virtualSwitchesPorts.add(new VirtualSwitchAndPorts());
        virtualSwitchesPorts.get(virtualSwitchIndex).ports.add(port);
    }
    
    public int getNumVirtualSwitches() {
        return virtualSwitchesPorts.size();
    }
    
    public VirtualSwitchAndPorts getVirtualSwitch(int virtualSwitchIndex) {
        return virtualSwitchesPorts.get(virtualSwitchIndex); 
    }
    
    /** 
     * Returns the virtual switch associated with the specified port number,
     * or null if no such port is virtualized.
     */
    public VirtualOpenFlowSwitch getVirtualSwitchByPort(int port) {
        for(VirtualSwitchAndPorts v : virtualSwitchesPorts)
            for(int p : v.ports)
                if(port == p)
                    return v.v;
        return null;
    }
}
