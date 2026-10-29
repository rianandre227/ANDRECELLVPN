package com.andrecell.vpn;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class AddConfigActivity extends Activity {

    Spinner spinnerProtocol, spinnerTls;
    EditText etConfigName, etHost, etPort, etUdpgwPort, etUsername, etPassword, etSni, etPayload;
    CheckBox cbDns;
    Button btnSaveConfig, btnEditPayload;

    int editPosition = -1; // -1 artinya mode Tambah Baru, bukan Edit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_config);

        // Cek apakah activity ini dibuka untuk Edit Config
        editPosition = getIntent().getIntExtra("EDIT_POSITION", -1);

        spinnerProtocol = findViewById(R.id.spinnerProtocol);
        spinnerTls = findViewById(R.id.spinnerTls);
        etConfigName = findViewById(R.id.etConfigName);
        etHost = findViewById(R.id.etHost);
        etPort = findViewById(R.id.etPort);
        etUdpgwPort = findViewById(R.id.etUdpgwPort);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etSni = findViewById(R.id.etSni);
        etPayload = findViewById(R.id.etPayload);
        cbDns = findViewById(R.id.cbDns);
        btnSaveConfig = findViewById(R.id.btnSaveConfig);
        btnEditPayload = findViewById(R.id.btnEditPayload);

        final String[] protocols = {
                "SSH-Direct", "SSH-Proxy", "SSH-Payload", "SSH-Proxy-Payload",
                "SSH-TLS", "SSH-TLS-Proxy", "SSH-TLS-Payload", "SSH-TLS-Proxy-Payload", "SSH-DNSTT"
        };
        ArrayAdapter<String> adapterProtocol = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, protocols);
        spinnerProtocol.setAdapter(adapterProtocol);

        final String[] tlsVersions = {"DEFAULT", "TLS 1.2", "TLS 1.3"};
        ArrayAdapter<String> adapterTls = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tlsVersions);
        spinnerTls.setAdapter(adapterTls);

        // JIKA MODE EDIT, AMBIL DATA LAMA DAN TAMPILKAN DI FORM
        if (editPosition != -1) {
            btnSaveConfig.setText("UPDATE CONFIG");
            SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
            String jsonString = sharedPref.getString("CONFIG_LIST", "[]");
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                JSONObject obj = jsonArray.getJSONObject(editPosition);
                
                etConfigName.setText(obj.optString("name"));
                etHost.setText(obj.optString("host"));
                etPort.setText(obj.optString("port"));
                etUsername.setText(obj.optString("username"));
                etPassword.setText(obj.optString("password"));
                etSni.setText(obj.optString("sni"));
                etPayload.setText(obj.optString("payload"));
                cbDns.setChecked(obj.optBoolean("dns", true));

                // Set Spinner Protocol
                String protocolLama = obj.optString("protocol");
                int indexProtocol = Arrays.asList(protocols).indexOf(protocolLama);
                if (indexProtocol >= 0) spinnerProtocol.setSelection(indexProtocol);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        btnEditPayload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AddConfigActivity.this, "Silakan ketik payload manual di kolom bawah.", Toast.LENGTH_SHORT).show();
            }
        });

        // PROSES SIMPAN (BISA BARU ATAU UPDATE)
        btnSaveConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nama = etConfigName.getText().toString();
                String host = etHost.getText().toString();
                String protokol = spinnerProtocol.getSelectedItem().toString();

                if (nama.isEmpty() || host.isEmpty()) {
                    Toast.makeText(AddConfigActivity.this, "Nama (Remarks) dan Host wajib diisi!", Toast.LENGTH_SHORT).show();
                } else {
                    SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
                    String jsonString = sharedPref.getString("CONFIG_LIST", "[]");
                    
                    try {
                        JSONArray jsonArray = new JSONArray(jsonString);
                        JSONObject configBaru = new JSONObject();
                        
                        configBaru.put("name", nama);
                        configBaru.put("host", host);
                        configBaru.put("protocol", protokol);
                        configBaru.put("port", etPort.getText().toString());
                        configBaru.put("username", etUsername.getText().toString());
                        configBaru.put("password", etPassword.getText().toString());
                        configBaru.put("sni", etSni.getText().toString());
                        configBaru.put("dns", cbDns.isChecked());
                        configBaru.put("payload", etPayload.getText().toString());
                        
                        // Jika Edit, status lock tetap false (karena user mengedit config yang belum lock)
                        configBaru.put("locked", false); 
                        
                        if (editPosition != -1) {
                            // MODE UPDATE: Ganti data lama dengan data baru
                            jsonArray.put(editPosition, configBaru);
                        } else {
                            // MODE TAMBAH BARU
                            jsonArray.put(configBaru);
                        }
                        
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("CONFIG_LIST", jsonArray.toString());
                        editor.apply();

                        Toast.makeText(AddConfigActivity.this, "Config berhasil disimpan!", Toast.LENGTH_SHORT).show();
                        finish(); 
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(AddConfigActivity.this, "Gagal menyimpan data!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}