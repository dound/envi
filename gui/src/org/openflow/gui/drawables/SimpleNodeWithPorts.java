package org.openflow.gui.drawables;

import org.openflow.gui.net.protocol.NodeType;
import org.openflow.util.string.DPIDUtil;
import org.pzgui.icon.Icon;

/**
 * Describes a simple NodeWithPorts.
 * 
 * @author David Underhill
 */
public class SimpleNodeWithPorts extends NodeWithPorts {
    public SimpleNodeWithPorts(NodeType type, long id, Icon icon) {
        this(type, "", 0, 0, id, icon);
        
    }
    
    public SimpleNodeWithPorts(NodeType type, String name, int x, int y, long id, Icon icon) {
        super(type, name, x, y, icon);
        this.id = id;
    }
    
    /** ID of this node */
    private long id;
    
    /** returns a string version of the node's datapath ID */
    public String getDebugName() {
        return DPIDUtil.toString(getID());
    }
    
    /** gets the id of this node */
    public long getID() {
        return id;
    }
    
    public int hashCode() {
        return super.hashCode();
    }
    
    public boolean equals(Object o) {
        if(this == o) return true;
        if((o == null) || (o.getClass() != this.getClass())) return false;
        SimpleNodeWithPorts n = (SimpleNodeWithPorts)o;
        return n.getID() == getID() && n.getType() == getType();
    }
    
    public String toString() {
        String body = getType().toString() + "-" + DPIDUtil.toString(getID());
        
        String name = getName();
        if(name == null || name.length()==0)
            return body;
        else
            return name + "{" + body + "}";
    }
}
