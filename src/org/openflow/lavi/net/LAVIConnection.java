package org.openflow.lavi.net;

import org.openflow.lavi.net.protocol.LAVIMessage;
import org.openflow.lavi.net.protocol.LinksSubscribe;
import org.openflow.lavi.net.protocol.SwitchesSubscribe;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.openflow.lavi.net.protocol.LAVIMessageType;

/**
 * Connects to a LAVI server instance to receive stats and send commands.
 * @author David Underhill
 */
public class LAVIConnection extends Thread {
    /** default port to connect to LAVI over */
    public static final short DEFAULT_PORT = 2503;
    
    /** maximum time to wait between tries to get connected */
    public static final int RETRY_WAIT_MSEC_MAX = 2 * 60 * 1000; // two minutes
    
    /**
     * Statistics about this connection.
     */
    public class NetStats {
        /** number of updates received from LAVI server */
        private long numUpdatesReceived = 0;

        /** time the last update was received */
        private long lastUpdateTime_ms;

        /** time we (dis)connected to LAVI server */
        private long timeConnected_ms;

        /** whether we are connected or disconnected */
        private boolean connected;

        public NetStats() {
            lastUpdateTime_ms = System.currentTimeMillis();
            disconnected();
        }

        /**
         * Returns the number of updates received from LAVI server.
         */
        public synchronized long getNumUpdatesdReceived() {
            return numUpdatesReceived;
        }

        /**
         * Notes that a new update has been received and updates the corresponding 
         * statistics.
         */
        public synchronized void updateReceived() {
            numUpdatesReceived += 1;
            lastUpdateTime_ms = System.currentTimeMillis();
        }

        /**
         * Gets the number of milliseconds which have passed since the last update 
         * was received.
         */
        public synchronized long getTimePassedSinceLastUpdate_ms() {
            return System.currentTimeMillis() - lastUpdateTime_ms;
        }

        /**
         * Returns how long the connection to the LAVI server server has been up.  A 
         * negative value indicates the connection has been down for the magnitude 
         * of the value.
         */
        public synchronized long getTimeConnected_ms() {
            long diff = System.currentTimeMillis() - timeConnected_ms;
            return connected ? diff : -diff;
        }

        /**
         * Returns how long the connection to the LAVI server server has been down.  A 
         * negative value indicates the connection has been up for the magnitude 
         * of the value.
         */
        public synchronized long getTimeDisconnected_ms() {
            return -getTimeConnected_ms();
        }

        public synchronized boolean isConnected() {
            return timeConnected_ms > 0;
        }

        /**
         * Sets the time the connection to the LAVI server server was established to the
         * current time.
         */
        public synchronized void connected() {
            setConnected(true);
        }

        /**
         * Sets the time the connection to the LAVI server server was lost to the
         * current time.
         */
        public synchronized void disconnected() {
            setConnected(false);
        }

        private void setConnected(boolean b) {
            timeConnected_ms = System.currentTimeMillis();
            connected = b;
        }
    }
    
    /** stats associated with this connection */
    private final NetStats stats = new NetStats();
    
    /** connection to the LAVI server */
    private SocketConnection conn = null;
    
    /** the IP the LAVI server server lives on */
    private final String serverIP;
    
    /** the port the LAVI server listens on */
    private final int serverPort;
    
    /** if true, then the connection should be re-initiated */
    private boolean reconnect = false;
    
    /** the object responsible for processing LAVI messages */
    private final LAVIMessageProcessor msgProcessor;

    /** whether the connection should be turned off */
    private boolean done = false;

    /** whether the connection has been turned off */
    private boolean shutdown = false;
    
    /** whether to subscribe to switch updates */
    private boolean subscribeToSwitchChanges = true;
    
    /** whether to subscribe to link updates */
    private boolean subscribeToLinkChanges = true;
    
    /** 
     * Connect to the LAVI server at the specified IP on DEFAULT_PORT.
     * 
     * @param ip  the IP where the LAVI server lives
     */
    public LAVIConnection(LAVIMessageProcessor mp, String ip) {
        this(mp, ip, DEFAULT_PORT);
    }
    
    /**
     * Connect to the LAVI server at the specified address and port.
     * 
     * @param ip  the IP where the LAVI server lives
     * @param port  the port the LAVI server listens on
     */
    public LAVIConnection(LAVIMessageProcessor mp, String ip, int port) {
        msgProcessor = mp;
        serverIP = ip;
        serverPort = port;
    }
    
    /**
     * The LAVIConnection thread will connect to the LAVI server and then 
     * continuously read from the socket and pass on received messages to the 
     * protocol handler.  It will automatically try to reconnect if disconnected
     * or if it fails to get connected.
     */
    public void run() {
        // start a thread to scrub expired cached stateful requests
        final LAVIConnection me = this;
        new Thread() {
            public void run() {
                scrubExpiredStatefulRequests();
                try { Thread.sleep(me.getRequestLifetime()); }
                catch(InterruptedException e) {}
            }
        };
        
        connect();

        while(!done) {
            try {
                msgProcessor.process(recvLAVIMessage());
            } catch(IOException e) {
                if(done)
                    break;
                
                System.err.println("LAVI Network Error: " + e);
                reconnect = true;
            }
            if(reconnect) {
                reconnect = false;
                disconnect();
                connect();
            }
        }

        shutdown = true;
    }

    /** tells the connection to shut down as soon as possible */
    public void shutdown() {
        done = true;
        disconnect();
    }

    /** gets whether the connection has been shutdown yet */
    public boolean isShutdown() {
        return shutdown;
    }
    
    /** returns true if the connection to the server is currently alive */
    public boolean isConnected() {
        return conn!=null && conn.s!=null;
    }
    
    /** Continuously tries to connect to the server. */
    private void connect() {
        stats.disconnected();
        int retry_ms = 250;
        
        int tries = 0;
        do {
            if(done) return;
            
            if(tries++ > 0) {
                System.err.println("Retrying to establish connection to LAVI server (try #" + tries + ")...");
                tryToClose(conn);
            }
            else
                System.err.println("Trying to establish connection to LAVI server ...");

            conn = new SocketConnection(serverIP, serverPort);

            if(conn.s == null) {
                System.err.println("Failed to establish connections to LAVI server! (will retry in " + retry_ms/1000.0f  + " seconds)");
                try {
                    Thread.sleep(retry_ms);
                    retry_ms = Math.min(retry_ms*2, RETRY_WAIT_MSEC_MAX);
                }
                catch (InterruptedException e) {
                    /* ignore */
                }
            }
        }
        while(conn==null && conn.s==null);
        
        System.err.println("Now connected to LAVI server");
        stats.connected();
        msgProcessor.connectionStateChange();
        
        // ask the backend for a list of switches and links
        try {
            if(isSubscribeToSwitchChanges())
                sendLAVIMessage(new SwitchesSubscribe(true));
            
            if(subscribeToLinkChanges)
                sendLAVIMessage(new LinksSubscribe(true));
        }
        catch(IOException e) {
            System.err.println("Error: unable to setup subscriptions");
        }
    }
    
    /** tells the LAVI connection to disconnect and then connect again */
    public void reconnect() {
        this.reconnect = true;
    }
    
    /** returns the next LAVI message received on the connection */
    private LAVIMessage recvLAVIMessage() throws IOException {
        final CountingDataInputStream in = conn.in;
        if(in == null)
            throw new IOException("connection is disconnected");
        
        long bytesReadBefore = in.getBytesRead();

        // determine how long the message is
        int len = in.readShort();

        // decode the message
        LAVIMessage msg = LAVIMessageType.decode(len, in);

        // make sure we consume exactly the specified number of bytes or problems have
        long bytesRead = in.getBytesRead() - bytesReadBefore;
        if(bytesRead < len) {
            int bytesLeftover = (int)(len - bytesRead);
            if(in.skipBytes(bytesLeftover) != bytesLeftover)
                throw new IOException("unable to skip leftover bytes (" + bytesLeftover + "B)");
            else
                System.err.println("Warning: " + bytesLeftover + "B leftover for message type " + msg.type.toString());
        }
        else if(bytesRead > len) {
            long bytesOverread = bytesRead - len;
            throw new IOException("read " + bytesOverread + "B over the specified length for message type " + msg.type.toString());
        }
        
        return msg;
    }
    
    /** next transaction ID to use */
    private int nextXID = 1;
    
    /** how much time to remember a request before expiring it */
    private long requestLifetime_msec = 2000;
    
    /** messages which are expecting a stateful response */
    protected ConcurrentHashMap<Integer, LAVIMessage> outstandingStatefulRequests = new ConcurrentHashMap<Integer, LAVIMessage>();
    
    /** tries to send a LAVI message */
    public void sendLAVIMessage(LAVIMessage m) throws IOException {
        // get the current connection
        java.io.DataOutput out = this.conn.out;
        
        // bail out if we are not connected - TODO: could try to mask this and 
        // buffer calls to this method and send them when we get reconnected
        if(out == null)
            throw new IOException("connection is down");
        
        m.xid = nextXID++;
        if(m.isStatefulRequest())
            outstandingStatefulRequests.put(m.xid, m);
        
        m.write(out);
        
        System.out.println("sent: " + m.toString());
    }
    
    /** 
     * Returns the request sent with the specified transaction ID, if any.  The 
     * stateful request returned will no longer be remembered. 
     */
    public LAVIMessage popAssociatedStatefulRequest(int xid) {
        return outstandingStatefulRequests.remove(xid);
    }
    
    /** Gets the number of milliseconds a stateful request will be stored */
    public long getRequestLifetime() {
        return requestLifetime_msec;
    }

    /** Sets the number of milliseconds a stateful request will be stored */
    public void setRequestLifetime(long msec) {
        requestLifetime_msec = msec;
    }
    
    /** Remove cached stateful requests which have been cached for longer than getRequestLifetime() */
    protected void scrubExpiredStatefulRequests() {
        long now = System.currentTimeMillis();
        
        Iterator<LAVIMessage> itr = outstandingStatefulRequests.values().iterator();
        while(itr.hasNext())
            if(now - itr.next().timeCreated() > requestLifetime_msec)
                itr.remove();
    }
    
    /** closes the connection to the LAVI server */
    private void disconnect() {
        System.err.println("Disconnecting from the LAVI server");
        tryToClose(conn);
        conn = null;
        stats.disconnected();
        outstandingStatefulRequests.clear();
        msgProcessor.connectionStateChange();
    }
    
    /** try to close the connection to the server */
    private static void tryToClose(SocketConnection sc) {
        if( sc != null ) {
            Socket s = sc.s;
            if(s != null && s.isConnected()) {
                // tell the backend we're disconnecting
                if(sc.out != null) {
                    try {
                        new LAVIMessage(LAVIMessageType.DISCONNECT, 0).write(sc.out);
                    }
                    catch(IOException e) { /* ignore */ }
                }

                try {
                    s.close();
                } catch(IOException e){}
            }
        }
    }

    /** Returns whether the connection is subscribed to switch changes */
    public boolean isSubscribeToSwitchChanges() {
        return subscribeToSwitchChanges;
    }
    
    /** 
     * Sets whether the connection is subscribed to switch changes and sends 
     * the appropriate subscription request if this is different. 
     */
    public void setSubscribeToSwitchChanges(boolean b) throws IOException {
        if(b == subscribeToSwitchChanges)
            return;
        
        sendLAVIMessage(new SwitchesSubscribe(b));
        subscribeToSwitchChanges = b;
    }

    /** Returns whether the connection is subscribed to link changes */
    public boolean isSubscribeToLinkChanges() {
        return subscribeToSwitchChanges;
    }
    
    /** 
     * Sets whether the connection is subscribed to link changes and sends 
     * the appropriate subscription request if this is different. 
     */
    public void setSubscribeToLinkChanges(boolean b) throws IOException {
        if(b == subscribeToLinkChanges)
            return;
        
        sendLAVIMessage(new LinksSubscribe(b));
        subscribeToLinkChanges = b;
    }
}
