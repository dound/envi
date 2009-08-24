package org.openflow.gui.displayshare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.openflow.gui.displayshare.protocol.DSFrame;
import org.openflow.gui.displayshare.protocol.DSParams;
import org.openflow.gui.net.CountingDataInputStream;
import org.pzgui.PZManager;
import org.pzgui.PZWindow;
import org.pzgui.math.Vector2i;

/**
 * DisplayShare server.  Listens for a DisplayShare client to connect, tell it 
 * what it wants to see, and then periodically communicates a snapsot of the GUI
 * to the client.
 * 
 * @author David Underhill
 */
public class DSServer extends PZWindow {
    /** minimum time between frames (limits refresh rate) */
    private int msec_per_frame = 0;
    
    /** when the frame was last redrawn */
    private long timeLastFrameSent = 0;
    
    /** handles the TCP connection to the client */
    private ClientHandler clientHandler;
    
    /** whether this window can be made visible */
    private final boolean mayBeVisible;
    
    /** whether to send the next frame */
    private boolean sendNextFrame = false;
    
    /**
     * A thread to handle DSServer clients.
     */
    private class ClientHandler extends Thread {
        /** the port this server listens on */
        private final short port;
        
        /** the socket which connects us to the NOX server */
        public final ServerSocket server;
        
        /** the output socket when a client is connected */
        private DataOutputStream out = null;
        
        /** the currently connected client */
        private Socket s;
        
        public ClientHandler(short port) throws IOException {
            this.port = port;
            server = new ServerSocket(port);
        }
        
        public void run() {
            while(true)
                handleClient();
        }
        
        private void handleClient() {
            // wait for a client to connect and send us configuration params
            try {
                System.out.println("DisplayShare server listening on port " + port);
                out = null;
                s = server.accept();
                updateConfiguration(readParams());
            } catch(IOException e) {
                System.err.println("DisplayShare server accept() failed: " + e.getMessage());
                return;
            }
            
            // send the client the requested data until it disconnects
            try {
                out = new DataOutputStream(s.getOutputStream());
            }
            catch(IOException e) {
                System.err.println("DisplayShare server send frame setup failed: " + e.getMessage());
                out = null;
                return;
            }
            
            // wait until notifyClientLost() is called
            while(out != null) {
                synchronized(this) {
                    try { wait(); } catch(InterruptedException e) {}
                }
            }
        }
        
        /** tell the handler that the client connection has been terminated */
        public void notifyClientLost() {
            if(out == null)
                return;
            else
                out = null;
            
            try {
                s.close();
            }
            catch(IOException e) { /* ignore */ }
            
            this.notify();
        }
        
        /** get the parameters for the display from the client */
        private DSParams readParams() {
            try {
                final CountingDataInputStream in = new CountingDataInputStream(s.getInputStream());
                in.readShort(); // ignore the length field
                in.readByte(); // ignore the type field
                return new DSParams(in);
            } catch(IOException e) {
                System.err.println("DisplayShare server unable to read parameters from client: " + e.getMessage());
                return null;
            }
        }
    }
    
    /** create a new DSServer listening on the specified port */
    public DSServer(PZManager manager, int screenX, int screenY, int width, int height, int drawX, int drawY, final short port) throws IOException {
        this(manager, screenX, screenY, width, height, drawX, drawY, port, false);
    }
    
    /** create a new DSServer listening on the specified port */
    public DSServer(PZManager manager, int screenX, int screenY, int width, int height, int drawX, int drawY, final short port, boolean mayBeVisible) throws IOException {
        super(manager, screenX, screenY, width, height, drawX, drawY);
        this.mayBeVisible = mayBeVisible;
        clientHandler = new ClientHandler(port);
        clientHandler.start();
    }
    
    /**
     * Wraps super.redraw() to ensure that the refresh rate is no faster than
     * needed.
     */
    public void redraw() {
        // determine whether the next frame should be sent now
        sendNextFrame = clientHandler.out!=null && timeLastFrameSent + msec_per_frame <= System.currentTimeMillis();
        
        // no reason to draw if it is too soon or nobody is using the display
        if((mayBeVisible && isVisible()) || sendNextFrame)
            super.redraw();
    }
    
    /**
     * Send the new canvas to the connected client, if any.  Also refresh the
     * window if it is visible.
     */
    protected void refreshCanvas() {
        DataOutputStream out = clientHandler.out;
        if(out != null && sendNextFrame) {
            try {
                timeLastFrameSent = System.currentTimeMillis();
                sendNextFrame = false;
                new DSFrame(super.img).write(out);
            } catch (IOException e) {
                clientHandler.notifyClientLost();
            }
        }
        
        if(mayBeVisible && isVisible())
            super.refreshCanvas();
    }
    
    /** only has an effect if the object was constructed with mayBeVisible=true */
    public void setVisible(boolean b) {
        if(mayBeVisible)
            super.setVisible(b);
    }
    
    /** updates the PZWindow's display configuration based on p */
    private void updateConfiguration(DSParams p) {
        setSize(p.width, p.height);
        setZoom(p.zoom);
        setPos(new Vector2i(p.x, p.y));
        msec_per_frame = p.msec_per_frame;
    }
}
