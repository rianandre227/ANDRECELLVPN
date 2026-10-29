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
        
        TextView tvLogs = findViewById(R.id.tvLogs);
        
        // Baca file log error asli
        File logFile = new File(getFilesDir(), "vpn_error.log");
        
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
            tvLogs.setText("> Tidak ada error yang tercatat.\n\n> Jika VPN gagal menyala (tidak ada ikon kunci), berarti kemungkinan error terjadi di dalam library native (C++) yang tidak bisa ditangkap oleh Java, atau aplikasi berhasil menyala tapi koneksi internetnya diblokir.");
        }
    }
}
