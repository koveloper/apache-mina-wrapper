/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network;

import java.util.Arrays;

/**
 *
 * @author kgn
 */
public class NetworkConnectionDefaultData implements NetworkConnectionData {
    
    private byte[] data = null;
    private boolean isForTransmit = false;
    
    private NetworkConnectionDefaultData(boolean isForTransmit, byte[] data) {
        this.data = data;
        this.isForTransmit = isForTransmit;
    }
    
    public static NetworkConnectionDefaultData getNewInstanceForTransmit(byte[] data) {
        return new NetworkConnectionDefaultData(true, data);
    }
    
    public static NetworkConnectionDefaultData getNewInstanceForReceive(byte[] data) {
        return new NetworkConnectionDefaultData(false, data);
    }

    public byte[] getData() {
        return data;
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
        return "NetworkConnectionDefaultData{" + "data=" + Arrays.toString(data) + ", isForTransmit=" + isForTransmit + '}';
    }
}
