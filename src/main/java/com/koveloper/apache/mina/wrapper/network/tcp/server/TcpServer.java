/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network.tcp.server;

import com.koveloper.apache.mina.wrapper.network.NetworkConnection;
import com.koveloper.apache.mina.wrapper.network.NetworkConnectionData;
import com.koveloper.apache.mina.wrapper.network.NetworkConnectionDefaultData;
import com.koveloper.apache.mina.wrapper.network.NetworkListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 *
 * @author koban
 */
public class TcpServer extends NetworkConnection {

    private int port = 0;
    private IoAcceptor acceptor = null;
    private LinkedList<IoSession> sessions = new LinkedList<>();
    private final IoHandler ioHandler = new IoHandlerAdapter() {

        //called on client accept
        @Override
        public void sessionCreated(IoSession session) {
        }

        //called after client accept
        @Override
        public void sessionOpened(IoSession session) {
            sessions.add(session);
            TcpServer.this.invokeEvent(new SessionEvent(session, OPERATION__CONNECTED));
        }

        //called on client disconnect
        @Override
        public void sessionClosed(IoSession session) {
            sessions.remove(session);
            TcpServer.this.invokeEvent(new SessionEvent(session, OPERATION__DISCONNECTED));
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
            if (message instanceof IoBuffer) {
                IoBuffer buf = (IoBuffer) message;
                TcpServer.this.invokeEvent(
                        NetworkConnectionDefaultData.getNewInstanceForReceive(
                                Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.arrayOffset() + buf.remaining()),
                                session
                        )
                );
            }
        }

        //???
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
    
    private void sendThrewSession(IoSession session, byte[] bytesToSend) {
        IoBuffer buffer = IoBuffer.allocate(bytesToSend.length);
        buffer.put(bytesToSend);
        buffer.flip();
        session.write(buffer);
        buffer.clear();
        buffer.free();
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
            sessions.stream().filter(s -> (s.equals(data.getAttachedSession()))).forEachOrdered(s -> {
                sendThrewSession(s, data.serialize());
            });
        }
    }

    @Override
    protected void NetworkConnection__handleReceivedData(NetworkConnectionData data) {
        this.commitData(data);
    }

    public static void main(String[] args) {
        final TcpServer server = new TcpServer(64300);
        server.addListener(new NetworkListener(){
            @Override
            public void error(Object iptr__, Object error) {
                System.out.println(error);
            }

            @Override
            public void connected(Object iptr__, Object src) {
                System.out.println("connected: " + src);
            }

            @Override
            public void disconnected(Object iptr__, Object src) {
                System.out.println("disconnected: " + src);
            }

            @Override
            public void finished(Object iptr__) {
            }

            @Override
            public void dataReceived(Object iptr__, NetworkConnectionData data) {
                server.send(data.serialize(), data.getAttachedSession());
            }
        });
        server.init();
        new Thread(){
            @Override
            public void run() {
                try {
                    sleep(30000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TcpServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                server.finish();
            }
        
            
        }.start();
    }
}