package org.openflow.gui.drawables;

public class OPModuleReg {
    /** Reg addr */
    private int addr;
    
    /** Name of reg */
    private String name;
    
    /** Description of reg */
    private String desc;
    
    /** Is register read-only */
    private boolean rdOnly;
    
    public OPModuleReg(int addr, String name, String desc, boolean rdOnly) {
        this.addr = addr;
        this.name = name;
        this.desc = desc;
        this.rdOnly = rdOnly;
    }

    public int getAddr() {
        return addr;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean canWrite() {
        return !rdOnly;
    }
}