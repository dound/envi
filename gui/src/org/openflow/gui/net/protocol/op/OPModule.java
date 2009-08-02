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
    
    /** check if the ID indicates that the node is being prepared */
    public static final boolean isPreparing(long id) {
        return (0x8000000000000000L & id) != 0;
    }

    /** name of the module */
    public final String name;
    public final OPModulePort ports[];
    public final OPStateField stateFields[];
    
    public OPModule(DataInput in) throws IOException {
        super(in);
        name = SocketConnection.readString(in, NAME_LEN);

        int numPorts = in.readShort();
        ports = new OPModulePort[numPorts];
        for (int i = 0; i < numPorts; i++) {
            ports[i] = new OPModulePort(in);
        }

        int numStateFields = in.readShort();
        stateFields = new OPStateField[numStateFields];
        for (int i = 0; i < numStateFields; i++) {
            stateFields[i] = OPStateField.createFieldFromStream(in);
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

        String strStateFields;
        if(stateFields.length > 0)
            strStateFields = stateFields[0].toString();
        else
            strStateFields = "";

        for(int i=1; i<stateFields.length; i++)
            strStateFields += ", " + stateFields[i].toString();


        return nodeType + "{" + DPIDUtil.toString(extractModuleID(id)) + "}-" + extractCopyID(id) + ":" + name +
        "-Ports[" + strPorts + "]" + "-StateFields[" + strStateFields + "]";
    }
}
