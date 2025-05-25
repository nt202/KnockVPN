package hev.htproxy;

public class TProxyService {
    public native void TProxyStartService(String configPath, int fd);
    public native void TProxyStopService();
    public native long[] TProxyGetStats();

    static {
        System.loadLibrary("hev-socks5-tunnel");
    }
}