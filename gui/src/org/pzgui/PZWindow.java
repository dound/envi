package org.pzgui;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.pzgui.math.Vector2i;

/**
 * A single pan-zoom GUI window.
 * 
 * @author  David Underhill
 */
public class PZWindow extends javax.swing.JFrame implements ComponentListener {
    /** the manager which is managing this window */
    protected final PZManager manager;

    /** Creates new form display window */
    public PZWindow(final PZManager manager, int screenX, int screenY, int width, int height, int drawX, int drawY) {
        this.manager = manager;
        java.net.URL url = ClassLoader.getSystemClassLoader().getResource("images/dgu.gif");
        if(url != null)
            setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url));
        
        setBounds(screenX, screenY, width, height);
        setMySize(width, height, 1.0f);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // setup the canvas for the window to draw
        final Container cp = getContentPane();
        cp.setLayout(null);
        cp.add(lblCanvas);
        lblCanvas.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        lblCanvas.setDoubleBuffered(true);
        lblCanvas.setFocusable(true);

        initEventListeners();
        drawOffset.set(drawX, drawY);
    }
   
    /** Gets the manager of this window */
    public PZManager getManager() {
        return manager;
    }
    
    /**
     * Registers the listener for component, key, mouse, and window events.
     */
    public void addEventListener(PZWindowEventListener l) {
        this.addComponentListener(l);
        lblCanvas.addKeyListener(l);
        lblCanvas.addMouseListener(l);
        lblCanvas.addMouseWheelListener(l);
        lblCanvas.addMouseMotionListener(l);
        this.addWindowListener(l);
    }

    /**
     * Registers the listener for component, key, mouse, and window events.
     */
    public void removeEventListener(PZWindowEventListener l) {
        this.removeComponentListener(l);
        lblCanvas.removeKeyListener(l);
        lblCanvas.removeMouseListener(l);
        lblCanvas.removeMouseWheelListener(l);
        lblCanvas.removeMouseMotionListener(l);
        this.removeWindowListener(l);
    }
    
    /** Initializes the GUI components. */
    private void initEventListeners() {
        final PZWindow me = this;

        // handle resize and move events itself
        addComponentListener(this);
        
        // notify the manager when the window is closed
        addWindowListener(new WindowAdapter() {
           public void windowClosed(WindowEvent evt) {
               manager.closeWindow(me);
           }
        });
    }
    

    // ------- Graphics ------- //
    // ************************ //

    /** string to prefix to the title in the window bar */
    public static String BASE_TITLE = "OpenFlow GUI";
    
    /** custom string to use for the title (if null, BASE_TITLE will be used) */ 
    public String customTitle = null;
    
    /** JLabel which contains a reference to its parent */
    class JLabelWithPZWindowParent extends JLabel {
        public final PZWindow window;
        public JLabelWithPZWindowParent(PZWindow w) {
            super();
            this.window = w;
        }
    }
    
    /** canvas to draw the scene on */
    private final JLabelWithPZWindowParent lblCanvas = new JLabelWithPZWindowParent(this);
    
    /** the image where the scene will be drawn */
    private BufferedImage img;

    /** a lock to prevent img from being changed in the middle of a redraw */
    private final Object imgLock = new Object();

    /** if non-null, a screenshot will be saved to the specified filename */
    private String saveScreenshotName = null;
    
    /** Gets the title of this window bar */
    public String getTitle() {
        if(customTitle == null)
            return BASE_TITLE;
        else
            return customTitle;
    }
    
    /** Gets the custom title of this window bar */
    public String getCustomTitle() {
        return customTitle;
    }
    
    /** Sets the custom title of this window bar */
    public void setCustomTitle(String title) {
        customTitle = title;
        if(customTitle != null)
            setTitle(customTitle);
    }
    
    /** Returns the canvas on which the scene will be drawn for this window */
    public javax.swing.JLabel getCanvas() {
        return lblCanvas;
    }

    public Graphics2D getDisplayGfx() {
        Graphics2D gfx = (Graphics2D)img.getGraphics();
        
        // make sure the gfx renders in high quality
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        return gfx;
    }

    public void redraw() {
        // update the title of the GUI
        if(getCustomTitle() == null) {
            int id = manager.getWindowIndex(this);
            if(id != 0)
                setTitle(getTitle() + " (view " + (id+1) + ")");
            else
                setTitle(getTitle());
        }
        
        stepPanZoomAnimation();

        synchronized(imgLock) {
            // redraw the scene
            manager.preRedraw(this);
            manager.redraw(this);

            // copy the image buffer into the JLabel on the JFrame
            Graphics cg = lblCanvas.getGraphics();
            if(cg != null)
                cg.drawImage(img, 0, 0, null);

            // save a screenshot if one was requested
            if(saveScreenshotName != null) {
                try {
                    ImageIO.write(img, "png", new java.io.File(saveScreenshotName));
                }
                catch(java.io.IOException e) {
                    DialogHelper.displayError("Screenshot could not be saved: " + e);
                }
                saveScreenshotName = null;
            }
        }
    }

    /** tells the GUI to save a screenshot when it finishes the next redraw */
    public void screenshot() {
        synchronized(imgLock) {
            saveScreenshotName = "gui-" + System.currentTimeMillis() + ".png";
        }
    }


    // ------- Window Position and Size ------- //
    // **************************************** //

    /** how much of the right side of the window is reserved for other content */
    private int reservedWidthRight = 0;
    
    /** how much of the bottom side of the window is reserved for other content */
    private int reservedHeightBottom = 0;
    
    /** gets how much of the right side of the window is reserved for other content */
    public int getReservedWidthRight() {
        return reservedWidthRight;
    }

    /** sets how much of the right side of the window is reserved for other content */
    public void setReservedWidthRight(int reservedWidthRight) {
        this.reservedWidthRight = reservedWidthRight;
    }
    
    /** gets how much of the bottom side of the window is reserved for other content */
    public int getReservedHeightBottom() {
        return reservedHeightBottom;
    }
    
    /** sets how much of the bottom side of the window is reserved for other content */
    public void setReservedHeightBottom(int reservedHeightBottom) {
        this.reservedHeightBottom = reservedHeightBottom;
    }
    
    /** 
     * How this window is docked to the previous window; 0=not docked, else see
     * SwingConstants (TOP, BOTTOM, LEFT, or RIGHT)
     */
    private DockLocation dockLocation = DockLocation.NOT_DOCKED;
    
    /** set the x-coordinate of this window */
    public void setX(int x) {
        setBounds(x, getY(), getWidth(), getHeight());
    }

    /** set the y-coordinate of this window */
    public void setY(int y) {
        setBounds(getX(), y, getWidth(), getHeight());
    }

    /** set the x and y coordinates of this window */
    public void setPos(Vector2i pos) {
        setX(pos.x);
        setY(pos.y);
    }

    /** get the location where the window is docked, if any */
    public DockLocation getDockLocation() {
        return dockLocation;
    }

    /** set how the window is docked */
    public void setDockLocation(DockLocation dockLocation) {
        this.dockLocation = dockLocation;
    }

    /** set the width of this window */
    public void setWidth(int w) {
        setBounds(getX(), getY(), w, getHeight());
    }

    /** set the height of this window */
    public void setHeight(int h) {
        setBounds(getX(), getY(), getWidth(), h);
    }

    /** set the height and width of this window */
    public void setMySize(int w, int h) {
        int oldW = img.getWidth();
        int oldH = img.getHeight();
        
        w -= reservedWidthRight;
        h -= reservedHeightBottom;
        if(w!=oldW || h!=oldH) {
            float oldMinZoomFactor = Math.min(oldW, oldH);
            float newMinZoomFactor = Math.min(w, h);
            setMySize(w+reservedWidthRight, h+reservedHeightBottom, zoom * newMinZoomFactor / oldMinZoomFactor);
        }
    }

    /** set the height, width, and zoom of this window */
    public void setMySize(int w, int h, float zoom) {
        this.zoom = zoom;
        this.zoom = 1.0f; // ET hack
        this.setBounds(getX(), getY(), w, h);
        w -= reservedWidthRight;
        h -= reservedHeightBottom;
        lblCanvas.setBounds(0, 0, w, h);
        
        synchronized(imgLock) {
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            
            Graphics2D gfx = (Graphics2D)img.getGraphics();
            gfx.setBackground(Color.WHITE);
            gfx.setFont(Constants.FONT_DEFAULT);
            gfx.setComposite(Constants.COMPOSITE_OPAQUE);
        }
    }

    public void componentResized(ComponentEvent e) {
        // handle the window being resized
        setMySize(getWidth(), getHeight());
        manager.windowResized(this);
    }

    public void componentMoved(ComponentEvent e)  {
        // handle the window moving
        setMySize(getWidth(), getHeight());
    }

    public void componentShown(ComponentEvent e)  { /* ignore */ }
    public void componentHidden(ComponentEvent e) { /* ignore */ }

    
    // ------- Pan and Zoom ------- //
    // **************************** //
    
    /** default factor to change zoom by */
    private static final float DEFAULT_ZOOM_PERCENT_CHANGE = 0.1f;

    /** default factor change pan by */
    private static final float DEFAULT_PAN_PERCENT = 0.01f;
    private static final float DEFAULT_PAN_DIVISOR = 1.0f / DEFAULT_PAN_PERCENT;

    /** current pan (not including any additional pan in progress) */
    private Vector2i drawOffset = new Vector2i(0, 0);

    /** additional pan (in progress of being set) */
    private Vector2i drawOffsetExtra = new Vector2i(0, 0);

    /** current zoom */
    private float zoom = 1.0f;

    /** get the aggregate x-axis pan */
    public int getDrawOffsetX() {
        return drawOffset.x + drawOffsetExtra.x;
    }

    /** get the aggregate y-axis pan */
    public int getDrawOffsetY() {
        return drawOffset.y + drawOffsetExtra.y;
    }

    /** get the current x-axis pan */
    public int getPanX() {
        return drawOffset.x;
    }

    /** set the aggregate x-axis pan */
    public void setPanX(int x) {
        drawOffset.x = x;
    }

    /** get the current y-axis pan */
    public int getPanY() {
        return drawOffset.y;
    }

    /** set the aggregate y-axis pan */
    public void setPanY(int y) {
        drawOffset.y = y;
    }

    /** pan left by the standard amount */
    public void panLeft() {
        drawOffset.x -= getWidth() / (DEFAULT_PAN_DIVISOR * zoom);
    }

    /** pan right by the standard amount */
    public void panRight() {
        drawOffset.x += getWidth() / (DEFAULT_PAN_DIVISOR * zoom);
    }

    /** pan down by the standard amount */
    public void panDown() {
        drawOffset.y += getHeight() / (DEFAULT_PAN_DIVISOR * zoom);
    }

    /** pan up by the standard amount */
    public void panUp() {
        drawOffset.y -= getHeight() / (DEFAULT_PAN_DIVISOR * zoom);
    }
    
    /** apply the pan in progress */
    public void applyPanInProgress() {
        drawOffset.add(drawOffsetExtra);
        drawOffsetExtra.set(0, 0);
    }
    
    /** set the pan in progress to the specified amount */
    public void setPanInProgress(int x, int y) {
        drawOffsetExtra.set(x, y);
    }

    /** get the y position of the mouse relative to the scene's origin (e.g. account for pan and zoom) */
    public int getMX(MouseEvent evt) {
        int tx = evt.getX() - drawOffset.x;
        return (int)(tx / getZoom());
    }

    /** get the y position of the mouse relative to the scene's origin (e.g. account for pan and zoom) */
    public int getMY(MouseEvent evt) {
        int ty = evt.getY() - drawOffset.y;
        return (int)(ty / getZoom());
    }

    /** get the current zoom */
    public float getZoom() {
        return zoom;
    }

    /** set the current zoom */
    public void setZoom(float z) {
        zoom = z;
    }

    /** zoom in by the standard amount */
    public void zoomIn() {
        setZoom(zoom * (1.0f + DEFAULT_ZOOM_PERCENT_CHANGE));
    }

    /** zoom out by the standard amount */
    public void zoomOut() {
        setZoom(zoom / (1.0f + DEFAULT_ZOOM_PERCENT_CHANGE));
    }

    /** reset the pan to the origin and zoom to 1.0 */
    public void resetView() {
        drawOffset.set(0, 0);
        setZoom(1.0f);
    }
    
    
    // -- Pan and Zoom Animation -- //
    // **************************** //
    
    /** time at which the current zoom/pan animation finishes */
    private long zoomPanAnimationEndTime = 0, zoomPanAnimationStartTime = 0;
    
    /** where the zoom is coming from and going to */
    private float zoomFrom, zoomTo;
    
    /** where the pan is coming from and going to */
    private Vector2i panFrom, panTo;
    
    /** see the description of startPanZoomAnimation() */
    private float zoomPanAnimationInterpolationPower = 1.0f;
    
    /** 
     * Starts a pan-zoom animation with zoomPanAnimationInterpolationPower ==
     * 1.0f (a linear interpolation between the endpoints).
     */
    public void startPanZoomAnimation(int toX, int toY, float zoomTo, long duration_msec) {
        startPanZoomAnimation(toX, toY, zoomTo, duration_msec, 1.0f);
    }
    
    /**
     * Starts a pan-zoom animation.
     * 
     * @param toX            what x coordinate to zoom to
     * @param toY            what y coordinate to zoom to
     * @param zoomTo         the new zoom factor
     * @param duration_msec  how long to animate
     * @param zoomPanAnimationInterpolationPower  1 means a linear 
     *                       interpolation will be done between the two 
     *                       animation endpoints.  2 would be quadratic, etc.  
     *                       A higher the number causes a faster beginning and
     *                       a slower and smoother the ending.
     */
    public void startPanZoomAnimation(int toX, int toY, float zoomTo, long duration_msec, float zoomPanAnimationInterpolationPower) {
        this.zoomTo = zoomTo;
        this.panTo = new Vector2i(toX, toY);
        this.panFrom = drawOffset.clone();
        this.zoomFrom = zoom;
        this.zoomPanAnimationStartTime = System.currentTimeMillis();
        this.zoomPanAnimationEndTime = System.currentTimeMillis() + duration_msec;
        this.zoomPanAnimationInterpolationPower = zoomPanAnimationInterpolationPower;
    }

    /** stops any ongoing pan-zoom animation in its tracks */
    public void stopPanZoomAnimation() {
        zoomPanAnimationEndTime = 0;
    }
    
    /** applies the next step in the pan-zoom animation, if any */
    private void stepPanZoomAnimation() {
        if(zoomPanAnimationEndTime == 0)
            return;
        
        long now = System.currentTimeMillis();
        if(now >= zoomPanAnimationEndTime) {
            zoom = zoomTo;
            drawOffset.x = panTo.x;
            drawOffset.y = panTo.y;
            stopPanZoomAnimation();
        }
        else {
            long duration = zoomPanAnimationEndTime - zoomPanAnimationStartTime;
            long diff = zoomPanAnimationEndTime - now;
            float alpha = (float)Math.pow(diff / (float)duration, zoomPanAnimationInterpolationPower);
            float beta = 1.0f - alpha; 
            zoom = (zoomFrom * alpha) + (zoomTo * beta);
            drawOffset.x = (int)((panFrom.x * alpha) + (panTo.x * beta));
            drawOffset.y = (int)((panFrom.y * alpha) + (panTo.y * beta));
        }
    }

    /** print out a string representation of the window's settings */
    public String toString() {
        return "pos=(" + getX() + ", " + getY() + ")  " +
               "sz=(" + getWidth() + ", " + getHeight() + ")  " +
               "pan=(" + drawOffset.x + ", " + drawOffset.y + ")  " +
               "zm=" + zoom;
    }
}
