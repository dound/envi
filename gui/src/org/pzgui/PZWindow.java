package org.pzgui;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.openflow.lavi.drawables.Link;
import org.openflow.lavi.drawables.OpenFlowSwitch;
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
        setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage("images/dgu.gif"));
        
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

        lblCanvas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                manager.setMousePos(getMX(evt), getMY(evt), false);
                manager.select(getMX(evt), getMY(evt));
            }
            
            public void mouseReleased(MouseEvent evt) {
                synchronized(manager) {
                    if(evt.isControlDown()) {
                        Drawable d = manager.getSelected();
                        if(d instanceof OpenFlowSwitch) {
                            OpenFlowSwitch o = (OpenFlowSwitch)d;
                            o.setFailed(!o.isFailed());
                            manager.fireDrawableEvent(d, "failure");
                        }
                        else if(d instanceof Link) {
                            Link l = (Link)d;
                            l.setFailed(!l.isFailed());
                            manager.fireDrawableEvent(d, "failure");
                        }
                    }
                    
                    manager.noteMouseUp();

                    // apply new panning, if any
                    drawOffset.add(drawOffsetExtra);
                    drawOffsetExtra.set(0, 0);
                }
                
                manager.deselect();
            }
        });

        // handle mouse movement (can drag switches)
        lblCanvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(MouseEvent evt) {
                synchronized(manager) {
                    manager.setMousePos(getMX(evt), getMY(evt), false);
                }
            }
            
            public void mouseDragged(MouseEvent evt) {
                synchronized(manager) {
                    manager.setMousePos(getMX(evt), getMY(evt), false);

                    Drawable selNode = manager.getSelected();
                    if(selNode == null) {
                        if(evt.getButton() == MouseEvent.BUTTON1 && evt.isAltDown()) {
                            // alt+left-click+drag = pan the screen
                            drawOffsetExtra.x = getMX(evt) - manager.getMouseStartPos().x;
                            drawOffsetExtra.y = getMY(evt) - manager.getMouseStartPos().y;
                            return;
                        }
                    }
                    else {
                        // if a node is selected, handle dragging it
                        selNode.drag(getMX(evt), getMY(evt));
                    }
                }
            }
        });

        // mouse wheel movement changes the zoom
        lblCanvas.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                synchronized(manager) {
                    int rotations = e.getWheelRotation();
                    
                    // zoom out when the mouse wheel rotates down
                    while(rotations > 0) {
                        zoomOut();
                        rotations -= 1;
                    }

                    // zoom in when the mouse wheel rotates up
                    while(rotations < 0) {
                        zoomIn();
                        rotations += 1;
                    }
                }
            }
        });

        lblCanvas.addKeyListener(new java.awt.event.KeyAdapter() {
            // pan and zoom with the arrow keys
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_LEFT) {
                    if(e.isAltDown())
                        zoomOut();
                    else
                        panLeft();
                }
                else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    if(e.isAltDown())
                        zoomIn();
                    else
                        panRight();
                }
                else if(e.getKeyCode() == KeyEvent.VK_UP) {
                    if(e.isAltDown())
                        zoomIn();
                    else
                        panUp();
                }
                else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if(e.isAltDown())
                        zoomOut();
                    else
                        panDown();
                }
            }

            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_V) {
                    resetView();
                    manager.displayIcon("<V> Reset View!", 2000);
                }
                else if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.out.println("Goodbye");
                    System.exit(0);
                }
                else if(e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    screenshot();
                }
                else if(e.getKeyCode() == KeyEvent.VK_H || e.getKeyChar()=='?') {
                    System.out.println(baseTitle + "\n" +
                                       "    right-click + drag = move a switch\n" +
                                       "    arrow keys         = pan the view\n" +
                                       "    alt + arrow keys   = zoom in/out\n" +
                                       "    mouse wheel        = zoom in/out\n" +
                                       "    page up            = take a screenshot\n" +
                                       "    escape             = exit\n" +
                                       "    v                  = reset the view to the default\n");
                }
            }
        });
    }
    

    // ------- Graphics ------- //
    // ************************ //

    /** string to prefix to the title in the window bar */
    private String baseTitle = "Stanford University - Elastic Tree";
    
    /** canvas to draw the scene on */
    private final javax.swing.JLabel lblCanvas = new javax.swing.JLabel();
    
    /** the image where the scene will be drawn */
    private BufferedImage img;

    /** a lock to prevent img from being changed in the middle of a redraw */
    private final Object imgLock = new Object();

    /** if non-null, a screenshot will be saved to the specified filename */
    private String saveScreenshotName = null;
    
    /** Returns the canvas on which the scene will be drawn for this window */
    public javax.swing.JLabel getCanvas() {
        return lblCanvas;
    }

    public Graphics2D getDisplayGfx() {
        return (Graphics2D)img.getGraphics();
    }

    public void redraw() {
        // update the title of the GUI
        int id = manager.getWindowIndex(this);
        if(id != 0)
            setTitle(baseTitle + " (view " + (id+1) + ")");
        else
            setTitle(baseTitle);

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

            // make sure the gfx renders in high quality
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
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

    /** get the y position of the mouse relative to the scene's origin (e.g. account for pan and zoom) */
    private int getMX(MouseEvent evt) {
        int tx = evt.getX() - drawOffset.x;
        return (int)(tx / getZoom());
    }

    /** get the y position of the mouse relative to the scene's origin (e.g. account for pan and zoom) */
    private int getMY(MouseEvent evt) {
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
        zoom = 1.0f; // ET hack
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


    /** print out a string representation of the window's settings */
    public String toString() {
        return "pos=(" + getX() + ", " + getY() + ")  " +
               "sz=(" + getWidth() + ", " + getHeight() + ")  " +
               "pan=(" + drawOffset.x + ", " + drawOffset.y + ")  " +
               "zm=" + zoom;
    }
}
