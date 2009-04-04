package org.openflow.gui.stats;

import org.openflow.gui.Options;
import org.openflow.protocol.Match;

/** 
 * Statistics associated with a link.
 * 
 * @author David Underhill  
 */
public class LinkStats {
    /** the statistics on traffic from the source switch over this link */
    public final PortStatsRates statsSrc;
    
    /** the statistics on traffic from the destination switch over this link */
    public final PortStatsRates statsDst;
    
    public LinkStats(Match m) {
        this.statsSrc = new PortStatsRates(m);
        if(Options.USE_DIRECTED_LINKS)
            this.statsDst = null;
        else
            this.statsDst = new PortStatsRates(m);
    }
    
    public double getCurrentAverageDataRate() {
        if(Options.USE_DIRECTED_LINKS)
            return statsSrc.getBitsPerSec();
        else
            return (statsSrc.getBitsPerSec() + statsDst.getBitsPerSec()) / 2.0;
    }
}