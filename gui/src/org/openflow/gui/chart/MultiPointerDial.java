package org.openflow.gui.chart;

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

/**
 * A dial which may have more than one pointer. 
 * 
 * @author David Underhill
 */
public class MultiPointerDial extends ChartPanel {
    /** chart title */
    private String title;
    
    /** units of the chart */
    private String unitsLabel;
    
    /** maximum value of the dial */
    private int max = -1;
    
    /** increment between major tick marks */
    private int majorStep;
    
    /** pointer values */
    private DefaultValueDataset[] values;
    

    /** 
     * Creates a dial panel.
     * 
     * @param title        title of this dial
     * @param unitsLabel   label for the units of this dial
     * @param pointerCount  number of pointers to put on the dial  
     * @param max          maximum value of the dial
     * @param majorStep    interval between major tick marks
     */
    public MultiPointerDial(String title, String unitsLabel,
                           int pointerCount, int max, int majorStep) {
        super(null);
        
        this.title = title;
        this.unitsLabel = unitsLabel;
        this.majorStep = majorStep;
        
        this.values = new DefaultValueDataset[pointerCount];
        for(int i=0; i<values.length; i++)
            values[i] = new DefaultValueDataset(0);
        
        setMax(max);
    }
    
    /** Gets the maximum value of the dial. */
    public int getMax() {
        return max;
    }
    
    /** 
     * Sets the maximum value of the dial and returns true if it had changed.
     */
    public boolean setMax(int max) {
        if(max != this.max) { 
            createChart(max);
            return true;
        }
        return false;
    }
    
    /**
     * Sets the value of the specified pointer.
     * 
     * @param pointerIndex  index of the pointer whose value is to be set
     * @param value        the new value the pointer should point to
     */
    public void setValue(int pointerIndex, int value) {
        values[pointerIndex].setValue(value);
    }

    /**
     * Sets the specified pointer to appear as a standard pointer.
     */
    public void setPointerStandard(int pointerIndex) {
        setPointer(new DialPointer.Pointer(pointerIndex));
    }
    
    /**
     * Sets the specified pointer to appear as a simple line pointer.
     * 
     * @param pointerIndex  the pointer the set
     * @param radius       the size of the pointer
     */
    public void setPointerLine(int pointerIndex, double radius) {
        DialPointer pointer = new DialPointer.Pin(pointerIndex);
        pointer.setRadius(radius);
        setPointer(pointer);
    }
    
    /** Sets the pointer for the specified dataset */
    public void setPointer(DialPointer p) {
        DialPlot plot = (DialPlot)getChart().getPlot();
        DialPointer pointerOld = plot.getPointerForDataset(p.getDatasetIndex());
        if(pointerOld != null)
            plot.removePointer(pointerOld);
        plot.addPointer(p);
    }
        

    // ---------- Chart Creation ------------ //
    // ************************************** //
    
    private void createChart(int max) {
        this.max = max;
        JFreeChart chart = createStandardDialChart(
                title,
                unitsLabel,
                0, max, 
                majorStep, majorStep / 4);
        
        setChart(chart);
        DialPlot plot = (DialPlot) chart.getPlot();
        
        for(int i=0; i<values.length; i++)
            setPointerStandard(i);
        
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
        for(int i=0; i<values.length; i++)
            plot.setDataset(i, values[i]);
        
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

        DialCap cap = new DialCap();
        plot.setCap(cap);

        return new JFreeChart(chartTitle, plot);
    }
}
