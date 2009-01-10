package org.openflow.protocol;

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
        
        /** Wildcard all fields */
        OFPFW_ALL((1 << 20) - 1),
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
     * @return
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
            if((bitfield & (1 << i)) != 0)
                return true;
        
        return false;
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
    
    /** Whether to wildcard on everything */
    public boolean isWildcardAll() {
        return isSet(Wildcard.OFPFW_ALL);
    }
    
    /** Enables all wildcards */
    public void setWildcardAll() {
        bitfield = Wildcard.OFPFW_ALL.bitfield;
    }
}