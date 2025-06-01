package com.nt202.knockvpn.vpn;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.nt202.knockvpn.VpnActivity;
import com.nt202.knockvpn.R;

public class VpnFragment extends Fragment {
    private static final String TAG = VpnFragment.class.getSimpleName();
    private static final String PREFS_NAME = "VpnOverSshPrefs";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_AUTH_METHOD = "auth_method"; // "password" or "private_key"
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_PRIVATE_KEY = "private_key";

    private enum VpnState {
        DISCONNECTED,
        FAILED,
        CONNECTING,
        CONNECTED,
    }

    private final VpnActivity knockvpnrust = new VpnActivity();
    private SharedPreferences prefs;
    private VpnState currentState;

    private int SOCKS_PORT = 0;
    private int SOCKS_FD = -1;

    private AnimatorSet pulseAnimator;
    private ImageButton vpnToggle;
    private TextView connectionStatus;
    private ActivityResultLauncher<Intent> vpnPermissionLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vpn, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vpnPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        final int resultCode = result.getResultCode();
                        if (resultCode == RESULT_OK) {
                            Toast.makeText(getContext(), "VPN permission granted!", Toast.LENGTH_SHORT).show();
                            showSettingsDialog();
                        } else {
                            showNoVpnAccessToast();
                        }
                    }
                });
    }

    private void showNoVpnAccessToast() {
        Toast.makeText(this.getContext(), "VPN permission was not granted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        prefs = this.getContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        knockvpnrust.initLogging();
        currentState = VpnState.DISCONNECTED;
        final Button buttonSettings = view.findViewById(R.id.change_settings);
        buttonSettings.setOnClickListener(v -> {
            final Intent intent = SocksVpnService.prepare(this.getContext());
            if (intent != null) {
                // Request user permission
                vpnPermissionLauncher.launch(intent);
            } else {
                showSettingsDialog();
            }
        });

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

    private void showSettingsDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        final LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_ssh_setup, null);
        builder.setView(dialogView);
        builder.setTitle("SSH Configuration");
        final EditText etHost = dialogView.findViewById(R.id.etHost);
        final EditText etPort = dialogView.findViewById(R.id.etPort);
        final EditText etPassword = dialogView.findViewById(R.id.etPassword);
        final EditText etPrivateKey = dialogView.findViewById(R.id.etPrivateKey);
        final ImageButton btnTogglePassword = dialogView.findViewById(R.id.btnTogglePassword);
        final ImageButton btnTogglePrivateKey = dialogView.findViewById(R.id.btnTogglePrivateKey);

        // Authentication sections
        final RadioGroup authMethodGroup = dialogView.findViewById(R.id.authMethodGroup);
        final RadioButton rbPassword = dialogView.findViewById(R.id.rbPassword);
        final RadioButton rbPrivateKey = dialogView.findViewById(R.id.rbPrivateKey);
        final LinearLayout passwordSection = dialogView.findViewById(R.id.passwordSection);
        final LinearLayout privateKeySection = dialogView.findViewById(R.id.privateKeySection);


        // Load saved values
        etHost.setText(prefs.getString(KEY_HOST, ""));
        etPort.setText(String.valueOf(prefs.getInt(KEY_PORT, 22)));

        String authMethod = prefs.getString(KEY_AUTH_METHOD, "password");
        if ("private_key".equals(authMethod)) {
            rbPrivateKey.setChecked(true);
            privateKeySection.setVisibility(View.VISIBLE);
        } else {
            rbPassword.setChecked(true);
            passwordSection.setVisibility(View.VISIBLE);
        }

        etPassword.setText(prefs.getString(KEY_PASSWORD, ""));
        etPrivateKey.setText(prefs.getString(KEY_PRIVATE_KEY, ""));

        // Setup toggle listeners
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility(etPassword, btnTogglePassword));
        btnTogglePrivateKey.setOnClickListener(v -> togglePrivateKeyVisibility(etPrivateKey, btnTogglePrivateKey));

        // Authentication method toggle
        authMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPassword) {
                passwordSection.setVisibility(View.VISIBLE);
                privateKeySection.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbPrivateKey) {
                passwordSection.setVisibility(View.GONE);
                privateKeySection.setVisibility(View.VISIBLE);
            }
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Get values
            String host = etHost.getText().toString().trim();
            int port = 22;
            try {
                port = Integer.parseInt(etPort.getText().toString());
            } catch (NumberFormatException e) {
                // Default to 22
            }

            String authMethodToSave = rbPassword.isChecked() ? "password" : "private_key";
            String password = etPassword.getText().toString().trim();
            String privateKey = etPrivateKey.getText().toString().trim();

            // Save to SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_HOST, host);
            editor.putInt(KEY_PORT, port);
            editor.putString(KEY_AUTH_METHOD, authMethodToSave);

            if (rbPassword.isChecked()) {
                editor.putString(KEY_PASSWORD, password);
                // Clear private key when not used
                editor.putString(KEY_PRIVATE_KEY, "");
            } else {
                editor.putString(KEY_PRIVATE_KEY, privateKey);
                // Clear password when not used
                editor.putString(KEY_PASSWORD, "");
            }

            editor.apply();
//            Toast.makeText(this, "SSH settings saved", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();


    }

    private void togglePasswordVisibility(EditText editText, ImageButton button) {
        int inputType = editText.getInputType();
        if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            // Show password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            button.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            // Hide password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            button.setImageResource(android.R.drawable.ic_menu_view);
        }
        // Move cursor to end
        editText.setSelection(editText.getText().length());
    }

    private void togglePrivateKeyVisibility(EditText editText, ImageButton button) {
        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            // Show private key
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            button.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            // Hide private key
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            button.setImageResource(android.R.drawable.ic_menu_view);
        }

        // Maintain multiline properties
        editText.setMinLines(3);
        editText.setGravity(Gravity.TOP);

        // Move cursor to end
        editText.setSelection(editText.getText().length());
    }



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
        if (intent != null) {
            showNoVpnAccessToast();
        } else {
            startServiceWithNewIntent();
        }
    }

    private void startServiceWithNewIntent() {
        final Intent intent = new Intent(this.getContext(), SocksVpnService.class);
        intent.putExtra("socksPort", SOCKS_PORT);
        intent.putExtra("socksFd", SOCKS_FD);
        this.getContext().startService(intent);
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