package org.pzgui;

import org.pzgui.icon.Icon;
import org.pzgui.icon.TemporalIcon;
import org.pzgui.icon.TextIcon;
import org.pzgui.math.Vector2i;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Manages a GUI consisting of multiple windows which each may each view the
 * scene from a different perspective.
 *
 * @author David Underhill
 */
public class PZManager extends Thread {
    // ------- GUI Windows (Scene Displayers) ------- //
    // ********************************************** //

    /** windows which are displaying the scene */
    private final Vector<PZWindow> windows = new Vector<PZWindow>();

    /** closing event listeners */
    private final LinkedList<PZClosing> closingListeners = new LinkedList<PZClosing>();

    /** adds a new display window in a thread-safe way */
    public void addWindow(int screenX, int screenY, int width, int height, int drawOffsetX, int drawOffsetY, float zoom) {
        PZWindow w = new PZWindow(this, screenX, screenY, width, height, drawOffsetX, drawOffsetY);
        w.setZoom(zoom);
        attachWindow(w);
    }

    /** 
     * Attach an existing window to this manager.  This registers event
     * callbacks with the window to notify the manager of relevant events.
     */
    public void attachWindow(final PZWindow w) {
        synchronized(windows) {
            // track the window
            if(windows.contains(w))
                return;
            windows.add(w);
            
            // show the window
            w.setVisible(true);
        }
    }

    /**
     * Cleans up after a window which is closing.  Terminates the application
     * if no windows are left.
     */
    public void closeWindow(PZWindow w) {
        synchronized(windows) {
            windows.remove(w);
            w.dispose();
        }
        terminateIfNoWindowsLeft();
    }
    
    /**
     * Returns the internal index of the frame specified.
     * @param frame  the frame to get the index of (may change over time)
     */
    public int getWindowIndex(PZWindow frame) {
        synchronized(windows) {
            return windows.indexOf(frame);
        }
    }

    /**
     * Terminates the application if no GUI windows are left.
     */
    private void terminateIfNoWindowsLeft() {
        synchronized(windows) {
            if(windows.size()==0) {
                for(PZClosing x : closingListeners)
                    x.pzClosing(this);

                System.exit(0);
            }
        }
    }
    
    /** adds a listener to be notified when the manager is terminating */
    public void addClosingListener(PZClosing c) {
        if(!closingListeners.contains(c))
            closingListeners.add(c);
    }

    /** removes the specified closing listener */
    public void removeClosingListener(PZClosing c) {
        closingListeners.remove(c);
    }

    
    // ------- Scene Elements (Drawables) ------- //
    // ****************************************** //

    /** Entities to draw on the GUIs */
    private Vector<Drawable> drawables = new Vector<Drawable>();

    /** the order in which certain types of objects should be drawn (first=front) */
    private LinkedList<Class> classDrawOrder = new LinkedList<Class>();
    
    /**
     * Add a new entity to draw on the GUI.
     * @param d  the entity to start drawing
     */
    public synchronized void addDrawable(Drawable d) {
        // only draw each entity once
        if(drawables.contains(d))
            return;

        // determine which objects e should be drawn on top of
        boolean found = false;
        LinkedList<Class> mustDrawOnTopOf = new LinkedList<Class>();
        for(Class c : classDrawOrder) {
            if(found)
                mustDrawOnTopOf.add(c);
            else if(c == d.getClass())
                found = true;
        }
        
        // search from back to front of z-order until we find something e should
        // NOT be drawn in front of and then insert e right after that something
        for(int i=drawables.size()-1; i>=0; i--) {
            Class c = drawables.get(i).getClass();

            // see if here is an ok spot (e.g. i's class is not in before)
            boolean here = true;
            for(Class cAfter : mustDrawOnTopOf) {
                if(cAfter == c) {
                    here = false;
                    break;
                }
            }

            // add it here if it was ok
            if(here) {
                drawables.insertElementAt(d, i + 1);
                return;
            }
        }

        // add to the front since it must be before everything else in drawables
        drawables.insertElementAt(d, 0);
    }

    /**
     * Stop drawing the specified entity.
     * @param d  the entity to stop drawing
     */
    public synchronized void removeEntity(Drawable d) {
        drawables.remove(d);
    }

    /**
     * Sets the default ordering of entity objects.  The existing entities will
     * be resorted into the new ordering.
     *
     * @param newOrder  The new order, with the first class specified being the
     *                  class of objects drawn on top (e.g. drawn last).
     */
	public synchronized void setDrawOrder(Collection<Class> newOrder) {
        // copy-in the new ordering
        classDrawOrder.clear();
        for(Class c : newOrder) {
            classDrawOrder.add(c);
        }

        // re-sort drawables based on the new ordering
        Vector<Drawable> oldDrawables = drawables;
        drawables = new Vector<Drawable>();
        for(Drawable d : oldDrawables)
            addDrawable(d);
    }


    // ------- Scene Elements (Icons) ------- //
    // ************************************** //

    /** Stores an icon and the location to draw it. */
    private class IconAndLocation {
        /** icon to draw */
        public TemporalIcon icon;

        /** where to draw the icon */
        public int x, y;

        /** Construct an icon-location pair */
        public IconAndLocation(TemporalIcon icon, int x, int y) {
            this.icon = icon;
            this.x = x;
            this.y = y;
        }
    }

    /** Icons to draw in the scene */
    private Vector<IconAndLocation> icons = new Vector<IconAndLocation>();

    /**
     * Display icon for duration_msec scaled by scale at the curret mouse location.
     * @param icon           the icon to draw
     * @param duration_msec  how long to display it
     * @param scale          scaling factor
     */
    public synchronized void displayIcon(Icon icon, int duration_msec, float scale) {
        displayIcon(icon, duration_msec, scale, mousePos.x, mousePos.y);
    }

    /**
     * Display icon for duration_msec scaled by scale at the specified location.
     * @param icon           the icon to draw
     * @param duration_msec  how long to display it
     * @param scale          scaling factor
     * @param x              x location of the icon
     * @param y              y location of the icon
     */
    public synchronized void displayIcon(Icon icon, int duration_msec, float scale,int x, int y) {
        icons.add(new IconAndLocation(new TemporalIcon(icon, duration_msec, scale), x, y));
    }

    /**
     * Display text for duration_msec at the default size and the curret mouse location.
     * @param s              the string to draw
     * @param duration_msec  how long to display it
     */
    public synchronized void displayIcon(String s, int duration_msec) {
        displayIcon(s, duration_msec, Constants.FONT_DEFAULT.getSize(),
                    mousePos.x, mousePos.y);
    }

    /**
     * Display text for duration_msec at the specified size and the curret mouse location.
     * @param s              the string to draw
     * @param duration_msec  how long to display it
     * @param sz             font size
     */
    public synchronized void displayIcon(String s, int duration_msec, int sz) {
        displayIcon(s, duration_msec, sz, mousePos.x, mousePos.y);
    }

    /**
     * Display text for duration_msec scaled by scale at the specified location.
     * @param s              the string to draw
     * @param duration_msec  how long to display it
     * @param sz             font size
     */
    public synchronized void displayIcon(String s, int duration_msec, int sz,int x, int y) {
        TextIcon icon = new TextIcon(s,
                                     Constants.FONT_TI,
                                     sz,
                                     Constants.FONT_TI_FILL,
                                     Constants.FONT_TI_OUTLINE);

        displayIcon(icon, duration_msec, 1.0f, x, y);
    }


    // ------- Redrawing ------- //
    // ************************************** //

    /** Continuously redraws the windows at the desired interval. */
    public void run() {
        final Vector2i prevPos  = new Vector2i();
        final Vector2i prevSize = new Vector2i();
        final Vector2i curPos   = new Vector2i();
        final Vector2i curSize  = new Vector2i();

        // create the initial GUI display if it wasn't specified in the config file
        if(windows.size() == 0)
            addWindow(0, 0, 1024, 768, 0, 0, 1.0f);

        // keep the display and moving objects positions up to date
        long t;
        while(true) {
            t = System.currentTimeMillis();

            // redraw each display
            boolean first = true;
            synchronized(windows) {
                // initialize the font metrics object used by the Icon class
                if(windows.size() > 0 && windows.firstElement().getGraphics()!=null)
                    Icon.FONT_METRICS = windows.firstElement().getGraphics().getFontMetrics();

                // refresh each window
                for(PZWindow display : windows) {
                    // get this display's current position and size
                    curPos.set(display.getX(), display.getY());
                    curSize.set(display.getWidth(), display.getHeight());

                    // set window's position based on docking settings
                    if(!first && display.getDockLocation()!=DockLocation.NOT_DOCKED)
                        display.setPos(display.getDockLocation().getChildPosition(prevPos, prevSize, curPos, curSize));
                    else
                        first = false;

                    // redraw the window's content
                    display.redraw();

                    // save this display's position and size in case the next window docks to it
                    prevPos.set(curPos);
                    prevSize.set(curSize);
                }
                
                postRedraw();
            }

            // wait until it is time for the next redraw
            try {
                redrawTimeActual_msec = System.currentTimeMillis() - t;
                t = redrawIntervalDesired_msec - redrawTimeActual_msec;
                if(t > 0)
                    Thread.sleep(t);
            }
            catch(InterruptedException e) { /* ignore */ }
        }
    }
    
    /** 
     * This method is called after each redraw.  This implementation is a no-op
     * but derived classes may override it to do something after each redraw.
     */
    protected void postRedraw() {}
    
    /** Sets ups gfx based on the pan and zoom settings */
    private static void setupGraphicsView(Graphics2D gfx, Vector2i offset, float zoom) {
        gfx.translate(offset.x, offset.y);
        gfx.scale(zoom, zoom);
    }
    
    /** Removes a gfx setup by unzooming and then untranslating */
    private static void resetGraphicsView(Graphics2D gfx, Vector2i offset, float zoom) {
        // back to the original view
        gfx.scale(1.0f / zoom, 1.0f / zoom);
        gfx.translate(-offset.x, -offset.y);
    }

    /**
     * Redraw the scene on the specified display.
     *
     * @param window  the display which is to be redrawn
     */
    public synchronized void redraw(PZWindow window) {
        // get GUI fields which affect the drawing process
        Graphics2D gfx = window.getDisplayGfx();
        if(gfx == null)
            return;

        // clear the drawing space
        gfx.setBackground(Color.WHITE);
        gfx.clearRect(0, 0, window.getWidth(), window.getHeight());
        gfx.setFont(Constants.FONT_DEFAULT);
        gfx.setPaint(Constants.PAINT_DEFAULT);

        // setup the view based on the pan and zoom settings
        Vector2i offset = new Vector2i(window.getDrawOffsetX(), window.getDrawOffsetY());
        float zoom = window.getZoom();
        setupGraphicsView(gfx, offset, zoom);
        
        // note that all nodes are undrawn at this point
        for(Drawable e : drawables)
            e.unsetDrawn();
        
        // draw anything which needs to be drawn before the objects themselves
        for(Drawable e : drawables)
            e.drawBeforeObject(gfx);
        
        // draw all of the objects
        for(Drawable e : drawables)
            e.drawObject(gfx);
        
        // draw any unexpired icons
        for(int i=0; i<icons.size(); i++) {
            IconAndLocation ial = icons.get(i);
            if(ial.icon.isExpired())
                icons.remove(i);
            else
                ial.icon.draw(gfx, ial.x, ial.y);
        }
        
        // back to the original view
        resetGraphicsView(gfx, offset, zoom);
    }


    // ------- Scene refresh rate ------- //
    // ********************************** //

    /** desired number of milliseconds between the beginnings of two redraws */
    private long redrawIntervalDesired_msec = 25;

    /** number of milliseconds between the beginnings of the previous two redraws */
    private long redrawTimeActual_msec = 0;

    /** number of milliseconds between clicks which counts as a double-click */
    private long doubleClickThreshold_msec = 250;

    /** Returns the target FPS */
    public double getTargetFPS() {
        return ((int)(10000.0 / redrawIntervalDesired_msec)) / 10.0;
    }

    /** Sets the target FPS */
    public void setTargetFPS(double fps) {
        redrawIntervalDesired_msec = (long)(1000 / fps);
    }

    
    // ------- Mouse ------- //
    // ********************* //

    /** Position of the mouse when mouse down was depressed */
    private Vector2i mouseStartPos = new Vector2i();

    /** Current position of the mouse */
    private Vector2i mousePos = new Vector2i();

    /** Time with the mouse down button was depressed */
    private long mouseStartTime = 0;

    /** Time the mouse was previously up */
    private long lastMouseUpTime = 0;

    /** Time the mouse was up */
    private long mouseUpTime = 0;

    /** Gets the position of the mouse when mouse down was depressed */
    public Vector2i getMouseStartPos() {
        return mouseStartPos;
    }

    /** Gets the current position of the mouse */
    public Vector2i getMousePos() {
        return mousePos;
    }

    /** Gets how long the mouse has been down */
    public long getMouseDownTime() {
        return System.currentTimeMillis() - mouseStartTime;
    }

    /** Account for a mouse up event */
    public void noteMouseUp() {
        lastMouseUpTime = mouseUpTime;
        mouseUpTime = System.currentTimeMillis();
    }

    /** Sets the current position of the mouse and whether dragging is going on */
    public void setMousePos(int x, int y, boolean dragging) {
        mousePos.set(x, y);
        if(!dragging) {
            mouseStartPos.set(x, y);
            mouseStartTime = System.currentTimeMillis();
        }
    }

    /** Get whether the last click was of a double-click */
    public boolean wasDoubleClick() {
        return mouseUpTime - lastMouseUpTime < doubleClickThreshold_msec;
    }


    // ------- Hovering and Selection ------- //
    // ************************************** //

    /** The entity which the mouse is currently over */
    private Drawable hoveredEntity;

    /** The entity which is currently selected */
    private Drawable selectedEntity;

    /** Returns the currently selected object, if any */
    public synchronized Drawable getSelected() {
        return selectedEntity;
    }

    /** Clears any selection */
    public synchronized void deselect() {
        if(selectedEntity != null)
            selectedEntity.setSelected(false);

        selectedEntity = null;
    }

    /** Selects the specified object */
    public synchronized void select(Drawable d) {
        if(d != selectedEntity) {
            deselect();
            selectedEntity = d;
            if(d != null)
                d.setSelected(true);
        }
    }

    /** Selects the object at the specified coordinates, if any */
    public synchronized void select(int x, int y) {
        Drawable d = selectFrom(x, y);
        if(d != null)
            select(d);
    }
    
    /**
     * Returns the Drawable which contains the location x, y.  If no such object
     * exists, then null is returned.  The node is selected if setSelectedToTrue
     * is true.
     *
     * @param x           x position the drawable must contain
     * @param y           y position the drawable must contain
     *
     * @return the drawable at the specified position
     */
    public synchronized Drawable selectFrom(int x, int y) {
        return selectFrom(0, x, y);
    }

    /**
     * Returns the Drawable which contains the location x, y.  If no such object
     * exists, then null is returned.  The node is selected if setSelectedToTrue
     * is true.
     *
     * @param startIndex  where to start searching within the drawables list
     * @param x           x position the drawable must contain
     * @param y           y position the drawable must contain
     *
     * @return the drawable at the specified position
     */
    public synchronized Drawable selectFrom(int startIndex, int x, int y) {
        for(int i=startIndex; i<drawables.size(); i++) {
            Drawable d = drawables.get(i);
            if(d.isWithin(x, y))
                return d;
        }

        return null;
    }

    /** Returns the currently hovered object, if any */
    public synchronized Drawable gethovered() {
        return hoveredEntity;
    }

    /** Clears any hovered state */
    public synchronized void dehover() {
        if(hoveredEntity != null)
            hoveredEntity.setHovered(false);

        hoveredEntity = null;
    }

    /** Sets that the specified object is being hovered over */
    public synchronized void hover(Drawable d) {
        if(d != hoveredEntity) {
            dehover();
            hoveredEntity = d;
            if(d != null)
                d.setHovered(true);
        }
    }
}
