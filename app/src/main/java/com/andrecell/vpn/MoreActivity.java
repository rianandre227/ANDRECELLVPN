package com.andrecell.vpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.util.UUID;

public class MoreActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        findViewById(R.id.menuDeviceId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate Random ID
                String uniqueID = UUID.randomUUID().toString();
                new AlertDialog.Builder(MoreActivity.this)
                    .setTitle("Device ID")
                    .setMessage(uniqueID)
                    .setPositiveButton("OK", null).show();
            }
        });

        findViewById(R.id.menuSettingsApp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MoreActivity.this, SettingsActivity.class));
            }
        });

        findViewById(R.id.menuVersion).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MoreActivity.this)
                    .setTitle("Version Info")
                    .setMessage("Andre Cell VPN\nVersion 1.0.0\nBuild 2026")
                    .setPositiveButton("OK", null).show();
            }
        });

        findViewById(R.id.menuAbout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MoreActivity.this)
                    .setTitle("About Creator")
                    .setMessage("Dikembangkan oleh:\nAndre Cell Dondong\n\n© 2026 All Rights Reserved.")
                    .setPositiveButton("OK", null).show();
            }
        });
    }
}