/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network;

/**
 *
 * @author kgn
 */
public interface NetworkConnectionData {
    
    public boolean isForTransmit();
    
    public byte[] serialize();
}
