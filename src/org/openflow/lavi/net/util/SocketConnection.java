package org.openflow.lavi.net.util;

import org.openflow.lavi.net.util.ByteBuffer;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Provides helper functions for setting up and sending and receiving binary 
 * and ASCII data over a TCP connection.
 * 
 * @author David Underihll
 */
public class SocketConnection extends ByteBuffer {
    /** default size of the internal socket buffer */
    private static final int DEFAULT_BUF_SIZE = 32768;
    
    /** the socket which connects us to the NOX server */
    public final Socket s;

    /** output stream to write to the socket */
    public final OutputStream out;

    /** input stream to read from the socket */
    public final DataInputStream in;
    
    /** 
     * Connect to the client on the specified port.
     * 
     * @param ip    the IP to connect to
     * @param port  the TCP port to connect on
     */
    public SocketConnection(String ip, int port) {
        this(ip, port, DEFAULT_BUF_SIZE);
    }
    
    /** 
     * Connect to the client on the specified port.  Use an internal buffer with 
     * the specified size.
     * 
     * @param ip    the IP to connect to
     * @param port  the TCP port to connect on
     * @param sz    size of the internal buffer
     */
    public SocketConnection(String ip, int port, int sz) {
        super(sz);
        
        // setup socket for listening for new clients connection requests
        Socket stmp;
        try {
            stmp = new Socket(ip, port);
        }
        catch(IOException e) {
            System.err.println( Integer.toString( port ) + ": " + e.getMessage() );
            s = null;
            out = null;
            in = null;
            return;
        }
        s = stmp;
        
        //try to establish the I/O streams: if we can't establish either, then close the socket
        OutputStream tmp;
        try {
            tmp = s.getOutputStream();
        } catch(IOException e) {
            System.err.println( "Client Socket Setup Error: " + e.getMessage() );
            tmp = null;
        } 
        out = tmp;

        DataInputStream tmp2;
        try {
            tmp2 = new DataInputStream(s.getInputStream());
        } catch(IOException e) {
            System.err.println( "Client Socket Setup Error: " + e.getMessage() );
            tmp2 = null;
        }
        in = tmp2;
    }
    
    /**
     * Returns the next unread received byte.
     */
    public synchronized byte nextByte() throws IOException {
        // grab a byte from the buffered data if any is available
        try {
            return super.nextByte();
        }
        catch(IOException e) {
            // otherwise get a new byte
            return in.readByte();
        }
    }
    
    /**
     * Buffers n bytes from the incoming TCP stream.  This call blocks until the
     * requested number of bytes has been buffered.
     * 
     * @param n  the number of bytes to buffer
     * @throws java.io.IOException  thrown if the socket fails
     */
    public synchronized void bufferBytes(int n) throws IOException {
        shiftBytesToFrontOfBuffer();
        
        // read in the requested number of bytes
        for(int i=0; i<n-bytesUsed(); i++) 
            addByte((byte)in.readByte());
    }
    
    /** Tries to write the two-byte int on the socket */
    public synchronized void sendShort(short value) throws IOException {
        out.write( (value >>  8) & 0x000000FF );
        out.write(  value        & 0x000000FF );
    }
    
    /** Tries to write the four-byte int on the socket */
    public synchronized void sendInt(int value) throws IOException {
        out.write(  value >> 24 );
        out.write( (value >> 16) & 0x000000FF );
        out.write( (value >>  8) & 0x000000FF );
        out.write(  value        &  0x000000FF );
    }

    /** Tries to write the string on the socket */
    public synchronized void sendString(String str) throws IOException {
    	for (byte b : str.getBytes()) {
            out.write(b);
    	}
    }

    /** Tries to write the eight-byte long on the socket */
    void sendLong(long l) throws IOException {
        sendInt( (int)((l & 0xFFFFFFFF00000000L) >> 32) );
        sendInt( (int)( l & 0x00000000FFFFFFFFL) );
    }
}
