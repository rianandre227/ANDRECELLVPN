package com.andrecell.vpn;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ConfigListActivity extends Activity {

    ListView listView;
    Button btnTambah;
    ArrayList<ConfigModel> listConfig;
    ConfigAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_list);

        listView = findViewById(R.id.listViewConfigs);
        btnTambah = findViewById(R.id.btnTambahConfig);

        btnTambah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddMenuDialog();
            }
        });
    }

    private void showAddMenuDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_config);
        dialog.setCancelable(true);

        TextView tvV2ray = dialog.findViewById(R.id.tvV2ray);
        TextView tvSsh = dialog.findViewById(R.id.tvSsh);
        TextView tvCancel = dialog.findViewById(R.id.tvCancel);

        tvSsh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startActivity(new Intent(ConfigListActivity.this, AddConfigActivity.class));
            }
        });

        tvV2ray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showImportV2rayDialog();
            }
        });

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showImportV2rayDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_import_v2ray);
        dialog.setCancelable(true);

        final EditText etImportLink = dialog.findViewById(R.id.etImportLink);
        Button btnImport = dialog.findViewById(R.id.btnImport);

        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String link = etImportLink.getText().toString().trim();
                
                if (link.isEmpty()) {
                    Toast.makeText(ConfigListActivity.this, "Link tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                } else if (link.startsWith("ANDREVPN://")) {
                    // ==========================================
                    // FITUR BARU: IMPORT DARI TEMAN/PELANGGAN
                    // ==========================================
                    importEncryptedConfig(link);
                    dialog.dismiss();
                } else if (!link.startsWith("vmess://") && !link.startsWith("vless://") && !link.startsWith("trojan://")) {
                    Toast.makeText(ConfigListActivity.this, "Format link salah! Harus vmess://, vless://, trojan://, atau ANDREVPN://", Toast.LENGTH_LONG).show();
                } else {
                    dialog.dismiss();
                    Intent intent = new Intent(ConfigListActivity.this, V2rayActivity.class);
                    intent.putExtra("IMPORT_LINK", link);
                    startActivity(intent);
                }
            }
        });

        dialog.show();
    }

    // ==========================================
    // FUNGSI MEMBACA LINK RAHASIA ANDREVPN://
    // ==========================================
    private void importEncryptedConfig(String link) {
        try {
            // 1. Buang awalan ANDREVPN://
            String base64Data = link.substring(11);
            
            // 2. Decode dari Base64 menjadi teks JSON
            String jsonString = new String(Base64.decode(base64Data, Base64.DEFAULT));
            
            // 3. Ubah teks JSON menjadi objek
            JSONObject importedObj = new JSONObject(jsonString);
            
            // Ambil data aslinya
            String name = importedObj.getString("name");
            String host = importedObj.getString("host");
            String protocol = importedObj.getString("protocol");
            boolean isLocked = importedObj.optBoolean("locked", true); // Pastikan terkunci

            // 4. Simpan ke Database aplikasi kita
            SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
            String currentList = sharedPref.getString("CONFIG_LIST", "[]");
            JSONArray jsonArray = new JSONArray(currentList);

            JSONObject newConfig = new JSONObject();
            newConfig.put("name", name);
            newConfig.put("host", host);
            newConfig.put("protocol", protocol);
            newConfig.put("locked", isLocked);

            jsonArray.put(newConfig);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("CONFIG_LIST", jsonArray.toString());
            editor.apply();

            Toast.makeText(this, "Config dari Admin berhasil di-import!", Toast.LENGTH_LONG).show();
            loadDataFromMemory();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal Import. Link mungkin rusak atau tidak valid.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDataFromMemory();
    }

    private void loadDataFromMemory() {
        listConfig = new ArrayList<>();
        SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
        String jsonString = sharedPref.getString("CONFIG_LIST", "[]");

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String name = obj.getString("name");
                String host = obj.getString("host"); 
                String protocol = obj.getString("protocol");
                boolean isLocked = obj.optBoolean("locked", false);
                
                listConfig.add(new ConfigModel(name, host, protocol, isLocked));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        adapter = new ConfigAdapter(this, listConfig);
        listView.setAdapter(adapter);
    }
}