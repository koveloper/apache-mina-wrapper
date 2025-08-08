/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.midomis.apache.mina.wrapper.network;

import com.midomis.apache.mina.wrapper.network.tcp.server.SessionEvent;
import com.midomis.thread.utils.TasksThread;
import com.midomis.thread.utils.TasksThreadInterfaceAdapter;
import java.util.LinkedList;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author kgn
 */
public abstract class NetworkConnection {

    public static final int OPERATION__INIT = 1;
    public static final int OPERATION__CONNECT = 2;
    public static final int OPERATION__CONNECTED = 3;
    public static final int OPERATION__DISCONNECTED = 4;
    public static final int OPERATION__FINISH = 5;
    private final LinkedList<NetworkListener> listeners = new LinkedList<>();
    private boolean finished = false;

    private TasksThread tasks = null;
    private final TasksThreadInterfaceAdapter tasksIface = new TasksThreadInterfaceAdapter() {
        @Override
        public void handleTask(Object task) {
            if (task instanceof Integer) {
                switch ((int) task) {
                    case OPERATION__INIT -> NetworkConnection.this.NetworkConnection__init();
                    case OPERATION__CONNECT -> NetworkConnection.this.NetworkConnection__connect();
                    case OPERATION__CONNECTED -> NetworkConnection.this.NetworkConnection__connected(null);
                    case OPERATION__DISCONNECTED -> NetworkConnection.this.NetworkConnection__disconnected(null);
                    case OPERATION__FINISH -> {
                        NetworkConnection.this.NetworkConnection__finish();
                        NetworkConnection.this.NetworkConnection__disconnected(null);
                        tasks.finish();
                    }
                    default -> {
                    }
                }
            } else if (task instanceof NetworkConnectionData networkConnectionData) {
                if (networkConnectionData.isForTransmit()) {
                    NetworkConnection.this.NetworkConnection__send(networkConnectionData);
                } else {
                    NetworkConnection.this.NetworkConnection__handleReceivedData(networkConnectionData);
                }
            } else if (task instanceof SessionEvent evt) {
                switch (evt.getCode()) {
                    case OPERATION__INIT -> NetworkConnection.this.NetworkConnection__init();
                    case OPERATION__CONNECT -> NetworkConnection.this.NetworkConnection__connect();
                    case OPERATION__CONNECTED -> NetworkConnection.this.NetworkConnection__connected(evt.getSession());
                    case OPERATION__DISCONNECTED -> NetworkConnection.this.NetworkConnection__disconnected(evt.getSession());
                    case OPERATION__FINISH -> {
                        NetworkConnection.this.NetworkConnection__finish();
                        NetworkConnection.this.NetworkConnection__disconnected(null);
                        tasks.finish();
                    }
                    default -> {
                    }
                }
            }
        }

    };

    protected NetworkConnection(String taskThreadName) {
        tasks = new TasksThread(taskThreadName + "-" + System.currentTimeMillis())
                .setIface(tasksIface);
    }

    public final void init() {
        if (this.finished) {
            return;
        }
        tasks.addTask(OPERATION__INIT);
    }

    public final void finish() {
        if (this.finished) {
            return;
        }
        this.finished = true;
        tasks.addTask(OPERATION__FINISH);
    }

    protected final void invokeEvent(Object event) {
        tasks.addTask(event);
    }

    public final void addListener(NetworkListener l) {
        listeners.add(l);
    }

    public final void removeListener(NetworkListener l) {
        listeners.remove(l);
    }

    public boolean isFinished() {
        return this.finished;
    }

    protected void commitConnected(Object src) {
        listeners.forEach(l -> {
            l.connected(this, src);
        });
    }

    protected void commitDisconnected(Object src) {
        listeners.forEach(l -> {
            l.disconnected(this, src);
        });
    }

    protected void commitFinished() {
        listeners.forEach(l -> {
            l.finished(this);
        });
    }

    protected void commitError(Object error) {
        listeners.forEach(l -> {
            l.error(this, error);
        });
    }

    protected void commitData(NetworkConnectionData data) {
        listeners.forEach(l -> {
            l.dataReceived(this, data);
        });
    }

    public final void send(NetworkConnectionData data) {
        if (this.finished) {
            return;
        }
        this.invokeEvent(data);
    }

    public final void send(byte[] data) {
        this.send(NetworkConnectionDefaultData.getNewInstanceForTransmit(data));
    }

    public final void send(byte[] data, IoSession sessionTo) {
        this.send(NetworkConnectionDefaultData.getNewInstanceForTransmit(data, sessionTo));
    }

    protected void sendThrewSession(IoSession session, byte[] bytesToSend) {
        IoBuffer buffer = IoBuffer.allocate(bytesToSend.length);
        buffer.put(bytesToSend);
        buffer.flip();
        session.write(buffer);
        buffer.clear();
        buffer.free();
    }

    protected abstract void NetworkConnection__init();

    protected abstract void NetworkConnection__connect();

    protected abstract void NetworkConnection__connected(Object src);

    protected abstract void NetworkConnection__disconnected(Object src);

    protected abstract void NetworkConnection__send(NetworkConnectionData data);

    protected abstract void NetworkConnection__handleReceivedData(NetworkConnectionData data);

    protected abstract void NetworkConnection__finish();

}
