package org.openflow.lavi.net;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper around DataInputStream which counts how many bytes have been read.
 * If an IOException occurs, the CountingDataInputStream will lose of bytes read
 * in the case where only some bytes were read before the exception occurred
 * (the count is only updated after a successful operation).
 *
 * @author David Underhill
 */
public class CountingDataInputStream implements DataInput {
    /** the stream this wraps */
    private final DataInputStream in;

    /** bytes read from this stream */
    private long bytesRead = 0;

    /**
     * Creates a CountingDataInputStream that uses the specified
     * underlying InputStream.
     *
     * @param  in   the specified input stream
     */
    public CountingDataInputStream(InputStream in) {
        this.in = new DataInputStream(in);
    }

    /** Returns the number of bytes read from this stream */
    public long getBytesRead() {
        return bytesRead;
    }

    public final int read(byte b[]) throws IOException {
        int ret = in.read(b);
        bytesRead += ret;
        return ret;
    }

    public final int read(byte b[], int off, int len) throws IOException {
        int ret = in.read(b, off, len);
        bytesRead += ret;
        return ret;
    }
    
    public final void readFully(byte b[]) throws IOException {
        in.readFully(b);
        bytesRead += b.length;
    }

    public final void readFully(byte b[], int off, int len) throws IOException {
        in.readFully(b, off, len);
        bytesRead += len;
    }

    public final int skipBytes(int n) throws IOException {
        int ret = in.skipBytes(n);
        bytesRead += ret;
        return ret;
    }

    public final boolean readBoolean() throws IOException {
        boolean ret = in.readBoolean();
        bytesRead += 1;
        return ret;
    }

    public final byte readByte() throws IOException {
        byte ret = in.readByte();
        bytesRead += 1;
        return ret;
    }

    public final int readUnsignedByte() throws IOException {
        int ret = in.readUnsignedByte();
        bytesRead += 1;
        return ret;
    }

    public final short readShort() throws IOException {
        short ret = in.readShort();
        bytesRead += 2;
        return ret;
    }

    public final int readUnsignedShort() throws IOException {
        int ret = in.readUnsignedShort();
        bytesRead += 2;
        return ret;
    }

    public final char readChar() throws IOException {
        char ret = in.readChar();
        bytesRead += 1;
        return ret;
    }

    public final int readInt() throws IOException {
        int ret = in.readInt();
        bytesRead += 4;
        return ret;
    }

    public final long readLong() throws IOException {
        long ret = in.readLong();
        bytesRead += 8;
        return ret;
    }

    public final float readFloat() throws IOException {
        float ret = in.readFloat();
        bytesRead += 4;
        return ret;
    }

    public final double readDouble() throws IOException {
        double ret = in.readDouble();
        bytesRead += 8;
        return ret;
    }

    public final String readLine() throws IOException {
        throw new UnsupportedOperationException("readLine() is not supported");
    }

    public final String readUTF() throws IOException {
        throw new UnsupportedOperationException("readUTF() is not supported");
    }
}
