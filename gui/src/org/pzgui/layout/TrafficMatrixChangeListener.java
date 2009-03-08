package org.pzgui.layout;

import org.openflow.lavi.net.protocol.ETTrafficMatrix;

/**
 * Callback issued when the traffic matrix changes.
 *
 * @author David Underhill
 */
public interface TrafficMatrixChangeListener {
    public void trafficMatrixChanged(ETTrafficMatrix tm);
}
