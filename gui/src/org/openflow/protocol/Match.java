package org.openflow.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.openflow.util.string.IPUtil;

/**
 * Fields to match against flows.  Equivalent to ofp_match.
 * 
 * @author David Underhill
 */
public class Match {
    /** size of the data contained in this object in bytes */
    public static int SIZEOF = 40;
    
    /** a Match which matches all fields */
    public static final Match MATCH_ALL = new Match();
    
    /** Wildcard fields */
    public FlowWildcards wildcards = new FlowWildcards();

    /** Input switch port */
    public short in_port;

    /** Ethernet source address */
    public final byte dl_src[] = new byte[6];

    /** Ethernet destination address */
    public final byte dl_dst[] = new byte[6];

    /** Input VLAN */
    public short dl_vlan;

    /** Vlan PCP */
    public byte dl_vlan_pcp;

    /** Ethernet frame type */
    public short dl_type;

    /** IP protocol */
    public byte nw_proto;

    /** IP tos bits */
    public byte nw_tos;

    /** IP source address */
    public int nw_src;

    /** IP destination address */
    public int nw_dst;

    /** TCP/UDP source port */
    public short tp_src;

    /** TCP/UDP destination port */
    public short tp_dst;
    
    /** Constructs fully wildcarded match. */
    public Match() {
        wildcards.setWildcardAll();
    }
    
    /** used to construct a message being received */
    public Match(DataInput in) throws IOException {
        wildcards.set(in.readInt());
        in_port = in.readShort();
        for(int i=0; i<6; i++) dl_src[i] = in.readByte();
        for(int i=0; i<6; i++) dl_dst[i] = in.readByte();
        dl_vlan = in.readShort();
        dl_vlan_pcp = in.readByte();
        in.readByte(); // 1B pad
        dl_type = in.readShort();
        nw_tos  = in.readByte();
        nw_proto = in.readByte();
        in.readShort(); // 2B pad
        nw_src = in.readInt();
        nw_dst = in.readInt();
        tp_src = in.readShort();
        tp_dst = in.readShort();
    }

    public void write(DataOutput out) throws IOException {
        out.writeInt(wildcards.getAssociatedBits());
        out.writeShort(in_port);
        out.write(dl_src);
        out.write(dl_dst);
        out.writeShort(dl_vlan);
        out.writeByte(dl_vlan_pcp);
        out.writeByte(0); // pad
        out.writeShort(dl_type);
        out.writeByte(nw_tos);
        out.writeByte(nw_proto);
        out.writeShort(0);  // pad
        out.writeInt(nw_src);
        out.writeInt(nw_dst);
        out.writeShort(tp_src);
        out.writeShort(tp_dst);
    }
    
    public int hashCode() {
        int ret = 7;
        ret = ret + wildcards.hashCode();
        ret = (15 * ret) + in_port;
        for(int i=0; i<6; i++) {
            ret = (7 * ret) + dl_src[i];
            ret = (7 * ret) + dl_dst[i];
        }
        ret = (15 * ret) + dl_vlan;
        ret = (15 * ret) + dl_vlan_pcp;
        ret = (15 * ret) + dl_type;
        ret = (15 * ret) + nw_proto;
        ret = (7 * ret) + nw_proto;
        ret = (31 * ret) + nw_src;
        ret = (31 * ret) + nw_dst;
        ret = (15 * ret) + tp_src;
        ret = (15 * ret) + tp_dst;
        return ret;
    }
    
    public boolean equals(Object o) {
        if(o!=null && o instanceof Match) {
            Match m = (Match)o;
            return wildcards.equals(m.wildcards) &&
                   in_port   == m.in_port &&
                   dl_src[0] == m.dl_src[0] &&
                   dl_src[1] == m.dl_src[1] &&
                   dl_src[2] == m.dl_src[2] &&
                   dl_src[3] == m.dl_src[3] &&
                   dl_src[4] == m.dl_src[4] &&
                   dl_src[5] == m.dl_src[5] &&
                   dl_dst[0] == m.dl_dst[0] &&
                   dl_dst[1] == m.dl_dst[1] &&
                   dl_dst[2] == m.dl_dst[2] &&
                   dl_dst[3] == m.dl_dst[3] &&
                   dl_dst[4] == m.dl_dst[4] &&
                   dl_dst[5] == m.dl_dst[5] &&
                   dl_vlan   == m.dl_vlan &&
                   dl_vlan_pcp   == m.dl_vlan_pcp &&
                   dl_type   == m.dl_type &&
                   nw_tos    == m.nw_tos &&
                   nw_proto  == m.nw_proto &&
                   nw_src    == m.nw_src &&
                   nw_dst    == m.nw_dst &&
                   tp_src    == m.tp_src &&
                   tp_dst    == m.tp_dst;
        }
        return false;
    }
    
    public String toString() {
        String ret = "{";
        
        if(wildcards.isWildcardAll())
            return "{all fields wilcarded}";
        
        ret =  "inputPort=" + (wildcards.isWildcardInputPort()    ? "*" : in_port);
        ret += ", VLAN="    + (wildcards.isWildcardVLAN()         ? "*" : dl_vlan);
        ret += ", VLAN="    + (wildcards.isWildcardVLANPCP()      ? "*" : dl_vlan_pcp);
        ret += ", EthSrc="  + (wildcards.isWildcardEthernetSrc()  ? "*" : dl_src);
        ret += ", EthDst="  + (wildcards.isWildcardEthernetDst()  ? "*" : dl_dst);
        ret += ", EthType=" + (wildcards.isWildcardEthernetType() ? "*" : dl_type);
        ret += ", IPSrc="   + (IPUtil.maskedIPToString(nw_src, wildcards.getWildcardIPSrcBitsMasked()));
        ret += ", IPDst="   + (IPUtil.maskedIPToString(nw_dst, wildcards.getWildcardIPDstBitsMasked()));
        ret += ", IPToS="   + (wildcards.isWildcardIPToS()        ? "*" : nw_tos);
        ret += ", IPProto=" + (wildcards.isWildcardIPProtocol()   ? "*" : nw_proto);
        ret += ", PortSrc=" + (wildcards.isWildcardPortSrc()      ? "*" : tp_src);
        ret += ", PortDst=" + (wildcards.isWildcardPortDst()      ? "*" : tp_dst);
        
        return ret + "}";
    }
}
