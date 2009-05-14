package org.openflow.gui.fv;

import java.awt.Color;

import org.openflow.gui.Topology;

/**
 * Includes what color to associate with this topology (aka slice).
 * 
 * @author David Underhill
 */
public class FVTopology extends Topology {
    /** colors to assign to each topology */
    private static final Color TOPOLOGY_COLORS[] = new Color[]{Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.PINK, Color.ORANGE};
    
    /** the next color to assign */
    private static int topologyColorOn = 0; 
    
    /** the fill color for this topology */
    private Color color;
    
    public FVTopology(final FVLayoutManager manager) {
        super(manager);
        color = TOPOLOGY_COLORS[topologyColorOn++];
    }

    /**
     * Gets the fill color associated with this topology.
     * 
     * @return the paint
     */
    public Color getFillColor() {
        return color;
    }
}
