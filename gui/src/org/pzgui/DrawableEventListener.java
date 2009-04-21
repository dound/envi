package org.pzgui;

import java.awt.AWTEvent;

/**
 * Callback issued when a Drawable object fires an event.
 *
 * @author David Underhill
 */
public interface DrawableEventListener {
    public void drawableEvent(Drawable d, AWTEvent e, String event);
}
