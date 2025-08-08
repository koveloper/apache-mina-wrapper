/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.midomis.apache.mina.wrapper.network.tcp.server;

import com.midomis.apache.mina.wrapper.network.NetworkConnection;
import com.midomis.apache.mina.wrapper.network.NetworkConnectionData;
import com.midomis.apache.mina.wrapper.network.NetworkConnectionDefaultData;
import com.midomis.apache.mina.wrapper.network.Params;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 *
 * @author koveloper
 */
public class TcpServer extends NetworkConnection {

    private static final Logger LOG = Logger.getLogger(TcpServer.class);

    private int port = 0;
    private IoAcceptor acceptor = null;
    private final LinkedList<IoSession> sessions = new LinkedList<>();
    private final IoHandler ioHandler = new IoHandlerAdapter() {

        //called on client accept
        @Override
        public void sessionCreated(IoSession session) {
        }

        //called after client accept
        @Override
        public void sessionOpened(IoSession session) {
            synchronized(sessions) {
                sessions.add(session);
                session.getConfig().setMinReadBufferSize(Params.TcpServer.SESSION_MIN_BUF_SIZE);
                TcpServer.this.invokeEvent(new SessionEvent(session, OPERATION__CONNECTED));
            }
        }

        //called on client disconnect
        @Override
        public void sessionClosed(IoSession session) {
            synchronized(sessions) {
                sessions.remove(session);
                TcpServer.this.invokeEvent(new SessionEvent(session, OPERATION__DISCONNECTED));
            }
        }

        //called on pause on line
        @Override
        public void sessionIdle(IoSession session, IdleStatus status) {
        }
        
        //called on error
        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            session.closeNow();
        }

        //called on RX
        @Override
        public void messageReceived(IoSession session, Object message) {
            if (message instanceof IoBuffer buf) {
                LOG.log(Level.DEBUG, "message received [" + buf + "]");
                TcpServer.this.invokeEvent(
                        NetworkConnectionDefaultData.getNewInstanceForReceive(
                                Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.arrayOffset() + buf.remaining()),
                                session
                        )
                );
            }
        }
        
        @Override
        public void inputClosed(IoSession session) {
            session.closeNow();
        }
    };

    public TcpServer(int port) {
        super(TcpServer.class.getCanonicalName());
        this.port = port;
    }

    @Override
    protected void NetworkConnection__init() {
        acceptor = new NioSocketAcceptor();
        acceptor.setHandler(ioHandler);
        this.invokeEvent(OPERATION__CONNECT);        
    }

    @Override
    protected void NetworkConnection__connect() {
        try {
            acceptor.bind(new InetSocketAddress(this.port));
        } catch (IOException ex) {
            LOG.log(Level.ERROR, "bind [" + this.port + "]", ex);
            this.commitError(ex);
        }
    }

    @Override
    protected void NetworkConnection__finish() {
        if(acceptor == null) {
            return;
        }
        sessions.forEach(s -> {
            s.closeNow();
        });
        acceptor.unbind();
        acceptor.dispose(true);
        acceptor = null;
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
        this.commitDisconnected(src);
    }    

    @Override
    protected void NetworkConnection__send(NetworkConnectionData data) {
        if(data.getAttachedSession() == null) {
            byte[] bytesToSend = data.serialize();
            IoBuffer buffer = IoBuffer.allocate(bytesToSend.length);
            buffer.put(bytesToSend);
            buffer.flip();
            acceptor.broadcast(buffer);
            buffer.clear();
            buffer.free();
        } else {
            try {
                synchronized(sessions) {
                    sessions.stream().filter(s -> (s.equals(data.getAttachedSession()))).forEachOrdered(s -> {
                        sendThrewSession(s, data.serialize());
                    });
                }
            } catch (Exception e) {
                LOG.log(Level.ERROR, "send error [" + this.port + "]", e);
            }            
        }
    }

    @Override
    protected void NetworkConnection__handleReceivedData(NetworkConnectionData data) {
        this.commitData(data);
    }
    
    public int getPort() {
        return port;
    }
}
