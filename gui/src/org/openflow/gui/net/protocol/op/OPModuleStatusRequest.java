package org.openflow.gui.net.protocol.op;

import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.Node;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Asks the backend about a status of a module.
 * 
 * @author David Underhill
 */
public class OPModuleStatusRequest extends OFGMessage {
    /** the node the module is on */
    public final Node node;
    
    /** the module */
    public final Node module;
    
    public OPModuleStatusRequest(Node node, Node module) {
        super(OFGMessageType.OP_MODULE_STATUS_REQUEST, 0);
        if(!module.nodeType.isModule())
            throw new Error("Error: module must be of type OPModule");
        
        this.node = node;
        this.module = module;
    }
    
    /** This returns the length of this message */
    public int length() {
        return super.length() + 2 * Node.SIZEOF;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        node.write(out);
        module.write(out);
    }
    
    public String toString() {
        return super.toString() + TSSEP + "Request for status of module " + module + " on " + node;
    }
}
