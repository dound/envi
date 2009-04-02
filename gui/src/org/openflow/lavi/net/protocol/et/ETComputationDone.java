package org.openflow.lavi.net.protocol.et;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.lavi.net.protocol.LAVIMessage;
import org.openflow.lavi.net.protocol.LAVIMessageType;

/**
 * Tells the GUI the computation finished and a little about the results.
 * 
 * @author David Underhill
 *
 */
public class ETComputationDone extends LAVIMessage {
    /** number of flows which could not be placed */
    public final int num_unplaced_flows;
    
    public ETComputationDone(final int len, final int xid, final DataInput in) throws IOException {
        super(LAVIMessageType.ET_COMPUTATION_DONE, xid);
        if(len != length())
            throw new IOException("ETBandwidth is " + len + "B - expected " + length() + "B");
        
        num_unplaced_flows = in.readInt();
    }
    
    public boolean isSuccess() {
        return num_unplaced_flows == 0;
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + 4;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(num_unplaced_flows);
    }
    
    public String toString() {
        return super.toString() + TSSEP + num_unplaced_flows  + " flows unplaced";
    }
}
