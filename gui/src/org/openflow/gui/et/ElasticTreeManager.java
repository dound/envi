package org.openflow.gui.et;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.HashSet;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.plot.dial.DialPointer;
import org.openflow.gui.net.protocol.et.ETTrafficMatrix;
import org.openflow.gui.chart.MultiPointerDial;
import org.openflow.gui.drawables.DrawableIcon;
import org.openflow.gui.drawables.Link;
import org.openflow.gui.drawables.OpenFlowSwitch;
import org.openflow.util.string.StringOps;
import org.pzgui.Constants;
import org.pzgui.Drawable;
import org.pzgui.PZWindow;
import org.pzgui.icon.GeometricIcon;
import org.pzgui.icon.ShapeIcon;
import org.pzgui.layout.Edge;
import org.pzgui.layout.PZLayoutManager;
import org.pzgui.layout.Vertex;
import org.tame.MThumbSlider;

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
        PZWindow.BASE_TITLE = "Stanford University - Elastic Tree";
        
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
        
        initAttachedPanel();
        initDetachedPanel();
        initControlWindow();
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
                        o.setIcon(new ShapeIcon(new Ellipse2D.Double(0,0,5,5), java.awt.Color.DARK_GRAY));
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
            w.getContentPane().add(pnlCustomAttached);
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
        int margin = 20;
        
        // relayout the custom part of the GUI 
        pnlCustomAttached.setBounds(0, h + margin, w, RESERVED_HEIGHT_BOTTOM - 2 * margin);
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
    
    private JPanel pnlCustomAttached = new JPanel();
    
    private JPanel pnlCustomDetached = new JPanel();
    private final MultiPointerDial dialPower, dialBandwidth, dialLatency;
    private JLabel lblTrafficMatrixCurrent = new JLabel();
    private JLabel lblTrafficMatrixNext = new JLabel();
    private JLabel lblResultInfo = new JLabel();
    private JLabel lblLegend = new JLabel();
    
    private JPanel pnlTraffic = new JPanel();
    private JPanel pnlDemand = new JPanel();
    private JSlider slDemand = new MyJSlider(SwingConstants.HORIZONTAL, 0, 1000*1000*1000, 1000*1000*1000);
    private JPanel pnlLocality = new JPanel();
    private MThumbSlider slLocality = new MThumbSlider(2);
    private JPanel pnlPLen = new JPanel();
    private JSlider slPLen    = new MyJSlider(SwingConstants.HORIZONTAL, 64, 1514, 1514);
    private JCheckBox chkSplit = new JCheckBox("May split flows", false);
    private static final int THUMB_EDGE = 0;
    private static final int THUMB_AGG = 1;
    
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
    private JLabel lblAnimVary = new JLabel("Vary: ");
    private ButtonGroup optgrpAnimVary = new ButtonGroup();
    private JRadioButton optAnimVaryDemand = new JRadioButton("Demand");
    private JRadioButton optAnimVaryLocality = new JRadioButton("Locality");
    
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
    
    /** layout and initialize the attached panel and its components */
    private void initAttachedPanel() {
        GroupLayout layout = initPanel(pnlCustomAttached, "");
        pnlCustomAttached.setBorder(null);
        pnlCustomAttached.setBackground(Color.BLACK);
    }
    
    /** layout and initialize the separate, detatched control panel and its components */
    private void initDetachedPanel() {
        GroupLayout layout = initPanel(pnlCustomDetached, "");
        pnlCustomDetached.setBorder(null);
        
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addComponent(dialPower)
                    .addComponent(dialBandwidth)
                    .addComponent(dialLatency)
                    .addGroup(layout.createParallelGroup()
                        .addComponent(pnlTraffic)
                        .addComponent(lblLegend))
                    .addGroup(layout.createParallelGroup()
                        .addComponent(pnlAnim)
                        .addComponent(pnlMode)
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
                        .addComponent(pnlTraffic)
                        .addComponent(lblLegend))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlAnim)
                        .addComponent(pnlMode)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, 0)
                        .addComponent(lblTrafficMatrixCurrent)
                        .addComponent(lblTrafficMatrixNext)
                        .addComponent(lblResultInfo)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, 0))
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
        layout.linkSize(SwingConstants.HORIZONTAL, pnlAnim, pnlMode);
        
        initTrafficPanel();
        initAnimPanel();
        initModePanel();
    }
    
    /** initialize and run the control panel window */
    private void initControlWindow() {
        // create a new frame to put the control panel in
        final JFrame controlWindow = new JFrame();
        
        // add the pnlDetatched to the contol panel (it contains all the controls)
        Container c = controlWindow.getContentPane();
        GroupLayout layout = initGroupLayout(c);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addComponent(pnlCustomDetached)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                    .addComponent(pnlCustomDetached)
        );
        
        // resize pnlDetached whenever its parent is resized
        ComponentListener cl = new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                final int margin = 20;
                int w = controlWindow.getWidth();
                
                // relayout the custom part of the GUI 
                pnlCustomDetached.setBounds(0, 0, w, RESERVED_HEIGHT_BOTTOM - 2 * margin);
                
                // choose a reasonable size for the dials based on the available width
                boolean showLatency;
                int sz;
                if(w < 1280) {
                    showLatency = false;
                    sz = (w - (pnlAnim.getWidth() + pnlMode.getWidth() - margin * 2)) / 2 - 35;
                }
                else {
                    showLatency = true;
                    sz = (w - (pnlAnim.getWidth() + pnlMode.getWidth() - margin * 2)) / 3 - 25;
                }
                sz = Math.min(sz, RESERVED_HEIGHT_BOTTOM - 2 * margin);
                
                Dimension prefDialSize = new Dimension(sz, sz);
                dialPower.setPreferredSize(prefDialSize);
                dialBandwidth.setPreferredSize(prefDialSize);
                dialLatency.setPreferredSize(prefDialSize);
                dialLatency.setVisible(showLatency);
            }
            
            public void componentHidden(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}
            public void componentShown(ComponentEvent e) {}
        };
        controlWindow.addComponentListener(cl);
        
        // hide the JFrame's menubar (the inner panel will be given its own)
        controlWindow.setUndecorated(true);
        
        // give the outermost panel a title bar without min/max buttons
        controlWindow.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
        
        // close the whole program if the control panel is closed
        controlWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        controlWindow.setTitle("ElasticTree Control Panel");
        controlWindow.setSize(1280, 360);
        controlWindow.setVisible(true);
    }

    /** layout and initialize the traffic panel and its components */
    private void initTrafficPanel() {
        GroupLayout layout = initPanel(pnlTraffic, "Traffic Control");
        
        setPanelTitle(pnlDemand, "Demand",            TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlLocality, "Locality: edge=100% agg=0% core=0%",  TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlPLen,   "Packet Length",     TITLE_BORDER_FONT_SMALL);
        
        pnlDemand.add(slDemand);
        pnlLocality.add(slLocality);
        pnlPLen.add(slPLen);

        Dimension d = pnlLocality.getPreferredSize();
        d.width = Math.max(265, d.width);
        pnlLocality.setPreferredSize(d);
        
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                    .addComponent(pnlLocality)    
                    .addComponent(pnlDemand)
                    .addComponent(pnlPLen)
                    .addComponent(chkSplit)
        );
        
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, 0)
                    .addComponent(pnlLocality)
                    .addComponent(pnlDemand)
                    .addComponent(pnlPLen)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, 0)
                    .addComponent(chkSplit)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, 5)

        );
        
        layout.linkSize(SwingConstants.HORIZONTAL, pnlDemand, pnlLocality, pnlPLen, chkSplit);
        layout.linkSize(SwingConstants.VERTICAL, pnlDemand, pnlLocality, pnlPLen, chkSplit);

        slLocality.setValueAt(100, THUMB_EDGE);
        slLocality.setFillColorAt(Color.GREEN,  THUMB_EDGE);
        slLocality.setValueAt(100, THUMB_AGG);
        slLocality.setFillColorAt(Color.YELLOW, THUMB_AGG); 
        slLocality.setTrackFillColor(Color.RED);
        slLocality.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slLocality.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                validateLocality();
            }
            
            public void mouseReleased(MouseEvent e) {
                validateLocality();
                notifyTrafficMatrixChangeListeners();
            }
        });
        
        chkSplit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                notifyTrafficMatrixChangeListeners();
            }
        });
    }
    
    /** 
     * Gets the amount of traffic which the server has been asked to send 
     * through the edge layer. 
     */
    private float getLocalityEdge() {
        return slLocality.getValueAt(THUMB_EDGE) / 100.0f;
    }
    
    /** 
     * Gets the amount of traffic which the server has been asked to send 
     * through the aggregation layer.
     */
    private float getLocalityAgg() {
        return  slLocality.getValueAt(THUMB_AGG) / 100.0f - getLocalityEdge();
    }
    
    /** 
     * Gets the amount of traffic which the server has been asked to send 
     * through the core layer.
     */
    private float getLocalityCore() {
        return 1.0f - getLocalityAgg() - getLocalityEdge();
    }

    /** 
     * Sets the amount of traffic which the server should send through both the 
     * edge and aggregation layers.
     */
    private void setLocality(float edge, float agg) {
        if(edge < 0.0f) edge = 0.0f; else if(edge > 100.0f) edge = 100.0f;
        if(agg  < 0.0f) agg  = 0.0f; else if(agg  > 100.0f) agg  = 100.0f;
        slLocality.setValueAt((int)(edge * 100), THUMB_EDGE);
        slLocality.setValueAt((int)(agg  * 100), THUMB_AGG);
        validateLocality();
    }
    
    /** tracks the previous value for the aggregation slider */
    private int lastAggValue = 100;
    
    /** 
     * Ensures that the edge locality slider does not go past the 
     * aggregation locality slider. 
     */
    private void validateLocality() {
        int edge = slLocality.getValueAt(THUMB_EDGE);
        int agg = slLocality.getValueAt(THUMB_AGG);
        
        // prevent the thumbs from crossing
        if(edge > agg) {
            if(slLocality.getValueAt(THUMB_AGG) == lastAggValue) {
                // edge is moving, so move agg to edge
                slLocality.setValueAt(edge, THUMB_AGG);
            }
            else
                slLocality.setValueAt(agg, THUMB_EDGE);
        }
        
        lastAggValue = slLocality.getValueAt(THUMB_AGG);
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
                    .addComponent(pnlAnimStepDuration)
                    .addComponent(pnlAnimStepSize)
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(lblAnimVary)
                            .addComponent(optAnimVaryDemand)
                            .addComponent(optAnimVaryLocality))
        );
        
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, 0)
                    .addGroup(layout.createParallelGroup()
                        .addComponent(optAnimNone)
                        .addComponent(optAnimPulse)
                        .addComponent(optAnimSawtooth)
                        .addComponent(optAnimSineWave))
                    .addComponent(pnlAnimStepDuration)
                    .addComponent(pnlAnimStepSize)
                    .addGroup(layout.createParallelGroup()
                            .addComponent(lblAnimVary)
                            .addComponent(optAnimVaryDemand)
                            .addComponent(optAnimVaryLocality))
        );
        
        optgrpAnim.add(optAnimNone);
        optgrpAnim.add(optAnimPulse);
        optgrpAnim.add(optAnimSawtooth);
        optgrpAnim.add(optAnimSineWave);
        optAnimNone.setSelected(true);
        animLastSelected = optAnimNone;
        
        optgrpAnimVary.add(optAnimVaryDemand);
        optgrpAnimVary.add(optAnimVaryLocality);
        optAnimVaryDemand.setSelected(true);
        animVaryLastSelected = optAnimVaryDemand;
        
        layout.linkSize(SwingConstants.VERTICAL, optAnimNone, optAnimPulse, optAnimSawtooth, optAnimSineWave, optAnimVaryDemand, optAnimVaryLocality, lblAnimVary);
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
        
        ActionListener animVaryListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(animVaryLastSelected != e.getSource()) {
                    animVaryLastSelected = e.getSource();
                    handleAnimationVaryChange();
                }
            }
        };
        optAnimVaryDemand.addActionListener(animVaryListener);
        optAnimVaryLocality.addActionListener(animVaryListener);
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

    /** pointer to the animation variation which was most recently selected */
    private Object animVaryLastSelected = null;
    
    /** called when the animation variation is being changed */
    private void handleAnimationVaryChange() {
        animationManager.setVaryDemand(optAnimVaryDemand.isSelected());
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
                layout.createSequentialGroup()
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, 0)
                    .addGroup(layout.createParallelGroup()
                        .addComponent(optModeHW)
                        .addComponent(optModeSW)
                        .addComponent(cboModeK))
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
        private float edge = 0.0f;
        private float agg  = 0.0f;
        
        /** current demand */
        private int demand = 0;
        
        /** current animation type */
        private AnimationStepType type;
        
        /** current place in the animation */
        private double step;
        
        /** step size */
        public double stepSize = 0.10;
        
        /** current direction of the animation */
        private double stepPolarity;
        
        /** whether demand is being varied (else locality is varied) */
        private boolean varyDemand = true;
        
        /** starts the animation */
        public synchronized void startAnimation(AnimationStepType type) {
            // ignore user-input while animating
            setIgnoreChangesToSliders(true);
            
            this.type = type;
            
            // start at the beginning with 0.0 (nextFrame() will advance this to 0.0)
            step = 0.0 - stepSize;
            stepPolarity = -1.0;
            
            // start with all core traffic (=> start with all edge traffic for pulse, doesn't matter for others)
            if(varyDemand)
                this.demand = 0;
            else {
                this.edge = 0.0f;
                this.agg = 0.0f;
            }
            setLive(true);
            
            // tell the thread it can have the lock
            animationManager.notifyAll();
            
            // wake it up if it was sleeping
            animationManager.interrupt();
        }
        
        /** stops the current animation */
        public synchronized void stopAnimation() {
            setLive(false);
            
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
        
        /** Sets which parameter to vary (true => demand, false => locality. */
        public void setVaryDemand(boolean b) {
            if(varyDemand != b) {
                varyDemand = b;
                if(varyDemand) {
                    edge = getLocalityEdge();
                    agg = getLocalityAgg();
                }
                else {
                    demand = slDemand.getValue();
                }
                setSliderStatus();
            }
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
                    if(varyDemand) {
                        slDemand.setValue(demand);
                    }
                    else {
                        setLocality(edge, agg);
                    }
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
                nextPulse();
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
                
                nextStep((float)p);
            }
        }
        
        /** 
         * Sets whether the animation is live and enables/disables sliders as 
         * appropriate for the current animation. 
         */
        private void setLive(boolean b) {
            this.live = b;
            setSliderStatus();
        }
        
        /** Enables/disables sliders as appropriate for the current animation. */ 
        private void setSliderStatus() {
            if(live) {
                if(varyDemand) {
                    slDemand.setEnabled(false);
                    slLocality.setEnabled(true);
                }
                else {
                    slDemand.setEnabled(true);
                    slLocality.setEnabled(false);
                }
            }
            else {
                slDemand.setEnabled(true);
                slLocality.setEnabled(true);
            }
        }
        
        private void nextPulse() {
            if(varyDemand) {
                if(demand == 0)
                    demand = 500 * 1000 * 1000;
                else if(demand == 500 * 1000 * 1000)
                    demand = 1000 * 1000 * 1000;
                else
                    demand = 0;
            }
            else {
                if(edge==1.0f) {
                    // switch from all edge to all agg
                    edge =  0.0f;
                    agg  = 1.0f;
                }
                else if(edge==0.0f && agg==1.0f) {
                    // switch from all agg to all core
                    agg = 0.0f;
                }
                else {
                    // switch to all edge
                    edge = 1.0f;
                    agg  = 0.0f;
                }
            }
        }
        
        private void nextStep(float p) {
            // compute the edge and agg settings
            if(varyDemand) {
                demand = (int)(p * 1000 * 1000 * 1000); 
            }
            else {
                if(p <= 0.5) {
                    // first half of animation is between all edge and all agg
                    p /= 0.5;
                    edge = (1.0f * (1-p)) + (0.0f * p);
                    agg = 1.0f - edge;
                }
                else {
                    // second half of animation is between all agg and all core
                    p = (p - 0.5f) / 0.5f;
                    edge = 0.0f;
                    agg = (1.0f * (1-p)) + (0.0f * p);
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
        float edge = getLocalityEdge();
        float agg = getLocalityAgg();
        return new ETTrafficMatrix(optModeHW.isSelected(), chkSplit.isSelected(), fatTreeLayout.getK(), demand, edge, agg, slPLen.getValue());
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
    }
    
    /**
     * Updates the slider labels and notify those listening for traffic matrix changes.
     */
    public void notifyTrafficMatrixChangeListeners() {
        int edge = (int)(100 * getLocalityEdge());
        int agg = (int)(100 * getLocalityAgg());
        int core = (int)(100 * getLocalityCore());
        
        setPanelTitle(pnlDemand, "Demand: " + StringOps.formatBitsPerSec(slDemand.getValue()), TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlLocality, "Locality: edge=" + edge + "% agg=" + agg + "% core=" + core + "%", TITLE_BORDER_FONT_SMALL);
        setPanelTitle(pnlPLen,   "Packet Length: " + slPLen.getValue() + "B", TITLE_BORDER_FONT_SMALL);
        
        ETTrafficMatrix tm = getCurrentTrafficMatrix();
        for(TrafficMatrixChangeListener c : trafficMatrixChangeListeneres)
            c.trafficMatrixChanged(tm);
    }
}
