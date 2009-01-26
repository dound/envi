package org.openflow.lavi.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Provides helper functions for setting up and sending and receiving binary 
 * and ASCII data over a TCP connection.
 * 
 * @author David Underihll
 */
public class SocketConnection implements DataInput, DataOutput {
    /** the socket which connects us to the NOX server */
    public final Socket s;

    /** output stream to write to the socket */
    public final DataOutputStream out;

    /** input stream to read from the socket */
    public final CountingDataInputStream in;
    
    /** 
     * Connect to the client on the specified port.
     * 
     * @param ip    the IP to connect to
     * @param port  the TCP port to connect on
     */
    public SocketConnection(String ip, int port) {
        // setup socket for listening for new clients connection requests
        Socket stmp;
        try {
            stmp = new Socket(ip, port);
        }
        catch(IOException e) {
            System.err.println(Integer.toString(port) + ": " + e.getMessage());
            s = null;
            out = null;
            in = null;
            return;
        }
        s = stmp;
        
        //try to establish the I/O streams: if we can't establish either, then close the socket
        DataOutputStream tmp;
        try {
            tmp = new DataOutputStream(s.getOutputStream());
        } catch(IOException e) {
            System.err.println("Client Socket Setup Error: " + e.getMessage());
            tmp = null;
        } 
        out = tmp;

        CountingDataInputStream tmp2;
        try {
            tmp2 = new CountingDataInputStream(s.getInputStream());
        } catch(IOException e) {
            System.err.println("Client Socket Setup Error: " + e.getMessage());
            tmp2 = null;
        }
        in = tmp2;
    }

    /**
     * Reads the specified number of bytes from in to form a string.  A zero
     * byte will be interpreted as the end of the string and the remaining bytes
     * will be ignored (still read from in, but ignored).
     * 
     * @param in           buffer to read from
     * @param bytesToRead  number of bytes to read
     * 
     * @return the string formed from the read bytes
     */
    public static String readString(DataInput in, int bytesToRead) throws IOException {
        // read the bytes which make up the string
        byte[] buf = new byte[bytesToRead];
        int len = 0;
        for(int i=0; i<bytesToRead; i++) {
            buf[i] = in.readByte();
            if(len==0 && buf[i]==0)
                len = i;
        }
        
        // handle the empty string case
        if(len == 0)
            return new String();
        
        // copy it into an array with no extra space so the string can be constructed
        byte[] strBuf = new byte[len];
        System.arraycopy(buf, 0, strBuf, 0, len);
        return new String(strBuf);
    }
    
    /**
     * Reads the specified number of bytes to form a string.  A zero byte will 
     * be interpreted as the end of the string and the remaining bytes will be 
     * ignored (still read, but ignored).
     * 
     * @param bytesToRead  number of bytes to read
     * 
     * @return the string formed from the read bytes
     */
    public String readString(int bytesToRead) throws IOException {
        return SocketConnection.readString(in, bytesToRead);
    }
    
    public long getBytesRead()                                           { return in.getBytesRead(); }
    public void readFully(byte b[])                   throws IOException { in.readFully(b); }
    public void readFully(byte b[], int off, int len) throws IOException { in.readFully(b, off, len); }
    public int skipBytes(int n)                       throws IOException { return in.skipBytes(n); }
    public boolean readBoolean()                      throws IOException { return in.readBoolean(); }
    public byte readByte()                            throws IOException { return in.readByte(); }
    public int readUnsignedByte()                     throws IOException { return in.readUnsignedByte(); }
    public short readShort()                          throws IOException { return in.readShort(); }
    public int readUnsignedShort()                    throws IOException { return in.readUnsignedShort(); }
    public char readChar()                            throws IOException { return in.readChar(); }
    public int readInt()                              throws IOException { return in.readInt(); }
    public long readLong()                            throws IOException { return in.readLong(); }
    public float readFloat()                          throws IOException { return in.readFloat(); }
    public double readDouble()                        throws IOException { return in.readDouble(); }
    public String readLine()                          throws IOException { return in.readLine(); }
    public String readUTF()                           throws IOException { return in.readUTF(); }

    public void write(int b)                          throws IOException { out.write(b); }
    public void write(byte b[])                       throws IOException { out.write(b); }
    public void write(byte b[], int off, int len)     throws IOException { out.write(b,off,len); }
    public void writeBoolean(boolean v)               throws IOException { out.writeBoolean(v); }
    public void writeByte(int v)                      throws IOException { out.writeByte(v); }
    public void writeShort(int v)                     throws IOException { out.writeShort(v); }
    public void writeChar(int v)                      throws IOException { out.writeChar(v); }
    public void writeInt(int v)                       throws IOException { out.writeInt(v); }
    public void writeLong(long v)                     throws IOException { out.writeLong(v); }
    public void writeFloat(float v)                   throws IOException { out.writeFloat(v); }
    public void writeDouble(double v)                 throws IOException { out.writeDouble(v); }
    public void writeBytes(String s)                  throws IOException { out.writeBytes(s); }
    public void writeChars(String s)                  throws IOException { out.writeChars(s); }
    public void writeUTF(String s)                    throws IOException { out.writeUTF(s); }
}
