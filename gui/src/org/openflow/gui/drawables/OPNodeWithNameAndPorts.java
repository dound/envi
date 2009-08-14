package org.openflow.gui.drawables;

import java.awt.Color;
import java.awt.Graphics2D;

import org.openflow.gui.net.protocol.NodeType;
import org.openflow.util.string.StringOps;
import org.pzgui.Constants;
import org.pzgui.StringDrawer;
import org.pzgui.icon.Icon;
import org.pzgui.icon.ImageIcon;

public class OPNodeWithNameAndPorts extends SimpleNodeWithPorts {
    public OPNodeWithNameAndPorts(NodeType type, String name, long id, Icon icon) {
        this(type, name, 0, 0, id, icon);
    }
    
    public OPNodeWithNameAndPorts(NodeType type, String name, int x, int y, long id, Icon icon) {
        super(type, name, x, y, id, icon);
    }
    
    /** color to draw the name */
    private Color nameColor = Constants.COLOR_DEFAULT;
    
    /** a marker to draw on the node, if any */
    private Icon marker = null;
    
    /** gets the color to draw the name */
    public Color getNameColor() {
        return nameColor;
    }
    
    /** sets the color to draw the name */
    public void setNameColor(Color c) {
        nameColor = c;
    }

    /** returns the current marker (may be null) */
    public Icon getMarker() {
        return marker;
    }
    
    /** sets the marker to m (if m is null, the marker will be removed) */
    public void setMarker(Icon m) {
        marker = m;
    }
    
    /** Draw the object using super.drawObject() and then add the name in the middle */
    public void drawObject(Graphics2D gfx) {
        super.drawObject(gfx);

        // draw the marker,  if there is one
        Icon m = getMarker();
        if(m != null)
            m.draw(gfx, getX(), getY());
        
        // Check if the node has an ImageIcon as the icon
        Icon icon = getIcon();
        if (icon != null && icon instanceof ImageIcon)
           	return;
        
        String name = getName();
        if(name==null || name.length()==0)
            return;
        
        gfx.setColor(nameColor);
        gfx.setFont(Constants.FONT_DEFAULT);
        
        // compute how many lines to split the name onto
        int charsPerLine = getWidth() / gfx.getFontMetrics().charWidth('x') - 1;
        String nameToDraw = StringOps.splitIntoLines(name, charsPerLine);
        String[] lines = nameToDraw.split("\n");
        
        // compute where to start drawing the name
        int numLinesForName = lines.length;
        int lh = gfx.getFontMetrics().getHeight();
        int yOffset = -numLinesForName/2 * lh;
        if(numLinesForName % 2 == 0) yOffset += lh / 2;
        yOffset += lh / 4;
        
        for(String line : lines) {
            StringDrawer.drawCenteredString(line, gfx, getX(), getY() + yOffset);
            yOffset += lh;
        }
        
        gfx.setColor(Constants.COLOR_DEFAULT);
    }
    
    public void setName(String name) {
        super.setName(name);
    }
}
