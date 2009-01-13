package org.openflow.lavi.net.protocol.auth;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Message containing authentication credentials in plain text.
 * 
 * @author David Underhill
 */
public class AuthPlainText extends AuthHeader {
    // -1 => leave room for null terminating char
    public static final int MAX_UNAME_LEN = 100;
    public static final int MAX_PW_LEN = 100;
    
    public final String uname, pw;
    
    /** Construct an AUTH_REPLY of with plain-text authentication credentials */
    public AuthPlainText(final String uname, final String pw) {
        super(AuthHeader.REPLY, AuthType.PLAIN_TEXT);
        
        // -1 to take into account the terminating \0 characters
        this.uname = uname.substring(0, MAX_UNAME_LEN - 1);
        this.pw = pw.substring(0, MAX_PW_LEN - 1);
    }
    
    /** 
     * Construct an AUTH_REPLY of with plain-text authentication credentials
     * extracted from buf.
     */
    public AuthPlainText(final DataInput in) throws IOException {
        super(AuthHeader.REPLY, AuthType.PLAIN_TEXT);
        
        byte strBuf[] = new byte[Math.max(MAX_UNAME_LEN, MAX_PW_LEN)];
        
        for(int i=0; i<MAX_UNAME_LEN; i++)
            strBuf[i] = in.readByte();
        
        this.uname = new String(strBuf);
                
        for(int i=0; i<MAX_PW_LEN; i++)
            strBuf[i] = in.readByte();
        
        this.pw = new String(strBuf);
    }
    
    public int length() {
        return super.length() + MAX_UNAME_LEN + MAX_PW_LEN;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        byte[] strBytes;
        
        strBytes = uname.getBytes();
        out.write(strBytes);
        for(int i=0; i<MAX_UNAME_LEN-strBytes.length; i++)
            out.writeByte(0);
        
        strBytes = pw.getBytes();
        out.write(strBytes);
        for(int i=0; i<MAX_PW_LEN-strBytes.length; i++)
            out.writeByte(0);
    }
}
