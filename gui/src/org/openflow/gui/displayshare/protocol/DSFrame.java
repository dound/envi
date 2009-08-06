package org.openflow.gui.displayshare.protocol;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Communicates an image.
 * 
 * @author David Underhill
 */
public class DSFrame extends DSMessage {
    /** image data in jpg format */
    public final byte[] jpgFrameBytes;
    
    public DSFrame(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        jpgFrameBytes = baos.toByteArray();
    }
    
    public DSFrame(byte[] bytes) {
        this.jpgFrameBytes = bytes;
    }
    
    public DSFrame(int len, DataInput in) throws IOException {
        int jpgLen = len - super.length();
        jpgFrameBytes = new byte[jpgLen];
        in.readFully(jpgFrameBytes);
    }
    
    /** gets the type of the message */
    public DSMessageType getType() {
        return DSMessageType.FRAME;
    }
    
    /** total length of this message in bytes */
    public int length() {
        return super.length() + jpgFrameBytes.length;
    }
    
    /** sends the message over the specified output stream */
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.write(jpgFrameBytes);
    }
}