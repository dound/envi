/**
 * 
 */
package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;
import org.openflow.gui.net.protocol.Node;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Set the state of a module
 * 
 * @author grg
 *
 */
public class OPSetStateValues extends OFGMessage {
    /** the module on which to set state */
    public final Node module;
    
    /** List of state values */
    public final OPStateValue[] values;

    public OPSetStateValues(final int len, final int xid, DataInput in) throws IOException {
        super(OFGMessageType.OP_SET_STATE_VALUES, xid);
        
        module = new Node(in);
        
        int numValues = in.readInt();
        values = new OPStateValue[numValues];
        
        for (int i = 0; i < numValues; i++)
            values[i] = OPStateValue.createValueFromStream(in);
    }
    
    /** Get the length */
    public int length() {
        int valuesLen = 0;
        for (OPStateValue v : values)
            valuesLen += v.length();
        
        return super.length() + Node.SIZEOF + valuesLen;
    }
}
