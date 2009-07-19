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
 * Message to read a collection of registers
 * @author grg
 *
 */
public class OPReadStateValues extends OFGMessage {
    /**
     * Maximum length of a register name
     */
    public static final int NAME_LEN = 16;
    
    /** the module from which to read state */
    public final Node module;
    
    /** List of values to read */
    public final String[] values;

    public OPReadStateValues(final int len, final int xid, DataInput in) throws IOException {
        super(OFGMessageType.OP_READ_STATE_VALUES, xid);
        
        module = new Node(in);
        
        int numValues = (len - super.length() - Node.SIZEOF) / NAME_LEN;
        values = new String[numValues];
        
        for (int i = 0; i < numValues; i++)
            values[i] = SocketConnection.readString(in, NAME_LEN);
    }
    
    /** Get the length */
    public int length() {
        return super.length() + Node.SIZEOF + values.length * NAME_LEN;
    }
}
