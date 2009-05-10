package org.openflow.gui.net.protocol.et;

import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;
import org.openflow.util.string.StringOps;

/**
 * Tells the backend about what kind of traffic matrix to generate.
 * 
 * @author David Underhill
 *
 */
public class ETTrafficMatrix extends OFGMessage {
    public final boolean use_hw;
    public final boolean use_original_alg; // false => use heuristic
    public final boolean may_split_flows;
    public final int k;
    public final float demand;
    public final float edge;
    public final float agg;
    public final int plen;
    
    public ETTrafficMatrix(boolean use_hw, boolean use_original_alg, boolean may_split_flows, int k, float demand, float edge, float agg, int plen) {
        super(OFGMessageType.ET_TRAFFIX_MATRIX, 0);
        if(demand<0 || demand>1)
            throw new IllegalArgumentException("demand must be between 0.0 and 1.0 inclusive");
        if(edge<0 || edge>1)
            throw new IllegalArgumentException("edge must be between 0.0 and 1.0 inclusive");
        if(agg<0 || agg>1)
            throw new IllegalArgumentException("agg must be between 0.0 and 1.0 inclusive");
        if(edge + agg > 1.0)
            throw new IllegalArgumentException("edge + agg must be less than 1.0 when summed");
        this.use_hw = use_hw;
        this.use_original_alg = use_original_alg;
        this.may_split_flows = may_split_flows;
        this.k = k;
        this.demand = demand;
        this.edge = edge;
        this.agg = agg;
        this.plen = plen;
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + 23;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeBoolean(use_hw);
        out.writeBoolean(use_original_alg);
        out.writeBoolean(may_split_flows);
        out.writeInt(k);
        out.writeFloat(demand);
        out.writeFloat(edge);
        out.writeFloat(agg);
        out.writeInt(plen);
    }
    
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof ETTrafficMatrix)) return false;
        ETTrafficMatrix em = (ETTrafficMatrix)o;
        return(use_original_alg==em.use_original_alg && k==em.k && demand==em.demand && edge==em.edge && agg==em.agg && plen==em.plen);
    }
    
    public int hashCode() {
        int hash = 29;
        hash *= k;
        hash = hash * 32 + Float.floatToIntBits(demand);
        hash = hash * 32 + Float.floatToIntBits(edge);
        hash = hash * 32 + Float.floatToIntBits(agg);
        return hash * 32 + plen + (use_hw ? 1 : 0) + (use_original_alg ? 7 : 0);
    }
    
    public String toString() {
        String alg = use_original_alg ? "original" : "heuristic";
        return super.toString() + TSSEP + toStringShort() + " hw=" + use_hw + " alg=" + alg + " split=" + may_split_flows + " k=" + k;
    }

    public String toStringShort() {
        return StringOps.formatBitsPerSec(demand,1000*1000*1000) + " edge=" + Math.round(100*edge) + "% agg=" + Math.round(100*agg) + "% plen=" + plen + "B";
    }
}
