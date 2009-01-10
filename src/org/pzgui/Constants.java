package org.pzgui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Paint;

/**
 * Graphics constants.
 * @author David Underhill
 */
public abstract class Constants {
    public static final BasicStroke STROKE_DEFAULT    = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static final Color       COLOR_DEFAULT     = Color.BLACK;
    public static final Color       COLOR_SELECTED    = new Color(196, 255, 196);
    public static final Color       COLOR_HOVERING    = new Color(255, 255, 196);
    public static final Paint       PAINT_DEFAULT     = COLOR_DEFAULT;
    public static final Font        FONT_DEFAULT      = new Font("Tahoma", Font.BOLD, 14);
    public static final Font        FONT_TI           = new Font("Tahoma", Font.BOLD, 44);
    public static final Color       FONT_TI_FILL      = Color.GRAY;
    public static final Color       FONT_TI_OUTLINE   = Color.BLACK;
    public static final Composite   COMPOSITE_OPAQUE  = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
}
