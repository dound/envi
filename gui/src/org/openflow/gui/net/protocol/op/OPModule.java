package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

import org.openflow.gui.net.SocketConnection;
import org.openflow.gui.net.protocol.Node;
import org.openflow.util.string.DPIDUtil;

/**
 * Structure to specify a module.
 * 
 * @author David Underhill
 */
public class OPModule extends Node {
    public static final int NAME_LEN = 32;

    /** extracts the portion of the ID which correspond to module ID */
    public static final int extractModuleID(long id) {
        return (int)(id & 0x00000000FFFFFFFFL);
    }
    
    /** extracts the portion of the ID which correspond to module copy ID */
    public static final int extractCopyID(long id) {
        return (int)(id >>> 32);
    }
    
    /** create the node ID from its constituent parts */
    public static final long createNodeID(long mid, int cid) {
        if((0xFFFFFFFF00000000L & mid) != 0)
            throw new Error("Error: upper 4 bytes of module IDs should be 0 for original modules!  Got: " + mid);
        
        long clid = cid;
        return (clid << 32L) | mid;
    }

    /** name of the module */
    public final String name;
    public final OPModulePort ports[];
    
    public OPModule(DataInput in) throws IOException {
        super(in);
        name = SocketConnection.readString(in, NAME_LEN);

        int num_ports = in.readShort();
        ports = new OPModulePort[num_ports];
        for (int i = 0; i < num_ports; i++) {
            ports[i] = new OPModulePort(in);
        }
    }

    public int length() {
        return Node.SIZEOF + NAME_LEN + 2;
    }

    public String toString() {
        String strPorts;
        if(ports.length > 0)
            strPorts = ports[0].toString();
        else
            strPorts = "";

        for(int i=1; i<ports.length; i++)
            strPorts += ", " + ports[i].toString();

        return nodeType + "{" + DPIDUtil.toString(extractModuleID(id)) + "}-" + extractCopyID(id) + ":" + name +
        "-Ports[" + strPorts + "]";
    }
}
