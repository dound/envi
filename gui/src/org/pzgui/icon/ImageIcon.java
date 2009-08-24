package org.pzgui.icon;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.util.HashMap;

/**
 * An icon backed by a rasterized image.
 * 
 * @author David Underhill
 */
public class ImageIcon extends Icon {
    /** Loads an image into memory */
    public static Image loadImage(String fn) {
        try {
            Image image = java.awt.Toolkit.getDefaultToolkit().createImage(fn);
            MediaTracker tracker = new MediaTracker(lblForFM);
            tracker.addImage(image, 0);
            tracker.waitForID(0);
            if (MediaTracker.COMPLETE != tracker.statusID(0, false))
                throw new IllegalStateException("Unable to load image from " + fn);
            else
                return image;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while loading image from " + fn, e);
        }
    }
    
    /** Scales the specified image to the requested size */
    public static final Image getScaledImage(Image img, int w, int h) {
        return img.getScaledInstance(w, h, w>img.getWidth(null) ? Image.SCALE_SMOOTH : Image.SCALE_AREA_AVERAGING);
    }
    
    private Image img;
    private Dimension size;
    private final HashMap<Dimension, Image> resampledImages = new HashMap<Dimension, Image>();
    
    /** Creates an image from the specified image object */
    public ImageIcon(Image img) {
        setImage(img);
    }
    
    /**
     * Creates an image from the specified filename.  The image's height and 
     * width will be scaled based on the ratio of the GUI's current size to 
     * the 1080p resolution.  In other words, the original image is assumed to 
     * be in native resolution for 1080p.
     * 
     * @param fn  path to the image file
     */
    public ImageIcon(String fn) {
        this(fn, 1.0f);
    }
    
    /**
     * Creates an image from the speccified filename with the specified width 
     * and height.  The original image will be resampled if needed.
     * 
     * @param fn      path to the image file
     * @param width   width of this icon
     * @param height  height of this icon
     */
    public ImageIcon(String fn, int width, int height) {
        Image orig = loadImage(fn);
        if( orig.getWidth(null)!=width || orig.getHeight(null)!=height )
            img = getScaledImage(orig, width, height);
        else
            img = orig;
        
        size = new Dimension(width, height);
        super.setSize(size);
    }
    
    /**
     * Creates an image from the speccified filename with its width and height
     * scaled by the specified scale value.
     * 
     * @param fn      path to the image file
     * @param scale   how much to scale this icon from its native size
     */
    public ImageIcon(String fn, float scale) {
        Image orig = loadImage(fn);
        int width = (int)(orig.getWidth(null) * scale);
        int height = (int)(orig.getHeight(null) * scale);
    
        if( orig.getWidth(null)!=width || orig.getHeight(null)!=height )
            img = getScaledImage(orig, width, height);
        else
            img = orig;
        
        size = new Dimension(width, height);
        super.setSize(size);
    }
    
    public void clearCache() {
        resampledImages.clear();
    }
    
    public void draw( Graphics2D gfx, int x, int y ) {
        draw(gfx, img, x, y, size.width, size.height);
    }
    
    public void draw( Graphics2D gfx, int x, int y, int w, int h ) {
        draw(gfx, getImage(new Dimension(w, h)), x, y, w, h);
    }
    
    public static void draw( Graphics2D gfx, Image img, int x, int y, int w, int h  ) {
        gfx.drawImage(img, x-w/2, y-h/2, w, h, null);
    }
    
    public Dimension getSize() {
        return size;
    }
    
    public Image getImage() {
        return img;
    }
    
    public Image getImage(Dimension sz) {
        // if the needed size matches the original, return the original
        if( sz.equals(size) )
            return img;
        
        // otherwise see if we alredy built the image
        Image ret = resampledImages.get(sz);
        if( ret == null ) {
            // create the scaled image and save it for later
            ret = getScaledImage(img, sz.width, sz.height);
            resampledImages.put(sz, ret);
            
        }
        return ret;
    }
    
    /** 
     * Changes the image displayed by this ImageIcon.  The resampled images 
     * cache is flushed.
     */
    public void setImage(Image img) {
        this.img = img;
        this.size = new Dimension(img.getWidth(null), img.getHeight(null));
        resampledImages.clear();
    }
}
