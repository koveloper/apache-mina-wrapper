/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network.tcp.server;

import org.apache.mina.core.session.IoSession;

/**
 *
 * @author koban
 */
public class SessionEvent {
    private final IoSession session;
    private final int code;

    public SessionEvent(IoSession session, int code) {
        this.session = session;
        this.code = code;
    }

    public IoSession getSession() {
        return session;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "SessionEvent{" + "session=" + session + ", code=" + code + '}';
    }
}
