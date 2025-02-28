/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network.udp;

import com.koveloper.apache.mina.wrapper.network.NetworkConnection;
import com.koveloper.apache.mina.wrapper.network.NetworkConnectionData;
import com.koveloper.apache.mina.wrapper.network.NetworkConnectionDefaultData;
import com.koveloper.thread.utils.TasksThread;
import com.koveloper.thread.utils.TasksThreadInterface;
import com.koveloper.thread.utils.TasksThreadInterfaceAdapter;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 *
 * @author koveloper
 */
public class Udp extends NetworkConnection {

    private static final Logger LOG = Logger.getLogger(Udp.class);
    
    private static final int MAKE_RX = 1;
    private Integer port = null;
    private SocketAddress dstHost = null;
    private boolean captureDstHost = false;
    private DatagramChannel channel = null;
    private TasksThread rxThread = null;
    private final TasksThreadInterface rxThreadIface = new TasksThreadInterfaceAdapter() {
        ByteBuffer buffer = ByteBuffer.allocate(4096);

        @Override
        public void handleTask(Object task) {
            try {
                SocketAddress remoteAdd = channel.receive(buffer);
                if (captureDstHost) {
                    dstHost = remoteAdd;
                }
                buffer.flip();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                buffer.flip();
                buffer.limit(buffer.capacity());
                Udp.this.invokeEvent(
                        NetworkConnectionDefaultData.getNewInstanceForReceive(bytes, remoteAdd)
                );
            } catch (Exception ex) {
                Udp.this.commitError(ex);
            }
            if(rxThread != null) {
                rxThread.addTask(MAKE_RX);
            }
        }
    };

    public Udp() {
        this(null);
    }

    public Udp(Integer port) {
        super(Udp.class.getCanonicalName());
        if (port != null && port > 0 && port < 65535) {
            this.port = port;
        }
    }

    public Udp setDefaultRemoteHost(String ip, int port, boolean captureRemoteHost) {
        if (ip != null && port > 0 && port < 65535) {
            dstHost = new InetSocketAddress(ip, port);
        }
        this.captureDstHost = captureRemoteHost;
        return this;
    }

    @Override
    protected void NetworkConnection__init() {
        NetworkConnection__finish();
        this.invokeEvent(OPERATION__CONNECT);
    }

    @Override
    protected void NetworkConnection__connect() {
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(true);
            channel.bind(this.port == null ? null : new InetSocketAddress(this.port));
            rxThread = new TasksThread(Udp.class.getCanonicalName() + "-rx")
                    .setIface(rxThreadIface);
            rxThread.addTask(MAKE_RX);
        } catch (IOException ex) {
            LOG.log(Level.ERROR, "connect error", ex);
            this.commitError(ex);
        }
    }

    @Override
    protected void NetworkConnection__connected(Object src) {
    }

    @Override
    protected void NetworkConnection__disconnected(Object src) {
    }

    @Override
    protected void NetworkConnection__send(NetworkConnectionData data) {
        try {
            if (channel == null) {
                return;
            }
            SocketAddress dst = data.getDestination();
            if (dst == null && dstHost == null) {
                return;
            }
            if (dst == null) {
                dst = dstHost;
            }
            ByteBuffer buf = ByteBuffer.wrap(data.serialize());
            channel.send(buf, dst);
        } catch (IOException ex) {
            LOG.log(Level.ERROR, "send error", ex);
            this.commitError(ex);
        }
    }

    @Override
    protected void NetworkConnection__handleReceivedData(NetworkConnectionData data) {
        this.commitData(data);
    }

    @Override
    protected void NetworkConnection__finish() {
        if (channel == null) {
            return;
        }
        rxThread.finish();
        try {
            channel.close();
        } catch (IOException ex) {
            LOG.log(Level.ERROR, "finish error", ex);
            this.commitError(ex);
        }
        rxThread = null;
        channel = null;
    }
}
