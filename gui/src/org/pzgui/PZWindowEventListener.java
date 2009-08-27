package org.pzgui;

import java.awt.AWTEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import org.pzgui.PZWindow.JLabelWithPZWindowParent;

/**
 * A class which provides default implementations of event callbacks on the 
 * topology area of a PZWindow.  Unless documented otherwise, the default
 * implementation is empty.
 * 
 * @author David Underhill
 */
public class PZWindowEventListener implements ComponentListener, 
                                              KeyListener, 
                                              MouseListener, 
                                              MouseMotionListener, 
                                              MouseWheelListener, 
                                              WindowListener {
    
    /** Default constructor. */
    public PZWindowEventListener() {}
    
    /**
     * Gets the PZWindow associated with the object on which the event was called.
     */
    protected final PZWindow getWindow(AWTEvent e) {
        Object src = e.getSource();
        if(src instanceof JLabelWithPZWindowParent) {
            JLabelWithPZWindowParent lbl = (JLabelWithPZWindowParent)src;
            return lbl.window;
        }
        else if(src instanceof PZWindow)
            return (PZWindow)src;
        else
            throw new Error("received event from unexpected source: " + src.toString());
    }
    
    
    // -------------- Component Events -------------- //
    
    public void componentHidden(ComponentEvent e)  { /* no-op */ }
    public void componentMoved(ComponentEvent e)   { /* no-op */ }
    public void componentResized(ComponentEvent e) { /* no-op */ }
    public void componentShown(ComponentEvent e)   { /* no-op */ }

    
    // ----------------- Key Events ----------------- //
    
    /**
     * If ALT is depressed, then the window is zoomed; otherwise the window is
     * panned.
     */
    public void keyPressed(KeyEvent e) {
        PZWindow window = getWindow(e);
        
        if(e.getKeyCode() == KeyEvent.VK_LEFT) {
            if(e.isAltDown())
                window.zoomOut();
            else
                window.panLeft();
        }
        else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
            if(e.isAltDown())
                window.zoomIn();
            else
                window.panRight();
        }
        else if(e.getKeyCode() == KeyEvent.VK_UP) {
            if(e.isAltDown())
                window.zoomIn();
            else
                window.panUp();
        }
        else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
            if(e.isAltDown())
                window.zoomOut();
            else
                window.panDown();
        }
    }

    /**
     * Returns the filename entered by the user with a ".yaml" extension, or 
     * null if no file was chosen.
     * 
     * @param prompt  the message to show in the prompt
     * @param defaultFN  the default filename
     * 
     * @return the filename, or null if none was chosen
     */
    private String getFilename(String prompt, String defaultFN) {
        String file = DialogHelper.getInput(prompt, defaultFN);
        if(file != null && !file.endsWith(".yaml"))
            return file + ".yaml";
        else
            return file;
    }
    
    /**
     * Provides default actions for some keys.
     *   H: prints help info to stdout
     *   Ctrl+O: load layout positions from a file
     *   Ctrl+S: save layout positions to a file
     *   V: calls window.restView()
     *   Escape: terminates the program
     *   Page Up: Take a screenshot
     */
    public void keyReleased(KeyEvent e) {
        PZWindow window = getWindow(e);
        PZManager manager = window.getManager();
        
        if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_O) {
            String file = getFilename("What file do you want to save to?", manager.getLastConfigFilename());
            if(file != null)
                manager.loadDrawablePositionsFromFile(file);
        }
        else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
            String file = getFilename("What file do you want to save to?", manager.getLastConfigFilename());
            if(file != null)
                manager.saveDrawablePositionsToFile(file);
        }
        else if(e.getKeyCode() == KeyEvent.VK_V) {
            window.resetView();
            manager.displayIcon("<V> Reset View!", 2000);
        }
        else if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            manager.exit(0);
        }
        else if(e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
            window.screenshot();
        }
        else if(e.getKeyCode() == KeyEvent.VK_H || e.getKeyChar()=='?') {
            System.out.println(window.getTitle() + "\n" +
                               "    drag               = move a node\n" +
                               "    arrow keys         = pan the view\n" +
                               "    alt + arrow keys   = zoom in/out\n" +
                               "    mouse wheel        = zoom in/out\n" +
                               "    page up            = take a screenshot\n" +
                               "    escape             = exit\n" +
                               "    ctrl + O           = load a layout from a Yaml file\n" +
                               "    ctrl + S           = save the current layout to a Yaml file\n" +
                               "    v                  = reset the view to the default\n");
        }
    }

    public void keyTyped(KeyEvent e) { /* no-op */ }

    
    // ---------------- Mouse Events ---------------- //
    
    public void mouseClicked(MouseEvent e) { /* no-op */ }
    public void mouseEntered(MouseEvent e) { /* no-op */ }
    public void mouseExited(MouseEvent e) { /* no-op */ }

    /**
     * Tell the manager about the new mouse position and ask it to select any
     * Drawable at that position.
     */
    public void mousePressed(MouseEvent e) {
        PZWindow window = getWindow(e);
        PZManager manager = window.getManager();
        
        int x = window.getMX(e);
        int y = window.getMY(e);
        manager.setMousePos(x, y, false);
        manager.select(x, y);
    }

    /**
     * Tells the manager the mouse has been released, fires a "mouse_released"
     * event on the selected Drawable (if there was one), deselects any selected
     * Drawable, and applies any pan in progress.
     */
    public void mouseReleased(MouseEvent e) {
        PZWindow window = getWindow(e);
        PZManager manager = window.getManager();
        
        Drawable d = manager.getSelected();
        if(d != null) {
            translateMousePos(e);
            manager.fireDrawableEvent(d, e, "mouse_released");
        }
        
        manager.noteMouseUp();
        manager.deselect();
        
        // apply new panning, if any
        window.applyPanInProgress();
    }

    /**
     * Tells the manager about the mouse's current position, and handles the
     * dragging event.  If a node is not selected and the ALT key is down, then
     * the view will be panned.  If a node is selected, then that node will have
     * it drag(x, y) method called.
     */
    public void mouseDragged(MouseEvent e) {
        PZWindow window = getWindow(e);
        PZManager manager = window.getManager();
        
        int x = window.getMX(e);
        int y = window.getMY(e);
        manager.setMousePos(x, y, true);

        Drawable selNode = manager.getSelected();
        if(selNode == null) {
            if(e.getButton() == MouseEvent.BUTTON1 && e.isAltDown()) {
                // alt+left-click+drag = pan the screen
                int panX = x - manager.getMouseStartPos().x;
                int panY = y - manager.getMouseStartPos().y;
                window.setPanInProgress(panX, panY);
                return;
            }
        }
        else {
            dragNode(selNode, x, y);
        }
    }
    
    /**
     * Called when a node has been dragged.
     * 
     * @param selNode  the node being dragged
     * @param x        current x coordinate of the drag
     * @param y        current y coordinate of the drag
     */
    public void dragNode(Drawable selNode, int x, int y) {
        // if a node is selected, handle dragging it
        selNode.drag(x, y);
    }

    /** Tells the manager where the mouse is now. */
    public void mouseMoved(MouseEvent e) {
        PZWindow window = getWindow(e);
        PZManager manager = window.getManager();
        
        int x = window.getMX(e);
        int y = window.getMY(e);
        manager.setMousePos(x, y, false);
    }
    
    /**
     * Zoom out when the mouse wheel rotates down and vice versa.
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        PZWindow window = getWindow(e);
        int rotations = e.getWheelRotation();
        
        // zoom out when the mouse wheel rotates down
        while(rotations > 0) {
            window.zoomOut();
            rotations -= 1;
        }

        // zoom in when the mouse wheel rotates up
        while(rotations < 0) {
            window.zoomIn();
            rotations += 1;
        }
    }

    
    /**
     * Translate the mouse position to reflect the zoom level of the selected
     * window
     */
    protected void translateMousePos(MouseEvent e) {
        PZWindow window = getWindow(e);

        int x = window.getMX(e);
        int y = window.getMY(e);
        
        e.translatePoint(x - e.getX(), y - e.getY());
    }

    // ---------------- Window Events --------------- //
    
    public void windowActivated(WindowEvent e)   { /* no-op */ }
    public void windowClosed(WindowEvent e)      { /* no-op */ }
    public void windowClosing(WindowEvent e)     { /* no-op */ }
    public void windowDeactivated(WindowEvent e) { /* no-op */ }
    public void windowDeiconified(WindowEvent e) { /* no-op */ }
    public void windowIconified(WindowEvent e)   { /* no-op */ }
    public void windowOpened(WindowEvent e)      { /* no-op */ }
}
