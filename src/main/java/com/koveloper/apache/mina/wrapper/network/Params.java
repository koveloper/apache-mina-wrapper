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
public class Params {
 
    public static class TcpClient {
        public static int CONNECT_TIMEOUT_MS    = 10000;
    }
    
    public static class TcpServer {
        public static int SESSION_MIN_BUF_SIZE  = 256;
    }
}
