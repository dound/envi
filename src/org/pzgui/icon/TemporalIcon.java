package org.pzgui.icon;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * A graphic which displays only for a limited time before fading out.
 * 
 * @author David Underhill
 */
public class TemporalIcon extends Icon {
    private final Icon icon;
    private final Dimension size;
    private final long endTime_millis;
    private final long duration_millis;
    
    public TemporalIcon(Icon icon, long duration_millis) {
        this(icon, duration_millis, 1.0f);
    }
    
    public TemporalIcon(Icon icon, long duration_millis, float scale) {
        this.icon = icon;
        this.endTime_millis = System.currentTimeMillis() + duration_millis;
        this.duration_millis = duration_millis;
        if( scale == 1.0f )
            this.size = icon.getSize();
        else
            this.size = new Dimension((int)(icon.getSize().width*scale), (int)(icon.getSize().height*scale));
        
        super.setSize(size);
    }
    
    public void clearCache() {
        icon.clearCache();
    }
    
    /**
     * Draws the icon associated with this object based on whether it has expired yet or not.
     */
    public void draw( Graphics2D gfx, int x, int y, int w, int h ) {
        long diff = endTime_millis - System.currentTimeMillis();
        if( diff > 0 ) {
            float per = diff / (float)duration_millis;
            java.awt.Composite originalComposite = gfx.getComposite();
            gfx.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, per ) );
            icon.draw(gfx, x,  y, w, h);
            gfx.setComposite(originalComposite);
        }
    }
    
    public Dimension getSize() {
        return size;
    }
    
    public boolean isExpired() {
        return endTime_millis < System.currentTimeMillis();
    }
}
