package org.openflow.gui.et;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.openflow.gui.net.protocol.et.ETTrafficMatrix;
import org.openflow.gui.drawables.DrawableIcon;
import org.openflow.gui.drawables.Link;
import org.openflow.gui.drawables.OpenFlowSwitch;
import org.openflow.util.string.StringOps;
import org.pzgui.Constants;
import org.pzgui.Drawable;
import org.pzgui.PZWindow;
import org.pzgui.StringDrawer;
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
    
    private static final int FONT_BIG_SIZE = 28;
    private static final Font FONT_BIG = new Font("Tahoma", Font.BOLD, FONT_BIG_SIZE);
    
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
        
        initAttachedPanel();
        initDetachedPanel();
        initControlWindow();
        animationManager.start();
    }
    
    /** the k-value of the current fat tree */
    public int getK() {
        return fatTreeLayout.getK();
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
        
        // relayout the custom part of the GUI 
        pnlCustomAttached.setBounds(0, h, w, RESERVED_HEIGHT_BOTTOM); 
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
        
        try {
            for(DrawableIcon d : liveIcons)
                d.drawObject(gfx);
        }
        catch(ConcurrentModificationException e) {
            /* blip, just ignore it: it should draw fine next time around */
        }
    }
    
    public void postRedraw() {
        Graphics cg = pnlSliders.getGraphics();
        if(cg != null) {
            synchronized(slidersImg) { // prevent tearing
                cg.drawImage(slidersImg, 0, 0, null);
            }
        }
    }

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
    
    // built-in panel components
    private JPanel pnlCustomAttached = new JPanel();
    
    private JPanel pnlModes = new JPanel();
    private ButtonGroup optgrpAlgMode = new ButtonGroup();
    private JRadioButton optAlgModeOrig = new JRadioButton("Original");
    private JRadioButton optAlgModeOpt = new JRadioButton("Optimized");
    private ButtonGroup optgrpNetMode = new ButtonGroup();
    private JRadioButton optNetModeHW = new JRadioButton("Deployment");
    private JRadioButton optNetModeSW = new JRadioButton("Example");
    
    private ChartPanel pnlChart;
    private JPanel pnlSliders = new JPanel();
    
    // control panel components
    private JPanel pnlCustomDetached = new JPanel();
    private JLabel lblTrafficMatrixCurrent = new JLabel();
    private JLabel lblTrafficMatrixNext = new JLabel();
    private JLabel lblResultInfo = new JLabel();
    
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
        pnlChart = new ChartPanel(chart);
        
        // maximize the width of pnlChart
        pnlChart.setPreferredSize(new Dimension(2000, 200));
        
        // don't let the pnlChart get too tall
        pnlChart.setMinimumSize(new Dimension(400, RESERVED_HEIGHT_BOTTOM - 98));
        pnlChart.setMaximumSize(new Dimension(2000, RESERVED_HEIGHT_BOTTOM - 98));
        
        // manual gap
        JLabel gap1 = new JLabel("");
        gap1.setMinimumSize(new Dimension(1, 25));
        gap1.setMaximumSize(gap1.getMinimumSize());
        
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addComponent(pnlModes)
                    .addGroup(layout.createParallelGroup()
                        .addComponent(gap1)
                        .addComponent(pnlChart))
                    .addComponent(pnlSliders)
        );
        
        layout.setVerticalGroup(
                layout.createParallelGroup()
                    .addComponent(pnlModes)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(gap1)
                        .addComponent(pnlChart))
                    .addComponent(pnlSliders)
        );
        
        initModesPanel();
        initSlidersPanel();
        
        // force all components within this panel to have a white-on-black color
        // scheme with a large font size
        LinkedList<Container> containers = new LinkedList<Container>();
        containers.add(pnlCustomAttached);
        while(containers.size() > 0) {
            Container c = containers.removeFirst();
            for(Component x : c.getComponents()) {
                if(x instanceof Container)
                    containers.add((Container)x);
                x.setBackground(Color.BLACK);
                x.setForeground(Color.WHITE);
                x.setFont(FONT_BIG);
                x.setFocusable(false);
            }
        }
    }
    
    private void initModesPanel() {
        GroupLayout layout = initGroupLayout(pnlModes);
        
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                    .addComponent(optNetModeHW)
                    .addComponent(optNetModeSW)
                    .addComponent(optAlgModeOrig)
                    .addComponent(optAlgModeOpt)
        );
        
        layout.setVerticalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, 40, 40)
                        .addComponent(optNetModeHW)
                        .addComponent(optNetModeSW)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, 50, 50)
                        .addComponent(optAlgModeOrig)
                        .addComponent(optAlgModeOpt))
        );
        
        optgrpNetMode.add(optNetModeHW);
        optgrpNetMode.add(optNetModeSW);
        optNetModeHW.setSelected(true);
        netModeLastSelected = optNetModeHW;
        optNetModeSW.setEnabled(false);
        
        layout.linkSize(SwingConstants.VERTICAL, optNetModeHW, optNetModeSW);
        
        ActionListener netModeListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(netModeLastSelected != e.getSource()) {
                    netModeLastSelected = e.getSource();
                    handleNetModeTypeChange();
                }
            }
        };
        optNetModeHW.addActionListener(netModeListener);
        optNetModeSW.addActionListener(netModeListener);
        
        optgrpAlgMode.add(optAlgModeOrig);
        optgrpAlgMode.add(optAlgModeOpt);
        optAlgModeOpt.setSelected(true);
        algModeLastSelected = optAlgModeOpt;
        optAlgModeOrig.setEnabled(false);
        
        layout.linkSize(SwingConstants.VERTICAL, optAlgModeOrig, optAlgModeOpt);
        
        ActionListener algModeListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(algModeLastSelected != e.getSource()) {
                    algModeLastSelected = e.getSource();
                    handleAlgModeTypeChange();
                }
            }
        };
        optAlgModeOrig.addActionListener(algModeListener);
        optAlgModeOpt.addActionListener(algModeListener);
    }
    
    // ----------- Chart Creation ----------- //
    /** prepare a basic JFreeChart */
    private JFreeChart prepareChart(String title, String xAxis, String yAxis, XYSeriesCollection coll, boolean useLegend) {
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            xAxis,
            yAxis,
            coll,
            PlotOrientation.VERTICAL,
            useLegend,
            false, //tooltips
            false  //URLs
        );    
        
        chart.setBorderVisible(false);
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        
        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setBackgroundPaint(Constants.cmap(Color.WHITE));
        Color gc = SHOW_CHART_GRIDLINES ? Constants.cmap(Color.LIGHT_GRAY) : Color.BLACK;
        plot.setDomainGridlinePaint(gc);
        plot.setRangeGridlinePaint(gc);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);
        
        ValueAxis domain = plot.getDomainAxis();
        domain.setLabelFont(FONT_BIG);
        domain.setTickLabelsVisible(false);
        domain.setTickMarksVisible(false);
        domain.setAutoRange(true);
        
        LegendTitle lt = chart.getLegend();
        if(lt != null)
            lt.setPosition(RectangleEdge.TOP);
        
        return chart;
    }
    
    // chart configuration parameters
    private static final int MAX_VIS_DATA_POINTS = 100;
    private static final int FONT_CHART_SIZE = 24;
    private static final Font FONT_CHART = new Font("Tahoma", Font.BOLD, FONT_CHART_SIZE);
    private static final boolean DEFAULT_SHOW_AXES = false;
    private static final boolean SHOW_CHART_GRIDLINES = false;
    private static final int MAX_LATENCY_MS = 25;
    
    private final XYSeries chartDataXput = new XYSeries("Throughput", false, false);
    private final XYSeries chartDataPower = new XYSeries("Power", false, false);
    private final XYSeries chartDataLatency = new XYSeries("Latency", false, false);
    private final JFreeChart chart = createChart(DEFAULT_SHOW_AXES);
    private int datapointOn = 0;
    
    /** create a JFreeChart for showing stats over time */
    private JFreeChart createChart(boolean showAxes) {
        XYSeriesCollection chartDatas[]  = new XYSeriesCollection[] {new XYSeriesCollection(), new XYSeriesCollection(), new XYSeriesCollection()};
        JFreeChart chart = prepareChart("", "", "", chartDatas[0], false);   
        chart.setBackgroundPaint(Color.BLACK);
        chart.setBorderPaint(Color.WHITE);
        XYPlot plot = (XYPlot)chart.getPlot();
        
        // use a fixed range
        ValueAxis domain = plot.getDomainAxis();
        domain.setAutoRange(false);
        
        // setup colors and axes for each line
        final XYSeries[] STATS_SERIES = new XYSeries[]{chartDataPower, chartDataXput, chartDataLatency};
        for(int i=0; i<getNumSliders(); i++) {
            // how to draw the data in this collection
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesPaint(0, STATS_COLORS[i]);
            float ssz = (i == 0) ? 3.7f : ((i == 1) ? 8.0f : 3.3f); // hack to make the lines appear the same size (silly jfreechart)
            BasicStroke bs = new BasicStroke(ssz, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
            renderer.setSeriesStroke(0, bs);
            renderer.setSeriesLinesVisible(0, true);
            renderer.setSeriesShapesVisible(0, false);
            renderer.setSeriesVisibleInLegend(0, true, false);
            plot.setRenderer(i, renderer);
            
            // how the axis should look
            NumberAxis n = new NumberAxis(showAxes ? STATS_NAMES[i] : "");
            switch(i) {
            case 0: n.setRange(0.0, 1.0);          break; // %
            case 1: n.setRange(0, 1000);           break; // 0 to 1000MB/s
            case 2: n.setRange(0, MAX_LATENCY_MS); break; // latency
            }
            if(!showAxes) {
                if(i <= 1) {
                    n.setAxisLinePaint(Color.WHITE);
                    n.setTickLabelsVisible(false);
                    n.setTickMarksVisible(false);
                }
                else
                    n.setVisible(false);
            }
            else {
                n.setStandardTickUnits(i==0 ? NumberAxis.createStandardTickUnits() : NumberAxis.createIntegerTickUnits());
                n.setAxisLinePaint(STATS_COLORS[i]);
                n.setLabelPaint(STATS_COLORS[i]);
                n.setTickLabelPaint(STATS_COLORS[i]);
                n.setLabelFont(FONT_CHART);
                n.setTickLabelFont(FONT_CHART);
            }
            plot.setRangeAxis(i, n);
            plot.setRangeAxisLocation(i, i==0 ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.TOP_OR_RIGHT);
            
            // add the data and associate it with its axis 
            XYSeriesCollection dataset = chartDatas[i];
            dataset.addSeries(STATS_SERIES[i]);
            plot.setDataset(i, dataset);
            plot.mapDatasetToRangeAxis(i, i);
        }
        
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        return chart;
    }
    
    // tweak-able slider drawing parameters
    private static final int SLIDER_MARGIN_X = 60;
    private static final int SLIDER_MARGIN_Y = 30;
    private static final int SLIDER_WIDTH = 50;
    private static final int FONT_SLIDER_LEFT_SIZE = 44;
    private static final int FONT_SLIDER_BTM_SIZE = 32;
    private static final int SLIDER_BORDER_WIDTH = 0;
    private static final String[] STATS_NAMES = new String[]{"power (% of traditional)", "traffic (Mb/s per host)", "latency (ms)"};
    private static final Color[] STATS_COLORS = new Color[]{new Color(255,0,255), new Color(0,0,255), new Color(0,255,255)};
    
    // computed slider drawing parameter constants
    private static final int SLIDER_HEIGHT = RESERVED_HEIGHT_BOTTOM - 2*SLIDER_MARGIN_Y - 50;
    private static final Font FONT_SLIDER_LEFT = new Font("Tahoma", Font.BOLD, FONT_SLIDER_LEFT_SIZE);
    private static final Font FONT_SLIDER_BTM = new Font("Tahoma", Font.BOLD, FONT_SLIDER_BTM_SIZE);
    private static final BasicStroke SLIDER_STROKE = new BasicStroke(SLIDER_BORDER_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    
    /** whether to show the latency slider or not */
    private final boolean showLatency = false;
    
    /** number of sliders currently being shown */
    private int getNumSliders() {
        return showLatency ? 3 : 2;
    }
    
    /** total width of all sliders combined */
    private int getSlidersWidth() {
        return getNumSliders()*(SLIDER_WIDTH+2*SLIDER_MARGIN_X);
    }
    
    /** the image where sliders are drawn */
    private final BufferedImage slidersImg = new BufferedImage(getSlidersWidth(), RESERVED_HEIGHT_BOTTOM, BufferedImage.TYPE_INT_RGB);
    
    private void initSlidersPanel() {
        Dimension sz = new Dimension(getSlidersWidth(), RESERVED_HEIGHT_BOTTOM);
        pnlSliders.setMinimumSize(sz);
        pnlSliders.setSize(getSlidersWidth(), RESERVED_HEIGHT_BOTTOM);
        pnlSliders.setMaximumSize(sz);
        pnlSliders.setDoubleBuffered(true);
        
        Graphics2D gfx = (Graphics2D)slidersImg.getGraphics();
        gfx.setBackground(Color.WHITE);

        // make sure the gfx renders in high quality
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gfx.setFont(Constants.FONT_DEFAULT);
        gfx.setComposite(Constants.COMPOSITE_OPAQUE);
        
        refreshStatsGraphics();
    }
    
    /**
     * Draws a slider on the sliders panel.
     * 
     * @param sliderNum  which position to draw this slider in
     * @param p          how "full" to draw the slider (0% to 100%, i.e., [0.0, 1.0])
     * @param name       name of the slider
     * @param value      value label for the slider
     */
    private void drawSlider(int sliderNum, double p, String name, String value) {
        Graphics2D gfx = (Graphics2D)slidersImg.getGraphics();
        int x = SLIDER_MARGIN_X + sliderNum*(SLIDER_WIDTH+2*SLIDER_MARGIN_X);
        int yNudge = 10;
        
        // set up the drawing context for this slider
        gfx.setStroke(SLIDER_STROKE);
        gfx.setColor(STATS_COLORS[sliderNum]);
        
        // draw the border
        if(SLIDER_BORDER_WIDTH > 0)
            gfx.drawRect(x, SLIDER_MARGIN_Y+yNudge, SLIDER_WIDTH, SLIDER_HEIGHT);
        
        // draw the title label
        gfx.setFont(FONT_SLIDER_LEFT);
        int tx = x - FONT_SLIDER_LEFT_SIZE / 2;
        int ty = SLIDER_MARGIN_Y + SLIDER_HEIGHT / 2+yNudge;
        AffineTransform t = gfx.getTransform();
        gfx.setTransform(AffineTransform.getRotateInstance(-Math.PI/2, tx, ty));
        StringDrawer.drawCenteredString(name, gfx, tx, ty);
        gfx.setTransform(t);
        
        // draw the value label
        gfx.setFont(FONT_SLIDER_BTM);
        StringDrawer.drawCenteredString(value, gfx, x+SLIDER_WIDTH/2, SLIDER_MARGIN_Y + SLIDER_HEIGHT + FONT_SLIDER_BTM_SIZE+yNudge);
        
        // draw the gradient
        p = (p < 0) ? 0 : ((p > 1) ? 1.0 : p);
        int gx1 = x + SLIDER_BORDER_WIDTH / 2 + 1;
        int gy = SLIDER_MARGIN_Y + SLIDER_HEIGHT - SLIDER_BORDER_WIDTH + 1+yNudge;
        int gx2 = gx1 + SLIDER_WIDTH - SLIDER_BORDER_WIDTH;
        int sh = SLIDER_HEIGHT - 2 * SLIDER_BORDER_WIDTH + 1;
        double usageColorsStep = Link.USAGE_COLORS.length / (double)sh;
        if(usageColorsStep <= 0) usageColorsStep = 1;
        double pPerOffset = 1.0 / sh;
        double i = 0;
        for(int yOffset=0; i<Link.USAGE_COLORS.length && yOffset*pPerOffset<p && yOffset<SLIDER_HEIGHT; i+=usageColorsStep, yOffset+=1) {
            gfx.setColor(Link.USAGE_COLORS[(int)i]);
            gfx.drawLine(gx1, gy - yOffset, gx2, gy - yOffset); 
        }
        
        // restore the defaults
        gfx.setStroke(Constants.STROKE_DEFAULT);
        gfx.setPaint(Constants.PAINT_DEFAULT);
        gfx.setFont(Constants.FONT_DEFAULT);
    }
    
    /** pointer to the network mode which was most recently selected */
    private Object netModeLastSelected = null;
    
    /** pointer to the algorithm mode which was most recently selected */
    private Object algModeLastSelected = null;
    
    /** called when the network mode is being changed */
    private void handleNetModeTypeChange() {
        if(optNetModeHW.isSelected() && fatTreeLayout.getK() != HW_FAT_TREE_K)
            fatTreeLayout.setK(HW_FAT_TREE_K);
        notifyTrafficMatrixChangeListeners();
    }

    /** called when the algorithm mode is being changed */
    private void handleAlgModeTypeChange() {
        notifyTrafficMatrixChangeListeners();
    }
    
    /** layout and initialize the separate, detatched control panel and its components */
    private void initDetachedPanel() {
        GroupLayout layout = initPanel(pnlCustomDetached, "");
        pnlCustomDetached.setBorder(null);
        
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup()
                        .addComponent(pnlTraffic))
                    .addGroup(layout.createParallelGroup()
                        .addComponent(pnlAnim)
                        .addComponent(lblTrafficMatrixCurrent)
                        .addComponent(lblTrafficMatrixNext)
                        .addComponent(lblResultInfo))
        );
        
        layout.setVerticalGroup(
                layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlTraffic))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlAnim)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, 0)
                        .addComponent(lblTrafficMatrixCurrent)
                        .addComponent(lblTrafficMatrixNext)
                        .addComponent(lblResultInfo)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, 0))
        );
        
        lblResultInfo.setVisible(false);        
        lblResultInfo.setForeground(Color.RED);
        lblResultInfo.setFont(new Font("Tahoma", Font.BOLD, 16));
        
        layout.linkSize(SwingConstants.VERTICAL, lblTrafficMatrixCurrent, lblTrafficMatrixNext, lblResultInfo);
        
        initTrafficPanel();
        initAnimPanel();
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
                int w = controlWindow.getWidth();
                int h = controlWindow.getHeight();
                
                // relayout the custom part of the GUI 
                pnlCustomDetached.setBounds(0, 0, w, h);
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
        controlWindow.setSize(748, 328);
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

    // power, throughput, and latency statistics
    private int powerCurrent, powerTraditional;
    private int xputExpected, xputAchieved;
    private int latencyEdge, latencyAgg, latencyCore;
    private double latencyAvg;
    
    public void setPowerData(int cur, int traditional, int max) {
        powerCurrent = cur;
        powerTraditional = traditional;
    }
    
    private void refreshPowerSlider() {
        double p = powerCurrent / (double)powerTraditional;
        drawSlider(0, p, "power", (int)(p*100) + "%");
    }
    
    public void setExpectedAggregateThroughput(double total_bps) {
        int expected_mbps = (int)(total_bps / (1000 * 1000));
        xputExpected = expected_mbps;
    }

    public void setAchievedAggregateThroughput(int bandwidth_achieved_mbps) {
        xputAchieved = bandwidth_achieved_mbps;
    }
    
    private void refreshXputSlider() {
        int demand_bps = slDemand.getValue();
        int demand_mbps = demand_bps / (1000*1000);
        double p = demand_mbps / 1000.0;
        String value = (demand_mbps!=1000) ? (demand_mbps + "Mb/s") : "1Gb/s";
        drawSlider(1, p, "traffic", value);
    }

    public void setLatencyData(int latency_ms_edge, int latency_ms_agg, int latency_ms_core) {
        latencyEdge = latency_ms_edge;
        latencyAgg = latency_ms_agg;
        latencyCore = latency_ms_core;
        latencyAvg = (latencyEdge + latencyAgg + latencyCore) / 3.0;
    }
    
    private void refreshLatencySlider() {
        double p = latencyAvg / MAX_LATENCY_MS;
        drawSlider(2, p, "latency", new org.openflow.util.string.PrintfFormat("%.0f").sprintf(latencyAvg) + "ms");
    }
    
    /**
     * Redraws the sliders and updates the chart to match the latest data.
     */
    private void refreshStatsGraphics() {
        final boolean TEMP_FAKE = true;
        if(TEMP_FAKE)
            latencyAvg = 15;
        
        synchronized(slidersImg) { // prevent tearing
            Graphics2D gfx = (Graphics2D)slidersImg.getGraphics();
            gfx.clearRect(0, 0, getSlidersWidth(), RESERVED_HEIGHT_BOTTOM);
            
            refreshPowerSlider();
            refreshXputSlider();
            if(showLatency)
                refreshLatencySlider();
        }
        
        // update the chart
        int x = datapointOn++;
        int xput = slDemand.getValue() / (1000*1000);
        chartDataXput.add(x, xput);
        chartDataPower.add(x, powerCurrent / (double)powerTraditional);
        if(showLatency)
            chartDataLatency.add(x, latencyAvg);
        
        // remove old data
        if(x >= MAX_VIS_DATA_POINTS) {
            chartDataXput.remove(0);
            chartDataPower.remove(0);
            if(showLatency)
                chartDataLatency.remove(0);
        }
        
        // adjust the x-axis to view only the most recent data points
        XYPlot plot = (XYPlot)chart.getPlot();
        ValueAxis domain = plot.getDomainAxis();
        domain.setRange(x-MAX_VIS_DATA_POINTS, x);
    }

    public void noteResult(int num_unplaced_flows) {
        if(num_unplaced_flows == 0)
            lblResultInfo.setVisible(false);
        else {
            lblResultInfo.setText("FAILED to place " + num_unplaced_flows + " flows!");
            lblResultInfo.setVisible(true);
        }
        
        refreshStatsGraphics();
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
        return new ETTrafficMatrix(optNetModeHW.isSelected(), chkSplit.isSelected(), fatTreeLayout.getK(), demand, edge, agg, slPLen.getValue());
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
