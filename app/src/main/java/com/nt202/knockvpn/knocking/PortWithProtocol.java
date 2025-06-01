package com.nt202.knockvpn.knocking;

import java.io.Serializable;

public class PortWithProtocol implements Serializable {
    private int port;
    private Protocol protocol;

    public PortWithProtocol(int port, Protocol protocol) {
        this.port = port;
        this.protocol = protocol;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public Protocol getProtocol() {
        return protocol;
    }
}
