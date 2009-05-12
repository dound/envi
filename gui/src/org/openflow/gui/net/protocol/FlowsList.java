package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;

import org.openflow.util.IDPortPair;

/**
 * A list of flows.
 * 
 * @author David Underhill
 */
public abstract class FlowsList extends OFGMessage {
    public Flow[] flows;
    
    public FlowsList(OFGMessageType t, final Flow[] flows) {
        this(t, 0, flows);
    }
    
    public FlowsList(OFGMessageType t, int xid, final Flow[] flows) {
        super(t, xid);
        this.flows = flows;
    }
    
    public FlowsList(final int len, final OFGMessageType t, final int xid, final DataInput in) throws IOException {
        super(t, xid);
        int left = len - super.length();
        
        // read in the flows
        LinkedList<Flow> flowList = new LinkedList<Flow>();
        while(left > 0) {
            if(left < 4)
                throw new IOException("Body of flows has a bad length (not enough for a flow length)");
            
            short type = in.readShort();
            int id = in.readInt();
            int pathLen = in.readInt();
            if(pathLen == 0)
                throw new IOException("Body of flows has a zero-length path");
            else if(left < pathLen * 12)
                throw new IOException("Body of flows has a bad length (not enough for a flow)");
            
            IDPortPair[] path = new IDPortPair[pathLen];
            for(int i=0; i<pathLen; i++)
                path[i] = new IDPortPair(in.readLong(), in.readShort());
            
            Flow f = new Flow(FlowType.typeValToMessageType(type), id, path);
            flowList.add(f);
            left -= f.length();
        }
    }
    
    public int length() {
        int len = super.length();
        for(Flow f : flows)
            len += f.length();
        return len;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out); 
        for(Flow f : flows)
            f.write(out);
    }
    
    public String toString() {
        String strFlows;
        if(flows.length > 0)
            strFlows = flows[0].toString();
        else
            strFlows = "";
        
        for(int i=1; i<flows.length; i++)
            strFlows += ", " + flows[i].toString();
        
        return super.toString() + TSSEP + strFlows;
    }
}
