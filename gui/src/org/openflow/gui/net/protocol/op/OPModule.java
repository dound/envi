package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;
import org.openflow.gui.net.protocol.Node;

/**
 * Structure to specify a module.
 * 
 * @author David Underhill
 */
public class OPModule extends Node {
    public static final int NAME_LEN = 32;
    public static final int SIZEOF = Node.SIZEOF + NAME_LEN;

    /** name of the module */
    public final String name;
    
    public OPModule(DataInput in) throws IOException {
        super(in);
        name = SocketConnection.readString(in, NAME_LEN);
    }
    
    public String toString() {
        return super.toString() + ":" + name;
    }
}
