package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Informs the GUI about how many users are using a node.
 * 
 * @author David Underhill
 */
public class NodeNumUsers extends OFGMessage {
    public int SIZEOF = OFGMessage.SIZEOF + Node.SIZEOF + 4;
    
    /** the node this utilization is for */
    public final Node node;
    
    /** how many users a using the node */
    public final int num_users;
    
    /**
     * Construct a NodeNumUsers message.
     * 
     * @param in  where to read the message from 
     * 
     * @throws IOException  if message is not the correct size
     */
    public NodeNumUsers(int len, int xid, DataInput in) throws IOException  {
        super(OFGMessageType.NODE_USER_COUNT, xid);
        if(len != SIZEOF) throw new IOException(this.getClass().getName() + " message should be exactly " + SIZEOF + "B but received " + len + "B");
        this.node = new Node(in);
        this.num_users = in.readInt();
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return SIZEOF;
    }
    
    /** Writes the message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        node.write(out);
        out.writeFloat(num_users);
    }
    
    public String toString() {
        return super.toString() + TSSEP +  node.toString() + "=" + num_users;
    }
}
