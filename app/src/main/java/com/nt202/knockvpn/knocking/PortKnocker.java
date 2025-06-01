package com.nt202.knockvpn.knocking;

import android.os.AsyncTask;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class PortKnocker {

    public static void knock(final String host, final ArrayList<PortWithProtocol> portsWithProtocols) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    for (final PortWithProtocol portWithProtocol : portsWithProtocols) {
                        switch (portWithProtocol.getProtocol()) {
                            case TCP:
                                sendTcpSyn(host, portWithProtocol.getPort());
                                break;
                            case UDP:
                                sendUdpPacket(host, portWithProtocol.getPort());
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private static void sendTcpSyn(String host, int port) {
        try (Socket socket = new Socket()) {
            // Short timeout to mimic "--max-retries 0"
            socket.connect(new InetSocketAddress(host, port), 100); // 100ms timeout
            // Immediately close to avoid full handshake
        } catch (Exception ignored) {}
    }

    private static void sendUdpPacket(String host, int port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] data = new byte[0];
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception ignored) {}
    }
}