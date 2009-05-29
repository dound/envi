package org.openflow.gui.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A list of link specs.
 * 
 * @author David Underhill
 */
public abstract class LinkSpecsList extends OFGMessage {
    public LinkSpec[] links;
    
    public LinkSpecsList(OFGMessageType t, final LinkSpec[] links) {
        this(t, 0, links);
    }
    
    public LinkSpecsList(OFGMessageType t, int xid, final LinkSpec[] links) {
        super(t, xid);
        this.links = links;
    }
    
    public LinkSpecsList(final int len, final OFGMessageType t, final int xid, final DataInput in) throws IOException {
        super(t, xid);
        
        // make sure the number of bytes leftover makes sense
        int left = len - super.length();
        if(left % LinkSpec.SIZEOF != 0) {
            throw new IOException("Body of link specs list is not a multiple of " + LinkSpec.SIZEOF + " (length of body is " + left + " bytes)");
        }
        
        // read in the DPIDs
        int index = 0;
        links = new LinkSpec[left / LinkSpec.SIZEOF];
        while(left >= LinkSpec.SIZEOF) {
            left -= LinkSpec.SIZEOF;
            links[index++] = new LinkSpec(in);
        }
    }
    
    public int length() {
        return super.length() + links.length * LinkSpec.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out); 
        for(LinkSpec l : links)
            l.write(out);
    }
    
    public String toString() {
        String strLinks;
        if(links.length > 0)
            strLinks = links[0].toString();
        else
            strLinks = "";
        
        for(int i=1; i<links.length; i++)
            strLinks += ", " + links[i].toString();
        
        return super.toString() + TSSEP + strLinks;
    }
}
