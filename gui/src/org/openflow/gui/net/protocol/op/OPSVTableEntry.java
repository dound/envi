/**
 * 
 */
package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Table entry of a state variable
 * 
 * @author grg
 *
 */
public class OPSVTableEntry extends OPStateValue {
    /** entry number in table */
    public final int entry;
    
    /** list of values for fields */
    public final OPStateValue[] values;
    
    protected OPSVTableEntry(String name, DataInput in) throws IOException {
        super(name);
        
        entry = in.readShort();
        
        int numValues = in.readShort();
        values = new OPStateValue[numValues];
        
        for (int i = 0; i < numValues; i++)
            values[i] = OPStateValue.createValueFromStream(in);
    }

    public int length() {
        int valuesLen = 0;
        for (OPStateValue v : values)
            valuesLen += v.length();
        
        return super.length() + 2 + 2 + valuesLen;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeShort(entry);
        out.writeShort(values.length);
        for (OPStateValue v : values)
            v.write(out);
    }

    @Override
    protected int getType() {
        return TYPE_TABLE_ENTRY;
    }

    public String toString() {
        String valuesStr = null;
        for (OPStateValue v : values) {
            if (valuesStr != null)
                valuesStr += ", ";
            valuesStr += v.toString();
        }

        return super.toString() + " type=table_entry entry=" + entry + 
            " values=[" + valuesStr + "]"; 
    }	
}
