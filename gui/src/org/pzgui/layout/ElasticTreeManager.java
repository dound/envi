package org.pzgui.layout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.plot.dial.DialPointer;
import org.openflow.lavi.drawables.DrawableIcon;
import org.openflow.lavi.drawables.Link;
import org.openflow.lavi.drawables.OpenFlowSwitch;
import org.openflow.lavi.net.protocol.ETTrafficMatrix;
import org.openflow.util.string.StringOps;
import org.pzgui.Constants;
import org.pzgui.Drawable;
import org.pzgui.PZWindow;
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
    public static final int RESERVED_HEIGHT_BOTTOM = 400;
    
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
        
        dialPower = new MultiPointerDial("Power Consumption", "Watts", 2, 459, 50);
        setPointersFor2PointerDial(dialPower);
        
        int max_gbps = 2 * fatTreeLayout.size_links(); /* 1Gbps in each direction per link */
        dialBandwidth = new MultiPointerDial("Aggregate Throughput", "Gbps", 2,  max_gbps, max_gbps/10);
        setPointersFor2PointerDial(dialBandwidth);
        
        dialLatency = new MultiPointerDial("Layer Latency", "msec", 3, 100, 10);
        setPointersFor3PointerDial(dialLatency);
        
        initSidebarPanel();
        animationManager.start();
    }
    
    /** the k-value of the current fat tree */
    public int getK() {
        return fatTreeLayout.getK();
    }
    
    /**
     * Adds two pointers to the specified dial.  The first is the usual, the 
     * second is a red pointer.
     */
    private void setPointersFor2PointerDial(MultiPointerDial d) {
        DialPointer.Pointer p;
        p = new DialPointer.Pointer(0);
        p.setRadius(0.95);
        d.setPointer(p);
        
        p = new DialPointer.Pointer(1);
        p.setRadius(1.0);
        p.setFillPaint(Color.RED);
        d.setPointer(p);
    }
    
    /**
     * Adds three pointers to the specified dial.  Each is a different size and
     * color.
     */
    private void setPointersFor3PointerDial(MultiPointerDial d) {
        DialPointer.Pointer p;
        p = new DialPointer.Pointer(0);
        p.setRadius(1.0);
        p.setFillPaint(Color.RED);
        p.setWidthRadius(0.1);
        d.setPointer(p);
        
        p = new DialPointer.Pointer(1);
        p.setRadius(0.75);
        p.setFillPaint(Color.YELLOW);
        p.setWidthRadius(0.075);
        d.setPointer(p);
        
        p = new DialPointer.Pointer(2);
        p.setRadius(0.5);
        p.setFillPaint(Color.GREEN);
        p.setWidthRadius(0.025);
        d.setPointer(p);
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
            w.getContentPane().add(pnlCustom);
            w.setReservedHeightBottom(RESERVED_HEIGHT_BOTTOM);
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
        
        // choose a reasonable size for the dials based on the available width
        int margin = 20;
        int sz = Math.min(RESERVED_HEIGHT_BOTTOM - 3 * margin, w/4);
        Dimension prefDialSize = new Dimension(sz, sz);
        dialPower.setPreferredSize(prefDialSize);
        dialBandwidth.setPreferredSize(prefDialSize);
        dialLatency.setPreferredSize(prefDialSize);
        
        // relayout the custom part of the GUI 
        pnlCustom.setBounds(0, h + margin, w, RESERVED_HEIGHT_BOTTOM - 2 * margin);
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
            if(i != getValue()) {
                super.setValue(i);
                
                if(!isIgnoreChangesToSliders())
                    notifyTrafficMatrixChangeListeners();
            }
        }
    }
    
    private JPanel pnlCustom = new JPanel();
    private final MultiPointerDial dialPower, dialBandwidth, dialLatency;
    private JLabel lblTrafficMatrixCurrent = new JLabel();
    private JLabel lblTrafficMatrixNext = new JLabel();
    private JLabel lblResultInfo = new JLabel();
    private JLabel lblLegend = new JLabel();
    
    private JPanel pnlTraffic = new JPanel();
    private JPanel pnlDemand = new JPanel();
    private JSlider slDemand = new MyJSlider(SwingConstants.HORIZONTAL, 0, 1000*1000*1000, 1000*1000*1000);
    private JPanel pnlEdge = new JPanel();
    private JSlider slEdge   = new MyJSlider(SwingConstants.HORIZONTAL, 0, 100, 100);
    private JPanel pnlAgg = new JPanel();
    private JSlider slAgg    = new MyJSlider(SwingConstants.HORIZONTAL, 0, 100, 100);
    private JPanel pnlPLen = new JPanel();
    private JSlider slPLen    = new MyJSlider(SwingConstants.HORIZONTAL, 64, 1514, 1514);

    private JPanel pnlAnim = new JPanel();
    private ButtonGroup optgrpAnim = new ButtonGroup();
    private JRadioButton optAnimNone = new JRadioButton("None");
    private JRadioButton optAnimPulse = new JRadioButton("Pulse");
    private JRadioButton optAnimSawtooth = new JRadioButton("Sawtooth");
    private JRadioButton optAnimSineWave = new JRadioButton("Sine Wave");
    private JPanel pnlAnimStepDuration = new JPanel();
    private JSlider slAnimStepDuration = new JSlider(SwingConstants.HORIZONTAL, 250, 15000, 250);
    private JPanel pnlAnimStepSize = new JPanel();
    private JSlider slAnimStepSize = new JSlider(SwingConstants.HORIZONTAL, 1, 100, 3);
    
    private JPanel pnlMode = new JPanel();
    private ButtonGroup optgrpMode = new ButtonGroup();
    private JRadioButton optModeHW = new JRadioButton("Hardware");
    private JRadioButton optModeSW = new JRadioButton("Simulation");
    private JComboBox cboModeK = new JComboBox(new String[] {"4", "6"});
    
    private static final Font TITLE_BORDER_FONT = new java.awt.Font("Tahoma", Font.BOLD, 16);
    private static final Font TITLE_BORDER_FONT_SMALL = new java.awt.Font("Tahoma", Font.BOLD, 12);
    private static final Color TITLE_BORDER_FONT_COLOR = new java.awt.Color(0, 0, 128);
    
    private GroupLayout initGroupLayout(Container c) {
        GroupLayout layout = new GroupLayout(c);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        c.setLayout(layout);
        return layout;
    }
    
    private void setPanelTitle(JPanel pnl, String title, Font f) {
        pnl.setBorder(BorderFactory.createTitledBorder(null, 
                title, 
                TitledBorder.DEFAULT_JUSTIFICATION, 
                TitledBorder.DEFAULT_POSITION, 
                f,
                TITLE_BORDER_FONT_COLOR));
    }
    
    private GroupLayout initPanel(JPanel pnl, String title) {
        return initPanel(pnl, title, TITLE_BORDER_FONT);
    }
    
    private GroupLayout initPanel(JPanel pnl, String title, Font f) {
        setPanelTitle(pnl, title, f);
        
        return initGroupLayout(pnl);
    }
    
    /** layout and initialize the sidebar panel and its components */
    private void initSidebarPanel() {
        GroupLayout layout = initPanel(pnlCustom, "");
        pnlCustom.setBorder(null);
        
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addComponent(dialPower)
                    .addComponent(dialBandwidth)
                    .addComponent(dialLatency)
                    .addGroup(layout.createParallelGroup()
                        .addComponent(pnlTraffic))
                    .addGroup(layout.createParallelGroup()
                        .addComponent(pnlAnim)
                        .addComponent(pnlMode)
                        .addComponent(lblLegend)
                        .addComponent(lblTrafficMatrixCurrent)
                        .addComponent(lblTrafficMatrixNext)
                        .addComponent(lblResultInfo))
        );
        
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addComponent(dialPower)
                        .addComponent(dialBandwidth)
                        .addComponent(dialLatency)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlTraffic))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlAnim)
                        .addComponent(pnlMode)
                        .addComponent(lblLegend)
                        .addComponent(lblTrafficMatrixCurrent)
                        .addComponent(lblTrafficMatrixNext)
                        .addComponent(lblResultInfo))
        );
        
        lblResultInfo.setVisible(false);        
        lblResultInfo.setForeground(Color.RED);
        lblResultInfo.setFont(new Font("Tahoma", Font.BOLD, 16));
        
        lblLegend.setIcon(new ImageIcon(Link.USAGE_LEGEND));
        lblLegend.setPreferredSize(new Dimension(Link.USAGE_LEGEND.getWidth(), Link.USAGE_LEGEND.getHeight()));
        lblLegend.setBorder(new javax.swing.border.LineBorder(Color.BLACK, 1));
        
        layout.linkSize(SwingConstants.HORIZONTAL, dialPower, dialBandwidth, dialLatency);
        layout.linkSize(SwingConstants.VERTICAL, dialPower, dialBandwidth, dialLatency);
        layout.linkSize(SwingConstants.VERTICAL, lblTrafficMatrixCurrent, lblTrafficMatrixNext, lblResultInfo);
        layout.linkSize(SwingConstants.HORIZONTAL, pnlTraffic, pnlAnim, pnlMode);
        
        initTrafficPanel();
        initAnimPanel();
        initModePanel();
    }

    /** layout and initialize the traffic panel and its components */
    private void initTrafficPanel() {
        GroupLayout layout = initPanel(pnlTraffic, "Traffic Control");
        
        setPanelTitle(pnlDemand, "Demand",            TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlAgg,    "Aggregation Layer", TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlEdge,   "Edge Layer",        TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlPLen,   "Packet Length",     TITLE_BORDER_FONT_SMALL);
        
        pnlDemand.add(slDemand);
        pnlAgg.add(slAgg);
        pnlEdge.add(slEdge);
        pnlPLen.add(slPLen);
        
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                    .addComponent(pnlDemand)
                    .addComponent(pnlAgg)
                    .addComponent(pnlEdge)
                    .addComponent(pnlPLen)
        );
        
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addComponent(pnlDemand)
                    .addComponent(pnlAgg)
                    .addComponent(pnlEdge)
                    .addComponent(pnlPLen)
        );
        
        layout.linkSize(SwingConstants.HORIZONTAL, pnlDemand, pnlAgg, pnlEdge, pnlPLen);
        layout.linkSize(SwingConstants.VERTICAL, pnlDemand, pnlAgg, pnlEdge, pnlPLen);
    }
    
    /** layout and initialize the animation panel and its components */
    private void initAnimPanel() {
        GroupLayout layout = initPanel(pnlAnim, "Animation");
        
        setPanelTitle(pnlAnimStepDuration, "Step Duration: 250msec", TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlAnimStepSize, "Step Size (% of Space): 3%",  TITLE_BORDER_FONT_SMALL);
        
        pnlAnimStepDuration.add(slAnimStepDuration);
        pnlAnimStepSize.add(slAnimStepSize);
        
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(optAnimNone)
                        .addComponent(optAnimPulse)
                        .addComponent(optAnimSawtooth)
                        .addComponent(optAnimSineWave))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlAnimStepDuration))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlAnimStepSize))
        );
        
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup()
                        .addComponent(optAnimNone)
                        .addComponent(optAnimPulse)
                        .addComponent(optAnimSawtooth)
                        .addComponent(optAnimSineWave))
                    .addGroup(layout.createParallelGroup()
                        .addComponent(pnlAnimStepDuration))
                    .addGroup(layout.createParallelGroup()
                        .addComponent(pnlAnimStepSize))
        );
        
        optgrpAnim.add(optAnimNone);
        optgrpAnim.add(optAnimPulse);
        optgrpAnim.add(optAnimSawtooth);
        optgrpAnim.add(optAnimSineWave);
        optAnimNone.setSelected(true);
        animLastSelected = optAnimNone;
        
        layout.linkSize(SwingConstants.VERTICAL, optAnimNone, optAnimPulse, optAnimSawtooth, optAnimSineWave);
        layout.linkSize(SwingConstants.VERTICAL, pnlAnimStepDuration, pnlAnimStepSize);
        
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
        
        slAnimStepDuration.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setPanelTitle(pnlAnimStepDuration, "Step Duration: " + StringOps.formatSecs(slAnimStepDuration.getValue()), TITLE_BORDER_FONT_SMALL);
                animationManager.setStepDuration(slAnimStepDuration.getValue());
            }
        });
        
        slAnimStepSize.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setPanelTitle(pnlAnimStepSize, "Step Size (% of Space): " + slAnimStepSize.getValue() + "%", TITLE_BORDER_FONT_SMALL);
                animationManager.setStepDuration(slAnimStepSize.getValue());
            }
        });
    }

    /** pointer to the animation mode which was most recently selected */
    private Object animLastSelected = null;
    
    /** called when the animation mode is being changed */
    private void handleAnimationTypeChange() {
        if(optAnimNone.isSelected()) {
            animationManager.stopAnimation();
        }
        else {
            AnimationStepType type = null;
            if(optAnimPulse.isSelected())
                type = AnimationStepType.PULSE;
            else if(optAnimSawtooth.isSelected())
                type = AnimationStepType.SAWTOOTH;
            else if(optAnimSineWave.isSelected())
                type = AnimationStepType.SINEWAVE;
            
            animationManager.startAnimation(type);
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
        
        // not quite supported yet
        cboModeK.setVisible(false);
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

    public void setPowerData(int cur, int traditional, int max) {
        dialPower.setValue(0, cur);
        dialPower.setValue(1, traditional);
        if(dialPower.setMax(max))
            setPointersFor2PointerDial(dialPower);
    }
    
    public void setExpectedAggregateThroughput(double total_bps) {
        int gbps = (int)(total_bps / (1000 * 1000 * 1000));
        dialBandwidth.setValue(1, gbps);
    }

    public void setAchievedAggregateThroughput(int bandwidth_achieved_mbps) {
        dialBandwidth.setValue(0, bandwidth_achieved_mbps / 1000);
    }

    public void setLatencyData(int latency_ms_edge, int latency_ms_agg, int latency_ms_core) {
        dialLatency.setValue(2, latency_ms_edge);
        dialLatency.setValue(1, latency_ms_agg);
        dialLatency.setValue(0, latency_ms_core);
    }

    public void noteResult(int num_unplaced_flows) {
        if(num_unplaced_flows == 0)
            lblResultInfo.setVisible(false);
        else {
            lblResultInfo.setText("FAILED to place " + num_unplaced_flows + " flows!");
            lblResultInfo.setVisible(true);
        }
    }
    
    
    // --------- Animation Handling --------- //
    // ************************************** //
    
    /** types of animation steps */
    private enum AnimationStepType {
        PULSE(),
        SAWTOOTH(),
        SINEWAVE();
    }
    
    /** handles animating locality variations over time */
    private class AnimationManager extends Thread {
        /** whether it is animating */
        private boolean live = false;
        
        /** time between animation steps */
        private int period_msec = 250;
        
        /** current traffic pattern */
        private int edge = 0;
        private int agg  = 0;
        
        /** current animation type */
        private AnimationStepType type;
        
        /** current place in the animation */
        private double step;
        
        /** step size */
        public double stepSize = 0.10;
        
        /** current direction of the animation */
        private double stepPolarity;
        
        /** starts the animation */
        public synchronized void startAnimation(AnimationStepType type) {
            // ignore user-input while animating
            setIgnoreChangesToSliders(true);
            
            this.type = type;
            
            // start at the beginning with 0.0 (nextFrame() will advance this to 0.0)
            step = 0.0 - stepSize;
            stepPolarity = -1.0;
            
            // start with all core traffic (=> start with all edge traffic for pulse, doesn't matter for others)
            this.edge = 0;
            this.agg = 0;
            live = true;
            
            // tell the thread it can have the lock
            animationManager.notifyAll();
            
            // wake it up if it was sleeping
            animationManager.interrupt();
        }
        
        /** stops the current animation */
        public synchronized void stopAnimation() {
            live = false;
            
            // re-enable user input
            setIgnoreChangesToSliders(false);
            
            // wake the thread up so we don't continue to sleep
            animationManager.interrupt();
        }

        /** Sets the step duration of an animation frame */
        public synchronized void setStepDuration(int period_msec) {
            this.period_msec = period_msec;
            animationManager.interrupt();
        }
        
        /** Sets the step size of the animation */
        public synchronized void setStepSize(int stepSize) {
            this.stepSize = stepSize / 100.0;
            if(this.stepSize <= 0 || this.stepSize > 1.0)
                throw new IllegalArgumentException("bad argument to setStepSize: " + stepSize);
            animationManager.interrupt();
        }
        
        /** main loop: animate while live */
        public void run() {
            while(true) {
                synchronized(animationManager) {
                    // wait until it is time to animate
                    while(!live) {
                        try {
                            Thread.sleep(1000);
                            animationManager.wait(); 
                        } 
                        catch(InterruptedException e) {}
                    }
    
                    // compute apply the edge and agg settings for the next step
                    nextFrame();
                    slEdge.setValue(edge);
                    slAgg.setValue(agg);
                    notifyTrafficMatrixChangeListeners();
                }
                
                // wait in between intervals
                try {
                    Thread.sleep(period_msec);
                }
                catch(InterruptedException e) {}
            }
        }
        
        /** render the next set of animation values */
        private void nextFrame() {
            // go to the next frame
            if(type == AnimationStepType.PULSE) {
                if(edge==100) {
                    // switch from all edge to all agg
                    edge = 0;
                    agg = 100;
                }
                else if(edge==0 && agg==100) {
                    // switch from all agg to all core
                    agg = 0;
                }
                else {
                    // switch to all edge
                    edge = 100;
                    agg = 0;
                }
            }
            else {
                // switch polarities at the endpoints
                if(step <= 0.0) {
                    step = 0.0;
                    stepPolarity = 1.0; 
                }
                else if(step >= 1.0) {
                    step = 1.0;
                    stepPolarity = -1.0; 
                }
                
                // advance the step
                step += stepSize * stepPolarity;
                if(step > 1.0)
                    step = 1.0;
                else if(step < 0.0)
                    step = 0.0;
                
                // compute the next frame: linear or sinewave step
                double p = (type == AnimationStepType.SAWTOOTH) ? step : Math.sin(step*Math.PI/2);
                
                // compute the edge and agg settings
                if(p <= 0.5) {
                    // first half of animation is between all edge and all agg
                    p /= 0.5;
                    edge = (int)((100 * (1-p)) + (0 * p));
                    agg = 100 - edge;
                }
                else {
                    // second half of animation is between all agg and all core
                    p = (p - 0.5) / 0.5;
                    edge = 0;
                    agg = (int)((100 * (1-p)) + (0 * p));
                }
            }
        }
    }
    private AnimationManager animationManager = new AnimationManager();
    
    
    // --- Traffic Matrix Change Handling --- //
    // ************************************** //
    
    /** whether to ignore changes to sliders */
    private boolean ignoreChangesToSliders = false;
    
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
    
    /** Gets whether slider changes are being ignored. */
    public boolean isIgnoreChangesToSliders() {
        return ignoreChangesToSliders;
    }

    /** 
     * Sets whether to ignore slider changes (if ignored, changes do not 
     * trigger commands to the server. 
     */
    private void setIgnoreChangesToSliders(boolean b) {
        ignoreChangesToSliders = b;
        slEdge.setEnabled(!b);
        slAgg.setEnabled(!b);
    }
    
    /**
     * Updates the slider labels and notify those listening for traffic matrix changes.
     */
    public void notifyTrafficMatrixChangeListeners() {
        setPanelTitle(pnlDemand, "Demand: " + StringOps.formatBitsPerSec(slDemand.getValue()), TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlAgg,    "Aggregation Layer: " + slAgg.getValue() + "%", TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlEdge,   "Edge Layer: " + slEdge.getValue() + "%", TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlPLen,   "Packet Length: " + slPLen.getValue() + "B", TITLE_BORDER_FONT_SMALL);
        
        ETTrafficMatrix tm = getCurrentTrafficMatrix();
        for(TrafficMatrixChangeListener c : trafficMatrixChangeListeneres)
            c.trafficMatrixChanged(tm);
    }
}
