/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.utils;

import java.util.LinkedList;

/**
 *
 * @author kgn
 */
public abstract class TasksThread extends Thread {

    private final LinkedList<Object> queue = new LinkedList<>();
    private final Object mutex = new Object();
    private boolean terminated = false;
    private String str = "";
    private int priority = Thread.NORM_PRIORITY;

    public TasksThread(String name) {
        str = name;
    }

    public TasksThread(String name, int priority) {
        str = name;
        this.priority = priority;
    }

    @Override
    public void run() {
        setPriority(priority);
        setName(str);
        while (!terminated) {
            LinkedList<Object> clone = null;
            synchronized (mutex) {
                clone = (LinkedList<Object>) queue.clone();
                queue.clear();
            }
            if (clone != null) {
                for (Object o : clone) {
                    handleTask(o);
                }
            }
            try {
                synchronized (mutex) {
                    if(queue.isEmpty() && !terminated) {
                        mutex.wait();
                    }
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void addTask(Object task) {
        if(terminated) {
            return;
        }
        synchronized (mutex) {
            if (!isAlive()) {
                try {
                    start();
                } catch (Exception e) {
                }
            }
            queue.add(task);
            mutex.notify();
        }
    }
    
    public void finish() {
        synchronized (mutex) {
            if (!isAlive()) {
                try {
                    start();
                } catch (Exception e) {
                }
            }
            terminated = true;
            mutex.notify();
        }
    }

    protected abstract void handleTask(Object task);

    @Override
    public String toString() {
        return "TasksThread{" + "str=" + str + '}';
    }    
}
