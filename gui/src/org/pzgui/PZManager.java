package org.pzgui;

import java.awt.AWTEvent;
import java.awt.Graphics2D;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.ho.yaml.YamlConfig;
import org.ho.yaml.wrapper.DelayedCreationBeanWrapper;
import org.ho.yaml.wrapper.ObjectWrapper;
import org.ho.yaml.wrapper.WrapperFactory;

import org.openflow.util.string.DPIDUtil;

import org.pzgui.icon.Icon;
import org.pzgui.icon.TemporalIcon;
import org.pzgui.icon.TextIcon;
import org.pzgui.layout.Layoutable;
import org.pzgui.math.Vector2i;

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
    protected final Vector<PZWindow> windows = new Vector<PZWindow>();

    /** closing event listeners */
    private final LinkedList<PZClosing> closingListeners = new LinkedList<PZClosing>();

    /** adds a new display window in a thread-safe way */
    public void addWindow(int screenX, int screenY, int width, int height, int drawOffsetX, int drawOffsetY, float zoom) {
        PZWindow w = new PZWindow(this, screenX, screenY, width, height, drawOffsetX, drawOffsetY);
        w.setZoom(zoom);
        attachWindow(w);
    }

    /** 
     * Calls attchWindow(w, true).
     */
    public void attachWindow(final PZWindow w) {
        attachWindow(w, true);
    }
    
    /** 
     * Attach an existing window to this manager.  This registers event
     * callbacks with the window to notify the manager of relevant events.  If
     * addDefaultEventListener is true, then a new PZWindowEventListener is 
     * setup for w.
     */
    public void attachWindow(final PZWindow w, boolean addDefaultEventListener) {
        synchronized(windows) {
            // track the window
            if(windows.contains(w))
                return;
            windows.add(w);
            
            // show the window
            w.setVisible(true);
        }
        
        if(addDefaultEventListener)
            w.addEventListener(new PZWindowEventListener());
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

                System.out.println("Goodbye");
                System.exit(0);
            }
        }
    }
    
    /**
     * This is called when a window is resized.  This implementation does 
     * nothing but is here for subclasses to override.
     */ 
    public void windowResized(PZWindow window) {}
    
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

    /** the order in which certain types of objects should be drawn (last=front) */
    private LinkedList<Class> classDrawOrder = new LinkedList<Class>();
    
    /**
     * Add a new entity to draw on the GUI.
     * @param d  the entity to start drawing
     */
    public synchronized void addDrawable(Drawable d) {
        // only draw each entity once
        if(drawables.contains(d))
            return;
        
        setLayoutableInfo(d);

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
            Class<? extends Drawable> c = drawables.get(i).getClass();

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
    public synchronized void removeDrawable(Drawable d) {
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
            classDrawOrder.addFirst(c);
        }

        // re-sort drawables based on the new ordering
        Vector<Drawable> oldDrawables = drawables;
        drawables = new Vector<Drawable>();
        for(Drawable d : oldDrawables)
            addDrawable(d);
    }
    
    
    // ------------- Saving Layoutable Positioning Info to File ------------- //
    
    /**
     * Information used to describe a Layoutable when it is serialized.
     */
    public static class LayoutableInfo {
        public final long idNum;
        public final String id;
        public final int x, y;
        public final boolean lock;
        
        public LayoutableInfo() {
            this(0, 0, 0, false);
        }
        
        public LayoutableInfo(long idNum, int x, int y, boolean lock) {
            this.idNum = idNum;
            this.x = x;
            this.y = y;
            this.lock = lock;
            id = DPIDUtil.toString(idNum);
        }
        
        public String toString() {
            return id + "@[" + x + "," + y + "]" + (lock ? "-LOCK" : "");
        }
    }
    
    /**
     * Describes how to serialize and deserialize the LayoutableInfo class.
     */
    private static class LayoutableInfoWrapper extends DelayedCreationBeanWrapper
                                               implements WrapperFactory {
        // define strings with names of our fields (so we can use ==)
        private static final String ID = "id";
        private static final String X = "x";
        private static final String Y = "y";
        private static final String LOCK = "lock";
        
        /** defines the order to present the fields */
        private static final Comparator<String> KEYS_COMPARATOR = new Comparator<String>() {
            public int compare(String s1, String s2) {
                if(s1 == s2)
                    return 0;
                
                if(s1 == ID)
                    return -1;
                else if(s2 == ID)
                    return 1;
                else if(s1 == X)
                    return -1;
                else if(s2 == X)
                    return 1;
                else if(s1 == Y)
                    return -1;
                
                return 1;
            }
        };
        
        /** define the set which stores our field keys in order */
        private static final TreeSet<String> sortedKeys = new TreeSet<String>(KEYS_COMPARATOR);
        private static final TreeSet<String> sortedKeysNoLock = new TreeSet<String>(KEYS_COMPARATOR);
        static {
            sortedKeys.add(ID);
            sortedKeys.add(X);
            sortedKeys.add(Y);
            sortedKeys.add(LOCK);
            
            sortedKeysNoLock.add(ID);
            sortedKeysNoLock.add(X);
            sortedKeysNoLock.add(Y);
        }
        
        public LayoutableInfoWrapper(Class<LayoutableInfo> type) {
            super(type);
        }
        
        public Set keys() {
            if(values.get(LOCK) == null && (getObject()==null || !((LayoutableInfo)getObject()).lock))
                return sortedKeysNoLock;
            else
                return sortedKeys;
        }

        public String[] getPropertyNames() {
            return new String[]{ID, X, Y, LOCK};
        }
        
        protected Object createObject() {
            String strId = (String)values.get(ID);
            long id;
            try {
                id = DPIDUtil.hexToDPID(strId);
            }
            catch(NumberFormatException e) {
                System.err.println("Bad ID field encountered when parsing YAML: " + strId);
                return null;
            }
            
            Boolean lock = (Boolean)values.get(LOCK);
            
            LayoutableInfo info = new LayoutableInfo(id, 
                                                     (Integer)values.get(X),
                                                     (Integer)values.get(Y),
                                                     (lock!=null && lock));
            
            return info;
        }
        
        public Object createPrototype() {
            return new LayoutableInfo(0, 0, 0, true);
        }
        
        public ObjectWrapper makeWrapper() {
            return new LayoutableInfoWrapper(LayoutableInfo.class);
        }
    }
    
    /** the Yaml configuration */
    public static final YamlConfig YAML = new YamlConfig();
    static {
        HashMap<String, Object> handlersMap = new HashMap<String, Object>();
        handlersMap.put(LayoutableInfo.class.getName(), new LayoutableInfoWrapper(LayoutableInfo.class));
        
        YAML.setHandlers(handlersMap);
        YAML.setIndentAmount("    ");
        YAML.setMinimalOutput(true);
        YAML.setSuppressWarnings(false);
    }
    
    /** where layout positions from a file is saved */
    protected ConcurrentHashMap<Long, LayoutableInfo> layoutablePositions = new ConcurrentHashMap<Long, LayoutableInfo>();
    
    /** name of the last config file used */
    private String lastConfigFilename = "";
    
    /** returns the last configuration file loaded or saved */ 
    public String getLastConfigFilename() {
        return lastConfigFilename;
    }
    
    /**
     * Loads positions for Layoutable objects from a file.
     * 
     * @param file  the filename to load from
     */
    public void loadDrawablePositionsFromFile(String file) {
        lastConfigFilename = file;
        
        LayoutableInfo[] infos;
        try {
            infos = YAML.loadType(new java.io.File(file), LayoutableInfo[].class);
        } catch (FileNotFoundException e) {
            DialogHelper.displayError(e);
            return;
        }
        
        layoutablePositions.clear();
        for(LayoutableInfo info : infos)
            layoutablePositions.put(info.idNum, info);
        
        for(Drawable d : drawables)
            setLayoutableInfo(d);
    }
    
    /** Sets the positions of d if there is info about it in layoutblePositions. */
    private void setLayoutableInfo(Drawable d) {
        if(d instanceof Layoutable) {
            Layoutable l = (Layoutable)d;
            LayoutableInfo i = layoutablePositions.get(l.getID());
            if(i == null)
                return;
            
            l.setCanPositionChange(true);
            l.setPos(i.x, i.y);
            l.setCanPositionChange(!i.lock);
        }
    }

    /**
     * Saves positions for Layoutable objects to a file.
     * 
     * @param file  the filename to save to
     */
    public void saveDrawablePositionsToFile(String file) {
        lastConfigFilename = file;
        
        ArrayList<LayoutableInfo> infos = new ArrayList<LayoutableInfo>();
        for(Drawable d : drawables) {
            if(d instanceof Layoutable) {
                Layoutable l = (Layoutable)d;
                infos.add(new LayoutableInfo(l.getID(), l.getX(), l.getY(), !l.canPositionChange()));
            }
        }
        
        try {
            YAML.dump(infos, new java.io.File(file));
        } catch (FileNotFoundException e) {
            DialogHelper.displayError(e);
        }
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


    // -------------- Redrawing ------------- //
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
     * This method is called before each redraw.
     */
    protected void preRedraw(PZWindow window) {
        // get GUI fields which affect the drawing process
        Graphics2D gfx = window.getDisplayGfx();
        if(gfx == null)
            return;

        // clear the drawing space
        gfx.setBackground(Constants.BG_DEFAULT);
        gfx.clearRect(0, 0, window.getWidth() - window.getReservedWidthRight(), window.getHeight() - window.getReservedHeightBottom());
        gfx.setFont(Constants.FONT_DEFAULT);
        gfx.setPaint(Constants.PAINT_DEFAULT);
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
        
        // All drawables from index drawSplits[i-1] to drawSplits[i] (both 
        // exclusive) will be drawn after the previous split and before the next
        int[] drawSplits = new int[classDrawOrder.size()+1];
        drawSplits[classDrawOrder.size()] = drawables.size();
        
        // determine the grouping ranges
        int cIndex=0, dIndex=0;
        for(; cIndex<classDrawOrder.size(); cIndex++) {
            Class c = classDrawOrder.get(cIndex);
            
            // find the first occurrence not from class c
            for(; dIndex<drawables.size(); dIndex++) {
                if(c.isInstance(drawables.get(dIndex))) {
                    // include up to but excluding dIndex in the cIndex round
                    drawSplits[cIndex] = dIndex;
                    break;
                }
            }
        }
        
        // setup the view based on the pan and zoom settings
        Vector2i offset = new Vector2i(window.getDrawOffsetX(), window.getDrawOffsetY());
        float zoom = window.getZoom();
        setupGraphicsView(gfx, offset, zoom);
        
        // note that all nodes are undrawn at this point
        for(Drawable e : drawables)
            e.unsetDrawn();
        
        int prevSplitEnd = 0;
        for(int end : drawSplits) {
            // draw anything which needs to be drawn before the objects themselves
            for(int i=prevSplitEnd; i<end; i++)
                drawBeforeObject(gfx, drawables.get(i));
        
            // draw all of the objects
            for(int i=prevSplitEnd; i<end; i++)
                drawObject(gfx, drawables.get(i));
            
            // don't go backwards
            if(end > prevSplitEnd)
                prevSplitEnd = end;
        }
        
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
    
    /** 
     * Calls the drawBeforeObject(gfx) method on d.  This method is simply 
     * present so it can be overriden to extend how the drawing of individual
     * Drawables is done.
     */
    protected void drawBeforeObject(Graphics2D gfx, Drawable d) {
        d.drawBeforeObject(gfx);
    }
    
    /** 
     * Calls the drawObject(gfx) method on d.  This method is simply 
     * present so it can be overriden to extend how the drawing of individual
     * Drawables is done.
     */
    protected void drawObject(Graphics2D gfx, Drawable d) {
        d.drawObject(gfx);
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

    /** 
     * Sets the current position of the mouse and whether dragging is going on.
     * Also track which object is being hovered over.
     */
    public void setMousePos(int x, int y, boolean dragging) {
        mousePos.set(x, y);
        hover(selectFrom(x, y, filterIgnoreSelectedNode));
    }

    /** Get whether the last click was of a double-click */
    public boolean wasDoubleClick() {
        return mouseUpTime - lastMouseUpTime < doubleClickThreshold_msec;
    }
    
    /** define a filter which accepts anything but currently selected object */
    private final DrawableFilter filterIgnoreSelectedNode = new DrawableFilter() {
        public boolean consider(Drawable d) {
            return d != selectedEntity;
        }
    };


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
     * exists, then null is returned.
     *
     * @param x           x position the drawable must contain
     * @param y           y position the drawable must contain
     *
     * @return the drawable at the specified position
     */
    public synchronized Drawable selectFrom(int x, int y) {
        return selectFrom(x, y, null);
    }

    /**
     * Returns the object of type C which contains the location x, y.  If no 
     * such object exists, then null is returned.
     *
     * @param x       x position the Drawable must contain
     * @param y       y position the Drawable must contain
     * @param fitler  a Drawable filter; only nodes for which 
     *                filter.consider() returns true will be considered for 
     *                selection.  If this filter is null, then all nodes will be
     *                considered.
     *
     * @return the Drawable at the specified position
     */
    public synchronized Drawable selectFrom(int x, int y, DrawableFilter filter) {
        // traverse the list from back to front so that we first consider 
        // elements which are drawn on top (i.e., select what you see)
        ListIterator<Drawable> itr = drawables.listIterator(drawables.size());
        while(itr.hasPrevious()) {
            Drawable d = itr.previous();
            if(d.contains(x, y) && (filter==null || filter.consider(d)))
                return d;
        }
        return null;
    }

    /** Returns the currently hovered object, if any */
    public synchronized Drawable getHovered() {
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


    // ------ Drawable Event Handling ------- //
    // ************************************** //
    
    /** closing event listeners */
    private final LinkedList<DrawableEventListener> drawableEventListeners = new LinkedList<DrawableEventListener>();
    
    /** adds a listener to be notified when the traffic matrix has changed */
    public void addDrawableEventListener(DrawableEventListener del) {
        if(!drawableEventListeners.contains(del))
            drawableEventListeners.add(del);
    }

    /** removes the specified traffic matrix change listener */
    public void removeDrawableEventListener(DrawableEventListener del) {
        drawableEventListeners.remove(del);
    }
        
    /**
     * Updates the slider labels and notify those listening for traffic matrix changes.
     */
    public void fireDrawableEvent(Drawable d, AWTEvent e, String event) {
        for(DrawableEventListener del : drawableEventListeners)
            del.drawableEvent(d, e, event);
    }
}
