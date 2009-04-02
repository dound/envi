package org.openflow.lavi.et;

import org.openflow.lavi.net.protocol.et.ETTrafficMatrix;

/**
 * Callback issued when the traffic matrix changes.
 *
 * @author David Underhill
 */
public interface TrafficMatrixChangeListener {
    public void trafficMatrixChanged(ETTrafficMatrix tm);
}
