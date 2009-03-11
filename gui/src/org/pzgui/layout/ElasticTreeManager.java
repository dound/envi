package org.pzgui.layout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.openflow.lavi.drawables.DrawableIcon;
import org.openflow.lavi.drawables.Link;
import org.openflow.lavi.drawables.OpenFlowSwitch;
import org.openflow.lavi.net.protocol.ETTrafficMatrix;
import org.openflow.util.string.StringOps;
import org.pzgui.Constants;
import org.pzgui.Drawable;
import org.pzgui.PZWindow;
import org.pzgui.StringDrawer;
import org.pzgui.icon.GeometricIcon;

/**
 * Elastic Tree GUI manager.
 * 
 * @author David Underhill
 */
public class ElasticTreeManager extends PZLayoutManager {
    public static final int HW_FAT_TREE_K = 6;
    public static final int SL_WIDTH = 50;
    public static final int LBL_HEIGHT = 20;
    public static final int LBL_WIDTH = 100;
    public static final int LBL_WIDTH_BIG = 400;
    public static final int GAP_X = 5;
    public static final int RESERVED_COLUMN_WIDTH = Math.max(GAP_X + SL_WIDTH + GAP_X + LBL_WIDTH, LBL_WIDTH_BIG);
    
    /** Creates a new Elastic Tree GUI for a k=6 fat tree */
    public ElasticTreeManager() {
        this(HW_FAT_TREE_K);
    }

    /** Creates a new Elastic Tree GUI for a k fat tree */
    public ElasticTreeManager(int k) {
        fatTreeLayout = new FatTreeLayout<Vertex, Edge>(getGraph(), k);
        this.setLayout(fatTreeLayout);
        setCurrentTrafficMatrixText(null);
        setNextTrafficMatrixText(null);
        
        dialPower = new MultiPointerDial("Power Consumption", "Watts", 2, 460, 50);
        dialPower.setPointerLine(1, 0.95);
        
        int max_gbps = 2 * fatTreeLayout.size_links(); /* 1Gbps in each direction per link */
        dialBandwidth = new MultiPointerDial("Aggregate Xput", "Gbps", 2,  max_gbps, max_gbps/10);
        dialBandwidth.setPointerLine(1, 0.95);
        
        dialLatency = new MultiPointerDial("Layer Latency", "msec", 3, 100, 10);
        
        pnlSidebar.setDoubleBuffered(true);
        pnlSidebar.setLayout(null);
        pnlSidebar.add(slDemand);
        pnlSidebar.add(lblDemand);
        pnlSidebar.add(lblDemandVal);
        pnlSidebar.add(slEdge);
        pnlSidebar.add(lblEdge);
        pnlSidebar.add(lblEdgeVal);
        pnlSidebar.add(slAgg);
        pnlSidebar.add(lblAgg);
        pnlSidebar.add(lblAggVal);
        pnlSidebar.add(slPLen);
        pnlSidebar.add(lblPLen);
        pnlSidebar.add(lblPLenVal);
        pnlSidebar.add(lblTrafficMatrixCurrent);
        pnlSidebar.add(lblTrafficMatrixNext);
        pnlSidebar.add(lblResultInfo);
        lblResultInfo.setVisible(false);
        pnlSidebar.add(dialPower);
        pnlSidebar.add(dialBandwidth);
        pnlSidebar.add(dialLatency);
        pnlSidebar.add(pnlAnim);
        pnlSidebar.add(pnlMode);
        initComponents();
    }
    
    // -------- Layout and Redrawing -------- //
    // ************************************** //
    
    private final FatTreeLayout<Vertex, Edge> fatTreeLayout;
    
    /** Gets the FatTreeLayout associated with this GUI */
    public FatTreeLayout getFatTreeLayout() {
        return fatTreeLayout;
    }
    
    /** Adds the drawable as usual and then invokes the fat tree layout engine */
    public void addDrawable(Drawable d) {
        super.addDrawable(d);
        synchronized(this) {
            if(d instanceof Vertex) {
                if(fatTreeLayout.noteVertex((Vertex)d)) {
                    // a bit of a hack: draw switches representing hosts a different color
                    if(d instanceof OpenFlowSwitch) {
                        OpenFlowSwitch o = ((OpenFlowSwitch)d);
                        o.setFillColor(java.awt.Color.DARK_GRAY);
                        o.setSize(OpenFlowSwitch.SIZE_SMALL);
                    }
                }
                relayout();
            }
        }
    }
    
    /** 
     * Removes the drawable as usual and then completely resets the fat tree 
     * layout engine (assumes all switches are being removed).
     */
    public void removeDrawable(Drawable d) {
        super.removeDrawable(d);
        synchronized(this) {
            if(d instanceof Vertex) {
                fatTreeLayout.clear();
            }
        }
    }

    /** Overrides parent to add my widgets to the new window. */
    public void attachWindow(final PZWindow w) {
        super.attachWindow(w);
        if(getWindowIndex(w) == 0) {
            w.getContentPane().add(pnlSidebar);
            w.setReservedWidthRight(RESERVED_COLUMN_WIDTH);
            w.setMySize(w.getWidth(), w.getHeight(), w.getZoom());
        }
    }
    
    /**
     * Overrides parent to reduce size of node layout to provide room for other
     * widgets on the GUI.  Also lays out the Elastic Tree specific widgets. 
     */
    public void setLayoutSize(int w, int h) {
        super.setLayoutSize(w, h);
        relayout();

        // place our custom components
        pnlSidebar.setBounds(w, 0, RESERVED_COLUMN_WIDTH-4, h);
        
        int x = GAP_X;
        int y = 0;
        lblTrafficMatrixCurrent.setBounds(x, y, LBL_WIDTH_BIG, LBL_HEIGHT);
        y += LBL_HEIGHT;
        lblTrafficMatrixNext.setBounds(x, y, LBL_WIDTH_BIG, LBL_HEIGHT);
        y += LBL_HEIGHT;
        lblResultInfo.setBounds(x, y, LBL_WIDTH_BIG, LBL_HEIGHT);
        
        h -= y;
        int SL_HEIGHT = h / 8;
        final int SL_X = GAP_X;
        
        x = GAP_X;
        y = h - SL_HEIGHT - LBL_HEIGHT;
        final int LOWER_SL_HEIGHT = SL_HEIGHT - LBL_HEIGHT;
        int LBL_Y = y + LOWER_SL_HEIGHT;
        slDemand.setBounds(x, y, SL_WIDTH, LOWER_SL_HEIGHT);
        lblDemand.setBounds(x, LBL_Y, LBL_WIDTH, LBL_HEIGHT);
        lblDemandVal.setBounds(x, LBL_Y+LBL_HEIGHT, LBL_WIDTH, LBL_HEIGHT);
        
        int x2 = x + SL_WIDTH + GAP_X;
        slPLen.setBounds(x2, y, LBL_WIDTH, LOWER_SL_HEIGHT);
        lblPLen.setBounds(x2, LBL_Y, LBL_WIDTH, LBL_HEIGHT);
        lblPLenVal.setBounds(x2, LBL_Y+LBL_HEIGHT, LBL_WIDTH, LBL_HEIGHT);
        
        x = SL_X;
        y = h / 3 - 2 * SL_HEIGHT / 3;
        LBL_Y = y + SL_HEIGHT;
        slAgg.setBounds(x, y, SL_WIDTH, SL_HEIGHT);
        lblAgg.setBounds(x, LBL_Y, LBL_WIDTH, LBL_HEIGHT);
        lblAggVal.setBounds(x, LBL_Y+LBL_HEIGHT, LBL_WIDTH, LBL_HEIGHT);
        
        y = 2 * h / 3 - SL_HEIGHT;
        LBL_Y = y + SL_HEIGHT;
        slEdge.setBounds(x, y, SL_WIDTH, SL_HEIGHT);
        lblEdge.setBounds(x, LBL_Y, LBL_WIDTH, LBL_HEIGHT);
        lblEdgeVal.setBounds(x, LBL_Y+LBL_HEIGHT, LBL_WIDTH, LBL_HEIGHT);
        
        int o = SL_WIDTH + GAP_X;
        int sz = LBL_WIDTH_BIG - o - GAP_X * 2;
        x = x + o;
        y = 3 * LBL_HEIGHT + 5;
        int y_margin = 10;
        if((sz+y_margin) * 3 + y > h)
            sz = (h - y) / 3 - y_margin;
        
        dialPower.setBounds(x, y, sz, sz);
        y += sz + y_margin;
        dialBandwidth.setBounds(x, y, sz, sz);
        y += sz + y_margin;
        dialLatency.setBounds(x, y, sz, sz);
        
        pnlAnim.setLocation(0, 0);
        pnlAnim.setSize(pnlAnim.getPreferredSize());
        pnlMode.setLocation(0, 500);
        pnlMode.setSize(pnlMode.getPreferredSize());
    }
    
    /**
     * Overrides the parent implementation by appending the drawing of the 
     * additional Elastic Tree widgets.
     */
    public void preRedraw(PZWindow window) {
        super.preRedraw(window);
        Graphics2D gfx = window.getDisplayGfx();
        if(gfx == null)
            return;
        
        for(DrawableIcon d : liveIcons)
            d.drawObject(gfx);
        
        int lw = Link.USAGE_LEGEND.getWidth();
        int lh = Link.USAGE_LEGEND.getHeight();
        int x = 5;
        int y = fatTreeLayout.getSize().height - lh;
        gfx.drawImage(Link.USAGE_LEGEND, 
                      x, y,
                      lw, lh, 
                      null);
        
        // draw the legend
        gfx.setFont(Constants.FONT_DEFAULT);
        gfx.setColor(Color.BLACK);
        y += lh / 2 + 2;
        int margin_x = 5;
        gfx.drawString("0%", x + margin_x, y);
        StringDrawer.drawCenteredString("Link Utilization (%)", gfx, x + lw / 2, y);
        StringDrawer.drawRightAlignedString("100%", gfx, x + lw - margin_x, y);
        gfx.setColor(Constants.COLOR_DEFAULT);
    }
    
    public void postRedraw() {}

    private int[] baseX = new int[]{0,1,1,0};
    private int[] baseY = new int[]{0,0,1,1};
    private HashSet<DrawableIcon> liveIcons = new HashSet<DrawableIcon>();
    private void addRectangle(int x, int y, int w, int h, Color f, Color b, Stroke s) {
        DrawableIcon d = new DrawableIcon(new GeometricIcon(baseX, baseY, w, h, f, b, s), x, y, w, h);
        liveIcons.add(d);
    }
    
    private static final Color POD_OUTLINE_COLOR = Constants.cmap(new Color(25, 25, 0, 128));
    private static final BasicStroke POD_SEP_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {5.0f, 5.0f}, 15.0f);
    
    private void relayout() {
        // only relayout once all nodes are present
        if(fatTreeLayout.getGraph().getVertexCount() < fatTreeLayout.size())
            return;
        
        liveIcons.clear();
        
        fatTreeLayout.reset();
        fatTreeLayout.relayout();
        
        int y = fatTreeLayout.agg_y;
        int w = fatTreeLayout.pod_sz;
        int h = fatTreeLayout.getSize().height - y;
        int i;
        for(i=1; i<fatTreeLayout.getK(); i++)
            addRectangle(w*i, y, 0, h, POD_OUTLINE_COLOR, POD_OUTLINE_COLOR, POD_SEP_STROKE);
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
            notifyTrafficMatrixChangeListeners();
        }
    }
    
    private JPanel pnlSidebar = new JPanel();
    
    private JLabel lblDemand = new JLabel("Demand", SwingConstants.LEFT);
    private JLabel lblEdge = new JLabel("Edge", SwingConstants.LEFT);
    private JLabel lblAgg = new JLabel("Agg", SwingConstants.LEFT);
    private JLabel lblPLen = new JLabel("PktLen", SwingConstants.CENTER);

    private JLabel lblDemandVal = new JLabel(StringOps.formatBitsPerSec(1000*1000*1000), SwingConstants.LEFT);
    private JLabel lblEdgeVal = new JLabel("100%", SwingConstants.LEFT);
    private JLabel lblAggVal = new JLabel("100%", SwingConstants.LEFT);
    private JLabel lblPLenVal = new JLabel("1514B", SwingConstants.CENTER);

    private JSlider slDemand = new MyJSlider(SwingConstants.VERTICAL, 0, 1000*1000*1000, 1000*1000*1000);
    private JSlider slEdge   = new MyJSlider(SwingConstants.VERTICAL, 0, 100, 100);
    private JSlider slAgg    = new MyJSlider(SwingConstants.VERTICAL, 0, 100, 100);
    private JSlider slPLen    = new MyJSlider(SwingConstants.VERTICAL, 64, 1514, 1514);

    private JLabel lblTrafficMatrixCurrent = new JLabel();
    private JLabel lblTrafficMatrixNext = new JLabel();
    private JLabel lblResultInfo = new JLabel();
    
    private JPanel pnlAnim = new JPanel();
    private ButtonGroup optgrpAnim = new ButtonGroup();
    private JRadioButton optAnimNone = new JRadioButton("None");
    private JRadioButton optAnimPulse = new JRadioButton("Pulse");
    private JRadioButton optAnimSawtooth = new JRadioButton("Sawtooth");
    private JRadioButton optAnimSineWave = new JRadioButton("Sine Wave");
    private JLabel lblAnimStepDuration = new JLabel("Step Duration:");
    private JSlider slAnimStepDuration = new JSlider(SwingConstants.HORIZONTAL, 1, 60, 60);
    
    private JPanel pnlMode = new JPanel();
    private ButtonGroup optgrpMode = new ButtonGroup();
    private JRadioButton optModeHW = new JRadioButton("Hardware");
    private JRadioButton optModeSW = new JRadioButton("Simulation");
    private JComboBox cboModeK = new JComboBox(new String[] {"4", "6"});
    
    private static final Font TITLE_BORDER_FONT = new java.awt.Font("Tahoma", Font.BOLD, 16);
    private static final Color TITLE_BORDER_FONT_COLOR = new java.awt.Color(0, 0, 128);
    
    private GroupLayout initGroupLayout(Container c) {
        GroupLayout layout = new GroupLayout(c);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        c.setLayout(layout);
        return layout;
    }
    
    private GroupLayout initPanel(JPanel pnl, String title) {
        return initPanel(pnl, title, TITLE_BORDER_FONT);
    }
    
    private GroupLayout initPanel(JPanel pnl, String title, Font f) {
        pnl.setBorder(BorderFactory.createTitledBorder(null, 
                                                       title, 
                                                       TitledBorder.DEFAULT_JUSTIFICATION, 
                                                       TitledBorder.DEFAULT_POSITION, 
                                                       f,
                                                       TITLE_BORDER_FONT_COLOR));
        
        return initGroupLayout(pnl);
    }
    
    /** layout and initialize all panels */
    private void initComponents() {
        initAnimPanel();
        initModePanel();
    }

    /** layout and initialize the animation panel and its components */
    private void initAnimPanel() {
        GroupLayout layout = initPanel(pnlAnim, "Animation");
        
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(optAnimNone)
                        .addComponent(optAnimPulse)
                        .addComponent(optAnimSawtooth)
                        .addComponent(optAnimSineWave))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblAnimStepDuration)
                        .addComponent(slAnimStepDuration))
        );
        
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup()
                        .addComponent(optAnimNone)
                        .addComponent(optAnimPulse)
                        .addComponent(optAnimSawtooth)
                        .addComponent(optAnimSineWave))
                    .addGroup(layout.createParallelGroup()
                        .addComponent(lblAnimStepDuration)
                        .addComponent(slAnimStepDuration))
        );
        
        optgrpAnim.add(optAnimNone);
        optgrpAnim.add(optAnimPulse);
        optgrpAnim.add(optAnimSawtooth);
        optgrpAnim.add(optAnimSineWave);
        optAnimNone.setSelected(true);
        animLastSelected = optAnimNone;
        
        layout.linkSize(SwingConstants.VERTICAL, optAnimNone, optAnimPulse, optAnimSawtooth, optAnimSineWave);
        layout.linkSize(SwingConstants.VERTICAL, lblAnimStepDuration, slAnimStepDuration);
        
        ActionListener animListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(animLastSelected != e.getSource()) {
                    animLastSelected = e.getSource();
                    handleAnimationTypeChange();
                }
            }
        };
        optAnimNone.addActionListener(animListener);
        optAnimPulse.addActionListener(animListener);
        optAnimSawtooth.addActionListener(animListener);
        optAnimSineWave.addActionListener(animListener);
    }

    /** pointer to the animation mode which was most recently selected */
    private Object animLastSelected = null;
    
    /** called when the animation mode is being changed */
    private void handleAnimationTypeChange() {
        if(optAnimNone.isSelected()) {
            // to do
        }
        else {
            // to do
        }
    }

    /** layout and initialize the mode panel and its components */
    private void initModePanel() {
        GroupLayout layout = initPanel(pnlMode, "Mode");
        
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addComponent(optModeHW)
                    .addComponent(optModeSW)
                    .addComponent(cboModeK)
        );
        
        layout.setVerticalGroup(
                layout.createParallelGroup()
                    .addComponent(optModeHW)
                    .addComponent(optModeSW)
                    .addComponent(cboModeK)
        );
        
        optgrpMode.add(optModeHW);
        optgrpMode.add(optModeSW);
        optModeSW.setSelected(true);
        modeLastSelected = optModeSW;
        cboModeK.setSelectedItem(Integer.toString(fatTreeLayout.getK()));
        
        layout.linkSize(SwingConstants.VERTICAL, optModeHW, optModeSW, cboModeK);
        
        ActionListener modeListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(modeLastSelected != e.getSource()) {
                    modeLastSelected = e.getSource();
                    handleModeTypeChange();
                }
            }
        };
        optModeHW.addActionListener(modeListener);
        optModeSW.addActionListener(modeListener);
        
        cboModeK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int newK = Integer.valueOf((String)cboModeK.getSelectedItem());
                if(fatTreeLayout.getK() != newK) {
                    handleKChange(newK);
                }
            }
        });
    }
    
    /** pointer to the mode which was most recently selected */
    private Object modeLastSelected = null;
    
    /** called when the mode is being changed */
    private void handleModeTypeChange() {
        if(optModeHW.isSelected() && fatTreeLayout.getK() != HW_FAT_TREE_K)
            fatTreeLayout.setK(HW_FAT_TREE_K);
        notifyTrafficMatrixChangeListeners();
    }
    
    /** called when K is to be changed */
    private void handleKChange(int newK) {
        if(newK != HW_FAT_TREE_K && optModeHW.isSelected()) {
            optModeSW.setSelected(true);
        }
        fatTreeLayout.setK(newK);
        notifyTrafficMatrixChangeListeners();
    }
    
    /**
     * Sets the text description of the data currently being visualized.
     * @param tm  the matrix which led to the latest data
     */
    public void setCurrentTrafficMatrixText(ETTrafficMatrix tm) {
        String s = (tm == null) ? "n/a" : tm.toStringShort();
        lblTrafficMatrixCurrent.setText("Traffic Now: " + s);
    }
    
    /**
     * Sets the text description of the data currently pending results.
     * @param tm  the matrix which has been sent to the server but not responded to yet
     */
    public void setNextTrafficMatrixText(ETTrafficMatrix tm) {
        String s = (tm == null) ? "n/a" : tm.toStringShort();
        lblTrafficMatrixNext.setText("Next Traffic: " + s);
    }

    private final MultiPointerDial dialPower, dialBandwidth, dialLatency;    
    
    public void setPowerData(int cur, int traditional, int max) {
        dialPower.setValue(0, cur);
        dialPower.setValue(1, traditional);
        dialPower.setMax(max);
    }
    
    public void setExpectedAggregateThroughput(double total_bps) {
        int gbps = (int)(total_bps / (1000 * 1000 * 1000));
        dialBandwidth.setValue(1, gbps);
    }

    public void setAchievedAggregateThroughput(int bandwidth_achieved_mbps) {
        dialBandwidth.setValue(0, bandwidth_achieved_mbps / 1000);
    }

    public void setLatencyData(int latency_ms_edge, int latency_ms_agg, int latency_ms_core) {
        dialLatency.setValue(0, latency_ms_edge);
        dialLatency.setValue(1, latency_ms_agg);
        dialLatency.setValue(2, latency_ms_core);
    }

    public void noteResult(int num_unplaced_flows) {
        if(num_unplaced_flows == 0)
            lblResultInfo.setVisible(false);
        else {
            lblResultInfo.setText("FAILED to place " + num_unplaced_flows + " flows!");
            lblResultInfo.setVisible(true);
        }
    }
    
    // --- Traffic Matrix Change Handling --- //
    // ************************************** //
    
    /** closing event listeners */
    private final LinkedList<TrafficMatrixChangeListener> trafficMatrixChangeListeneres = new LinkedList<TrafficMatrixChangeListener>();
    
    /** adds a listener to be notified when the traffic matrix has changed */
    public void addTrafficMatrixChangeListener(TrafficMatrixChangeListener c) {
        if(!trafficMatrixChangeListeneres.contains(c))
            trafficMatrixChangeListeneres.add(c);
    }

    /** removes the specified traffic matrix change listener */
    public void removeTrafficMatrixChangeListener(TrafficMatrixChangeListener c) {
        trafficMatrixChangeListeneres.remove(c);
    }
    
    /** gets the current traffic matrix */
    public ETTrafficMatrix getCurrentTrafficMatrix() {
        float demand = slDemand.getValue() / (float)slDemand.getMaximum();
        float edge = slEdge.getValue() / (float)slEdge.getMaximum();
        float aggDef = slAgg.getValue() / (float)slAgg.getMaximum();
        float agg = Math.min(1.0f-edge, aggDef);
        if(agg != aggDef) {
            slAgg.setValue((int)(100*agg));
        }
        return new ETTrafficMatrix(optModeHW.isSelected(), fatTreeLayout.getK(), demand, edge, agg, slPLen.getValue());
    }

    /**
     * Updates the slider labels and notify those listening for traffic matrix changes.
     */
    public void notifyTrafficMatrixChangeListeners() {
        lblDemandVal.setText(StringOps.formatBitsPerSec(slDemand.getValue()));
        lblEdgeVal.setText(slEdge.getValue() + "%");
        lblAggVal.setText(slAgg.getValue() + "%");
        lblPLenVal.setText(slPLen.getValue() + "B");
        
        ETTrafficMatrix tm = getCurrentTrafficMatrix();
        for(TrafficMatrixChangeListener c : trafficMatrixChangeListeneres)
            c.trafficMatrixChanged(tm);
    }
}
