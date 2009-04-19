package org.openflow.gui.net.protocol.op;

import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.Node;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Tells the backend to move a module.
 * 
 * @author David Underhill
 *
 */
public class OPMoveModule extends OFGMessage {
    /** the module being moved */
    public final Node module;
    
    /** the place it is being moved from */
    public final Node from;
    
    /** the place it is being moved to */
    public final Node to;
    
    public OPMoveModule(Node module, Node from, Node to) {
        super(OFGMessageType.OP_MOVE_MODULE, 0);
        this.module = module;
        this.from = from;
        this.to = to;
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + 3 * Node.SIZEOF;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        module.write(out);
        from.write(out);
        to.write(out);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "move " + module + " from " + from + " to " + to;
    }
}
