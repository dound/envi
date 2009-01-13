/**
 * Utilities for handling DPIDs
 */
package org.openflow.util.string;

import java.nio.ByteBuffer;

/**
 * DPIDUtil -- Utility class with DPID related functions
 * @author Glen Gibb
 *
 */
public class DPIDUtil {
    /**
     * Size of an DPID in bytes
     */
    public static final int DPID_LEN = 8;
    
    /**
     * ByteArray -- convert DPID into a byte array
     * @param dpid DPID to convert
     * @return byte array representation of DPID
     */
    public static byte[] toByteArray(long dpid) {
        byte[] dpidArray = new byte[DPID_LEN];
        ByteBuffer buf = ByteBuffer.wrap(dpidArray);
        buf.putLong(dpid);
        return dpidArray;
    }
    
    /**
     * toString -- convert a DPID into a string
     * @param dpid DPID to convert
     * @return String representation of DPID
     */
    public static String toString(long dpid) {
        byte[] dpidArray = toByteArray(dpid);
        Integer[] dpidIntArray = new Integer[dpidArray.length];
        
        for (int i = 0; i < dpidArray.length; i++) {
            int d = dpidArray[i];
            if (d < 0)
                d += 256;
            dpidIntArray[i] = Integer.valueOf(d);
        }
        
        return new PrintfFormat("%02x:%02x:%02x:%02x:%02x:%02x:%02x:%02x").sprintf(dpidIntArray);
    }
        
        public static String dpidToHex(long dpid) {
            String hex = Long.toHexString(dpid).toUpperCase();
            String ret = "";
            while( hex.length() > 1 ) {
                ret = hex.substring( hex.length() - 2 ) + (ret.length()>0 ? ":" : "") + ret;
                hex = hex.substring(0, hex.length() - 2);
            }

            if( hex.length() > 0 )
                ret = hex + (ret.length()>0 ? ":" : "") + ret;

            return ret;
        }
        
        public static int hexToDec(char h) throws NumberFormatException {
            switch(Character.toLowerCase(h)) {
                case '0': return 0;
                case '1': return 1;
                case '2': return 2;
                case '3': return 3;
                case '4': return 4;
                case '5': return 5;
                case '6': return 6;
                case '7': return 7;
                case '8': return 8;
                case '9': return 9;
                case 'a': return 10;
                case 'b': return 11;
                case 'c': return 12;
                case 'd': return 13;
                case 'e': return 14;
                case 'f': return 15;
                default: throw new NumberFormatException("Invalid argument to hexToDec: " + h);
            }
        }
        
        public static long hexToDPID(String hex) throws NumberFormatException {
            long ret = 0;
            String[] terms = hex.split(":");
            
            long shift = 0;
            for(int i=terms.length-1; i>=0; i--) {
                String term = terms[i];
                if(term.length() == 1)
                    ret += (((long)hexToDec(term.charAt(0))) << shift);
                else if(term.length() == 2)
                    ret += (((long)hexToDec(term.charAt(0))*16 + hexToDec(term.charAt(1))) << shift);
                
                shift += 8;
            }
            
            return ret;
        }
        
        public static void main(String[] args) {
            long dpid = (long)(Math.random() * Long.MAX_VALUE);
            System.err.println(dpid);
            System.err.println(dpidToHex(dpid));
            System.err.println(hexToDPID(dpidToHex(dpid)));
        }
}