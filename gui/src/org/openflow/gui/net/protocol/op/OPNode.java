package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;
import org.openflow.gui.net.protocol.Node;

/**
 * Structure to specify a node.
 * 
 * @author Glen Gibb
 */
public class OPNode extends Node {
    public static final int NAME_LEN = 32;
    public static final int DESC_LEN = 128;
    public static final int SIZEOF = Node.SIZEOF + NAME_LEN + DESC_LEN;

    /** name of the node */
    public final String name;
    
    /** description of the node */
    public final String desc;
    
    public OPNode(DataInput in) throws IOException {
        super(in);
        name = SocketConnection.readString(in, NAME_LEN);
        desc = SocketConnection.readString(in, DESC_LEN);
    }

    public String toString() {
        return super.toString() + " name=" + name + " desc=" + desc; 
    }
}
