package com.andrecell.vpn;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class LogsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        
        TextView tvLogs = findViewById(R.id.tvLogContent);
        
        // Baca file log debug asli
        File logFile = new File(getFilesDir(), "vpn_debug.log");
        
        if (logFile.exists()) {
            StringBuilder text = new StringBuilder();
            try {
                FileInputStream fis = new FileInputStream(logFile);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line).append('\n');
                }
                br.close();
            } catch (Exception e) {
                text.append("Gagal membaca log: ").append(e.getMessage());
            }
            tvLogs.setText(text.toString());
        } else {
            tvLogs.setText("> Belum ada log. Silakan coba klik CONNECT.");
        }
    }
}
