package org.openflow.protocol;

import java.util.LinkedList;

import org.openflow.util.string.IPUtil;

/**
 * Flow wildcards.  Equivalent to ofp_flow_wildcards.
 * 
 * @author David Underhill
 */
public class FlowWildcards {
    public enum Wildcard {
        /** Switch input port. */
        OFPFW_IN_PORT(1 << 0),

        /** VLAN. */
        OFPFW_DL_VLAN(1 << 1),

        /** Ethernet source address. */
        OFPFW_DL_SRC(1 << 2),

        /** Ethernet destination address. */
        OFPFW_DL_DST(1 << 3),

        /** Ethernet frame type. */
        OFPFW_DL_TYPE(1 << 4),

        /** IP protocol. */
        OFPFW_NW_PROTO(1 << 5),

        /** TCP/UDP source port. */
        OFPFW_TP_SRC(1 << 6),

        /** TCP/UDP destination port. */
        OFPFW_TP_DST(1 << 7),
        
        /** IP source address. */
        OFPFW_NW_SRC_SHIFT(1 << 8),
        
        /** IP destination address. */
        OFPFW_NW_DST_SHIFT(1 << 14),

        OFPFW_DL_VLAN_PCP( 1 << 20),
        OFPFW_NW_TOS(1 << 21), 

        
        /** Wildcard all fields */
        OFPFW_ALL((1 << 22) - 1),
        ;
        
        /** bits corresponding to the wildcard in the bitfield */
        public final int bitfield;

        private Wildcard(int bitfield) {
            this.bitfield = bitfield;
        }
    }
    
    /** wildcard bits */
    private int bitfield;
    
    public FlowWildcards() {
        this.bitfield = 0;
    }
    
    public FlowWildcards(int bitfield) {
        this.bitfield = bitfield;
    }

    /** returns the bits representing this wildcard */
    public int getAssociatedBits() {
        return bitfield;
    }

    /** Returns whether the bit for Wildcard w is set in bitfield */
    public boolean isSet(Wildcard w) {
        return isBitsSet(w.bitfield);
    }
    
    /** Sets whether the bit for Wildcard w is set in bitfield */
    public void set(Wildcard w, boolean enable) {
        setBits(enable, w.bitfield);
    }
    
    /** Sets all of the wiidcard bits directly */
    public void set(int bitfield) {
        this.bitfield = bitfield;
    }
    
    /**
     * Returns true if any bits in the bitmask are also set in bitfield.
     * @param bitmask  which bits to check to see if they are set
     * 
     * @return true if any of the bits are set
     */
    private boolean isBitsSet(int bitmask) {
        return (bitfield & bitmask) != 0;
    }
    
    /**
     * Returns true if any bits in the specified range are set.
     * @param start   first bit to set or unset
     * @param len     number of bits to set or unset
     */
    private boolean isBitsSet(int start, int len) {
        for(int i=start; i<start+len; i++)
            if(isBitSet(i))
                return true;
        
        return false;
    }
    
    /** Returns true if the specified bit offset is set */
    private boolean isBitSet(int bitOffset) {
        return (bitfield & (1 << bitOffset)) != 0;
    }
    
    /**
     * Sets or unsets the bits which are set in bitmask.
     * @param enable  whether to set or unset bits
     * @param bitmask  which bits to set
     */
    private void setBits(boolean enable, int bitmask) {
        if(enable)
            bitfield |= bitmask;
        else
            bitfield &= ~bitmask;
    }
    
    /**
     * Sets or unsets bits in the specified range.
     * @param enable  whether to set or unset bits
     * @param start   first bit to set or unset
     * @param len     number of bits to set or unset
     */
    private void setBits(boolean enable, int start, int len) {
        for(int i=start; i<start+len; i++) {
            if(enable)
                bitfield |= (1 << i);
            else
                bitfield &= ~(1 << i);
        }
    }

    /** Whether to wildcard on input port */
    public boolean isWildcardInputPort() {
        return isSet(Wildcard.OFPFW_IN_PORT);
    }
    
    /** Set whether to wildcard on input port */
    public void setWildcardInputPort(boolean enable) {
        set(Wildcard.OFPFW_IN_PORT, enable);
    }
    
    /** Whether to wildcard on VLAN */
    public boolean isWildcardVLAN() {
        return isSet(Wildcard.OFPFW_DL_VLAN);
    }
    
    /** Set whether to wildcard on VLAN */
    public void setWildcardVLAN(boolean enable) {
        set(Wildcard.OFPFW_DL_VLAN, enable);
    }

    /** Whether to wildcard on IP protocol */
    public boolean isWildcardVLANPCP() {
        return isSet(Wildcard.OFPFW_DL_VLAN_PCP);
    }
    
    
    /** Whether to wildcard on source MAC address */
    public boolean isWildcardEthernetSrc() {
        return isSet(Wildcard.OFPFW_DL_SRC);
    }
    
    /** Set whether to wildcard on source MAC address */
    public void setWildcardEthernetSrc(boolean enable) {
        set(Wildcard.OFPFW_DL_SRC, enable);
    }
    
    /** Whether to wildcard on destination MAC address */
    public boolean isWildcardEthernetDst() {
        return isSet(Wildcard.OFPFW_DL_DST);
    }
    
    /** Set whether to wildcard on destination MAC address */
    public void setWildcardEthernetDst(boolean enable) {
        set(Wildcard.OFPFW_DL_DST, enable);
    }
    
    /** Whether to wildcard on Ethernet type */
    public boolean isWildcardEthernetType() {
        return isSet(Wildcard.OFPFW_DL_TYPE);
    }
    
    /** Set whether to wildcard on Ethernet type */
    public void setWildcardEthernetType(boolean enable) {
        set(Wildcard.OFPFW_DL_TYPE, enable);
    }
    
    /** Whether to wildcard on IP protocol */
    public boolean isWildcardIPProtocol() {
        return isSet(Wildcard.OFPFW_NW_PROTO);
    }

    /** Whether to wildcard on IP protocol */
    public boolean isWildcardIPToS() {
        return isSet(Wildcard.OFPFW_NW_TOS);
    }
    
    /** Set whether to wildcard on IP protocol */
    public void setWildcardIPProtocol(boolean enable) {
        set(Wildcard.OFPFW_NW_PROTO, enable);
    }
    
    /** Whether to wildcard on TCP/UDP source port */
    public boolean isWildcardPortSrc() {
        return isSet(Wildcard.OFPFW_TP_SRC);
    }
    
    /** Set whether to wildcard on TCP/UDP source port */
    public void setWildcardPortSrc(boolean enable) {
        set(Wildcard.OFPFW_TP_SRC, enable);
    }
    
    /** Whether to wildcard on TCP/UDP destination port */
    public boolean isWildcardPortDst() {
        return isSet(Wildcard.OFPFW_TP_DST);
    }
    
    /** Set whether to wildcard on TCP/UDP destination port */
    public void setWildcardPortDst(boolean enable) {
        set(Wildcard.OFPFW_TP_DST, enable);
    }
    
    /** Whether to wildcard on source IP address */
    public boolean isWildcardIPSrc() {
        return isBitsSet(8, 6);
    }
    
    /** Sets whether to wildcard on IP source address (0 => no wildcard). */
    public void setWildcardIPSrc(int numBitsToMask) {
        setBits(false, 8, 6);
        if(numBitsToMask > 0) {
            int bits = Math.max(0, Math.min(32, numBitsToMask)) << Wildcard.OFPFW_NW_SRC_SHIFT.bitfield;
            bitfield |= bits;
        }
    }
    
    /** Gets the number of bits in the IP source address which are wildcarded */
    public int getWildcardIPSrcBitsMasked() {
        int cur = 1;
        int total = 0;
        for(int i=8; i<8+6; i++) {
            total += (isBitSet(i) ? cur : 0);
            cur *= 2;
        }
        return total;
    }

    /** Whether to wildcard on destination IP address */
    public boolean isWildcardIPDst() {
        return isBitsSet(14, 6);
    }

    /** Sets whether to wildcard on IP destination address (0 => no wildcard). */
    public void setWildcardIPDst(int numBitsToMask) {
        setBits(false, 14, 6);
        if(numBitsToMask > 0) {
            int bits = Math.max(0, Math.min(32, numBitsToMask)) << Wildcard.OFPFW_NW_DST_SHIFT.bitfield;
            bitfield |= bits;
        }
    }
    
    /** Gets the number of bits in the IP destination address which are wildcarded */
    public int getWildcardIPDstBitsMasked() {
        int cur = 1;
        int total = 0;
        for(int i=14; i<14+6; i++) {
            total += (isBitSet(i) ? cur : 0);
            cur *= 2;
        }
        return total;
    }
    
    /** Whether to wildcard on everything */
    public boolean isWildcardAll() {
        return isSet(Wildcard.OFPFW_ALL);
    }
    
    /** Enables all wildcards */
    public void setWildcardAll() {
        bitfield = Wildcard.OFPFW_ALL.bitfield;
    }

    /** Returns the netmask with the specified number of bits masked */ 
    public static String bitCountToString(int numBitsMasked) {
        int ip = 0;
        int cur = 1;
        while(numBitsMasked > 0) {
            ip += cur;
            cur *= 2;
            numBitsMasked -= 1;
        }
        return IPUtil.toString(ip);
    }

    public int hashCode() {
        return bitfield;
    }

    public boolean equals(Object o) {
        if(o!=null && o instanceof FlowWildcards) {
            FlowWildcards w = (FlowWildcards)o;
            return w.bitfield == bitfield;
        }
        return false;
    }
    
    public String toString() {
        if(isWildcardAll())
            return "wildcard{all}";
        
        LinkedList<String> wildcards = new LinkedList<String>();
        if(isWildcardInputPort())    wildcards.add("input port");
        if(isWildcardVLAN())         wildcards.add("VLAN");
        if(isWildcardEthernetSrc())  wildcards.add("MAC Src");
        if(isWildcardEthernetDst())  wildcards.add("MAC Dst");
        if(isWildcardEthernetType()) wildcards.add("MAC Type");
        if(isWildcardIPSrc())        wildcards.add("IP Src(" + bitCountToString(getWildcardIPSrcBitsMasked()) + ")");
        if(isWildcardIPDst())        wildcards.add("IP Dst(" + bitCountToString(getWildcardIPDstBitsMasked()) + ")");
        if(isWildcardIPProtocol())   wildcards.add("IP Proto");
        if(isWildcardPortSrc())      wildcards.add("Port Src");
        if(isWildcardPortDst())      wildcards.add("Port Dst");
        
        if(wildcards.size() == 0)
            return "wildcard{none}";
        
        boolean first = true;
        String strWildcards = "wildcard{";
        for(String s : wildcards) {
            if(first)
                strWildcards += s;
            else
                strWildcards += ", " + s;
        }
        return strWildcards + "}";
    }
}
