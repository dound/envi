package org.openflow.gui.net.protocol.auth;

import java.io.DataOutput;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.openflow.gui.net.protocol.OFGMessage;
import org.openflow.gui.net.protocol.OFGMessageType;

/**
 * Reply to an authentication request with a user's credentials.  The password 
 * is converted to a salted SHA-1 hash.  Specifically, the SHA-1 of the password
 * is computed.  These salt is appended to that digest.  Finally, the SHA-1 of
 * this is computed.  This final SHA-1 will be sent to the server along with 
 * the username. 
 * 
 * @author David Underhill
 */
public class AuthReply extends OFGMessage {
    public final String username;
    public final byte[] saltedSHA1ofPassword;
    
    /**
     * Constructs a new authentication reply. 
     */
    public AuthReply(int xid, final String username, final String pw, final byte[] salt) {
        super(OFGMessageType.AUTH_REPLY, xid);
        
        this.username = username;
        
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Unable to load the SHA-1 algorithm: " + e.getMessage());
        }
        
        byte[] sha1OfPassword = md.digest(pw.getBytes());
        md.reset();
        
        byte[] sha1OfPasswordPlusSalt = new byte[sha1OfPassword.length + salt.length];
        System.arraycopy(sha1OfPassword, 0, sha1OfPasswordPlusSalt, 0, sha1OfPassword.length);
        System.arraycopy(salt, 0, sha1OfPasswordPlusSalt, sha1OfPassword.length, salt.length);
        saltedSHA1ofPassword = md.digest(sha1OfPasswordPlusSalt);
    }
    
    public int length() {
        return super.length() + 4 + username.length() + saltedSHA1ofPassword.length;
    }
    
    public void write(DataOutput out) throws IOException {
        super.write(out);
        byte[] strBytes;
        
        strBytes = username.getBytes();
        out.writeInt(strBytes.length);
        out.write(strBytes);
        out.write(saltedSHA1ofPassword);
    }

    public String toString() {
        return super.toString() + TSSEP + "username=" + username + " salted SHA1 of pw=" + saltedSHA1ofPassword;
    }
}
