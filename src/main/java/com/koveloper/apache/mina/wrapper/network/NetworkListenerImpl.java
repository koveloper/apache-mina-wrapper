/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network;

/**
 *
 * @author koveloper
 */
public class NetworkListenerImpl implements NetworkListener{
    
    @Override
    public void error(Object iptr__, Object error) {}
    
    @Override
    public void connected(Object iptr__, Object src) {}
    
    @Override
    public void disconnected(Object iptr__, Object src) {}
    
    @Override
    public void finished(Object iptr__) {}
    
    @Override
    public void dataReceived(Object iptr__, NetworkConnectionData data) {}
}
