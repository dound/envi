package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * A list of nodes.
 * 
 * @author Glen Gibb
 */
public abstract class OPNodesList extends OFGMessage {
    public OPNode[] nodes;
    
    public OPNodesList(OFGMessageType t, final OPNode[] nodes) {
        this(t, 0, nodes);
    }
    
    public OPNodesList(OFGMessageType t, int xid, final OPNode[] nodes) {
        super(t, xid);
        this.nodes = nodes;
    }
    
    public OPNodesList(final int len, final OFGMessageType t, final int xid, final DataInput in) throws IOException {
        super(t, xid);
        // make sure the number of bytes leftover makes sense
        int left = len - super.length();
        if(left % OPNode.SIZEOF != 0) {
            throw new IOException("Body of nodes list is not a multiple of " + OPNode.SIZEOF + " (length of body is " + left + " bytes)");
        }
        
        // read in the DPIDs
        int index = 0;
        nodes = new OPNode[left / OPNode.SIZEOF];
        while(left >= OPNode.SIZEOF) {
            left -= OPNode.SIZEOF;
            nodes[index++] = new OPNode(in);
        }
    }
    
    public int length() {
        return super.length() + nodes.length * OPNode.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        for(OPNode n : nodes)
            n.write(out);
    }
    
    public String toString() {
        String strNodes;
        if(nodes.length > 0)
            strNodes = nodes[0].toString();
        else
            strNodes = "";
        
        for(int i=1; i<nodes.length; i++)
            strNodes += ", " + nodes[i].toString();
        
        return super.toString() + TSSEP + strNodes;
    }
}
