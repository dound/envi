package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

/**
 * Type of a state variable
 * 
 * @author grg
 *
 */
public abstract class OPStateType {
    public static final int SIZE = 1;
    
    public static final int TYPE_INT = 1;
    public static final int TYPE_INT_CHOICE = 2;
    public static final int TYPE_TABLE = 3;

    public static OPStateType createTypeFromStream(DataInput in) throws IOException {
        byte type = in.readByte();
        switch (type) {
            case TYPE_INT: return new OPSTInt(in);
            case TYPE_INT_CHOICE: return new OPSTIntChoice(in);
            case TYPE_TABLE: return new OPSTTable(in);
        }
        
        // Should never get here, but just in case
        return null;
    }
}
