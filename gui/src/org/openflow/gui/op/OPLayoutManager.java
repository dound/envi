package org.openflow.gui.op;

import java.awt.Graphics2D;
import java.util.Vector;

import org.openflow.gui.drawables.OPModule;
import org.openflow.gui.drawables.OPNodeWithNameAndPorts;
import org.pzgui.Drawable;
import org.pzgui.PZManager;
import org.pzgui.PZWindow;
import org.pzgui.math.Vector2i;

/**
 * Provides OpenPipes-specific layout enhancements.
 * 
 * @author David Underhill
 */
public class OPLayoutManager extends PZManager {
	/* Should we show everything? */
	private boolean fullView;
	
    /** Which entities are always visible */
    private Vector<Drawable> alwaysVisibleDrawables = new Vector<Drawable>();

	public OPLayoutManager() {
        super(); 
        fullView = true;
    }
    
    /**
     * Extends the base implementation to use OPWindowEventListener instead of
     * the default.
     */
    public void attachWindow(final PZWindow w, boolean addDefaultEventListener) {
        super.attachWindow(w, false);
        
        w.setCustomTitle(OpenPipes.OPENPIPES_TITLE);
        
        if(addDefaultEventListener)
            w.addEventListener(new OPWindowEventListener());
    }
    
    public void setMousePos(int x, int y, boolean dragging) {
        super.setMousePos(x, y, dragging);
        Drawable ds = getSelected();
        Drawable dh = getHovered();
        
        // always ok if we aren't dragging a module or nothing is selected
        if(ds==null || dh==null || !dragging || !(ds instanceof OPModule))
            return;
        
        // do not highlight when hovering over nodes in certain cases
        if(dh instanceof OPNodeWithNameAndPorts) {
            OPModule m = (OPModule)ds;
            OPNodeWithNameAndPorts n = (OPNodeWithNameAndPorts)dh;
            switch(n.getType()) {
            case UNKNOWN:
            case TYPE_IN:
            case TYPE_OUT:
            case TYPE_MODULE_HW:
            case TYPE_MODULE_SW:
                dehover();
                return;
            
            // don't highlight if we drag a SW module over a NetFPGA
            case TYPE_NETFPGA:
                if(!m.isHardwareModule()) {
                    dehover();
                    return;
                }
                break;
            
            // don't highlight if we drag a HW module over a PC
            case TYPE_PC:
                if(m.isHardwareModule()) {
                    dehover();
                    return;
                }
                break;
                
            default: 
                // remaining cases ok
            }
        }
    }
    
    public void run() {
        // create the initial GUI display if it wasn't specified in the config file
        if(windows.size() == 0)
            addWindow(0, 0, 1366, 768, 0, 0, 1.0f);
        super.run();
    }

    @Override
	public synchronized void redraw(PZWindow window) {
    	if (fullView)
    		super.redraw(window);
    	else {
	    	
	        // get GUI fields which affect the drawing process
	        Graphics2D gfx = window.getDisplayGfx();
	        if(gfx == null)
	            return;
	        
	        // setup the view based on the pan and zoom settings
	        Vector2i offset = new Vector2i(window.getDrawOffsetX(), window.getDrawOffsetY());
	        float zoom = window.getZoom();
	        setupGraphicsView(gfx, offset, zoom);
	        
	        // note that all nodes are undrawn at this point
	        for(Drawable e : alwaysVisibleDrawables)
	            e.unsetDrawn();
	        
	        // note that all nodes are undrawn at this point
	        for(Drawable e : alwaysVisibleDrawables)
	            drawObject(gfx, e);
	        
	        // back to the original view
	        resetGraphicsView(gfx, offset, zoom);
    	}
	}
    
    /**
     * Add a new entity to always draw on the GUI.
     * @param d  the entity to start drawing
     */
    public synchronized void addAlwaysVisibleDrawable(Drawable d) {
        // only draw each entity once
        if(alwaysVisibleDrawables.contains(d))
            return;
        
        // add to the front since it must be before everything else in drawables
        alwaysVisibleDrawables.insertElementAt(d, 0);
    }

    /**
     * Stop always drawing the specified entity.
     * @param d  the entity to stop drawing
     */
    public synchronized void removeAlwaysVisibleDrawable(Drawable d) {
        alwaysVisibleDrawables.remove(d);
    }
    
    /**
     * Show the full view with all drawables
     */
    public void showFullView() {
    	fullView = true;
    }
    
    /**
     * Show the minimal view with only always visible drawables
     */
    public void showMinimalView() {
    	fullView = false;
    }

}
