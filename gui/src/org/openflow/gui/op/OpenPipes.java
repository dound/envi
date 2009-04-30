package org.openflow.gui.op;

import java.util.ArrayList;

import org.openflow.gui.ConnectionHandler;
import org.openflow.gui.OpenFlowGUI;
import org.openflow.gui.op.OPConnectionHandler;
import org.openflow.gui.op.OPLayoutManager;
import org.openflow.util.Pair;

public final class OpenPipes {
    public static final String OPENPIPES_TITLE = "OpenFlow GUI: OpenPipes";

    /** cannot instantiate this class */
    private OpenPipes() {}
    
    /** run the front-end */
    public static void main(String args[]) {
        Pair<String, Short> serverPort = OpenFlowGUI.getServer(args);
        String server = serverPort.a;
        short port = serverPort.b;
        
        // create a manager to handle drawing the topology info received by the connection
        OPLayoutManager gm = new OPLayoutManager();
        gm.loadDrawablePositionsFromFile("op.yaml");
        ArrayList<Class> drawOrder = new ArrayList<Class>();
        drawOrder.add(org.openflow.gui.drawables.OPModule.class);
        drawOrder.add(org.openflow.gui.drawables.OPNodeWithNameAndPorts.class);
        gm.setDrawOrder(drawOrder);
        
        // create a manager to handle the connection itself
        ConnectionHandler cm = new OPConnectionHandler(gm, server, port);
        
        // start our managers
        gm.start();
        cm.getConnection().start();
    }
}
