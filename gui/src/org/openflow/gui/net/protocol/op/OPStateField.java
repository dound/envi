/**
 * 
 */
package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;

/**
 * Describe a state variable field
 * 
 * @author grg
 *
 */
public class OPStateField {
    /** Length of a state variable name */
    public static final int NAME_LEN = 16;

    /** name of the field */
    public final String name;
    
    /** description of the field */
    public final String desc;
    
    /** read only */
    public final boolean readOnly;
    
    public final OPStateType type;
    
    public OPStateField(DataInput in) throws IOException {
        name = SocketConnection.readString(in, NAME_LEN);
        int descLen = in.readByte();
        desc = SocketConnection.readString(in, descLen);
        readOnly = in.readBoolean();
        type = OPStateType.createTypeFromStream(in);
    }

    public String toString() {
        return "OP_STATE_FIELD: name=" + name + " desc=" + desc + 
        " readOnly=" + readOnly + " type=[" + type + "]"; 
    }	
}
