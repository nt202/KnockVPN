package com.nt202.knockvpn;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class VpnActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView digit = findViewById(R.id.id_digit);
        int d = sumFromRust(2, 5);
        digit.setText(String.valueOf(d));

        TextView text = findViewById(R.id.id_text);
        text.setText(helloFromRust());
    }

    public native int sumFromRust(int a, int b);
    public native String helloFromRust();

    static {
        System.loadLibrary("knockvpnrust");
    }
}