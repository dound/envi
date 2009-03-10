package org.openflow.lavi.net.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A list of link utilizations.
 * 
 * @author David Underhill
 */
public class ETLinkUtilsList extends LAVIMessage {
    public final ETLinkUtil[] utils;
    
    public ETLinkUtilsList(final ETLinkUtil[] utils) {
        this(0, utils);
    }
    
    public ETLinkUtilsList(int xid, final ETLinkUtil[] utils) {
        super(LAVIMessageType.ET_LINK_UTILS, xid);
        this.utils = utils;
    }
    
    public ETLinkUtilsList(final int len, final int xid, final DataInput in) throws IOException {
        super(LAVIMessageType.ET_LINK_UTILS, xid);
        
        // make sure the number of bytes makes sense
        int left = len - super.length();
        if(left % ETLinkUtil.SIZEOF != 0) {
            throw new IOException("Body of links util list is not a multiple of " + ETLinkUtil.SIZEOF + " (length of body is " + left + " bytes)");
        }
        
        // read in the DPIDs
        int index = 0;
        utils = new ETLinkUtil[left / ETLinkUtil.SIZEOF];
        while(left >= ETLinkUtil.SIZEOF) {
            left -= ETLinkUtil.SIZEOF;
            utils[index++] = new ETLinkUtil(in.readLong(), in.readShort(), in.readLong(), in.readShort(), in.readFloat());
        }
    }
    
    public int length() {
        return super.length() + utils.length * ETLinkUtil.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        for(ETLinkUtil u : utils)
            u.write(out);
    }
    
    public String toString() {
        String strUtils;
        if(utils.length > 0)
            strUtils = utils[0].toString();
        else
            strUtils = "";
        
        for(int i=1; i<utils.length; i++)
            strUtils += ", " + utils[i].toString();
        
        return super.toString() + TSSEP + strUtils;
    }
}
