/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network.tcp.client;

import com.koveloper.apache.mina.wrapper.network.NetworkConnection;
import com.koveloper.apache.mina.wrapper.network.NetworkConnectionData;
import com.koveloper.apache.mina.wrapper.network.NetworkConnectionDefaultData;
import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
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

    private static final Logger LOG = Logger.getLogger(TcpClient.class);
    
    public static int CONNECT_TIMEOUT_MS = 10000;
    
    private String host = null;
    private int port = 0;
    private NioSocketConnector connector = null;
    private IoSession session = null;
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
    
    @Override
    protected void NetworkConnection__finish() {
        if(this.session != null) {
            this.session.closeOnFlush();            
        } 
        this.connector.dispose(true);
        this.commitFinished();            
    }

    @Override
    protected void NetworkConnection__init() {
        LOG.log(Level.INFO, "init [" + this.host + ":" + this.port + "]");
        connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(CONNECT_TIMEOUT_MS);
        connector.setHandler(ioHandler);
        this.invokeEvent(OPERATION__CONNECT);
    }

    @Override
    protected void NetworkConnection__connect() {
        LOG.log(Level.INFO, "connect try [" + this.host + ":" + this.port + "]");
        ConnectFuture future = connector.connect(new InetSocketAddress(this.host, this.port));
        try {
            if(!future.await(CONNECT_TIMEOUT_MS)) {
                throw new Exception("connect failed");
            }
            session = future.getSession();
            this.invokeEvent(OPERATION__CONNECTED);
            LOG.log(Level.INFO, "connected [" + this.host + ":" + this.port + "]");
        } catch (Exception ex) {
            LOG.log(Level.INFO, "connect fails [" + this.host + ":" + this.port + "]");
            session = null;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex1) {
                Logger.getLogger(TcpClient.class.getName()).log(Level.ERROR, null, ex1);
            }
            this.invokeEvent(OPERATION__CONNECT);
        }
    }

    @Override
    protected void NetworkConnection__send(NetworkConnectionData data) {
        if(this.session == null) {
            return;
        }
        byte[] bytesToSend = data.serialize();
        IoBuffer buffer = IoBuffer.allocate(bytesToSend.length);
        buffer.put(bytesToSend);
        buffer.flip();
        this.session.write(buffer);
        buffer.clear();
        buffer.free();
    }

    @Override
    protected void NetworkConnection__handleReceivedData(NetworkConnectionData data) {
        this.commitData(data);
    }

    @Override
    protected void NetworkConnection__connected(Object src) {
        this.commitConnected(src);
    }

    @Override
    protected void NetworkConnection__disconnected(Object src) {
        if(this.isFinished()) {
            return;
        }
        TcpClient.this.invokeEvent(OPERATION__CONNECT);
        this.commitDisconnected(src);
    }
}
