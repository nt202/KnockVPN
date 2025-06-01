package com.nt202.knockvpn;

public class VpnActivity {
    public native int getSocksPort(); // todo refactoring?
    public native int getSocksFd(); // todo refactoring?
    public native void initLogging();
    //    public native String concatenateStrings(String a, String b);
    public native String startSocksServer(String username, String address, int port, String password, String key);

    static {
        System.loadLibrary("knockvpnrust");
    }
}
