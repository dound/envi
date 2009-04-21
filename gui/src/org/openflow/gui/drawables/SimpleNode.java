package org.openflow.gui.drawables;

import java.util.ArrayList;

import org.openflow.gui.net.protocol.NodeType;
import org.openflow.util.string.DPIDUtil;
import org.pzgui.icon.Icon;

/**
 * Describes a simple Node.
 * 
 * @author David Underhill
 */
public class SimpleNode extends Node {
    public SimpleNode(NodeType type, long id, Icon icon) {
        this(type, "", 0, 0, id, icon);
        
    }
    
    public SimpleNode(NodeType type, String name, int x, int y, long id, Icon icon) {
        super(name, x, y, icon);
        this.id = id;
        this.type = type;
    }
    
    private static final ArrayList<Link> empty = new ArrayList<Link>();
    
    /** returns an empty as the SimpleNode has no edges */
    public Iterable<Link> getEdges() {
        return empty;
    }
    
    /** type of this node */
    private NodeType type;
    
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
    
    /** gets the type of this node */
    public NodeType getType() {
        return type;
    }
     
    public String toString() {
        String body = type.toString() + "-" + DPIDUtil.toString(getID());
        
        String name = getName();
        if(name == null || name.length()==0)
            return body;
        else
            return name + "{" + body + "}";
    }
}
