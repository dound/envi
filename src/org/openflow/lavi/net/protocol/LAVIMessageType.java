package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.IOException;
import org.openflow.lavi.net.protocol.auth.AuthType;
import org.openflow.protocol.StatsType;

/**
 * Enumerates what types of messages are in the LAVI protocol.
 * 
 * @author David Underhill
 */
public enum LAVIMessageType {
    /** Disconnection message */
    DISCONNECT((byte)0x00),

    /** Authentication request */
    AUTH_REQUEST((byte)0x01),

    /** Authentication challenge */
    AUTH_CHALLENGE((byte)0x02),

    /** Authentication reply */
    AUTH_REPLY((byte)0x03),

    /** Query for list of switches.  Switch datapath id is ignored. */
    SWITCHES_REQUEST((byte)0x10),

    /** Reply with list of switches added. Body is array of 8B datapath ids. */
    SWITCHES_ADD((byte)0x11),

    /** Reply with list of switches deleted.  Body is array of 8B datapath ids. */
    SWITCHES_DELETE((byte)0x12),

    /** Query for list of links for the specified switch */
    LINKS_REQUEST((byte)0x13),

    /** Reply with list of links added.  Body is array of book_link_spec. */
    LINKS_ADD((byte)0x14),

    /** Reply with list of links deleted.  Body is array of book_link_spec. */
    LINKS_DELETE((byte)0x15),

    /**
     * Statistics request.  Body is book_stat_message, with osr_body as defined 
     * in OpenFlow for ofp_stats_request.
     */
    STAT_REQUEST((byte)0x20),

    /**
     * Aggregated statistics reply.  Body is book_stat_message, with osr_body
     * as defined for ofp_stats_reply in OpenFlow, with exception of 
     * ofp_desc_stats having ofp_switch_features appended.
     */
    STAT_REPLY((byte)0x21),

    ;

    /** the special value used to identify messages of this type */
    private final byte typeID;

    private LAVIMessageType(byte typeID) {
        this.typeID = typeID;
    }

    /** returns the special value used to identify this type */
    public byte getTypeID() {
        return typeID;
    }

    /** Returns the LAVIMessageType constant associated with typeID, if any */
    public static LAVIMessageType typeValToMessageType(byte typeID) {
        for(LAVIMessageType t : LAVIMessageType.values())
            if(t.getTypeID() == typeID)
                return t;

        return null;
    }
    
    /** 
     * Constructs the object representing the received message.  The message is 
     * known to be of length len and len - 4 bytes representing the rest of the 
     * message should be extracted from buf.
     */
    public static LAVIMessage decode(int len, DataInput in) throws IOException {
        // parse the LAVI message header (except length which was already done)
        byte typeByte = in.readByte();
        LAVIMessageType t = LAVIMessageType.typeValToMessageType(typeByte);
        if(t == null)
            throw new IOException("Unknown type ID: " + typeByte);
        
        int xid = in.readInt();
        
        // parse the rest of the message
        switch(t) {
            case AUTH_REQUEST:
                return AuthType.decode(len, t, xid, in);
                
            case SWITCHES_ADD:
                return new SwitchesAdd(len, xid, in);
                
            case SWITCHES_DELETE:
                return new SwitchesDel(len, xid, in);
                
            case LINKS_ADD:
                return new LinksAdd(len, xid, in);
                
            case LINKS_DELETE:
                return new LinksDel(len, xid, in);
                
            case STAT_REPLY:
                return StatsType.decode(len, t, xid, in);

            case DISCONNECT:
            case AUTH_REPLY:
            case SWITCHES_REQUEST:
            case LINKS_REQUEST:
            case STAT_REQUEST:
                throw new IOException("Received unexpected message type: " + t.toString());
                
            default:
                throw new IOException("Unhandled type received: " + t.toString());
        }
    }
}
