package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;

/**
 * Tells the GUI about a register associated with a module.
 * 
 * @author Glen Gibb
 *
 */
public class OPModuleReg {
    /** reg addr */
    public final int addr;
    
    /** reg name */
    public final String name;
    
    /** reg description */
    public final String desc;
    
    /** reg read-only */
    public final boolean rdOnly;
    
    public OPModuleReg(final int addr, final String name, final String desc, final boolean rdOnly) {
        this.addr = addr;
        this.name = name;
        this.desc = desc;
        this.rdOnly = rdOnly;
    }
    
    public OPModuleReg(final DataInput in) throws IOException {
        this.addr = in.readInt();
        in.readByte();
        this.name  = SocketConnection.readNullTerminatedString(in);
        in.readByte();
        this.desc = SocketConnection.readNullTerminatedString(in);
        this.rdOnly = in.readBoolean();
    }
    
    /** This returns the length of this message */
    public int length() {
        return 4 + 1 + name.length() + 1 + 1 + desc.length() + 1 + 1;
    }
    
    public String toString() {
        return "MODULE_REG: addr=" + String.format("0x%08x", addr) + " name='" + name + 
        "' desc='" + desc + "'" + " rdOnly=" + rdOnly;
    }
}