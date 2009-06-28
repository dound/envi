package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * A list of ModuleInstances.
 * 
 * @author Glen Gibb
 */
public abstract class OPModuleInstancesList extends OFGMessage {
    public OPModuleInstance[] instances;
    
    public OPModuleInstancesList(OFGMessageType t, final OPModuleInstance[] instances) {
        this(t, 0, instances);
    }
    
    public OPModuleInstancesList(OFGMessageType t, int xid, final OPModuleInstance[] instances) {
        super(t, xid);
        this.instances = instances;
    }
    
    public OPModuleInstancesList(final int len, final OFGMessageType t, final int xid, final DataInput in) throws IOException {
        super(t, xid);
        // make sure the number of bytes leftover makes sense
        int left = len - super.length();
        if(left % OPModuleInstance.SIZEOF != 0) {
            throw new IOException("Body of module instances list is not a multiple of " + OPModuleInstance.SIZEOF + " (length of body is " + left + " bytes)");
        }
        
        // read in the DPIDs
        int index = 0;
        instances = new OPModuleInstance[left / OPModuleInstance.SIZEOF];
        while(left >= OPModuleInstance.SIZEOF) {
            left -= OPModuleInstance.SIZEOF;
            instances[index++] = new OPModuleInstance(in);
        }
    }
    
    public int length() {
        return super.length() + instances.length * OPModuleInstance.SIZEOF;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        for(OPModuleInstance mi : instances)
            mi.write(out);
    }
    
    public String toString() {
        String strModuleInstances;
        if(instances.length > 0)
            strModuleInstances = instances[0].toString();
        else
            strModuleInstances = "";
        
        for(int i=1; i<instances.length; i++)
            strModuleInstances += ", " + instances[i].toString();
        
        return super.toString() + TSSEP + strModuleInstances;
    }
}
