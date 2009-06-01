package org.openflow.gui.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.util.LinkedList;
import java.util.Vector;

import org.openflow.gui.net.protocol.FlowType;
import org.openflow.util.FlowHop;
import org.openflow.util.Pair;
import org.pzgui.AbstractDrawable;
import org.pzgui.Constants;
import org.pzgui.math.Vector2f;

/**
 * A flow of packets along some path through some set of node-port pairs.
 * 
 * @author David Underhill
 */
public class Flow extends AbstractDrawable {
    /** whether flows may be selected or highlighted */
    public static boolean ALLOW_FLOW_SELECTION = false;
    
    private FlowType type;
    private int flowID;
    
    /** the nodes from the flow's source to its destination */
    private FlowHop[] path;
    
    /** 
     * Segments of the flow which should not be drawn (a segment is defined as 
     * the link between two adjacent hops.
     */
    private LinkedList<Pair<FlowHop, FlowHop>> segmentsToIgnore = new LinkedList<Pair<FlowHop, FlowHop>>();
    
    /** 
     * Creates a flow between two endpoints.
     * 
     * @param path  the path being taken by this flow from src to dst
     */
    public Flow(FlowType type, int flowID, FlowHop[]  path) {
        this.type = type;
        this.flowID = flowID;
        this.path = path;
    }
    
    /** Gets the type of this flow */
    public FlowType getType() {
        return type;
    }
    
    /** Gets the ID of this flow */
    public int getID() {
        return flowID;
    }
    
    /** Gets the path of this flow */
    public FlowHop[] getPath() {
        return path;
    }
    
    /** whether a flow contains a particular segment */
    
    /** whether to draw a given segment */
    private boolean shouldDrawSegment(FlowHop from, FlowHop to) {
        for(Pair<FlowHop, FlowHop> p : segmentsToIgnore)
            if(p.a.equals(from) && p.b.equals(to))
                return false;
        
        return true;
    }
    
    /** whether this flow contains a particular segment */
    public boolean hasSegment(Pair<FlowHop, FlowHop> s) {
        for(int i=0; i<path.length-1; i++) {
            FlowHop a = path[i];
            FlowHop b = path[i];
            if(s.a.equals(a) && s.b.equals(b))
                return true;
        }
        
        return false;
    }
    
    /** add a segment to ignore */
    public void ignoreSegment(Pair<FlowHop, FlowHop> s) {
        segmentsToIgnore.add(s);
    }

    /** stop ignoring a particular segment */
    public void unignoreSegment(Pair<FlowHop, FlowHop> s) {
        segmentsToIgnore.remove(s);
    }
    
    
    // ------------------- Drawing ------------------ //
    
    /** whether flows should be animated */
    public static final boolean ANIMATE = true;
    
    /** radius of circles which make up the flow */
    public static final int POINT_SIZE = 20;
    
    /** gap between points */
    private static final int GAP_BETWEEN_POINTS = POINT_SIZE;
    
    /** how much to offset the points per second */
    private static final double MOVING_AMOUNT_PER_SEC = POINT_SIZE;
    
    /** offset due to the current animation, if any */
    private double movingOffset = 0.0;
    
    /** when this flow was last redrawn */
    private long lastRedraw = System.currentTimeMillis();
    
    /** color of the interior of the circles making up the flow */
    private Paint paintConn = Color.BLUE;
    
    /** color of the exterior of the circles making up the flow */
    private Color colorConnBorder = Color.BLACK;
    
    /** Draw the flow */
    public void drawObject(Graphics2D gfx) {
        // ignore paths which doesn't have at least a start and endpoint
        if(path.length <= 1 )
           return;
                
        // determine the offset to make the line appear to be moving
        if(ANIMATE) {
            movingOffset += Flow.MOVING_AMOUNT_PER_SEC * ((System.currentTimeMillis() - lastRedraw) / 1000.0);
            movingOffset %= (getPointSize() + GAP_BETWEEN_POINTS);
            lastRedraw = System.currentTimeMillis();
        }
        
        // advance the sliding back animation
        if(selSlidingBack.hasSelection()) {
            Vector2f diff = new Vector2f(selSlidingBack.dragPos, selSlidingBack.selPos);
            Vector2f slidingBackDir = Vector2f.makeUnit(diff);
            Vector2f slidingBackAmt = slidingBackDir.multiply((float)Flow.MOVING_AMOUNT_PER_SEC);
            if(slidingBackAmt.lengthSq() > diff.lengthSq())
                selSlidingBack.dragPos.subtract(slidingBackAmt);
            else
                selSlidingBack.clearSelection();
        }

        // draw the flow
        int prevPathEltOn = 0;
        Vector2f to = null;
        for(int pathEltOn=1; pathEltOn<path.length; pathEltOn++) {
            FlowHop prev = path[pathEltOn-1];
            FlowHop next = path[pathEltOn];
            
            // only need to draw legs of non-zero distance
            if(prev.node == next.node) {
                boundingBoxesNew.add(null); // placeholder bounding box
                continue;
            }
            
            // skip segments we aren't supposed to draw
            if(!shouldDrawSegment(prev, next)) {
                boundingBoxesNew.add(null); // placeholder bounding box
                continue;
            }
            
            Vector2f from = (to != null) ? to : new Vector2f(prev.node.getX(), prev.node.getY());
            to = new Vector2f(next.node.getX(), next.node.getY());
            
            // if the flow is being dragged off the "to" node, then relocate "to" 
            // to the location it has been dragged to
            if(selNow.isDraggingFromNode(pathEltOn))
                to = selNow.dragPos;
            else if(selSlidingBack.isDraggingFromNode(pathEltOn))
                to = selSlidingBack.dragPos;
            
            // draw the leg between the two current endpoints
            if(selNow.isSelectedBetween(prevPathEltOn)) {
                drawLine(gfx, from, selNow.dragPos, prevPathEltOn, pathEltOn);
                drawLine(gfx, selNow.dragPos, to, prevPathEltOn, pathEltOn);
            }
            else if(selSlidingBack.isSelectedBetween(prevPathEltOn)) {
                drawLine(gfx, from, selSlidingBack.dragPos, prevPathEltOn, pathEltOn);
                drawLine(gfx, selSlidingBack.dragPos, to, prevPathEltOn, pathEltOn);
            }
            else
                drawLine(gfx, from, to, prevPathEltOn, pathEltOn);
            
            prevPathEltOn = pathEltOn;
        }
        
        // update the bounding box and restore painting defaults
        boundingBoxes = boundingBoxesNew;
        boundingBoxesNew = new Vector<PathInfoPolygon>();
        gfx.setStroke(Constants.STROKE_DEFAULT);
        gfx.setPaint(Constants.PAINT_DEFAULT);
    }
    
    /**
     * Draw a line between actualFrom and actualTo.
     * 
     * @param gfx             the graphics to draw the line with
     * @param actualFrom      starting point of the line
     * @param actualTo        finishing point of the line
     * @param startPathIndex  index of the element in path which starts this line
     * @param endPathIndex    index of the element in path which ends this line
     */
    private void drawLine(Graphics2D gfx, 
                          Vector2f actualFrom, Vector2f actualTo, 
                          int startPathIndex, int endPathIndex) {
        // do not draw lines smaller than one dot
        if(Vector2f.distanceSq(actualFrom.x, actualFrom.y, actualTo.x, actualTo.y) < Flow.POINT_SIZE*Flow.POINT_SIZE) {
            boundingBoxesNew.add(null); // placeholder bounding box
            return;
        }
        
        // nudge from and to so the points are centered on the line between from and to
        Vector2f from = new Vector2f(actualFrom.x - getPointSize()/2, actualFrom.y - getPointSize()/2);
        Vector2f to = new Vector2f(actualTo.x - getPointSize()/2, actualTo.y - getPointSize()/2);
        Vector2f dir = Vector2f.subtract(actualTo, actualFrom);
        
        // determine vector to take us from point to point
        int d = getPointSize() + GAP_BETWEEN_POINTS;
        Vector2f unitDir = Vector2f.makeUnit(dir);
        Vector2f dp = Vector2f.multiply(unitDir, d);
        float x = from.x;
        float y = from.y;
        
        // don't let the movement take us beyond to
        to = to.clone();
        to.x -= dp.x;
        to.y -= dp.y;

        // apply the line movement offset
        Vector2f vMovingOffset = Vector2f.multiply(unitDir, (float)movingOffset);
        x += vMovingOffset.x;
        y += vMovingOffset.y;

        // save initial points
        float initX = x, initY = y;

        // loop until we are completely on the other side of "to"
        boolean movingAway = false;
        float dToSq, dToSqPrev = Float.POSITIVE_INFINITY, dx, dy;
        do {
            drawCircle(gfx, x, y);
            x += dp.x;
            y += dp.y;
            
            // check to see how far we are from our destination now
            dx = to.x - x;
            dy = to.y - y;
            dToSq = dx*dx + dy*dy;
            
            // determine whether the next mov would make us closer or not
            movingAway = dToSq > dToSqPrev;
            dToSqPrev = dToSq;
        }
        while( !movingAway );

        // create a bounding box for this segment of the line and order them in same order as
        // the path (e.g. dst at bottom which corresponds to index 0)
        
        // get a vector perpendicular to the line with length of half a dot
        Vector2f perp = new Vector2f(unitDir.y, -unitDir.x).multiply(getPointSize() / 2.0f + 4.0f);
        
        // build the bounding box
        float o = getPointSize() / 2.0f;
        int[] bx = new int[]{ (int)(initX - perp.x + o), (int)(initX + perp.x + o), (int)(x + perp.x + o), (int)(x - perp.x + o) };
        int[] by = new int[]{ (int)(initY - perp.y + o), (int)(initY + perp.y + o), (int)(y + perp.y + o), (int)(y - perp.y + o) };
        PathInfoPolygon boundingBox = new PathInfoPolygon(bx, by, bx.length, startPathIndex, endPathIndex);
        boundingBoxesNew.add(boundingBox);
    }
    
    /**
     * Draw a circle at the specified location.
     * 
     * @param gfx  the graphics to draw the line with
     * @param x    x coordinate to center the circle at
     * @param y    y coordinate to center the circle at
     */
    private void drawCircle(Graphics2D gfx, double x, double y) {
        int size = getPointSize();
        
        gfx.setPaint(paintConn);
        gfx.fillOval((int)x, (int)y, size, size);

        gfx.setPaint(colorConnBorder);
        gfx.drawOval((int)x, (int)y, size, size);
    }
    
    /** Gets the width of the line within which segments of the flow are drawn */
    public int getPointSize() {
        return POINT_SIZE;
    }
    
    /** Gets the paint for this flow */
    public Paint getPaint() {
        return paintConn;
    }
    
    /** Sets the paint for this flow */
    public void setPaint(Paint p) {
        paintConn = p;
    }
    
    
    // ------------------ Selection ----------------- //
    
    /** Tracks what part of a flow is selected, if any. */ 
    private class FlowSelection {
        /** index of a NodePathPair in path */
        private int index;
        
        /** 
         * If true, then the flow is selected between two ports (i.e., dragging
         * to a new intermediate node).  If false, then the flow is selected on 
         * a port (i.e., dragging away from a node)
         */
        private boolean between;
        
        /** The point at which the flow was selected. */
        public Vector2f selPos = new Vector2f(-1f, -1f);
        
        /** The point at which the flow was dragged to. */
        public Vector2f dragPos = new Vector2f(-1f, -1f);
        
        /** No selection by default. */
        public FlowSelection() {
            index = -1;
        }

        /** Returns true if some part of the flow is selected. */
        public boolean hasSelection() {
            return index != -1;
        }

        /** Select a flow at a particular node/port. */
        public void selectAtNode(int nodeToSelectAt, int x, int y) {
            index = nodeToSelectAt;
            between = false;
            selPos.set(x, y);
            dragPos.set(x, y);
        }
        
        /** Select a flow at a between two path elements. */
        public void selectBetweenNodes(int nodeBefore, int nodeAfter, int x, int y) {
            index = Math.min(nodeBefore, nodeAfter);
            between = true;
            selPos.set(x, y);
            dragPos.set(x, y);
        }
        
        /**
         * Returns true if the specified leg in the path is part of a 
         * selection (leg 0 is between path[0] and path[1], etc.).
         */
        public boolean isSelectedBetween(int legIndex) {
            if(between)
                return legIndex == index;
            
            return false;
        }
        
        /**
         * Returns true if the specified flow is being dragged away from the
         * specified node.
         */
        public boolean isDraggingFromNode(int nodeIndex) {
            if(!between)
                return nodeIndex == index;
            
            return false;
        }
        
        /** Clear the selection */
        public void clearSelection() {
            index = -1;
        }
        
        /** Copy the values of s into this FlowSelection. */
        public void set(FlowSelection s) {
            index = s.index;
            between = s.between;
            selPos.set(s.selPos.x, s.selPos.y);
            dragPos.set(s.dragPos.x, s.dragPos.y);
        }
    }
    
    /** description of the current part of the flow which is selected/hovered */
    private FlowSelection selNow = new FlowSelection();
    
    /** description of a selection which is sliding back into place */
    private FlowSelection selSlidingBack = new FlowSelection();
    
    /** a bounding box along with the index of the path element it corresponds to */
    private class PathInfoPolygon extends Polygon {
        /** the path index associated with the start of this bounding box */
        public final int startPathIndex;
        
        /** the path index associated with the end of this bounding box */
        public final int endPathIndex;
        
        public PathInfoPolygon(int[] xs, int[] ys, int len, int startPathIndex, int endPathIndex) {
            super(xs, ys, len);
            this.startPathIndex = startPathIndex;
            this.endPathIndex = endPathIndex;
        }
    }
    
    /** the set of bounding boxes which describe the area used by the flow drawing */
    private Vector<PathInfoPolygon> boundingBoxes = new Vector<PathInfoPolygon>();
    
    /** the set of bounding boxes which describe the area used by the flow drawing (being put together) */
    private Vector<PathInfoPolygon> boundingBoxesNew = new Vector<PathInfoPolygon>();

    /** returns true if the specified coordinates are in the area covered by this flow */
    public boolean contains(int x, int y) {
        return isWithin(x, y, false);
    }

    /**
     * Returns true if the specified coordinates are in the area covered by this
     * flow.  If select is true, and this returns true, then the flow will be
     * selected too.
     * 
     * @param x       the x coordinate
     * @param y       the y coordinate
     * @param select  whether to select the flow if x,y is within the flow
     * 
     * @return  true if x,y is in the area covered by the flow
     */
    public boolean isWithin(int x, int y, boolean select) {
        if(!ALLOW_FLOW_SELECTION)
            return false; 
        
        // test to see if a rectangle around x, y intersects the bounding lines
        // at the center of the flow graphic
        for( int i=0; i<boundingBoxes.size() && i<path.length-1; i++ ) {
            PathInfoPolygon bb = boundingBoxes.get(i);
            if(bb!=null && bb.contains(x, y)) {
                // select at a node if we are over a node, otherwise select the flow between nodes
                if(path[bb.endPathIndex-1].node.contains(x, y))
                    selNow.selectAtNode(bb.startPathIndex, x, y);
                else if(path[bb.endPathIndex].node.contains(x, y))
                    selNow.selectAtNode(bb.endPathIndex-1, x, y);
                else
                    selNow.selectBetweenNodes(bb.startPathIndex, bb.endPathIndex, x, y);
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Sets whether this flow is selected - should only be directly used when
     * deselecting the flow (otherwise use selectFlow()).  If selected is false,
     * then the current selection will slide back to its original position.
     */
    public void setSelected(boolean selected) {
        if( isSelected() == selected )
            return;
        
        super.setSelected(selected);
        if(!selected && selNow.hasSelection()) {
            selSlidingBack.set(selNow);
            selNow.clearSelection();
        }
    }
}
