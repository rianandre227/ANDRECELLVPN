package com.andrecell.vpn;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLDecoder;

public class V2rayActivity extends Activity {

    Spinner spProtocol, spTransport, spSecurity;
    EditText etName, etServer, etPort, etId, etFlow, etPath, etHost, etSni, etAlpn, etFingerprint;
    Switch swInsecure;
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v2ray);

        // Inisialisasi UI
        spProtocol = findViewById(R.id.spV2Protocol);
        spTransport = findViewById(R.id.spV2Transport);
        spSecurity = findViewById(R.id.spV2Security);
        
        etName = findViewById(R.id.etV2Name);
        etServer = findViewById(R.id.etV2Server);
        etPort = findViewById(R.id.etV2Port);
        etId = findViewById(R.id.etV2Id);
        etFlow = findViewById(R.id.etV2Flow);
        etPath = findViewById(R.id.etV2Path);
        etHost = findViewById(R.id.etV2Host);
        etSni = findViewById(R.id.etV2Sni);
        etAlpn = findViewById(R.id.etV2Alpn);
        etFingerprint = findViewById(R.id.etV2Fingerprint);
        swInsecure = findViewById(R.id.swInsecure);
        
        btnSave = findViewById(R.id.btnUpdateV2ray);

        // Setup Dropdown
        String[] protocols = {"VMESS", "VLESS", "TROJAN"};
        spProtocol.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, protocols));

        String[] transports = {"TCP", "WebSocket", "gRPC", "HTTPUpgrade"};
        spTransport.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, transports));

        String[] securities = {"None", "TLS", "REALITY"};
        spSecurity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, securities));

        // ==========================================
        // LOGIKA PEMBUKAAN HALAMAN (EDIT vs IMPORT)
        // ==========================================
        int editPos = getIntent().getIntExtra("EDIT_POSITION", -1);
        String importLink = getIntent().getStringExtra("IMPORT_LINK");

        if (editPos != -1) {
            loadDataForEdit(editPos);
        } else if (importLink != null && !importLink.isEmpty()) {
            parseV2rayLink(importLink);
        }

        // Tombol Save/Update
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfig();
            }
        });
    }

    // FUNGSI LOAD DATA LAMA UNTUK DIEDIT
    private void loadDataForEdit(int position) {
        SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
        String jsonString = sharedPref.getString("CONFIG_LIST", "[]");

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            JSONObject obj = jsonArray.getJSONObject(position);
            
            String savedName = obj.getString("name");
            String savedHost = obj.getString("host"); 

            // Set Nama dari database
            etName.setText(savedName);

            // Jika data lama berupa link mentah, parse ulang agar form terisi
            if (savedHost.startsWith("vmess://") || savedHost.startsWith("vless://") || savedHost.startsWith("trojan://")) {
                parseV2rayLink(savedHost);
                etName.setText(savedName); // Kembalikan nama asli yang mungkin diedit admin
            } else if (savedHost.startsWith("{")) {
                // Jika JSON (dari input manual), baca datanya
                JSONObject detail = new JSONObject(savedHost);
                etServer.setText(detail.optString("server"));
                etPort.setText(detail.optString("port"));
                etId.setText(detail.optString("id"));
                etFlow.setText(detail.optString("flow"));
                etPath.setText(detail.optString("path"));
                etHost.setText(detail.optString("host"));
                etSni.setText(detail.optString("sni"));
                etAlpn.setText(detail.optString("alpn"));
                etFingerprint.setText(detail.optString("fingerprint"));
                swInsecure.setChecked(detail.optBoolean("insecure", false));
                
                setSpinnerValue(spProtocol, obj.getString("protocol"));
                setSpinnerValue(spTransport, detail.optString("transport"));
                setSpinnerValue(spSecurity, detail.optString("security"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // FUNGSI PARSER: MEMBACA LINK MENJADI FORM
    private void parseV2rayLink(String link) {
        try {
            if (link.startsWith("vmess://")) {
                String base64Data = link.substring(8);
                String jsonString = new String(Base64.decode(base64Data, Base64.DEFAULT));
                JSONObject json = new JSONObject(jsonString);

                setSpinnerValue(spProtocol, "VMESS");
                // VMESS mengambil nama dari field "ps" di dalam JSON
                etName.setText(json.optString("ps", "Imported VMESS"));
                etServer.setText(json.optString("add"));
                etPort.setText(json.optString("port"));
                etId.setText(json.optString("id"));
                etPath.setText(json.optString("path"));
                etHost.setText(json.optString("host"));
                etSni.setText(json.optString("sni"));
                
                String net = json.optString("net", "tcp");
                setSpinnerValue(spTransport, net.equals("ws") ? "WebSocket" : net.toUpperCase());
                
                String tls = json.optString("tls", "none");
                setSpinnerValue(spSecurity, tls.equals("tls") ? "TLS" : "None");

            } else if (link.startsWith("vless://") || link.startsWith("trojan://")) {
                String protocol = link.split("://")[0].toUpperCase();
                setSpinnerValue(spProtocol, protocol);

                // MENGAMBIL NAMA DARI CLIPBOARD (Setelah tanda #)
                if (link.contains("#")) {
                    String[] parts = link.split("#");
                    String rawName = parts[1];
                    // Decode URL (mengubah %20 menjadi spasi, dll)
                    etName.setText(URLDecoder.decode(rawName, "UTF-8"));
                    link = parts[0]; // Sisa link tanpa nama
                } else {
                    etName.setText("Imported " + protocol);
                }

                if (link.contains("@")) {
                    String[] parts = link.split("@");
                    String id = parts[0].split("://")[1];
                    etId.setText(URLDecoder.decode(id, "UTF-8"));

                    String serverPart = parts[1];
                    String server = serverPart.split(":")[0];
                    String portAndParams = serverPart.split(":")[1];

                    etServer.setText(server);
                    
                    if (portAndParams.contains("?")) {
                        String[] portSplit = portAndParams.split("\\?");
                        etPort.setText(portSplit[0]);

                        String params = portSplit[1];
                        for (String param : params.split("&")) {
                            String[] kv = param.split("=");
                            if (kv.length == 2) {
                                String key = kv[0];
                                String value = URLDecoder.decode(kv[1], "UTF-8");
                                if (key.equals("type")) setSpinnerValue(spTransport, value.equals("ws") ? "WebSocket" : value.toUpperCase());
                                if (key.equals("security")) setSpinnerValue(spSecurity, value.toUpperCase());
                                if (key.equals("sni")) etSni.setText(value);
                                if (key.equals("path")) etPath.setText(value);
                                if (key.equals("host")) etHost.setText(value);
                            }
                        }
                    } else {
                        etPort.setText(portAndParams);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membaca format link.", Toast.LENGTH_LONG).show();
        }
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }

    // FUNGSI SIMPAN KE DATABASE
    private void saveConfig() {
        // MENGAMBIL NAMA DARI KOLOM INPUT
        String name = etName.getText().toString();
        String protocol = spProtocol.getSelectedItem().toString();

        if (name.isEmpty() || etServer.getText().toString().isEmpty()) {
            Toast.makeText(this, "Name dan Server wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
        String jsonString = sharedPref.getString("CONFIG_LIST", "[]");

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            JSONObject obj = new JSONObject();

            // MENYIMPAN NAMA YANG SUDAH DI-PARSE
            obj.put("name", name);
            obj.put("protocol", protocol);
            obj.put("locked", false);

            JSONObject detail = new JSONObject();
            detail.put("server", etServer.getText().toString());
            detail.put("port", etPort.getText().toString());
            detail.put("id", etId.getText().toString());
            detail.put("flow", etFlow.getText().toString());
            detail.put("transport", spTransport.getSelectedItem().toString());
            detail.put("path", etPath.getText().toString());
            detail.put("host", etHost.getText().toString());
            detail.put("security", spSecurity.getSelectedItem().toString());
            detail.put("sni", etSni.getText().toString());
            detail.put("alpn", etAlpn.getText().toString());
            detail.put("fingerprint", etFingerprint.getText().toString());
            detail.put("insecure", swInsecure.isChecked());

            obj.put("host", detail.toString());

            int editPos = getIntent().getIntExtra("EDIT_POSITION", -1);
            if (editPos != -1) {
                jsonArray.put(editPos, obj);
            } else {
                jsonArray.put(obj);
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("CONFIG_LIST", jsonArray.toString());
            editor.apply();

            Toast.makeText(this, "Config berhasil disimpan!", Toast.LENGTH_SHORT).show();
            finish();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}