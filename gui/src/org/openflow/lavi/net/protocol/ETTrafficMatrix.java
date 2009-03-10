package org.openflow.lavi.net.protocol;

import java.io.DataOutput;
import java.io.IOException;

import org.openflow.util.string.StringOps;

/**
 * Tells the backend about what kind of traffic matrix to generate.
 * 
 * @author David Underhill
 *
 */
public class ETTrafficMatrix extends LAVIMessage {
    public final boolean use_hw;
    public final int k;
    public final float demand;
    public final float edge;
    public final float agg;
    public final int plen;
    
    public ETTrafficMatrix(boolean use_hw, int k, float demand, float edge, float agg, int plen) {
        super(LAVIMessageType.ET_TRAFFIX_MATRIX, 0);
        if(demand<0 || demand>1)
            throw new IllegalArgumentException("demand must be between 0.0 and 1.0 inclusive");
        if(edge<0 || edge>1)
            throw new IllegalArgumentException("edge must be between 0.0 and 1.0 inclusive");
        if(agg<0 || agg>1)
            throw new IllegalArgumentException("agg must be between 0.0 and 1.0 inclusive");
        if(edge + agg > 1.0)
            throw new IllegalArgumentException("edge + agg must be less than 1.0 when summed");
        this.use_hw = use_hw;
        this.k = k;
        this.demand = demand;
        this.edge = edge;
        this.agg = agg;
        this.plen = plen;
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + 24;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(use_hw ? 1 : 0);
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
        return(k==em.k && demand==em.demand && edge==em.edge && agg==em.agg && plen==em.plen);
    }
    
    public int hashCode() {
        int hash = 29;
        hash *= k;
        hash = hash * 32 + Float.floatToIntBits(demand);
        hash = hash * 32 + Float.floatToIntBits(edge);
        hash = hash * 32 + Float.floatToIntBits(agg);
        return hash * 32 + plen + (use_hw ? 1 : 0);
    }
    
    public String toString() {
        return super.toString() + TSSEP + toStringShort();
    }

    public String toStringShort() {
        return StringOps.formatBitsPerSec(demand,1000*1000*1000) + " edge=" + edge + "% agg=" + agg + "% plen=" + plen + "B";
    }
}
