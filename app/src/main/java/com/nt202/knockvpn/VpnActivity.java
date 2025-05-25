package com.nt202.knockvpn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.atomic.AtomicInteger;

public class VpnActivity extends AppCompatActivity {
    private int PORT = 0;
    private static final  String TAG = VpnActivity.class.getSimpleName();
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_vpn);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//        TextView digit = findViewById(R.id.id_digit);
//        int d = sumFromRust(2, 5);
//        digit.setText(String.valueOf(d));
//

//
//        TextView concatenatedText = findViewById(R.id.id_concatenated);
//        concatenatedText.setText(concatenateStrings("Mystring1", "Mystring2"));
//    }

//    public native int sumFromRust(int a, int b);
    public native String helloFromRust();
    public native int getServerPort();
    public native void initLogging();
//    public native String concatenateStrings(String a, String b);
    public native String startSocksServer(String username, String address, int port, String password, String key);

    static {
        System.loadLibrary("knockvpnrust");
    }

    private static final int VPN_REQUEST_CODE = 0x01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initLogging();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn);

        TextView text = findViewById(R.id.id_text);
        text.setText(helloFromRust());

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String errorJson = startSocksServer("testuser", "192.168.0.103", 2222, "qwerty", "");

                Log.i(TAG, "errorJson: " + errorJson);
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                int port = getServerPort();
                PORT = port;
                TextView digit = findViewById(R.id.id_digit);
                digit.post(new Runnable() {
                    @Override
                    public void run() {
                        digit.setText(port + "");
                    }
                });
            }
        }).start();

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> startVpn());
    }

    private void startVpn() {
        Intent intent = SocksVpnService.prepare(this);
        try {
            intent.putExtra("socksPort", PORT);
        } catch (Exception e) {
            Log.e(TAG, "startVpn: ", e);
        }
        if (intent != null) {
            // Request user permission
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            // Permission already granted
            final Intent grantedIntent = new Intent(this, SocksVpnService.class);
            try {
                grantedIntent.putExtra("socksPort", PORT);
            } catch (Exception e) {
                Log.e(TAG, "startVpn: ", e);
            }
            startService(grantedIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startService(new Intent(this, SocksVpnService.class));
        }
    }
}