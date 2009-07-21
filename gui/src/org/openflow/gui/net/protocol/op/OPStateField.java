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

    /** Types for use when unpacking */
    public static final int TYPE_INT = 1;
    public static final int TYPE_INT_CHOICE = 2;
    public static final int TYPE_TABLE = 3;

    /** name of the field */
    public final String name;

    /** description of the field */
    public final String desc;

    /** read only */
    public final boolean readOnly;

    public static OPStateField createFieldFromStream(DataInput in) throws IOException {
        byte type = in.readByte();
        String name = SocketConnection.readString(in, NAME_LEN);
        int descLen = in.readByte();
        String desc = SocketConnection.readString(in, descLen);
        boolean readOnly = in.readBoolean();
        switch (type) {
            case TYPE_INT: return new OPSFInt(name, desc, readOnly, in);
            case TYPE_INT_CHOICE: return new OPSFIntChoice(name, desc, readOnly, in);
            case TYPE_TABLE: return new OPSFTable(name, desc, readOnly, in);
        }

        // Should never get here, but just in case
        return null;
    }

    protected OPStateField(String name, String desc, boolean readOnly) {
        this.name = name;
        this.desc = desc;
        this.readOnly = readOnly;
    }

    public String toString() {
        return "OP_STATE_FIELD: name=" + name + " desc=" + desc +
        " readOnly=" + readOnly;
    }
}
