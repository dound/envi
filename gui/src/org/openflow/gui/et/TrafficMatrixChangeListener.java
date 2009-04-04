package org.openflow.gui.et;

import org.openflow.gui.net.protocol.et.ETTrafficMatrix;

/**
 * Callback issued when the traffic matrix changes.
 *
 * @author David Underhill
 */
public interface TrafficMatrixChangeListener {
    public void trafficMatrixChanged(ETTrafficMatrix tm);
}
