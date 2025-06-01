package com.nt202.knockvpn.vpn;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nt202.knockvpn.VpnActivity;
import com.nt202.knockvpn.R;

public class VpnFragment extends Fragment {
    private static final String TAG = VpnFragment.class.getSimpleName();

    private enum VpnState {
        DISCONNECTED,
        FAILED,
        CONNECTING,
        CONNECTED,
    }

    private final VpnActivity knockvpnrust = new VpnActivity();

    private VpnState currentState;

    private int SOCKS_PORT = 0;
    private int SOCKS_FD = -1;
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
//    public native String helloFromRust();


    private static final int VPN_REQUEST_CODE = 0x01;

    private AnimatorSet pulseAnimator;
    private ImageButton vpnToggle;
    private TextView connectionStatus;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vpn, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        knockvpnrust.initLogging();
        currentState = VpnState.DISCONNECTED;
        TextView text = view.findViewById(R.id.id_text);
//        text.setText(helloFromRust());
        vpnToggle = view.findViewById(R.id.vpn_toggle);
        connectionStatus = view.findViewById(R.id.connection_status);
        connectionStatus.setText(currentState.name());
        setupAnimations();
        setupButtonBehavior();
        setDisconnectedState();
        knockvpnrust.initLogging();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
//            final Intent intent = new Intent(this, SocksVpnService.class);
//            try {
//                intent.putExtra("socksPort", SOCKS_PORT);
//                intent.putExtra("socksFd", SOCKS_FD);
//            } catch (Exception e) {
//                Log.e(TAG, "startVpn: ", e);
//            }
//            startService(intent);
//        }
//    }

    private void setupAnimations() {
        pulseAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(this.getContext(), R.animator.pulse_animation);
        pulseAnimator.setTarget(vpnToggle);
    }

    private void setupButtonBehavior() {
        vpnToggle.setOnClickListener(v -> {
            switch (currentState) {
                case DISCONNECTED:
                    // 1. Is initial state
                    // 2. After connected we can disconnect
                    //    - close VPN (+ tun2socks)
                    //    - close Socks server
                    //    - close SSH connection
                    setConnectingState();
                    connect();
                    break;
                case FAILED:
                    // Print to logs why failed
                    // Red color for failed state
                    // NOTE: Can we fail at disconnecting?
                    // Something might happen with network => we should check current connections
                    break;
                case CONNECTING:
                    // 1. SSH connection
                    // 2. Socks server
                    // 3. VPN on Android
                    break;
                case CONNECTED:
                    this.getContext().sendBroadcast(new Intent("STOP_VPN_ACTION"));
                    setDisconnectedState();
                    break;
            }
        });
    }

    private void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String errorJson =                                                                                                                                                       knockvpnrust.startSocksServer("testuser", "192.168.0.103", 2222, "qwerty", "");

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
                SOCKS_PORT = knockvpnrust.getSocksPort();
                SOCKS_FD = knockvpnrust.getSocksFd();
                TextView digit = getView().findViewById(R.id.id_digit);
                digit.post(new Runnable() {
                    @Override
                    public void run() {
                        digit.setText("SOCKS_PORT: " + SOCKS_PORT + " " + "SOCKS_FD: " + SOCKS_FD);
                        createVpn();
                        setConnectedState();
                    }
                });
            }
        }).start();
    }

    private void createVpn() {
        final Intent intent = SocksVpnService.prepare(this.getContext());
        try {
            intent.putExtra("socksPort", SOCKS_PORT);
            intent.putExtra("socksFd", SOCKS_FD);
        } catch (Exception e) {
            Log.e(TAG, "startVpn: ", e);
        }
        if (intent != null) {
            // Request user permission
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            // Permission already granted
            final Intent grantedIntent = new Intent(this.getContext(), SocksVpnService.class);
            try {
                grantedIntent.putExtra("socksPort", SOCKS_PORT);
                grantedIntent.putExtra("socksFd", SOCKS_FD);
            } catch (Exception e) {
                Log.e(TAG, "startVpn: ", e);
            }
            this.getContext().startService(grantedIntent);
        }
    }

    private void setDisconnectedState() {
        currentState = VpnState.DISCONNECTED;
        vpnToggle.setImageResource(R.drawable.nothing);
        vpnToggle.setActivated(false);
        vpnToggle.setSelected(false);
        vpnToggle.setEnabled(true);
        connectionStatus.setText(VpnState.DISCONNECTED.name());
        pulseAnimator.end();
        vpnToggle.setScaleX(1.0f);
        vpnToggle.setScaleY(1.0f);
    }

    private void setFailedState() {
        currentState = VpnState.FAILED;
        vpnToggle.setImageResource(R.drawable.nothing);
        vpnToggle.setActivated(false);
        vpnToggle.setSelected(true);
        vpnToggle.setEnabled(true);
        connectionStatus.setText(VpnState.DISCONNECTED.name());
        pulseAnimator.end();
        vpnToggle.setScaleX(1.0f);
        vpnToggle.setScaleY(1.0f);
    }

    private void setConnectingState() {
        currentState = VpnState.CONNECTING;
        vpnToggle.setImageResource(R.drawable.knocking);
        vpnToggle.setActivated(true);
        vpnToggle.setEnabled(false);
        vpnToggle.setSelected(false);
        connectionStatus.setText(VpnState.CONNECTING.name());
        pulseAnimator.start();
    }

    private void setConnectedState() {
        currentState = VpnState.CONNECTED;
        vpnToggle.setImageResource(R.drawable.thumbsup);
        vpnToggle.setActivated(true);
        vpnToggle.setSelected(true);
        vpnToggle.setEnabled(true);
        connectionStatus.setText(VpnState.CONNECTED.name());
        pulseAnimator.end();
        vpnToggle.setScaleX(1.0f);
        vpnToggle.setScaleY(1.0f);
    }
}