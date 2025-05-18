package com.nt202.knockvpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SocksVpnService extends VpnService {
    private static final String TAG = "SocksVpnService";
    private ParcelFileDescriptor tunInterface;
    private Process tun2socksProcess;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: NEW VPN");
        // Start VPN in a new thread to avoid blocking the UI
        new Thread(() -> {
            try {
                runVpn();
            } catch (Exception e) {
                Log.e(TAG, "VPN error", e);
                stopSelf();
            }
        }).start();
        return START_STICKY;
    }

    private void runVpn() {
        try {
            // Configure the TUN interface
            Builder builder = new Builder();
            builder.setSession("KnockVPN");
            builder.addAddress("10.0.0.2", 24); // Virtual IP for the device
            builder.addRoute("0.0.0.0", 0); // Route all traffic through the VPN
            builder.addDnsServer("8.8.8.8"); // Prevent DNS leaks

            // Create the TUN interface
            tunInterface = builder.establish();

            // Get the TUN file descriptor (needed for tun2socks)
            int tunFd = tunInterface.getFd();

            // Start tun2socks binary (see step 3 for setup)
            startTun2Socks(tunFd);
        } catch (Exception e) {
            Log.e(TAG, "VPN setup failed", e);
        }
    }

    private void startTun2Socks(int tunFd) throws IOException {

        Log.i(TAG, "startTun2Socks: " + tunFd);
//        // Path to the compiled tun2socks binary (placed in assets/)
//        String binaryPath = getFilesDir() + "/tun2socks";
//
//        // Command to run tun2socks
//        String[] command = {
//                binaryPath,
//                "--netif-ipaddr", "10.0.0.1", // Gateway IP (matches TUN subnet)
//                "--netif-netmask", "255.255.255.0",
//                "--socks-server-addr", "127.0.0.1:1080", // Your SOCKS proxy
//                "--tunfd", String.valueOf(tunFd),
//                "--tunmtu", "1500",
//                "--loglevel", "info"
//        };
//
//        // Execute the binary
//        tun2socksProcess = new ProcessBuilder(command)
//                .redirectErrorStream(true)
//                .start();
//
//        // Log output from tun2socks (optional)
//        new Thread(() -> {
//            try (BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(tun2socksProcess.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    Log.d(TAG, "tun2socks: " + line);
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "tun2socks output error", e);
//            }
//        }).start();
    }

    @Override
    public void onDestroy() {
        // Cleanup
        if (tun2socksProcess != null) {
            tun2socksProcess.destroy();
        }
        if (tunInterface != null) {
            try {
                tunInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "TUN close error", e);
            }
        }
        super.onDestroy();
    }
}