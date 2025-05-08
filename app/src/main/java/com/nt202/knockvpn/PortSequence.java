package com.nt202.knockvpn;

import java.io.Serializable;
import java.util.ArrayList;

public class PortSequence implements Serializable {
    private String host;
    private ArrayList<PortWithProtocol> sequence;

    public PortSequence(final String host, ArrayList<PortWithProtocol> sequence) {
        this.host = host;
        this.sequence = sequence;
    }

    public String getHost() {
        return host;
    }

    public ArrayList<PortWithProtocol> getSequence() {
        return sequence;
    }
}