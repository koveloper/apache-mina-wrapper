/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network;

import com.koveloper.apache.mina.wrapper.network.tcp.server.SessionEvent;
import com.koveloper.apache.mina.wrapper.utils.TasksThread;
import java.util.LinkedList;
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

    protected NetworkConnection(String taskThreadName) {
        tasks = new TasksThread(taskThreadName + "-" + System.currentTimeMillis()) {
            @Override
            protected void handleTask(Object task) {
                if (task instanceof Integer) {
                    switch ((int) task) {
                        case OPERATION__INIT:
                            NetworkConnection.this.NetworkConnection__init();
                            break;
                        case OPERATION__CONNECT:
                            NetworkConnection.this.NetworkConnection__connect();
                            break;
                        case OPERATION__CONNECTED:
                            NetworkConnection.this.NetworkConnection__connected(null);
                            break;
                        case OPERATION__DISCONNECTED:
                            NetworkConnection.this.NetworkConnection__disconnected(null);
                            break;
                        case OPERATION__FINISH:
                            NetworkConnection.this.NetworkConnection__finish();
                            NetworkConnection.this.NetworkConnection__disconnected(null);
                            this.finish();
                            break;
                        default:
                            break;
                    }
                } else if (task instanceof NetworkConnectionData) {
                    if (((NetworkConnectionData) task).isForTransmit()) {
                        NetworkConnection.this.NetworkConnection__send((NetworkConnectionData) task);
                    } else {
                        NetworkConnection.this.NetworkConnection__handleReceivedData((NetworkConnectionData) task);
                    }
                } else if(task instanceof SessionEvent) {
                    SessionEvent evt = (SessionEvent) task;
                    switch (evt.getCode()) {
                        case OPERATION__INIT:
                            NetworkConnection.this.NetworkConnection__init();
                            break;
                        case OPERATION__CONNECT:
                            NetworkConnection.this.NetworkConnection__connect();
                            break;
                        case OPERATION__CONNECTED:
                            NetworkConnection.this.NetworkConnection__connected(evt.getSession());
                            break;
                        case OPERATION__DISCONNECTED:
                            NetworkConnection.this.NetworkConnection__disconnected(evt.getSession());
                            break;
                        case OPERATION__FINISH:
                            NetworkConnection.this.NetworkConnection__finish();
                            NetworkConnection.this.NetworkConnection__disconnected(null);
                            this.finish();
                            break;
                        default:
                            break;
                    }
                }
            }
        };
    }

    public final void init() {
        if(this.finished) {
            return;
        }
        tasks.addTask(OPERATION__INIT);
    }
    
    public final void finish() {
        if(this.finished) {
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
        for(NetworkListener l : listeners) {
            l.connected(this, src);
        }
    }
    
    protected void commitDisconnected(Object src) {
        for(NetworkListener l : listeners) {
            l.disconnected(this, src);
        }
    }
    
    protected void commitFinished() {
        for(NetworkListener l : listeners) {
            l.finished(this);
        }
    }
    
    protected void commitError(Object error) {
        for(NetworkListener l : listeners) {
            l.error(this, error);
        }
    }
    
    protected void commitData(NetworkConnectionData data) {
        for(NetworkListener l : listeners) {
            l.dataReceived(this, data);
        }
    }
    
    public final void send(NetworkConnectionData data) {
        if(this.finished) {
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

    protected abstract void NetworkConnection__init();

    protected abstract void NetworkConnection__connect();
    
    protected abstract void NetworkConnection__connected(Object src);
    
    protected abstract void NetworkConnection__disconnected(Object src);

    protected abstract void NetworkConnection__send(NetworkConnectionData data);

    protected abstract void NetworkConnection__handleReceivedData(NetworkConnectionData data);
    
    protected abstract void NetworkConnection__finish();

}
