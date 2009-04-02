package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.IOException;
import org.openflow.lavi.net.protocol.auth.AuthType;
import org.openflow.lavi.net.protocol.et.*;
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
    
    /** Tell the backend to start polling a message */
    POLL_START((byte)0x0E),
    
    /** Tell the backend to stop polling a message */
    POLL_STOP((byte)0x0F),

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
    
    /** Request to change whether the one is subscribed to have switch changes pushed */ 
    SWITCHES_SUBSCRIBE((byte)0x16),
    
    /** Request to change whether the one is subscribed to have link changes pushed */
    LINKS_SUBSCRIBE((byte)0x17),

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

    /**
     * Tells the Elastic Tree backend what kind of traffic matrix to generate.
     */
    ET_TRAFFIX_MATRIX((byte)0xF0),
    
    /**
     * Tells the Elastic Tree GUI how utilized every link is.
     */
    ET_LINK_UTILS((byte)0xF1),
    
    /**
     * Tells the Elastic Tree GUI the current power usage of the system.
     */
    ET_POWER_USAGE((byte)0xF2),
    
    /**
     * Tells the Elastic Tree GUI which switches are powered off.
     */
    ET_SWITCHES_OFF((byte)0xF3),

    /**
     * Tells the Elastic Tree GUI about the achieved bandwidth.
     */
    ET_BANDWIDTH((byte)0xF4),

    /**
     * Tells the Elastic Tree GUI about latency data.
     */
    ET_LATENCY((byte)0xF5),

    /**
     * Asks the Elastic Tree backend to tells us which switches are in a fat 
     * tree of a particular size.
     */
    ET_SWITCHES_REQUEST((byte)0xF6),
    
    /**
     * Tells the Elastic Tree backend about switch failures.
     */
    ET_SWITCH_FAILURES((byte)0xF7),

    /**
     * Tells the Elastic Tree backend about link failures.
     */
    ET_LINK_FAILURES((byte)0xF8),
    
    /**
     * Tells the Elastic Tree GUI when a computation resulting from the last
     * ET_TRAFFIC_MATRIX message has completed.
     */
    ET_COMPUTATION_DONE((byte)0xFF),
    
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
        LAVIMessage msg = decode(len, t, xid, in);
        msg.xid = xid;
        return msg;
    }
     
    /** 
     * Constructs the object representing the received message.  The message is 
     * known to be of length len.  The header of the message (length, type,
     * and transaction ID) have been read, but the remainder is still on the 
     * input stream.
     */
    private static LAVIMessage decode(int len, LAVIMessageType t, int xid, DataInput in) throws IOException {
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

            case ET_BANDWIDTH:
                return new ETBandwidth(len, xid, in);
            
            case ET_LATENCY:
                return new ETLatency(len, xid, in);
            
            case ET_LINK_UTILS:
                return new ETLinkUtilsList(len, xid, in);
            
            case ET_POWER_USAGE:
                return new ETPowerUsage(len, xid, in);
            
            case ET_SWITCHES_OFF:
                return new ETSwitchesOff(len, xid, in);
            
            case ET_COMPUTATION_DONE:
                return new ETComputationDone(len, xid, in);
                
            case DISCONNECT:
            case AUTH_REPLY:
            case POLL_START:
            case POLL_STOP:
            case SWITCHES_REQUEST:
            case LINKS_REQUEST:
            case STAT_REQUEST:
            case ET_TRAFFIX_MATRIX:
            case ET_SWITCHES_REQUEST:
            case ET_SWITCH_FAILURES:
            case ET_LINK_FAILURES:
                throw new IOException("Received unexpected message type: " + t.toString());
                
            default:
                throw new IOException("Unhandled type received: " + t.toString());
        }
    }
}
