/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.midomis.apache.mina.wrapper.network;

import com.midomis.apache.mina.wrapper.network.tcp.client.TcpClient;
import com.midomis.apache.mina.wrapper.network.tcp.server.TcpServer;
import com.midomis.apache.mina.wrapper.network.udp.Udp;
import com.midomis.thread.utils.TasksThread;
import com.midomis.thread.utils.TasksThreadInterface;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koveloper
 */
public class Launcher {

    public static void main(String[] args) {
        new Launcher(args);
    }

    private NetworkConnection connection = null;
//    private Udp udp = null;
//    private TcpClient tcp = null;
    private TcpServer server = null;
    private final TasksThread tasks = new TasksThread("t");

    private String type = "ktp";
    private String host = "localhost";
    private int exitOnFault = 0;
    private int port = 64300;
    private int id = 56;
    private int uid = 20;
    private int hz = 10;
    private int baudrate = 10000;
    private int testDurationSeconds = 3600;
    private int pauseAfterTestInSeconds = 20;
    private boolean connected = false;
    private final ByteArrayOutputStream sendedBytes = new ByteArrayOutputStream();
    private final SecureRandom secureRandom = new SecureRandom();
    private int testNumber = 0;
    private int successTests = 0;
    private int faultTests = 0;

    private final NetworkListener netListener = new NetworkListener() {
        @Override
        public void error(Object iptr__, Object error) {
        }

        @Override
        public void connected(Object iptr__, Object src) {
            if (type.equals("ktp")) {
                connection.send(("user link command b" + id + "d" + uid).getBytes());
            }
            new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    connected = true;
                }
            }.start();
        }

        @Override
        public void disconnected(Object iptr__, Object src) {
            connected = false;
        }

        @Override
        public void finished(Object iptr__) {
        }

        @Override
        public void dataReceived(Object iptr__, NetworkConnectionData data) {
            synchronized (sendedBytes) {
                byte[] sended = sendedBytes.toByteArray();
                byte[] received = data.serialize();
                for (int i = 0; i < (sended.length - received.length) + 1; i++) {
                    if (Arrays.equals(received, Arrays.copyOfRange(sended, i, i + received.length))) {
                        sendedBytes.reset();
                        if (i != 0) {
                            sendedBytes.write(sended, 0, i);
                            System.out.println("Delete middle portion: " + new String(received));
                        }
                        sendedBytes.write(sended, i + received.length, sended.length - received.length - i);
                        return;
                    }
                }
                System.out.println("NO EQUALS to: " + new String(received));
            }
        }
    };

    private int toHex(int v) {
        if(v >= 10) {
            return 65 + v - 10;
        }
        return 48 + v;
    } 
    
    private void test() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        long startMoment = System.currentTimeMillis();
        int portionInBytes = baudrate / 8 / hz;
        long txIndex = 0;
        testNumber++;
        System.out.println("");
        System.out.println("TEST #" + testNumber + " (duration: " + testDurationSeconds + " seconds)");
        System.out.println("baudrate: " + baudrate);
        System.out.println("bytes per tx-portion: " + portionInBytes + " (" + hz + " times in second)");
        while ((System.currentTimeMillis() - startMoment) < (testDurationSeconds * 1000)) {
            if (connected) {
                byte[] hex = secureRandom.generateSeed(portionInBytes / 2);
                
                byte[] bytes = new byte[hex.length * 2];
                
                int offset = 0;
                
                for(byte b : hex) {
                    int h = (b >> 4) & 15;
                    int l = b & 15;
                    bytes[offset++] = (byte)toHex(h);
                    bytes[offset++] = (byte)toHex(l);
                }
                
                String index = Long.toHexString(txIndex++).toUpperCase();
                while (index.length() < 16) {
                    index = "0" + index;
                }
                index += ":";
                String testNumStr = "" + testNumber;
                while (testNumStr.length() < 5) {
                    testNumStr = "0" + testNumStr;
                }
                testNumStr += "-";
                if (index.length() < bytes.length) {
                    System.arraycopy(index.getBytes(), 0, bytes, 0, index.length());
                    System.arraycopy(testNumStr.getBytes(), 0, bytes, 0, testNumStr.length());
                }
                bytes[bytes.length - 2] = 13;
                bytes[bytes.length - 1] = 10;
                try {
                    synchronized (sendedBytes) {
                        sendedBytes.write(bytes);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
                }
                connection.send(bytes);
            }
            try {
                Thread.sleep(1000 / hz);
            } catch (InterruptedException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void pause() {
        int seconds = 0;
        System.out.println("PAUSE after TEST #" + testNumber);
        while (seconds++ < pauseAfterTestInSeconds) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("sendedBytes.size(): " + sendedBytes.size());
            if (sendedBytes.size() == 0) {
                break;
            }
        }
        if (sendedBytes.size() != 0) {
            System.err.println("Delivery data errors during TEST #" + testNumber);
            System.err.println("sendedBytes.size(): " + sendedBytes.size());
            System.err.println(NetworkConnectionDefaultData.getNewInstanceForTransmit(sendedBytes.toByteArray()));
            System.err.println(new String(sendedBytes.toByteArray()));
            sendedBytes.reset();
            faultTests++;
            if (exitOnFault == 1) {
                System.exit(-1);
            }
        } else {
            successTests++;
            System.out.println("TEST #" + testNumber + " IS OK!");
        }

        System.out.println("OVERALL STATS: success=" + successTests + " faults=" + faultTests);
    }

    public Launcher(String[] args) {
        System.out.println("---------------------");
        System.out.println("cli arguments:");
        System.out.println(String.format("\ttype=ktp|direct|udp (default %s)", type));
        System.out.println(String.format("\thost={server_ip} (default %s)", host));
        System.out.println(String.format("\tport={server_port} (default %d)", port));
        System.out.println(String.format("\tid={remote_modem_id} (default %d)", id));
        System.out.println(String.format("\tuid={remote_modem_iface_uid} (default %d, only for ktp type connection)", uid));
        System.out.println(String.format("\tbaudrate={data_flow_baudrate_bps} (default %d)", baudrate));
        System.out.println(String.format("\thz={tx operations per seconds} (default %d)", hz));
        System.out.println(String.format("\ttestDurationSeconds={single test duration in seconds} (default %d)", testDurationSeconds));
        System.out.println(String.format("\tpauseAfterTestInSeconds={pause after single test in seconds} (default %d)", pauseAfterTestInSeconds));
        System.out.println(String.format("\texitOnFault={1 - exit on any fault test} (default %d)", exitOnFault));
        for (String arg : args) {
            if (arg.startsWith("type=")) {
                type = arg.replace("type=", "");
            }
            if (arg.startsWith("host=")) {
                host = arg.replace("host=", "");
            }
            if (arg.startsWith("port=")) {
                port = Integer.parseInt(arg.replace("port=", ""));
            }
            if (arg.startsWith("id=")) {
                id = Integer.parseInt(arg.replace("id=", ""));
            }
            if (arg.startsWith("uid=")) {
                uid = Integer.parseInt(arg.replace("uid=", ""));
            }
            if (arg.startsWith("baudrate=")) {
                baudrate = Integer.parseInt(arg.replace("baudrate=", ""));
            }
            if (arg.startsWith("hz=")) {
                hz = Integer.parseInt(arg.replace("hz=", ""));
            }
            if (arg.startsWith("testDurationSeconds=")) {
                testDurationSeconds = Integer.parseInt(arg.replace("testDurationSeconds=", ""));
            }
            if (arg.startsWith("pauseAfterTestInSeconds=")) {
                pauseAfterTestInSeconds = Integer.parseInt(arg.replace("pauseAfterTestInSeconds=", ""));
            }
            if (arg.startsWith("exitOnFault=")) {
                exitOnFault = Integer.parseInt(arg.replace("exitOnFault=", ""));
            }
        }
        System.out.println("---------------------");
        System.out.println("launch params:");
        System.out.println("\ttype: " + type);
        System.out.println("\thost: " + host);
        System.out.println("\tport: " + port);
        System.out.println("\tid: " + id);
        System.out.println("\tuid: " + uid);
        System.out.println("\tbaudrate: " + baudrate);
        System.out.println("\thz: " + hz);
        System.out.println("\ttestDurationSeconds: " + testDurationSeconds);
        System.out.println("\tpauseAfterTestInSeconds: " + pauseAfterTestInSeconds);
        System.out.println("\texitOnFault: " + exitOnFault);

//        server = new TcpServer(64400);
//        server.addListener(new NetworkListener() {
//            @Override
//            public void error(Object iptr__, Object error) {
//            }
//
//            @Override
//            public void connected(Object iptr__, Object src) {
//            }
//
//            @Override
//            public void disconnected(Object iptr__, Object src) {
//            }
//
//            @Override
//            public void finished(Object iptr__) {
//            }
//
//            @Override
//            public void dataReceived(Object iptr__, NetworkConnectionData data) {
//                server.send(data.serialize());
//            }
//        });
//        server.init();
        connection = "udp".equals(type) 
                ? new Udp().setDefaultRemoteHost(host, port, false) 
                : new TcpClient(host, port);
        connection.addListener(netListener);
        tasks.setIface(new TasksThreadInterface() {
            @Override
            public void started() {
            }

            @Override
            public void handleTask(Object task) {
                if ("test".equals(task)) {
                    test();
                    tasks.addTask("pause");
                } else if ("pause".equals(task)) {
                    pause();
                    tasks.addTask("test");
                }
            }

            @Override
            public void finished() {
            }
        });
        tasks.addTask("test");
        connection.init();
        if("udp".equals(type)) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    connected = true;
                }
            }.start();
        }
    }
}
