/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package koveloper.tools.network.tcp.client;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.logging.Logger;
import koveloper.tools.network.NetworkConnection;
import koveloper.tools.network.NetworkConnectionData;
import koveloper.tools.network.NetworkConnectionDefaultData;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

/**
 *
 * @author kgn
 */
public class TcpClient extends NetworkConnection {

    public static int CONNECT_TIMEOUT_MS = 10000;
    
    private String host = null;
    private int port = 0;
    private NioSocketConnector connector = null;
    private IoSession session = null;
    private boolean finished = false;
    private final IoHandler ioHandler = new IoHandlerAdapter() {
        @Override
        public void sessionClosed(IoSession session) throws Exception {
            TcpClient.this.invokeEvent(OPERATION__DISCONNECTED);
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            if (message instanceof IoBuffer) {
                IoBuffer buf = (IoBuffer) message;
                TcpClient.this.invokeEvent(NetworkConnectionDefaultData.getNewInstanceForReceive(Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.arrayOffset() + buf.remaining())));
            }
        }

        @Override
        public void inputClosed(IoSession session) throws Exception {
            session.closeNow();
        }
    };

    public TcpClient(String host, int port) {
        super(TcpClient.class.getCanonicalName());
        this.host = host;
        this.port = port;
    }
    
    public void finish() {
        this.finished = true;
        if(this.session != null) {
            this.session.closeOnFlush();
        } else {
            TcpClient.this.invokeEvent(OPERATION__DISCONNECTED);
        }
    }

    @Override
    protected void NetworkConnection__init() {
        if(this.finished) {
            return;
        }
        Logger.getLogger(TcpClient.class.getName()).info("init");
        connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(CONNECT_TIMEOUT_MS);
        connector.setHandler(ioHandler);
        this.invokeEvent(OPERATION__CONNECT);
    }

    @Override
    protected void NetworkConnection__connect() {
        if(this.finished) {
            return;
        }
        Logger.getLogger(TcpClient.class.getName()).info("connect");
        ConnectFuture future = connector.connect(new InetSocketAddress(this.host, this.port));
        try {
            future.await(CONNECT_TIMEOUT_MS);
            session = future.getSession();
            this.invokeEvent(OPERATION__CONNECTED);
        } catch (Exception ex) {
            session = null;
            this.invokeEvent(OPERATION__CONNECT);
        }
    }

    @Override
    protected void NetworkConnection__send(NetworkConnectionData data) {
        if(this.finished) {
            return;
        }
    }

    @Override
    protected void NetworkConnection__handleReceivedData(NetworkConnectionData data) {
        this.commitData(data);
    }

    @Override
    protected void NetworkConnection__connected() {
        this.commitConnected();
    }

    @Override
    protected void NetworkConnection__disconnected() {
        if(this.finished) {
            this.destroyTasksHandler();
            this.connector.dispose(true);
            this.commitFinished();
            return;
        }
        TcpClient.this.invokeEvent(OPERATION__CONNECT);
        this.commitDisconnected();
    }
}
