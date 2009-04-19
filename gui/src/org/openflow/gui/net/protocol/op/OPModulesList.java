package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * A list of links.
 * 
 * @author David Underhill
 */
public abstract class OPModulesList extends OFGMessage {
    public OPModule[] modules;
    
    public OPModulesList(OFGMessageType t, final OPModule[] modules) {
        this(t, 0, modules);
    }
    
    public OPModulesList(OFGMessageType t, int xid, final OPModule[] modules) {
        super(t, xid);
        this.modules = modules;
    }
    
    public OPModulesList(final int len, final OFGMessageType t, final int xid, final DataInput in) throws IOException {
        super(t, xid);
        
        // make sure the number of bytes leftover makes sense
        int left = len - super.length();
        if(left % OPModule.SIZEOF != 0) {
            throw new IOException("Body of modules list is not a multiple of " + OPModule.SIZEOF + " (length of body is " + left + " bytes)");
        }
        
        // read in the DPIDs
        int index = 0;
        modules = new OPModule[left / OPModule.SIZEOF];
        while(left >= OPModule.SIZEOF) {
            left -= OPModule.SIZEOF;
            modules[index++] = new OPModule(in);
        }
    }
    
    public int length() {
        return super.length() + modules.length * OPModule.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out); 
        for(OPModule m : modules)
            m.write(out);
    }
    
    public String toString() {
        String strModules;
        if(modules.length > 0)
            strModules = modules[0].toString();
        else
            strModules = "";
        
        for(int i=1; i<modules.length; i++)
            strModules += ", " + modules[i].toString();
        
        return super.toString() + TSSEP + strModules;
    }
}
