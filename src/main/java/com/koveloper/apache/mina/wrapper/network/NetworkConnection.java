/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network;

import com.koveloper.apache.mina.wrapper.utils.TasksThread;
import java.util.LinkedList;

/**
 *
 * @author kgn
 */
public abstract class NetworkConnection {

    public static final int OPERATION__INIT = 1;
    public static final int OPERATION__CONNECT = 2;
    public static final int OPERATION__CONNECTED = 3;
    public static final int OPERATION__DISCONNECTED = 4;
    private final LinkedList<NetworkListener> listeners = new LinkedList<>();

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
                            NetworkConnection.this.NetworkConnection__connected();
                            break;
                        case OPERATION__DISCONNECTED:
                            NetworkConnection.this.NetworkConnection__disconnected();
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
                }
            }
        };
    }

    public void init() {
        tasks.addTask(OPERATION__INIT);
    }

    protected void invokeEvent(Object event) {
        tasks.addTask(event);
    }

    public void addListener(NetworkListener l) {
        listeners.add(l);
    }

    public void removeListener(NetworkListener l) {
        listeners.remove(l);
    }
    
    protected void destroyTasksHandler() {
        this.tasks.finish();
    }
    
    protected void commitConnected() {
        for(NetworkListener l : listeners) {
            l.connected(this);
        }
    }
    
    protected void commitDisconnected() {
        for(NetworkListener l : listeners) {
            l.disconnected(this);
        }
    }
    
    protected void commitFinished() {
        for(NetworkListener l : listeners) {
            l.finished(this);
        }
    }
    
    protected void commitData(NetworkConnectionData data) {
        for(NetworkListener l : listeners) {
            l.dataReceived(this, data);
        }
    }
    
    public void send(NetworkConnectionData data) {
        this.invokeEvent(data);
    }
    
    public void send(byte[] data) {
        this.invokeEvent(NetworkConnectionDefaultData.getNewInstanceForTransmit(data));
    }

    protected abstract void NetworkConnection__init();

    protected abstract void NetworkConnection__connect();
    
    protected abstract void NetworkConnection__connected();
    
    protected abstract void NetworkConnection__disconnected();

    protected abstract void NetworkConnection__send(NetworkConnectionData data);

    protected abstract void NetworkConnection__handleReceivedData(NetworkConnectionData data);

}
