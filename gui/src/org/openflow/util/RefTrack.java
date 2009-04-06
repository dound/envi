package org.openflow.util;

import java.util.ArrayList;

/** 
 * Tracks which objects refer to some object.
 */ 
public class RefTrack<REF_TO, REF_FROM> {
    /** the object being tracked */
    public final REF_TO obj;
    
    /** references to obj */
    private final ArrayList<REF_FROM> refs = new ArrayList<REF_FROM>();
    
    /**
     * Construct a RefTrack with one object referring to another.
     * 
     * @param objToTrack  the object whose referants are being tracked
     * @param ref         an object which refers to objToTrack
     */
    public RefTrack(REF_TO objToTrack, REF_FROM ref) {
        obj = objToTrack;
        refs.add(ref); 
    }
    
    /** Adds a reference to obj */
    public void addRef(REF_FROM o) {
        if(!refs.contains(o))
            refs.add(o);
    }
    
    /** 
     * Removes the reference o and returns true if obj no longer has any
     * references to it.
     */
    public boolean removeRef(REF_FROM o) {
        refs.remove(o);
        return refs.size() == 0;
    }
}
