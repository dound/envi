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
public class OPSFTable extends OPStateField {
    /** Depth of table */
    public final int depth;
    
    /** List of fields */
    public final OPStateField[] fields;

    public OPSFTable(String name, String desc, boolean readOnly, DataInput in) throws IOException {
        super(name, desc, readOnly);

        depth = in.readShort();
        
        int numFields = in.readShort();
        fields = new OPStateField[numFields];
        
        for (int i = 0; i < numFields; i++)
            fields[i] = OPStateField.createFieldFromStream(in);
    }

    public String toString() {
        String fieldsStr = null;
        for (OPStateField f : fields) {
            if (fieldsStr != null)
                fieldsStr += ", ";
            fieldsStr += f.toString();
        }

        return super.toString() + " type=table depth=" + depth +
            " fields=[" + fieldsStr + "]"; 
    }
}
