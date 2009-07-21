/**
 * 
 */
package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;

/**
 * Integer state variable type
 * 
 * @author grg
 *
 */
public class OPSFInt extends OPStateField {
    public static final int DISP_INT 	= 1;
    public static final int DISP_IP 	= 2;
    public static final int DISP_MAC 	= 3;
    public static final int DISP_BOOL 	= 4;
    public static final int DISP_CHOICE	= 5;
    
    /** Width of the variable in bytes */
    public final int width;
    
    /** How should the variable be displayed */
    public final int display;
    
    protected OPSFInt(String name, String desc, boolean readOnly, int width, int display) {
        super(name, desc, readOnly);
        this.width = width;
        this.display = display;
    }
    
    public OPSFInt(String name, String desc, boolean readOnly, DataInput in) throws IOException {
        super(name, desc, readOnly);
        width = in.readByte();
        display = in.readByte();
    }

    public String toString() {
        String displayStr;
        switch (display) {
            case DISP_INT: 	displayStr = "integer";
            case DISP_IP: 	displayStr = "IP";
            case DISP_MAC: 	displayStr = "MAC";
            case DISP_BOOL: displayStr = "boolean";
            case DISP_CHOICE: displayStr = "choice";
            default:		displayStr = "unknown";
        }

        return super.toString() +
            " type=int width=" + width + " display=" + displayStr;
    }	
}
