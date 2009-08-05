package org.openflow.gui.net;

/** A message */
public interface Message<TYPE> {
    /** gets the type of this message */
    public TYPE getType();
    
    /** writes the message to out */
    public void write(java.io.DataOutput out) throws java.io.IOException;
}
