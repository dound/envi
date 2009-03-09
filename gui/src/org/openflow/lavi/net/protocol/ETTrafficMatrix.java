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
    public final int k;
    public final int demand;
    public final int edge;
    public final int agg;
    public final int plen;
    
    public ETTrafficMatrix(int k, int demand, int edge, int agg, int plen) {
        super(LAVIMessageType.ET_TRAFFIX_MATRIX, 0);
        this.k = k;
        this.demand = demand;
        this.edge = edge;
        this.agg = agg;
        this.plen = plen;
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + 20;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(k);
        out.writeInt(demand);
        out.writeInt(edge);
        out.writeInt(agg);
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
        hash = hash * 32 + demand;
        hash = hash * 32 + edge;
        hash = hash * 32 + agg;
        return hash * 32 + plen;
    }
    
    public String toString() {
        return super.toString() + TSSEP + toStringShort();
    }

    public String toStringShort() {
        return "k=" + k + " " +  StringOps.formatBitsPerSec(demand) + " edge=" + edge + "% agg=" + agg + "% plen=" + plen + "B";
    }
}
