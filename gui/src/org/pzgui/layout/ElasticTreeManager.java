package org.pzgui.layout;

import java.awt.Graphics2D;
import java.util.LinkedList;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import org.openflow.lavi.net.protocol.ETTrafficMatrix;
import org.pzgui.Drawable;
import org.pzgui.PZClosing;
import org.pzgui.PZWindow;

/**
 * Elastic Tree GUI manager.
 * 
 * @author David Underhill
 */
public class ElastricTreeManager extends PZLayoutManager {
    public static final int SL_WIDTH = 50;
    public static final int LBL_HEIGHT = 20;
    public static final int LBL_WIDTH = 100;
    public static final int GAP_X = 15;
    public static final int RESERVED_COLUMN_WIDTH = GAP_X + SL_WIDTH + GAP_X + LBL_WIDTH;
    public static final int RESERVED_ROW_HEIGHT   = 0;
    
    /** Creates a new Elastic Tree GUI for a k=6 fat tree */
    public ElastricTreeManager() {
        this(6);
    }

    /** Creates a new Elastic Tree GUI for a k fat tree */
    public ElastricTreeManager(int k) {
        fatTreeLayout = new FatTreeLayout<Vertex, Edge>(getGraph(), k);
        this.setLayout(fatTreeLayout);
    }
    
    
    // -------- Layout and Redrawing -------- //
    // ************************************** //
    
    private final FatTreeLayout<Vertex, Edge> fatTreeLayout;
    
    /** Gets the FatTreeLayout associated with this GUI */
    public FatTreeLayout getFatTreeLayout() {
        return fatTreeLayout;
    }
    
    /** Adds the drawable as usual and then invokes the fat tree layout engine */
    public synchronized void addDrawable(Drawable d) {
        super.addDrawable(d);
        fatTreeLayout.relayout();
    }
    
    /**
     * Overrides parent to reduce size of node layout to provide room for other
     * widgets on the GUI.  Also lays out the Elastic Tree specific widgets. 
     */
    public void setLayoutSize(int width, int height) {
        int w = width - RESERVED_COLUMN_WIDTH;
        int h = height - RESERVED_ROW_HEIGHT;
        super.setLayoutSize(w, h);
        
        // place our custom components
        final int SL_HEIGHT = h / 4;
        final int Y_GAP = ((h / 3) - SL_HEIGHT) / 2;
        final int SL_X = w + GAP_X;
        final int LBL_X = SL_X + SL_WIDTH + GAP_X;
        int y = Y_GAP;
        
        slEdge.setBounds(SL_X, y, SL_WIDTH, SL_HEIGHT);
        lblEdge.setBounds(LBL_X, y, LBL_WIDTH, LBL_HEIGHT);
        lblEdgeVal.setBounds(LBL_X, y+LBL_HEIGHT, LBL_WIDTH, LBL_HEIGHT);
        
        y += SL_HEIGHT + Y_GAP + Y_GAP;
        slAgg.setBounds(SL_X, y, SL_WIDTH, SL_HEIGHT);
        lblAgg.setBounds(LBL_X, y, LBL_WIDTH, LBL_HEIGHT);
        lblAggVal.setBounds(LBL_X, y+LBL_HEIGHT, LBL_WIDTH, LBL_HEIGHT);
        
        y += SL_HEIGHT + Y_GAP + Y_GAP;
        final int LOWER_SL_HEIGHT = SL_HEIGHT - LBL_HEIGHT;
        final int LBL_Y = y + LOWER_SL_HEIGHT;
        slDemand.setBounds(SL_X, y, SL_WIDTH, LOWER_SL_HEIGHT);
        lblDemand.setBounds(SL_X, LBL_Y, LBL_WIDTH, LBL_HEIGHT);
        lblDemandVal.setBounds(SL_X, LBL_Y+LBL_HEIGHT, LBL_WIDTH, LBL_HEIGHT);
        slPLen.setBounds(SL_X, y, SL_WIDTH, LOWER_SL_HEIGHT);
        lblPLen.setBounds(SL_X, LBL_Y, LBL_WIDTH, LBL_HEIGHT);
        lblPLenVal.setBounds(SL_X, LBL_Y+LBL_HEIGHT, LBL_WIDTH, LBL_HEIGHT);
    }
    
    /**
     * Overrides the parent implementation by appending the drawing of the 
     * additional Elastic Tree widgets.
     */
    public synchronized void redraw(PZWindow window) {
        super.redraw(window);
        Graphics2D gfx = window.getDisplayGfx();
        if(gfx == null)
            return;
        
        // draw place our custom components
        slDemand.repaint();
        lblDemand.repaint();
        lblDemandVal.repaint();
        slEdge.repaint();
        lblEdge.repaint();
        lblEdgeVal.repaint();
        slAgg.repaint();
        lblAgg.repaint();
        lblAggVal.repaint();
        slPLen.repaint();
        lblPLen.repaint();
        lblPLenVal.repaint();
    }

    
    // ----- Traffic Matrix Components ------ //
    // ************************************** //
    
    /** JSlider which calls back when its value changes. */
    private class MyJSlider extends JSlider {
        public MyJSlider(int orientation, int min, int max, int value) {
            super(orientation, min, max, value);
        }
        public void setValue(int i) {
            super.setValue(i);
            sliderChange();
        }
    }
    
    private JLabel lblDemand = new JLabel("Demand");
    private JLabel lblEdge = new JLabel("Edge");
    private JLabel lblAgg = new JLabel("Agg");
    private JLabel lblPLen = new JLabel("Len");

    private JLabel lblDemandVal = new JLabel("1000000Gbps");
    private JLabel lblEdgeVal = new JLabel("100%");
    private JLabel lblAggVal = new JLabel("100%");
    private JLabel lblPLenVal = new JLabel("1514B");

    private JSlider slDemand = new MyJSlider(SwingConstants.VERTICAL, 0, 1000*1000*1000, 1000*1000*1000);
    private JSlider slEdge   = new MyJSlider(SwingConstants.VERTICAL, 0, 100, 100);
    private JSlider slAgg    = new MyJSlider(SwingConstants.VERTICAL, 0, 100, 100);
    private JSlider slPLen    = new MyJSlider(SwingConstants.VERTICAL, 64, 1514, 1514);
    
    
    // --- Traffic Matrix Change Handling --- //
    // ************************************** //
    
    /** closing event listeners */
    private final LinkedList<TrafficMatrixChangeListener> trafficMatrixChangeListeneres = new LinkedList<TrafficMatrixChangeListener>();
    
    /** adds a listener to be notified when the traffic matrix has changed */
    public void addClosingListener(TrafficMatrixChangeListener c) {
        if(!trafficMatrixChangeListeneres.contains(c))
            trafficMatrixChangeListeneres.add(c);
    }

    /** removes the specified traffic matrix change listener */
    public void removeClosingListener(PZClosing c) {
        trafficMatrixChangeListeneres.remove(c);
    }

    /**
     * Updates the slider labels and notify those listening for traffic matrix changes.
     */
    private void sliderChange() {
        lblDemandVal.setText(formatBitsPerSec(slDemand.getValue()));
        lblEdgeVal.setText(slEdge.getValue() + "%");
        lblAggVal.setText(slAgg.getValue() + "%");
        lblPLenVal.setText(slPLen.getValue() + "B");
        
        ETTrafficMatrix tm = new ETTrafficMatrix(slDemand.getValue(), slEdge.getValue(), slAgg.getValue(), slPLen.getValue());
        for(TrafficMatrixChangeListener c : trafficMatrixChangeListeneres)
            c.trafficMatrixChanged(tm);
    }

    /**
     * Formats a number of bits as a human-readable rate.
     */
    public String formatBitsPerSec(long bits) {
        int units = 0;
        while( bits >= 1000 ) {
            bits /= 1000;
            units += 1;
        }
        String strUnit;
        switch( units ) {
            case  0: strUnit = "";  break;
            case  1: strUnit = "k"; break;
            case  2: strUnit = "M"; break;
            case  3: strUnit = "G"; break;
            case  4: strUnit = "T"; break;
            case  5: strUnit = "P"; break;
            default: strUnit = "?"; break;
        }
        return bits + strUnit + "bps";
    }
}
