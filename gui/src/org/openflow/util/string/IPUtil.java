/**
 * Utilities for handling IP addresses
 */
package org.openflow.util.string;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * IPUtil -- Utility class with IP related functions
 * @author grg
 *
 */
public class IPUtil {
    /**
     * Size of an IPv4 address in bytes
     */
    public static final int IP4_ADDR_LEN = 4;
    
    /**
     * ByteArray -- convert an integer IP address into a byte array
     * @param ip IP address to convert
     * @return byte array representation of IP
     */
    public static byte[] toByteArray(int ip) {
        byte[] ipArray = new byte[IP4_ADDR_LEN];
        ByteBuffer buf = ByteBuffer.wrap(ipArray);
        buf.putInt(ip);
        return ipArray;
    }
    
    /**
     * toInetAddress -- convert an integer IP address into an InetAddress
     * @param ip IP address to convert
     * @return InetAddress object
     */
    public static InetAddress toInetAddress(int ip) {
        try {
            return InetAddress.getByAddress(toByteArray(ip));
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Converts a masked IP to a string.
     * @param ip          IP address
     * @param maskedBits  number of bits to mask
     * @return string version of the masked IP address
     */
    public static String maskedIPToString(int ip, int maskedBits) {
        if(maskedBits == 0)
            return IPUtil.toString(ip);
        
        int v = 0;
        int c = 1;
        for(int i=0; i<maskedBits; i++) {
            v += c;
            c *= 2;
        }
        
        int bitmask = v << (32 - maskedBits);
        return IPUtil.toString(ip & bitmask) + "/" + maskedBits;
    }

    /**
     * toString -- convert an integer IP address into a string
     * @param ip IP address to convert
     * @return String representation of IP address
     */
    public static String toString(int ip) {
        byte[] ipArray = toByteArray(ip);
        int[] ipIntArray = new int[ipArray.length];
        for (int i = 0; i < ipArray.length; i++) {
            ipIntArray[i] = ipArray[i];
            if (ipIntArray[i] < 0)
                ipIntArray[i] += 256;
        }
        
        String ret = Integer.toString(ipIntArray[0]);
        for (int i = 1; i < ipArray.length; i++)
            ret += "." + Integer.toString(ipIntArray[i]);
        return ret;
    }
        
        public static int stringToIP(String ip) {
            String[] terms = ip.split("\\.");
            if(terms.length != 4)
                throw new NumberFormatException("stringToIP needs correct ip, got " + ip);
            
            int o1 = Integer.valueOf(terms[0]);
            int o2 = Integer.valueOf(terms[1]);
            int o3 = Integer.valueOf(terms[2]);
            int o4 = Integer.valueOf(terms[3]);
            
            return (o1<<24) + (o2<<16) + (o3<<8) + o4;
        }
        
        public static void main(String args[]) {
            int ip = (int)(Math.random() * Integer.MAX_VALUE);
            System.err.println(ip);
            System.err.println(toString(ip));
            System.err.println(stringToIP(toString(ip)));
        }
}
