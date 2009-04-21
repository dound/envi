package org.openflow.gui.drawables;

import java.awt.Graphics2D;
import java.util.ArrayList;

import org.pzgui.icon.Icon;
import org.pzgui.layout.AbstractLayoutable;
import org.pzgui.layout.Vertex;

public class LayoutableIcon extends AbstractLayoutable implements Vertex<Link> {
    private Icon icon;
    private int w, h;
    private long id;
    
    public LayoutableIcon(long id, Icon icon, int x, int y) {
        this(id, icon, x, y, icon.getWidth(), icon.getHeight());
    }

    public LayoutableIcon(long id, Icon icon, int x, int y, int w, int h) {
        setPos(x, y);
        this.icon = icon;
        this.w = w;
        this.h = h;
        this.id = id;
    }
    
    public void drawObject(Graphics2D gfx) {
        icon.draw(gfx, getX(), getY(), w, h);
    }
    
    public boolean contains(int x, int y) {
        return icon.contains(x, y, getX(), getY());
    }
    
    private static final ArrayList<Link> EMPTY = new ArrayList<Link>();
    
    public Iterable<Link> getEdges() {
        return EMPTY;
    }
    
    public int getHeight() {
        return h;
    }

    public long getID() {
        return id;
    }
    
    public int getWidth() {
        return w;
    }
}
