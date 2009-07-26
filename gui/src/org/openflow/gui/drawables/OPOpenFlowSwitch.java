package org.openflow.gui.drawables;

/**
 * OpenPipes version of OpenFlowSwitch
 * Incorporates a name and description
 * 
 * @author grg
 *
 */
public class OPOpenFlowSwitch extends OpenFlowSwitch {
    /** Description of switch */
    private String desc;
    
    public OPOpenFlowSwitch(long dpid) {
        this("", "", 0, 0, dpid);
    }
    
    public OPOpenFlowSwitch(String name, String desc, long dpid) {
        this(name, desc, 0, 0, dpid);
    }

    public OPOpenFlowSwitch(String name, String desc, int x, int y, long dpid) {
        super(name, x, y, dpid);
        this.setDesc(desc);
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
