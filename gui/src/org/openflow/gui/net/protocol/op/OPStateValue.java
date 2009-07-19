/**
 * 
 */
package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;

/**
 * Value of a state variable
 * 
 * @author grg
 *
 */
public abstract class OPStateValue {
    /** length of a name */
    public static final int NAME_LEN = 16;
    
    /** Field types */
    public static final int TYPE_INT = 1;
    public static final int TYPE_TABLE_ENTRY = 2;
    
    /** field name */
    public final String name;
    
    public static OPStateValue createValueFromStream(DataInput in) throws IOException {
        byte type = in.readByte();
        String name = SocketConnection.readString(in, NAME_LEN);
        switch (type) {
            case TYPE_INT: return new OPSVInt(name, in);
            case TYPE_TABLE_ENTRY: return new OPSVTableEntry(name, in);
        }
        
        // Should never get here, but just in case
        return null;
    }
    
    protected OPStateValue(String name) {
        this.name = name;
    }
    
    public String toString() {
        return "OP_STATE_VALUE name=" + name;
    }

    public int length() {
        return 1 + NAME_LEN;
    }
}
