package com.andrecell.vpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    SharedPreferences sharedPref;

    // Edit Texts
    EditText etRemoteDnsServer, etBootstrapDns;

    // Switches
    Switch swAdBlocker, swGamingMode, swBatterySaver, swShowSpeed, swMultiplexing, swIpv6, swBypassPrivate, swAllowVpnUpstream;

    // TextViews for Dropdowns
    TextView tvRemoteDnsType, tvDoubleVpn, tvLogLevel, tvMtuPreset, tvTunStack, tvPerApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        // 1. Inisialisasi EditText
        etRemoteDnsServer = findViewById(R.id.etRemoteDnsServer);
        etBootstrapDns = findViewById(R.id.etBootstrapDns);

        // 2. Inisialisasi Switch
        swAdBlocker = findViewById(R.id.swAdBlocker);
        swGamingMode = findViewById(R.id.swGamingMode);
        swBatterySaver = findViewById(R.id.swBatterySaver);
        swShowSpeed = findViewById(R.id.swShowSpeed);
        swMultiplexing = findViewById(R.id.swMultiplexing);
        swIpv6 = findViewById(R.id.swIpv6);
        swBypassPrivate = findViewById(R.id.swBypassPrivate);
        swAllowVpnUpstream = findViewById(R.id.swAllowVpnUpstream);

        // 3. Inisialisasi TextView (Dropdown)
        tvRemoteDnsType = findViewById(R.id.tvRemoteDnsType);
        tvDoubleVpn = findViewById(R.id.tvDoubleVpn);
        tvLogLevel = findViewById(R.id.tvLogLevel);
        tvMtuPreset = findViewById(R.id.tvMtuPreset);
        tvTunStack = findViewById(R.id.tvTunStack);
        tvPerApp = findViewById(R.id.tvPerApp);

        // Load Data Tersimpan
        loadData();

        // Simpan otomatis saat Switch diubah
        setupSwitchListener(swAdBlocker, "AD_BLOCKER");
        setupSwitchListener(swGamingMode, "GAMING_MODE");
        setupSwitchListener(swBatterySaver, "BATTERY_SAVER");
        setupSwitchListener(swShowSpeed, "SHOW_SPEED");
        setupSwitchListener(swMultiplexing, "MULTIPLEXING");
        setupSwitchListener(swIpv6, "IPV6");
        setupSwitchListener(swBypassPrivate, "BYPASS_PRIVATE");
        setupSwitchListener(swAllowVpnUpstream, "ALLOW_VPN_UPSTREAM");

        // Simpan otomatis saat EditText diubah (DNS)
        setupTextListener(etRemoteDnsServer, "REMOTE_DNS_SERVER");
        setupTextListener(etBootstrapDns, "BOOTSTRAP_DNS");

        // ==========================================
        // FITUR POP-UP DROPDOWN (BISA DIKLIK)
        // ==========================================
        
        String[] dnsTypes = {"DoH (HTTPS)", "DoT (TLS)", "TCP", "UDP"};
        setupDropdown(findViewById(R.id.rowRemoteDnsType), tvRemoteDnsType, "REMOTE_DNS_TYPE", dnsTypes, "  >");

        String[] vpnModes = {"Disabled", "Enabled"};
        setupDropdown(findViewById(R.id.rowDoubleVpn), tvDoubleVpn, "DOUBLE_VPN", vpnModes, "  >");

        String[] logLevels = {"debug", "info", "warn", "error", "none"};
        setupDropdown(findViewById(R.id.rowLogLevel), tvLogLevel, "LOG_LEVEL", logLevels, "  >");

        String[] mtuPresets = {"Standard (1500)", "Gaming (1280)", "Custom"};
        setupDropdown(findViewById(R.id.rowMtuPreset), tvMtuPreset, "MTU_PRESET", mtuPresets, "  >");

        String[] tunStacks = {"gVisor", "System", "Mixed"};
        setupDropdown(findViewById(R.id.rowTunStack), tvTunStack, "TUN_STACK", tunStacks, "  >");

        String[] perAppRouting = {"All apps (VPN enabled)", "Bypass selected apps", "Only selected apps"};
        setupDropdown(findViewById(R.id.rowPerApp), tvPerApp, "PER_APP_ROUTING", perAppRouting, "  >");
    }

    private void loadData() {
        // Load EditText
        etRemoteDnsServer.setText(sharedPref.getString("REMOTE_DNS_SERVER", "8.8.8.8"));
        etBootstrapDns.setText(sharedPref.getString("BOOTSTRAP_DNS", "8.8.4.4"));

        // Load Switch
        swAdBlocker.setChecked(sharedPref.getBoolean("AD_BLOCKER", false));
        swGamingMode.setChecked(sharedPref.getBoolean("GAMING_MODE", false));
        swBatterySaver.setChecked(sharedPref.getBoolean("BATTERY_SAVER", false));
        swShowSpeed.setChecked(sharedPref.getBoolean("SHOW_SPEED", true));
        swMultiplexing.setChecked(sharedPref.getBoolean("MULTIPLEXING", false));
        swIpv6.setChecked(sharedPref.getBoolean("IPV6", false));
        swBypassPrivate.setChecked(sharedPref.getBoolean("BYPASS_PRIVATE", true));
        swAllowVpnUpstream.setChecked(sharedPref.getBoolean("ALLOW_VPN_UPSTREAM", false));

        // Load Dropdown Texts
        tvRemoteDnsType.setText(sharedPref.getString("REMOTE_DNS_TYPE", "DoH (HTTPS)") + "  >");
        tvDoubleVpn.setText(sharedPref.getString("DOUBLE_VPN", "Disabled") + "  >");
        tvLogLevel.setText(sharedPref.getString("LOG_LEVEL", "warn") + "  >");
        tvMtuPreset.setText(sharedPref.getString("MTU_PRESET", "Standard (1500)") + "  >");
        tvTunStack.setText(sharedPref.getString("TUN_STACK", "gVisor") + "  >");
        tvPerApp.setText(sharedPref.getString("PER_APP_ROUTING", "All apps (VPN enabled)") + "  >");
    }

    private void setupSwitchListener(final Switch sw, final String key) {
        sw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPref.edit().putBoolean(key, sw.isChecked()).apply();
            }
        });
    }

    private void setupTextListener(final EditText et, final String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sharedPref.edit().putString(key, s.toString()).apply();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Fungsi Pop-up Dropdown Universal
    private void setupDropdown(View parentRow, final TextView targetTextView, final String prefKey, final String[] options, final String suffix) {
        parentRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                
                String currentValue = targetTextView.getText().toString().replace(suffix, "");
                int checkedItem = 0;
                for (int i = 0; i < options.length; i++) {
                    if (options[i].equals(currentValue)) {
                        checkedItem = i;
                        break;
                    }
                }

                builder.setSingleChoiceItems(options, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedValue = options[which];
                        targetTextView.setText(selectedValue + suffix);
                        sharedPref.edit().putString(prefKey, selectedValue).apply();
                        dialog.dismiss();
                    }
                });
                
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });
    }
}