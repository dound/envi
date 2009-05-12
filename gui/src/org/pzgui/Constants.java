package org.pzgui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Paint;

import org.openflow.gui.Options;

/**
 * Graphics constants.
 * @author David Underhill
 */
public abstract class Constants {
    public static final Color cmap(Color c) {
        if(!Options.USE_LIGHT_COLOR_SCHEME)
            return new Color(255-c.getRed(), 255-c.getGreen(), 255-c.getBlue(), c.getAlpha());
        else
            return c;
    }
    
    public static final BasicStroke STROKE_DEFAULT    = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static final BasicStroke STROKE_THICK      = new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static final Color       COLOR_DEFAULT     = cmap(Color.BLACK);
    public static final Color       BG_DEFAULT        = cmap(Color.WHITE);
    public static final Color       COLOR_SELECTED    = cmap(new Color(128, 255, 128));
    public static final Color       COLOR_HOVERING    = cmap(new Color(255, 255, 128));
    public static final Paint       PAINT_DEFAULT     = COLOR_DEFAULT;
    public static final Font        FONT_DEFAULT      = new Font("Tahoma", Font.BOLD, 14);
    public static final Font        FONT_TI           = new Font("Tahoma", Font.BOLD, 44);
    public static final Color       FONT_TI_FILL      = cmap(Color.GRAY);
    public static final Color       FONT_TI_OUTLINE   = cmap(Color.BLACK);
    public static final Composite   COMPOSITE_OPAQUE  = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    public static final Composite   COMPOSITE_HALF    = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    public static final Composite   COMPOSITE_QUARTER = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
}
