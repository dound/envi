package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.openflow.protocol.*;
import org.openflow.util.string.DPIDUtil;

/**
 * Stats related request or reply.
 *
 * @author David Underhill
 */
public abstract class StatsHeader extends OFGMessage {
    public static final boolean REQUEST = true;
    public static final boolean REPLY = false;
    
    public long dpid;
    public StatsType statsType;
    public StatsFlag flags;
    
    public StatsHeader(boolean request, long dpid, StatsType statsType, StatsFlag flag) {
        this(request, 0, dpid, statsType, flag);
    }
    
    public StatsHeader(boolean request, int xid, long dpid, StatsType statsType, StatsFlag flags) {
        super(request ? OFGMessageType.STAT_REQUEST : OFGMessageType.STAT_REPLY, xid);
        this.statsType = statsType;
        this.flags = flags;
        this.dpid = dpid;
    }
    
    /** used to construct a message being received */
    public StatsHeader(boolean request, final int xid, final DataInput in) throws IOException {
        super(request ? OFGMessageType.STAT_REQUEST : OFGMessageType.STAT_REPLY, 
              xid);
        
        dpid = in.readLong();
        statsType = StatsType.typeValToStatsType(in.readShort());
        flags = StatsFlag.typeValToStatsFlag(in.readShort());
    }
    
    public int length() {
        return super.length() + 12;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeLong(dpid);
        out.writeShort(statsType.getTypeID());
        out.writeShort(flags.getTypeID());
    }
    
    public String toString() {
        return super.toString() + TSSEP + "type=" + statsType.toString()
                                        + " switch=" + DPIDUtil.toString(dpid)
                                        + " flags=" + flags.toString();
    }
}
