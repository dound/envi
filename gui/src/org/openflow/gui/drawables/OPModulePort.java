package org.openflow.gui.drawables;

public class OPModulePort {
    /** Port ID */
    private short id;
    
    /** Name of port */
    private String name;
    
    /** Description of port */
    private String desc;
    
    public OPModulePort(short id, String name, String desc) {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}