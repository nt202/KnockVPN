package com.nt202.knockvpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import hev.htproxy.TProxyService;

public class SocksVpnService extends VpnService {
    private static final String TAG = "SocksVpnService";
    private ParcelFileDescriptor tunInterface;
    private TProxyService proxyService = null;

    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            killVpn();
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (proxyService == null) {
            proxyService = new TProxyService();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(stopReceiver, new IntentFilter("STOP_VPN_ACTION"), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(stopReceiver, new IntentFilter("STOP_VPN_ACTION"));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: NEW VPN");
        final int socksPort = intent.getIntExtra("socksPort", 0);
        final int socksFd = intent.getIntExtra("socksFd", 0);
        // Start VPN in a new thread to avoid blocking the UI
        new Thread(() -> {
            try {
                runVpn(socksPort, socksFd);
            } catch (Exception e) {
                Log.e(TAG, "VPN error", e);
                stopSelf();
            }
        }).start();
        return START_STICKY;
    }

    private void runVpn(int socksPort, int socksFd) {
        final boolean isProtected = protect(socksFd);
        if (!isProtected) {
            throw new IllegalStateException("SOCKS socket protection failed");
        }

        try {
            // Configure the TUN interface
            Builder builder = new Builder();
            builder.setSession("KnockVPN");
            builder.addAddress("10.0.0.2", 24); // Virtual IP for the device
            builder.addRoute("0.0.0.0", 0); // Route all traffic through the VPN
            builder.addDnsServer("8.8.8.8"); // Prevent DNS leaks
            builder.addDnsServer("1.1.1.1"); // Prevent DNS leaks
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.allowFamily(OsConstants.AF_INET);
                    builder.addDisallowedApplication(getPackageName()); // Self
//                    builder.addDisallowedApplication("com.nt202.knockvpn");
//                    builder.addDisallowedApplication("com.termux");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("VPN", "App not found: " + e.getMessage());
            }
            // Create the TUN interface
            tunInterface = builder.establish();

            // Get the TUN file descriptor (needed for tun2socks)
            int tunFd = tunInterface.getFd();

            // Start tun2socks binary (see step 3 for setup)
            startTun2Socks(socksPort, tunFd);
        } catch (Exception e) {
            Log.e(TAG, "VPN setup failed", e);
        }
    }

    private void startTun2Socks(int socksPort, int tunFd) throws IOException {

        Log.i(TAG, "startTun2Socks: " + tunFd);

        final String contentTemplate;
        try {
            contentTemplate = RawFileReader.getRawFileAsString(this, R.raw.config);
            // Use the content string
        } catch (IOException e) {
            e.printStackTrace();
            return;
            // Handle the exception
        }
        final String content = contentTemplate.replace("socksPort", socksPort + "");

        /* TProxy */
        File configFile = new File(getCacheDir(), "config.yaml");
        try {
            configFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(configFile, false);
            fos.write(content.getBytes());
            fos.close();
        } catch (IOException e) {
            return;
        }

        proxyService.TProxyStartService(configFile.getAbsolutePath(), tunFd);
        getStats();
    }

    private void getStats() {
        long[] stats = proxyService.TProxyGetStats();
        for (long stat : stats) {
            Log.i(TAG, "getStats: stats: " + stat);
        }
//         Process stats: tx_packets, tx_bytes, rx_packets, rx_bytes
    }

    private void killVpn() {
        if (tunInterface != null) {
            try {
                tunInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "TUN close error", e);
            }
        }
        if (proxyService != null) {
            try {
                proxyService.TProxyStopService();
            } catch (Exception e) {
                Log.e(TAG, "Proxy close error", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(stopReceiver);
        killVpn();
        super.onDestroy();
    }
}