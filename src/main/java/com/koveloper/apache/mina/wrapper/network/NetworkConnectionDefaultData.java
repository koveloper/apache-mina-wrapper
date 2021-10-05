/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network;

import java.util.Arrays;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author kgn
 */
public class NetworkConnectionDefaultData implements NetworkConnectionData {
    
    private byte[] data = null;
    private boolean isForTransmit = false;
    private IoSession session = null;
    
    private NetworkConnectionDefaultData(boolean isForTransmit, byte[] data) {
        this(isForTransmit, data, null);
    }
    
    
    private NetworkConnectionDefaultData(boolean isForTransmit, byte[] data, IoSession session) {
        this.data = data;
        this.isForTransmit = isForTransmit;
        this.session = session;
    }
    
    public static NetworkConnectionDefaultData getNewInstanceForTransmit(byte[] data) {
        return new NetworkConnectionDefaultData(true, data);
    }
    
    public static NetworkConnectionDefaultData getNewInstanceForTransmit(byte[] data, IoSession session) {
        return new NetworkConnectionDefaultData(true, data, session);
    }
    
    public static NetworkConnectionDefaultData getNewInstanceForReceive(byte[] data) {
        return new NetworkConnectionDefaultData(false, data);
    }
    
    public static NetworkConnectionDefaultData getNewInstanceForReceive(byte[] data, IoSession session) {
        return new NetworkConnectionDefaultData(false, data, session);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public IoSession getAttachedSession() {
        return session;
    }

    @Override
    public boolean isForTransmit() {
        return isForTransmit;
    }
    
    @Override
    public byte[] serialize() {
        return data;
    }

    @Override
    public String toString() {
        return "NetworkConnectionDefaultData{" + "data=" + data + ", isForTransmit=" + isForTransmit + ", session=" + session + '}';
    }
}
