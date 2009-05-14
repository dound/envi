package org.openflow.gui.fv;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.openflow.gui.drawables.Flow;
import org.openflow.gui.drawables.NodeWithPorts;
import org.pzgui.Drawable;
import org.pzgui.DrawableFilter;
import org.pzgui.PZWindow;
import org.pzgui.PZWindowEventListener;
import org.pzgui.layout.Layoutable;
import org.pzgui.layout.PZLayoutManager;

/**
 * Draws multiple topologies each as a 2D slices of a 3D area.
 * 
 * @author David Underhill
 */
public class FVLayoutManager extends PZLayoutManager {
    /** tracks the active connections and topologies */
    private final FVMultipleConnectionAndTopologyHandler mch;
    
    /** the slices being displayed */
    private final CopyOnWriteArrayList<DisplaySlice> displaySlices = new CopyOnWriteArrayList<DisplaySlice>();
    
    /**
     * Construct the FlowVisor GUI layout manager.
     * 
     * @param mch        will contain all connections and topologies
     */
    public FVLayoutManager(FVMultipleConnectionAndTopologyHandler mch) {
        super();
        this.mch = mch;
    }
    
    /** adds a slice to be displayed */
    public void addDisplaySlice(FVTopology topology) {
        DisplaySlice ds = new DisplaySlice();
        ds.addTopology(topology);
        displaySlices.add(ds);
        refreshSlices();
    }
    
    /** swap the order of two slices */
    private void swapDisplaySlices(int i, int j) {
        if(i < 0 || j < 0 || i >= displaySlices.size() || j >= displaySlices.size())
            return;
        DisplaySlice tmp = displaySlices.get(i);
        displaySlices.set(i, displaySlices.get(j));
        displaySlices.set(j, tmp);
        refreshSlices();
    }
    
    /** force the slices layout to be refreshed as if the window had been resized */ 
    private void refreshSlices() {
        if(windows.size() > 0)
            windows.get(0).componentResized(null);
    }
    
    /**
     * Augments the superclass implementation by replacing the default event
     * listener with one which properly handles dragging nodes in slices.
     */
    public void attachWindow(final PZWindow w, boolean addDefaultEventListener) {
        super.attachWindow(w, false);
        
        if(addDefaultEventListener) {
            w.addEventListener(new PZWindowEventListener() {
                /** */
                public void mouseReleased(MouseEvent e) {
                    super.mouseReleased(e);
                    if(e.getButton() != MouseEvent.BUTTON1) {
                        PZWindow window = getWindow(e);
                        int x = window.getMX(e);
                        int y = window.getMY(e);
                        for(DisplaySlice ds : displaySlices) {
                            if(x>=ds.getXOffset() && x <=ds.getXOffset()+ds.getWidth() &&
                               y>=ds.getYOffset() && y <=ds.getYOffset()+ds.getHeight()) {
                                showContextMenu(ds, x, y);
                            }
                        }
                    }
                }

                /** take into account slice offsets when dragging within slices */
                public void dragNode(Drawable selNode, int x, int y) {
                    if(sliceDrawableSelectedIn != null) {
                        x -= sliceDrawableSelectedIn.getXOffset();
                        y -= sliceDrawableSelectedIn.getYOffset();
                    }
                    
                    super.dragNode(selNode, x, y);
                }
            });
        }
    }
    
    /** Augment the superclass implementation by drawing the slice planes */
    public void preRedraw(PZWindow window) {
        super.preRedraw(window);
        Graphics2D gfx = window.getDisplayGfx();
        if(gfx == null)
            return;
        
        for(DisplaySlice ds : displaySlices)
            ds.draw(gfx);
    }
    
    /** 
     * Draws the items associated with d which need to be drawn before it in the
     * appropriate plane(s) based on the slice(s) it lives in. 
     */
    protected void drawBeforeObject(Graphics2D gfx, Drawable d) {
        drawWrap(gfx, d, true);
    }
    
    /** 
     * Draws d in the appropriate plane(s) based on the slice(s) it lives in.
     */
    protected void drawObject(Graphics2D gfx, Drawable d) {
        drawWrap(gfx, d, false);
    }
    
    /**
     * Applies the necessary slice transformation and then draws d.
     * 
     *  @param gfx     where to draw
     *  @param d       what to draw
     *  @param before  if true, d.drawBeforeObject(gfx) is called, else d.drawObject(gfx)
     */
    private void drawWrap(Graphics2D gfx, Drawable d, boolean before) {
        if(!(d instanceof Layoutable)) {
            draw(gfx, d, before);
            return;
        }
        
        Layoutable l = (Layoutable)d;
        for(DisplaySlice ds : displaySlices) {
            if(ds.isVisible() && ds.hasNode(l.getID())) {
                ds.apply(gfx, d);
                draw(gfx, d, before);
                ds.unapply(gfx, d);
            }
        }
    }
    
    /**
     * Draws d.
     * 
     *  @param gfx     where to draw
     *  @param d       what to draw
     *  @param before  if true, d.drawBeforeObject(gfx) is called, else d.drawObject(gfx)
     */
    private void draw(Graphics2D gfx, Drawable d, boolean before) {
        if(before)
            d.drawBeforeObject(gfx);
        else
            d.drawObject(gfx);
    }
    
    /** returns the number of slices which are visible */
    private int numVisibleSlices() {
        int ret = 0;
        for(DisplaySlice ds : displaySlices)
            if(ds.isVisible())
                ret += 1;
        
        return ret;
    }
    
    /**
     * Augments the collision detection algorithm to work with slices.
     */
    public Drawable selectFrom(int x, int y, DrawableFilter filter) {
        for(int i=0; i<displaySlices.size(); i++) {
            DisplaySlice ds = displaySlices.get(i);
            Drawable d = super.selectFrom(x-ds.getXOffset(), y-ds.getYOffset(), filter);
            if(d != null) {
                // make sure the drawable we got is actually being shown in this plane
                if(d instanceof NodeWithPorts) {
                    if(!ds.hasNode(((NodeWithPorts)d).getID()))
                        continue;
                }
                else if(d instanceof Flow) {
                    Flow f = (Flow)d;
                    if(f.getPath().length==0 || !ds.hasNode(f.getPath()[0].node.getID()))
                        continue;
                }
                else if(i != 0) {
                    // for Drawables which do not appear in planes, only select 
                    // them if the yOffset is 0 (since they are not translated)
                    continue;
                }
                
                sliceDrawableSelectedIn = ds; 
                return d;
            }
        }
        
        return null;
    }
    
    /** the slice in which the currently selected Drawable is in, if any */
    private DisplaySlice sliceDrawableSelectedIn = null;
    
    /**
     * Augments the superclass implementation so that it computes layout 
     * positions based on slice size instead of window size.  It causes slices
     * to update their transforms according to the new layout area.
     */
    public void setLayoutSize(int width, int height) {
        int sliceHeight = height/Math.max(1, numVisibleSlices());
        super.setLayoutSize(width, sliceHeight);
        
        int i = 0;
        for(DisplaySlice ds : displaySlices) {
            if(ds.isVisible()) {
                ds.updateDisplayTransform(0, sliceHeight * i, width, sliceHeight);
                i += 1;
            }
        }
    }
    
    /** create and display a context menu for ds at the specified location */
    private void showContextMenu(final DisplaySlice ds, final int x, final int y) {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false); // force the menu on top of the dashboard
        JPopupMenu cmnu = new JPopupMenu();
        
        JMenu mnu;
        JMenuItem mnui;
        
        final int index = displaySlices.indexOf(ds);
        if(index == -1)
            return;
        
        // move slices up or down
        mnui = new JMenuItem("Move Up", KeyEvent.VK_U);
        mnui.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                swapDisplaySlices(index, index-1);
            }});
        mnui.setEnabled(index > 0);
        cmnu.add(mnui);
        
        mnui = new JMenuItem("Move Down", KeyEvent.VK_D);
        mnui.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                swapDisplaySlices(index, index+1);
            }});
        mnui.setEnabled(index < displaySlices.size() - 1);
        cmnu.add(mnui);
        
        cmnu.add(new JSeparator());
        
        // Create new slices
        mnu = new JMenu("Create");
        mnu.setMnemonic(KeyEvent.VK_C);
        for(int i=0; i<mch.getNumTopologies(); i++) {
            final FVTopology t = (FVTopology)mch.getTopology(i);
            mnui = new JMenuItem(t.getName());
            mnui.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addDisplaySlice(t);
                    
                }});
            mnu.add(mnui);
        }
        cmnu.add(mnu);
        
        // Delete this slice
        mnui = new JMenuItem("Delete this Slice");
        mnui.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displaySlices.remove(ds);
                refreshSlices();
            }});
        // cannot delete the last slice, or the last visible slice
        mnui.setEnabled(displaySlices.size()>0 && numVisibleSlices()>1);
        cmnu.add(mnui);
        
        cmnu.add(new JSeparator());
        
        // Hide this slice
        mnui = new JMenuItem("Hide this Slice", KeyEvent.VK_H);
        mnui.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ds.setVisible(false);
                refreshSlices();
            }});
        mnui.setEnabled(numVisibleSlices() > 1); // cannot hide the last slice
        cmnu.add(mnui);
        
        // Show hidden slices
        mnu = new JMenu("Show Hidden Slices");
        mnu.setMnemonic(KeyEvent.VK_S);
        boolean enabled = false;
        for(final DisplaySlice tds : displaySlices) {
            if(!tds.isVisible()) {
                mnui = new JMenuItem(tds.getName());
                mnui.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        tds.setVisible(true);
                        refreshSlices();
                    }});
                mnu.add(mnui);
                enabled = true;
            }
        }
        mnu.setEnabled(enabled);
        cmnu.add(mnu);
        
        cmnu.add(new JSeparator());
        
        // Change which topologies are on this slice
        mnu = new JMenu("Topologies on this Display Slice");
        mnu.setMnemonic(KeyEvent.VK_T);
        for(int i=0; i<mch.getNumTopologies(); i++) {
            final FVTopology t = (FVTopology)mch.getTopology(i);
            final JCheckBoxMenuItem mnucb = new JCheckBoxMenuItem(t.getName());
            mnucb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(ds.hasTopology(t)) {
                        ds.removeTopology(t);
                        mnucb.setSelected(false);
                    }
                    else {
                        ds.addTopology(t);
                        mnucb.setSelected(true);
                    }
                }});
            mnucb.setSelected(ds.hasTopology(t));
            mnu.add(mnucb);
        }
        cmnu.add(mnu);
        
        cmnu.show(windows.get(0), x, y);
    }
}
