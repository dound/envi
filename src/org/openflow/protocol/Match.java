package org.openflow.protocol;

import java.io.IOException;
import org.openflow.lavi.net.util.ByteBuffer;

/**
 * Fields to match against flows.  Equivalent to ofp_match.
 * 
 * @author David Underhill
 */
public class Match {
    /** size of the data contained in this object in bytes */
    public static int SIZEOF = 36;
    
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

    /** Ethernet frame type */
    public short dl_type;

    /** IP protocol */
    public byte nw_proto;

    /** Align to 32-bits */
    public byte pad;

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
    public Match(ByteBuffer buf) throws IOException {
        wildcards.set(buf.nextInt());
        in_port = buf.nextShort();
        for(int i=0; i<6; i++) dl_src[i] = buf.nextByte();
        for(int i=0; i<6; i++) dl_dst[i] = buf.nextByte();
        dl_vlan = buf.nextShort();
        dl_type = buf.nextShort();
        nw_proto = buf.nextByte();
        pad = buf.nextByte();
        nw_src = buf.nextInt();
        nw_dst = buf.nextInt();
        tp_src = buf.nextShort();
        tp_dst = buf.nextShort();
    }
}
