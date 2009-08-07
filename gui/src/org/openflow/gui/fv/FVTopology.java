package org.openflow.gui.fv;

import java.awt.Color;

import org.openflow.gui.Topology;

/**
 * Includes what color to associate with this topology (aka slice) and the name
 * of the topology.
 * 
 * @author David Underhill
 */
public class FVTopology extends Topology {
    /** colors to assign to each topology */
    private static final Color TOPOLOGY_COLORS[] = new Color[]{Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.BLACK, Color.ORANGE, Color.MAGENTA};
    
    /** the next color to assign */
    private static int topologyColorOn = 0; 
    
    /** the fill color for this topology */
    private Color color;
    
    /** the name of the topology */
    private String name;
    
    public FVTopology(final FVLayoutManager manager, String name) {
        super(manager);
        color = TOPOLOGY_COLORS[topologyColorOn++];
        this.name = name;
    }

    /**
     * Gets the fill color associated with this topology.
     * 
     * @return the paint
     */
    public Color getFillColor() {
        return color;
    }
    
    /**
     * Gets the name of the topology.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name of the topology.
     */
    public void setName(String name) {
        this.name = name;
    }
}
