package org.openflow.gui.op;

import java.awt.event.KeyEvent;

import org.pzgui.PZWindow;
import org.pzgui.PZWindowEventListener;

public class OPWindowEventListener extends PZWindowEventListener {
    /** the global link add mode */
    private static boolean linkAddMode = false;
    
    /** the global link delete mode status */
    private static boolean linkDeleteMode = false;
    
    /** whether the mode has changed since last asked */
    private static boolean modeHasChanged = false;
    
    /** returns true if we are in link add mode */
    public static boolean isLinkAddMode() {
        return linkAddMode;
    }
    
    /** returns true if we are in link delete mode */
    public static boolean isLinkDeleteMode() {
        return linkDeleteMode;
    }
    
    /** returns true if we are in module status mode */
    public static boolean isModuleStatusMode() {
        return !isLinkAddMode() && !isLinkDeleteMode();
    }
    
    /**
     * Returns true if the mode has changed since the last time this method was
     * called.
     */
    public static boolean hasModeChanged() {
        boolean ret = modeHasChanged;
        modeHasChanged = false;
        return ret;
    }
    
    /** 
     * Updates the specified window's title wth the new mode and records the 
     * the mode change.
     */
    private void modeChanged(PZWindow w) {
        modeHasChanged = true;
        if(isModuleStatusMode())
            w.setCustomTitle(OpenPipes.OPENPIPES_TITLE);
        else
            w.setCustomTitle(OpenPipes.OPENPIPES_TITLE + " - " + getModeName());
    }
    
    /** returns a string representation of the current mode */
    private static String getModeName() {
        if(isLinkAddMode())
            return "Link Add Mode";
        else if(isLinkDeleteMode())
            return "Link Delete Mode";
        else if(isModuleStatusMode())
            return "Module Status Mode";
        else
            return "Unknown Mode";
            
    }
    
    /**
     * Adds key bindings for A (toggle add link mode) and D (toggle remove 
     * link mode).
     */
    public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_A) {
            linkAddMode = !linkAddMode;
            linkDeleteMode = false;
            modeChanged(getWindow(e));
        }
        else if(e.getKeyCode() == KeyEvent.VK_D) {
            linkDeleteMode = !linkDeleteMode;
            linkAddMode = false;
            modeChanged(getWindow(e));
        }
        else
            super.keyReleased(e);
    }
}
