package com.andrecell.vpn;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class ExportConfigActivity extends Activity {

    EditText etFilename, etPassword, etExpiry, etDeviceIds, etMessage, etServerMessage;
    CheckBox cbLock, cbCloud, cbMobileData, cbBlockRooted, cbOfficialStore;
    Button btnDoExport;

    String configName, configHost, configProtocol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_config);

        // Ambil data config dari Intent
        configName = getIntent().getStringExtra("CONFIG_NAME");
        configHost = getIntent().getStringExtra("CONFIG_HOST");
        configProtocol = getIntent().getStringExtra("CONFIG_PROTOCOL");

        // Inisialisasi UI
        etFilename = findViewById(R.id.etExportFilename);
        etPassword = findViewById(R.id.etExportPassword);
        etExpiry = findViewById(R.id.etExportExpiry);
        etDeviceIds = findViewById(R.id.etExportDeviceIds);
        etMessage = findViewById(R.id.etExportMessage);
        etServerMessage = findViewById(R.id.etExportServerMessage);

        cbLock = findViewById(R.id.cbLockConfig);
        cbCloud = findViewById(R.id.cbExportCloud);
        cbMobileData = findViewById(R.id.cbMobileData);
        cbBlockRooted = findViewById(R.id.cbBlockRooted);
        cbOfficialStore = findViewById(R.id.cbOfficialStore);

        btnDoExport = findViewById(R.id.btnDoExport);

        // Set nama default di filename
        etFilename.setText(configName);

        // Setup Date Picker untuk Expiry
        etExpiry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(ExportConfigActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                etExpiry.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        // Tombol Export
        btnDoExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doExport();
            }
        });
    }

    private void doExport() {
        String filename = etFilename.getText().toString();
        if (filename.isEmpty()) {
            Toast.makeText(this, "Nama file tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Kumpulkan semua data ke dalam JSON
            JSONObject exportObj = new JSONObject();
            exportObj.put("name", filename);
            exportObj.put("host", configHost);
            exportObj.put("protocol", configProtocol);
            
            // Status Lock
            exportObj.put("locked", cbLock.isChecked());
            
            // Data tambahan
            exportObj.put("password", etPassword.getText().toString());
            exportObj.put("expiry", etExpiry.getText().toString());
            exportObj.put("device_ids", etDeviceIds.getText().toString());
            exportObj.put("message", etMessage.getText().toString());
            exportObj.put("server_message", etServerMessage.getText().toString());
            
            // Opsi keamanan
            exportObj.put("cloud", cbCloud.isChecked());
            exportObj.put("mobile_only", cbMobileData.isChecked());
            exportObj.put("block_rooted", cbBlockRooted.isChecked());
            exportObj.put("official_store", cbOfficialStore.isChecked());

            // Encode ke Base64
            String jsonStr = exportObj.toString();
            String base64Str = Base64.encodeToString(jsonStr.getBytes(), Base64.NO_WRAP);
            String exportLink = "ANDREVPN://" + base64Str;

            // Buka Menu Share
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, exportLink);
            startActivity(Intent.createChooser(shareIntent, "Bagikan Config via"));

            // Tutup halaman ini setelah export
            finish();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal Export Config", Toast.LENGTH_SHORT).show();
        }
    }
}