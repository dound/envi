package org.pzgui.icon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LayoutManager2;
import javax.swing.JPanel;

/**
 * A icon which is composed of other icons.
 * 
 * @author David Underhill
 */
public class CompositeIcon extends Icon {
    private JPanel layout;
    
    public CompositeIcon(int w, int h, LayoutManager2 layoutManager) {
        layout = new JPanel();
        layout.setOpaque(false);
        layout.setSize(w, h);
        layout.setLayout(layoutManager);
        super.setSize(w, h);
    }
    
    /** Adds a component to this composite icon.
     * 
     * @param icon   an icon to layout with this composite icon
     * @param where  the layout tip
     */
    public void add(Icon icon, String where) {
        layout.add(icon, where);
    }
    
    /** TextIcon has no cache, so this method is a no-op. */
    public void clearCache() {
        /* no-op */
    }
    
    public void draw( Graphics2D gfx, int x, int y ) {
        layout.setBounds(x, y, layout.getWidth(), layout.getHeight());
        layout.paint(gfx);
    }
    
    public void draw( Graphics2D gfx, int x, int y, int w, int h ) {
        // figure out the scale of the base container to the requested size
        float rh = (h / (float)layout.getHeight());
        float rw = (w / (float)layout.getWidth());
        
        // tell the icons what size they should be
        for( Component c : layout.getComponents() ) {
            Dimension sz = c.getSize();
            c.setSize((int)(sz.width*rw), (int)(sz.height*rh));
        }
        
        layout.doLayout();
        
        for( Component c : layout.getComponents() ) {
            // determine size we asked components to be, even if the layout manager ignored our size requests
            Dimension sz = c.getSize();
            int cw = (int)(sz.width*rw);
            int ch = (int)(sz.height*rh);
            
            // center icon in the space given by the layout manager
            int cx = x + c.getX() + (c.getWidth() - cw) / 2;
            int cy = y + c.getY() + (c.getHeight() - ch) / 2;
            
            // take into account centering efforts by Icon draw methods
            cx += cw / 2;
            cy += cy / 2;
            
            ((Icon)c).draw(gfx, cx, cy, cw, ch);
        }
    }
    
    public Dimension getSize() {
        return layout.getSize();
    }
}
