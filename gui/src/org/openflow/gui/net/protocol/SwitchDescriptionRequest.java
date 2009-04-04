package org.openflow.gui.net.protocol;

import java.io.IOException;
import org.openflow.protocol.StatsFlag;
import org.openflow.protocol.StatsType;

/**
 * Request for info describing a switch.
 *  
 * @author David Underhill
 */
public class SwitchDescriptionRequest extends StatsHeader {
    /** Create a request for stats from the switch with its description */
    public SwitchDescriptionRequest(long dpid) throws IOException {
        this(dpid, StatsFlag.NONE);
    }
    
    /** Create a request for stats from the switch with its description */
    public SwitchDescriptionRequest(long dpid, StatsFlag flag) throws IOException {
        super(StatsHeader.REQUEST,
              dpid,
              StatsType.DESC,
              StatsFlag.NONE);
    }
}
