package org.openflow.gui.net;

/** A message */
public interface Message<TYPE> {
    /** gets the type of this message */
    public TYPE getType();
}
