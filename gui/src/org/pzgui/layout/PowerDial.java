package org.pzgui.layout;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Point;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.dial.*;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.StandardGradientPaintTransformer;

public class PowerDial extends ChartPanel {
    DefaultValueDataset data_watts_current;
    DefaultValueDataset data_watts_traditional;
    private int max = -1;

    /** Creates a power dial panel. */
    public PowerDial() {
        this(10000);
    }
    
    /** Creates a power dial panel whose chart has the specified upper bound. */
    public PowerDial(int max) {
        super(null);
        data_watts_traditional = new DefaultValueDataset(0);
        data_watts_current = new DefaultValueDataset(0);
        setData(0, 0, max);
    }
        
    /**
     * Creates a chart displaying a circular dial.
     *
     * @param chartTitle  the chart title.
     * @param dialLabel  the dial label.
     * @param lowerBound  the lower bound.
     * @param upperBound  the upper bound.
     * @param increment  the major tick increment.
     * @param minorTickCount  the minor tick count.
     *
     * @return A chart that displays a value as a dial.
     */
    private JFreeChart createStandardDialChart(String chartTitle,
            String dialLabel, 
            double lowerBound, double upperBound,
            double increment, int minorTickCount) {
        DialPlot plot = new DialPlot();
        plot.setDataset(0, data_watts_traditional);
        plot.setDataset(1, data_watts_current);
        plot.setDialFrame(new StandardDialFrame());

        plot.setBackground(new DialBackground());
        DialTextAnnotation annotation1 = new DialTextAnnotation(dialLabel);
        annotation1.setFont(new Font("Dialog", Font.BOLD, 14));
        annotation1.setRadius(0.7);
        plot.addLayer(annotation1);

        DialValueIndicator dvi = new DialValueIndicator(0);
        plot.addLayer(dvi);

        StandardDialScale scale = new StandardDialScale(lowerBound, upperBound, -120, -300, 1000, 250);
        scale.setMajorTickIncrement(increment);
        scale.setMinorTickCount(minorTickCount);
        scale.setTickRadius(1.0);
        scale.setTickLabelOffset(0.2);
        scale.setTickLabelFont(new Font("Dialog", Font.PLAIN, 14));
        plot.addScale(0, scale);
        plot.addScale(1, scale);

        plot.addPointer(new DialPointer.Pointer());
        
        DialPointer needle2 = new DialPointer.Pin(1);
        needle2.setRadius(0.55);
        plot.addPointer(needle2);
        
        DialCap cap = new DialCap();
        plot.setCap(cap);

        return new JFreeChart(chartTitle, plot);
    }

    public void setData(int cur, int traditional, int max) {
        if(max != this.max)
            createChart(max);
        this.data_watts_current.setValue(cur);
        this.data_watts_traditional.setValue(traditional);
    }
    
    private void createChart(int max) {
        this.max = max;
        createDialChart(max);
    }
     
    private void createDialChart(int max) {
        JFreeChart chart = createStandardDialChart(
                "Power Consumption",
                "Watts",
                0, max, 
                1000, 250);
        
        setChart(chart);
        DialPlot plot = (DialPlot) chart.getPlot();
        
        addRange(plot, 0, max/3, Color.GREEN);
        addRange(plot, max/3, max*2/3, Color.ORANGE);
        addRange(plot, max*2/3, max, Color.RED);
        
        GradientPaint gp = new GradientPaint(
                new Point(),
                new Color(255, 255, 255), 
                new Point(),
                new Color(220, 220, 220));
        DialBackground db = new DialBackground(gp);
        db.setGradientPaintTransformer(new StandardGradientPaintTransformer(GradientPaintTransformType.VERTICAL));
        plot.setBackground(db);
    }
    
    private static void addRange(DialPlot plot, int min, int max, Color c) {
        StandardDialRange range = new StandardDialRange(min, max, c);
        range.setInnerRadius(0.52);
        range.setOuterRadius(0.55);
        plot.addLayer(range);
    }
}
