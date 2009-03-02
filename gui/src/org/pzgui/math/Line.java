package org.pzgui.math;

/**
 * A simple line class.
 * @author David Underhill
 */
public class Line {
    public final float x1, x2, y1, y2;
    public final float m, b;
    public final boolean isVertical;

    public Line(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;

        m = (y2 - y1) / (x2 - x1);
        b = y1 - m * x1;
        isVertical = Double.isInfinite(m);
    }
}