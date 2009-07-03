package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;

/**
 * Tells the GUI about a test of the system.
 * 
 * @author Glen Gibb
 *
 */
public class OPModulePort {
    /** port id */
    public final short id;
    
    /** port name */
    public final String name;
    
    /** port description */
    public final String desc;
    
    public OPModulePort(final short id, final String name, final String desc) {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }
    
    public OPModulePort(final DataInput in) throws IOException {
        this.id = in.readShort();
        in.readByte();
        this.name  = SocketConnection.readNullTerminatedString(in);
        in.readByte();
        this.desc = SocketConnection.readNullTerminatedString(in);
    }
    
    /** This returns the length of this message */
    public int length() {
        return 2 + 1 + name.length() + 1 + 1 + desc.length() + 1;
    }
    
    public String toString() {
        return "MODULE_PORT: id=" + id + " name='" + name + 
        "' desc='" + desc + "'";
    }
}