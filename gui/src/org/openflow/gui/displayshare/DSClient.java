package org.openflow.gui.displayshare;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.openflow.gui.displayshare.protocol.DSFrame;
import org.openflow.gui.displayshare.protocol.DSMessage;
import org.openflow.gui.displayshare.protocol.DSMessageType;
import org.openflow.gui.displayshare.protocol.DSParams;
import org.openflow.gui.net.BackendConnection;
import org.openflow.gui.net.MessageProcessor;
import org.pzgui.icon.ImageIcon;

/**
 * DisplayShare client.  Connects to a server, requests images, and stores 
 * received image data into an ImageIcon object.
 * 
 * @author David Underhill
 */
public class DSClient extends BackendConnection<DSMessage>
                      implements MessageProcessor<DSMessage> {
    
    /** parameters to specify for the DisplayShare */
    private DSParams params;
    
    /** wrapper for the most recently received frame */
    private final ImageIcon icon = new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
    
    public DSClient(String ip, int port, DSParams params) {
        super(null /* process own messages */, ip, port);
        this.params = params;
    }

    /** 
     * If the connection goes up, then send the configuration info to the 
     * server.  Also prints a message to stdout for both connecting and
     * disconnecting. 
     */
    public void connectionStateChange(boolean connected) {
        String suffix = "to the DisplayShare server at " + getServerAddr() + ":" + getServerPort();
        if(connected) {
            System.out.println("now connected " + suffix);
            try {
                this.sendMessage(params);
            }
            catch(IOException e) {
                System.err.println("DisplayShare client unable to send initial parameters to server");
            }
        }
        else
            System.out.println("lost connection " + suffix);
    }

    /** decode the received message */
    public DSMessage decode(int len, DataInput in) throws IOException {
        return DSMessageType.decode(len, in);
    }

    /** process messages received from the DisplayShare server */
    public void process(DSMessage msg) {
        switch(msg.getType()) {
        case FRAME:
            processFrame((DSFrame)msg);
            return;
            
        case PARAMS:
            System.err.println("DisplayShare client received unexpected message type (server-only message): " + msg.toString());
            return;
            
        default:
            System.err.println("DisplayShare client received unexpected message type (unknown type): " + msg.toString());
        }
    }

    /** save the latest frame */
    private void processFrame(DSFrame frame) {
        ByteArrayInputStream bais = new ByteArrayInputStream(frame.jpgFrameBytes);
        try {
            BufferedImage img = ImageIO.read(bais);
            icon.setImage(img);
        }
        catch(IOException e) {
            System.err.println("DisplayShare client error: unable to decode received image: " + e.getMessage());
        }
    }
    
    /**
     * Gets the icon containing the image associated with this DisplayShare 
     * client.  The image contained within this icon will update as new frames
     * are received.
     */
    public ImageIcon getIcon() {
        return icon;
    }
}
