package org.openflow.lavi.net.protocol;

import java.io.IOException;
import org.openflow.lavi.net.util.ByteBuffer;
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
    public StatsHeader(boolean request, final ByteBuffer buf) throws IOException {
        super(request ? LAVIMessageType.STAT_REQUEST : LAVIMessageType.STAT_REPLY, 
              buf);
        
        dpid = buf.nextLong();
        statsType = StatsType.typeValToStatsType(buf.nextShort());
        flags = StatsFlag.typeValToStatsFlag(buf.nextShort());
    }
    
    public int length() {
        return super.length() + 12;
    }
}
