package com.nt202.knockvpn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class VpnActivity extends AppCompatActivity {
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
//        TextView text = findViewById(R.id.id_text);
//        text.setText(helloFromRust());
//
//        TextView concatenatedText = findViewById(R.id.id_concatenated);
//        concatenatedText.setText(concatenateStrings("Mystring1", "Mystring2"));
//    }

    public native int sumFromRust(int a, int b);
    public native String helloFromRust();
    public native String concatenateStrings(String a, String b);

    static {
        System.loadLibrary("knockvpnrust");
    }


    private static final int VPN_REQUEST_CODE = 0x01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn);

//        copyTun2SocksBinary(); // Call the helper method above

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> startVpn());
    }

    private void startVpn() {
        Intent intent = SocksVpnService.prepare(this);
        if (intent != null) {
            // Request user permission
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            // Permission already granted
            startService(new Intent(this, SocksVpnService.class));
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