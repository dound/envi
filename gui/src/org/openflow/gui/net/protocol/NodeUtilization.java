package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Informs the GUI about how utilized a node is.
 * 
 * @author David Underhill
 */
public class NodeUtilization extends OFGMessage {
    public int SIZEOF = OFGMessage.SIZEOF + Node.SIZEOF + 4;
    
    /** the node this utilization is for */
    public final Node node;
    
    /** how utilized the node is */
    public final float utilization;
    
    /**
     * Construct a NodeUtilization message.
     * 
     * @param in  where to read the message from 
     * 
     * @throws IOException  if message is not the correct size
     */
    public NodeUtilization(int len, int xid, DataInput in) throws IOException  {
        super(OFGMessageType.NODE_UTILIZATION, xid);
        if(len != SIZEOF) throw new IOException(this.getClass().getName() + " message should be exactly " + SIZEOF + "B but received " + len + "B");
        this.node = new Node(in);
        this.utilization = in.readFloat();
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return SIZEOF;
    }
    
    /** Writes the message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        node.write(out);
        out.writeFloat(utilization);
    }
    
    public String toString() {
        return super.toString() + TSSEP +  node.toString() + "=" + utilization;
    }
}
