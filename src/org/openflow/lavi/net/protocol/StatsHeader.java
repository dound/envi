package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.openflow.protocol.*;

/**
 * Stats related request or reply.
 *
 * @author David Underhill
 */
public abstract class StatsHeader extends LAVIMessage {
    public static final boolean REQUEST = true;
    public static final boolean REPLY = false;
    
    public long dpid;
    public StatsType statsType;
    public StatsFlag flags;
    
    public StatsHeader(boolean request, long dpid, StatsType statsType, StatsFlag flag) {
        this(request, 0, dpid, statsType, flag);
    }
    
    public StatsHeader(boolean request, int xid, long dpid, StatsType statsType, StatsFlag flags) {
        super(request ? LAVIMessageType.STAT_REQUEST : LAVIMessageType.STAT_REPLY, xid);
        this.statsType = statsType;
        this.flags = flags;
        this.dpid = dpid;
    }
    
    /** used to construct a message being received */
    public StatsHeader(boolean request, final DataInput in) throws IOException {
        super(request ? LAVIMessageType.STAT_REQUEST : LAVIMessageType.STAT_REPLY, 
              in);
        
        dpid = in.readLong();
        statsType = StatsType.typeValToStatsType(in.readShort());
        flags = StatsFlag.typeValToStatsFlag(in.readShort());
    }
    
    public int length() {
        return super.length() + 12;
    }
    
    public void send(DataOutput out) throws IOException {
    	super.write(out);
    	out.writeLong(dpid);
    	out.writeShort(statsType.getTypeID());
    	out.writeShort(flags.getTypeID());
    }
}
