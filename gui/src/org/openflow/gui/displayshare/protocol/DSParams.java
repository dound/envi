package org.openflow.gui.displayshare.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Describes what kind view of a GUI to share (and how often). 
 * 
 * @author David Underhill
 */
public class DSParams extends DSMessage {
    public final int msec_per_frame;
    public final int x, y, width, height;
    public final float zoom;
    
    public DSParams(int msec_per_frame, int x, int y, int width, int height, float zoom) {
        this.msec_per_frame = msec_per_frame;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.zoom = zoom;
    }
    
    public DSParams(DataInput in) throws IOException {
        msec_per_frame = in.readInt();
        x = in.readInt();
        y = in.readInt();
        width = in.readInt();
        height = in.readInt();
        zoom = in.readFloat();
    }
    
    /** gets the type of the message */
    public DSMessageType getType() {
        return DSMessageType.PARAMS;
    }
    
    /** total length of this message in bytes */
    public int length() {
        return super.length() + 24;
    }
    
    /** sends the message over the specified output stream */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(msec_per_frame);
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(width);
        out.writeInt(height);
        out.writeFloat(zoom);
    }
}
