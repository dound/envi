package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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
        if(left < 4)
            throw new IOException("Body of flows has a bad length (not enough bytes for # of flows field): " + left + "B left, need >=4B");
        
        // read in the flows
        flows = new Flow[in.readInt()];
        for(int flowOn=0; flowOn<flows.length; flowOn++) { 
            if(left < 32)
                throw new IOException("Body of flows has a bad length (not enough for a flow length): " + left + "B left, need >=32B");
            
            short type = in.readShort();
            int id = in.readInt();
            Node srcNode = new Node(in);
            short srcPort = in.readShort();
            Node dstNode = new Node(in);
            short dstPort = in.readShort();
            short pathLen = in.readShort();
            if(left < pathLen * (2 + Node.SIZEOF + 2))
                throw new IOException("Body of flows has a bad length (not enough for a flow)");
            
            FlowHop[] path = new FlowHop[pathLen];
            for(int i=0; i<pathLen; i++)
                path[i] = new FlowHop(in.readShort(), new Node(in), in.readShort());
            
            Flow f = new Flow(FlowType.typeValToMessageType(type), id, srcNode, srcPort, dstNode, dstPort, path);
            flows[flowOn] = f;
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
