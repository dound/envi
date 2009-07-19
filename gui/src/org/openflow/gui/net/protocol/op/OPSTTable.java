/**
 * 
 */
package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

/**
 * @author grg
 *
 */
public class OPSTTable extends OPStateType {
    /** Depth of table */
    public final int depth;
    
    /** List of fields */
    public final OPStateField[] fields;

    public OPSTTable(DataInput in) throws IOException {
        depth = in.readShort();
        
        int numFields = in.readShort();
        fields = new OPStateField[numFields];
        
        for (int i = 0; i < numFields; i++)
            fields[i] = new OPStateField(in);
    }

    public String toString() {
        String fieldsStr = null;
        for (OPStateField f : fields) {
            if (fieldsStr != null)
                fieldsStr += ", ";
            fieldsStr += f.toString();
        }

        return "OP_STATE_TYPE: type=table depth=" + depth + 
            " fields=[" + fieldsStr + "]"; 
    }
}
