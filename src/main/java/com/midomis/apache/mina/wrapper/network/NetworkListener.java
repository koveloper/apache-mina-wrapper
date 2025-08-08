/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.midomis.apache.mina.wrapper.network;

/**
 *
 * @author kgn
 */
public interface NetworkListener {
    
    public void error(Object iptr__, Object error);
    
    public void connected(Object iptr__, Object src);
    
    public void disconnected(Object iptr__, Object src);
    
    public void finished(Object iptr__);
    
    public void dataReceived(Object iptr__, NetworkConnectionData data);
}
