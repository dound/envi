/**
 * 
 */
package org.openflow.gui.net.protocol.op;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import org.openflow.gui.net.SocketConnection;
import org.openflow.util.Pair;

/**
 * @author grg
 *
 */
public class OPSFIntChoice extends OPSFInt {
    public static final int CHOICE_LEN = 40;
    
    /** List of choices */
    public final ArrayList<Pair<Integer, String>> choices;
    
    public OPSFIntChoice(String name, String desc, boolean readOnly, DataInput in) throws IOException {
        super(name, desc, readOnly, 4, DISP_CHOICE);
        
        // Read the width and the display
        int width = in.readByte();
        int display = in.readByte();
        
        int numChoices = in.readShort();
        choices = new ArrayList<Pair<Integer, String>>();

        for (int i = 0; i < numChoices; i++) {
            int val = in.readInt();
            String choice = SocketConnection.readString(in, CHOICE_LEN);
            choices.add(new Pair<Integer, String>(Integer.valueOf(val), choice));
        }
    }

    public String toString() {
        String choicesStr = null;
        for (Pair<Integer, String> p : choices) {
            if (choicesStr != null)
                choicesStr += ", ";
            choicesStr += "(" + p.a + ", " + p.b + ")";
        }

        return super.toString() + " choices=[" + choicesStr + "]";
    }	
}
