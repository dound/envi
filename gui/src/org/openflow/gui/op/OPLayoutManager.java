package org.openflow.gui.op;

import org.openflow.gui.drawables.OPModule;
import org.openflow.gui.drawables.OPNodeWithNameAndPorts;
import org.pzgui.Drawable;
import org.pzgui.PZManager;
import org.pzgui.PZWindow;

/**
 * Provides OpenPipes-specific layout enhancements.
 * 
 * @author David Underhill
 */
public class OPLayoutManager extends PZManager {
    public OPLayoutManager() {
        super(); 
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
}
