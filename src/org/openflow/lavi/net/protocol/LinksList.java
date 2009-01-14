package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A list of switches.
 * 
 * @author David Underhill
 */
public abstract class LinksList extends LAVIMessage {
    public Link[] links;
    
    public LinksList(LAVIMessageType t, final Link[] links) {
        this(t, 0, links);
    }
    
    public LinksList(LAVIMessageType t, int xid, final Link[] links) {
        super(t, xid);
        this.links = links;
    }
    
    public LinksList(final int len, final LAVIMessageType t, final int xid, final DataInput in) throws IOException {
        super(t, xid);
        
        // make sure the number of bytes leftover makes sense
        int left = len - super.length();
        if(left % Link.SIZEOF != 0) {
            throw new IOException("Body of links list is not a multiple of " + Link.SIZEOF + " (length of body is " + left + " bytes)");
        }
        
        // read in the DPIDs
        int index = 0;
        links = new Link[left / Link.SIZEOF];
        while(left >= Link.SIZEOF) {
            left -= Link.SIZEOF;
            links[index++] = new Link(in.readLong(), in.readShort(), in.readLong(), in.readShort());
        }
    }
    
    public int length() {
        return super.length() + links.length * Link.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        for(Link l : links)
            l.write(out);
    }
}
