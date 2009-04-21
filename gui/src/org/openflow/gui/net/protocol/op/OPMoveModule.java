package org.openflow.gui.net.protocol.op;

import java.io.DataOutput;
import java.io.IOException;

import org.openflow.gui.drawables.OPNodeWithNameAndPorts;
import org.openflow.gui.net.protocol.Node;
import org.openflow.gui.net.protocol.NodeType;
import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Tells the backend to move a module.
 * 
 * @author David Underhill
 *
 */
public class OPMoveModule extends OFGMessage {
    /** 
     * Used by MoveModule to represent when a module is added (from_node is 
     * NONE) or removed (to_node is NONE).
     */
    public static final Node NODE_NONE = new Node(NodeType.UNKNOWN, -1);
        
    /** the module being moved */
    public final Node module;
    
    /** the place it is being moved from */
    public final Node from;
    
    /** the place it is being moved to */
    public final Node to;
    
    /** Convert the Drawable version to the wire version */
    private static final Node fromDrawable(OPNodeWithNameAndPorts n) {
        if(n == null)
            return OPMoveModule.NODE_NONE;
        else
            return new Node(n.getType(), n.getID());
    }
    
    /**
     * Convenience constructor which takes Drawable-derived arguments.  Converts
     * the arguments to wire-format objects and calls the appropriate 
     * constructor with those objects.  null arguments will be set to 
     * OPMoveModule.NODE_NONE.
     */
    public OPMoveModule(org.openflow.gui.drawables.OPModule m,
                        OPNodeWithNameAndPorts from,
                        OPNodeWithNameAndPorts to) {
        this(fromDrawable(m), fromDrawable(from), fromDrawable(to));
    }
    
    /** adding or removing a module */
    public OPMoveModule(Node module, Node node, boolean add) {
        this(module, add ? NODE_NONE : node, add ? node : NODE_NONE);
    }
    
    /** moving a module */
    public OPMoveModule(Node module, Node from, Node to) {
        super(OFGMessageType.OP_MOVE_MODULE, 0);
        if(!module.nodeType.isModule())
            throw new Error("Error: module must be of type OPModule");
        this.module = module;
        this.from = from;
        this.to = to;
    }
    
    /** This returns the maximum length of this message */
    public int length() {
        return super.length() + 3 * Node.SIZEOF;
    }
    
    /** Writes the header (via super.write()), and this message */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        module.write(out);
        from.write(out);
        to.write(out);
    }
    
    public String toString() {
        if(from.equals(NODE_NONE))
            return super.toString() + TSSEP + "add " + module + " to " + to;
        else if(to.equals(NODE_NONE))
            return super.toString() + TSSEP + "remove " + module + " from " + from;
        else
            return super.toString() + TSSEP + "move " + module + " from " + from + " to " + to;
    }
}
