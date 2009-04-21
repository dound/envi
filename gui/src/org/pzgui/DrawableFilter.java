package org.pzgui;

/**
 * Provides a mechanism for filtering Drawables.
 * 
 * @author David Underhill
 */
public interface DrawableFilter {
    /**
     * Considers d and returns some boolean decision.
     */
    public boolean consider(Drawable d);
}
