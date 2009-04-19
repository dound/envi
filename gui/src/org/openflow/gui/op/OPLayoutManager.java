package org.openflow.gui.op;

import java.awt.Graphics2D;
import javax.swing.JPanel;

import org.openflow.gui.drawables.OpenFlowSwitch;
import org.pzgui.Drawable;
import org.pzgui.PZWindow;
import org.pzgui.icon.ImageIcon;
import org.pzgui.layout.PZLayoutManager;

/**
 * Example extension to the base GUI manager.
 * 
 * @author David Underhill
 */
public class OPLayoutManager extends PZLayoutManager {
    /** specifies the height in pixels of the area of the screen reserved for custom controls */
    public static final int RESERVED_HEIGHT_BOTTOM = 400;
    
    /** panel where custom controls can be placed */
    private JPanel pnlCustom = new JPanel();
    
    public OPLayoutManager() {
        super();
        // TODO: layout the custom control panel 
    }
    
    /** Adds the drawable as usual and then sets a custom switch graphic for some switch */
    public void addDrawable(Drawable d) {
        super.addDrawable(d);
        // TODO: any custom processing when a Drawable is added
        // example of drawing a particular switch specially
        if(d instanceof OpenFlowSwitch) {
            OpenFlowSwitch s = (OpenFlowSwitch)d;
            if(s.getID() == 0x00ABCD00) {
                s.setIcon(new ImageIcon("dgu.gif", 25, 25));
            }
        }
    }
    
    /** 
     * Removes the drawable as usual and then completely resets the fat tree 
     * layout engine (assumes all switches are being removed).
     */
    public void removeDrawable(Drawable d) {
        super.removeDrawable(d);
        // TODO: any custom processing when a Drawable is removed
    }

    /** Overrides parent to reserve space for custom controls in the new window. */
    public void attachWindow(final PZWindow w) {
        super.attachWindow(w);
        if(getWindowIndex(w) == 0) {
            // set the title
            w.setTitle("Example");
            
            // reserve space for a custom panel
            w.getContentPane().add(pnlCustom);
            w.setReservedHeightBottom(RESERVED_HEIGHT_BOTTOM);
            w.setMySize(w.getWidth(), w.getHeight(), w.getZoom());
        }
    }

    /**
     * Extends the parent implementation to relayout the custom controls section
     * to fit in the new area. 
     */
    public void setLayoutSize(int w, int h) {
        super.setLayoutSize(w, h);
        int margin = 20; 
        pnlCustom.setBounds(0, h + margin, w, RESERVED_HEIGHT_BOTTOM - 2 * margin);
    }
    
    /**
     * Overrides the parent implementation by appending the drawing of the 
     * additional widgets.
     */
    public void preRedraw(PZWindow window) {
        super.preRedraw(window);
        Graphics2D gfx = window.getDisplayGfx();
        if(gfx == null)
            return;
        
        // TODO: draw any desired additional stuff
    }
    
    /** 
     * Extends the parent implementation to do extra processing after a redraw.
     */
    public void postRedraw() {
        super.postRedraw();
        // TODO: any processing you need to do after a redraw
    }
}
