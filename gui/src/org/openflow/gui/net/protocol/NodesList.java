package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A list of nodes.
 * 
 * @author David Underhill
 */
public abstract class NodesList extends OFGMessage {
    public final Node[] nodes;
    
    public NodesList(OFGMessageType t, final Node[] nodes) {
        this(t, 0, nodes);
    }
    
    public NodesList(OFGMessageType t, int xid, final Node[] nodes) {
        super(t, xid);
        this.nodes = nodes;
    }
    
    public NodesList(final int len, final OFGMessageType t, final int xid, final DataInput in) throws IOException {
        super(t, xid);
        
        // make sure the number of bytes leftover makes sense
        int left = len - super.length();
        if(left % Node.SIZEOF != 0) {
            throw new IOException("Body of switch list is not a multiple of " + 
                                  Node.SIZEOF + 
                                  " (length of body is " + left + " bytes)");
        }
        
        // read in the DPIDs
        int index = 0;
        nodes = new Node[left / Node.SIZEOF];
        while(left >= Node.SIZEOF) {
            left -= Node.SIZEOF;
            nodes[index++] = new Node(in);
        }
    }
    
    public int length() {
        return super.length() + nodes.length * Node.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        for(Node n : nodes)
            n.write(out);
    }
    
    public String toString() {
        String ret;
        if(nodes.length > 0)
            ret = nodes[0].toString();
        else
            ret = "";
        
        for(int i=1; i<nodes.length; i++)
            ret += ", " + nodes[i].toString();
        
        return super.toString() + TSSEP + ret;
    }
}
