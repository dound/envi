package org.openflow.lavi.net.util;

import java.io.IOException;

/**
 * Provides helper functions for reading binary and ASCII data from a byte 
 * buffer.
 * 
 * @author David Underihll
 */
public class ByteBuffer {
    /** buffering of data received from the network */
    public final byte[] buf;
    public int used = 0;
    public int offset = 0;
    
    /** 
     * Create a buffer with the specified size.
     * 
     * @param sz  size of the internal buffer
     */
    public ByteBuffer(int sz) {
        buf = new byte[sz];
    }
    
    public void shiftBytesToFrontOfBuffer() {
        // nothing to do if the bytes are already at the front of the buffer
        if(offset == 0)
            return;
        
        // shift bytes to the front of buf
        for(int i=0; i<used; i++)
            buf[i] = buf[i+offset];
        
        offset = 0;
    }
    
    public void addByte(byte b) {
        buf[offset + used] = b;
        used += 1;
    }
    
    /**
     * Reads a single byte from the stream (tries to pull from the buffered 
     * bytes if any are buffered).
     * 
     * @return the next unread byte
     * @throws java.io.IOException  thrown if the socket fails
     */
    public byte nextByte() throws IOException {
        // grab a byte from the buffered data if any is available
        if(used > 0) {
            used -= 1;
            return buf[offset++];
        }
            
        // otherwise no bytes are available
        throw new IOException("no bytes in buffer");
    }
    
    /** reads byte i to i+1 to form a short */
    public synchronized short nextShort() throws IOException {
        // convert to signed ints, clearing any bits set due to sign extension
        int a = nextByte() & 0x000000FF;
        int b = nextByte() & 0x000000FF;
        
        // create the short int
        return (short)(a<<8 | b);
    }
    
    /** reads byte i to i+1 to form a short (use for unsigned shorts which may use the MSB) */
    public synchronized int nextUint16AsInt() throws IOException {
        // clear any sign extended bits
        int ret = nextShort();
        return ret & 0x0000FFFF;
    }
    
    /** reads byte i to i+3 to form an int */
    public synchronized int nextInt() throws IOException {
        // convert to signed ints, clearing any bits set due to sign extension
        int a = nextByte() & 0x000000FF;
        int b = nextByte() & 0x000000FF;
        int c = nextByte() & 0x000000FF;
        int d = nextByte() & 0x000000FF;
        
        // create the int
        return a<<24 | b<<16 | c<<8 | d;
    }
    
    /** reads byte i to i+3 to form a long (use for unsigned ints which may use the MSB) */
    public synchronized long nextUintAsLong() throws IOException {
        long ret = 0;
        
        // convert to signed ints, clearing any bits set due to sign extension
        int a = nextByte() & 0x000000FF;
        int b = nextByte() & 0x000000FF;
        int c = nextByte() & 0x000000FF;
        int d = nextByte() & 0x000000FF;
        
        // create the int
        ret = ((long)a)<<24 | b<<16 | c<<8 | d;
        
        // clear any sign extended bits
        return ret & 0x00000000FFFFFFFFL;
    }
    
    /** reads byte i to i+5 to form a mac address returned as a byte-string */
    public synchronized String nextMAC() throws IOException {
        // convert to signed ints, clearing any bits set due to sign extension
        // and then store in a char (one byte) and concatenate them to get the 
        // byte-string for a mac
        return new String(new char[]{
            (char)(nextByte() & 0x000000FF),
            (char)(nextByte() & 0x000000FF),
            (char)(nextByte() & 0x000000FF),
            (char)(nextByte() & 0x000000FF),
            (char)(nextByte() & 0x000000FF),
            (char)(nextByte() & 0x000000FF)
        });
    }
    
    /** reads byte i to i+7 to form an eight byte long */
    public synchronized long nextLong() throws IOException {
        // convert to signed ints, clearing any bits set due to sign extension
        long a = nextByte() & 0x00000000000000FF;
        long b = nextByte() & 0x00000000000000FF;
        long c = nextByte() & 0x00000000000000FF;
        long d = nextByte() & 0x00000000000000FF;
        long e = nextByte() & 0x00000000000000FF;
        long f = nextByte() & 0x00000000000000FF;
        long g = nextByte() & 0x00000000000000FF;
        long h = nextByte() & 0x00000000000000FF;
        
        // create the long
        return a<<56 | b<<48 | c<<40 | d<<32 | e<<24 | f<<16 | g<<8 | h;
    }
    
    /** returns the number of bytes available at the end of the buffer */
    public int bytesAvailable() {
        return buf.length - offset - used;
    }
    
    /** returns the maximum number of bytes the buffer can hold */
    public int bytesMax() {
        return buf.length;
    }
    
    /** returns the number of bytes being stored in the buffer */
    public int bytesUsed() {
        return used;
    }
}
