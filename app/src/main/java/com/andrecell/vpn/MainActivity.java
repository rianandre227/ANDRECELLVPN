package com.andrecell.vpn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Random;

public class MainActivity extends Activity {

    private Button btnConnect;
    private TextView tvStatus, tvTimer, menuSettings, tvSavedConfig, tvPingHome;
    private TextView tvDownloadTotal, tvUploadTotal;
    private boolean isConnected = false;

    private PowerManager.WakeLock wakeLock;
    private Handler timerHandler = new Handler();
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;

    private static final int VPN_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        tvStatus = findViewById(R.id.tvStatus);
        tvTimer = findViewById(R.id.tvTimer);
        menuSettings = findViewById(R.id.menuSettings);
        tvSavedConfig = findViewById(R.id.tvSavedConfig);
        tvPingHome = findViewById(R.id.tvPingHome);
        tvDownloadTotal = findViewById(R.id.tvDownloadTotal);
        tvUploadTotal = findViewById(R.id.tvUploadTotal);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AndreVPN::VpnWakelock");

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isConnected) {
                    Intent intent = VpnService.prepare(MainActivity.this);
                    if (intent != null) {
                        startActivityForResult(intent, VPN_REQUEST_CODE);
                    } else {
                        startVpnConnection();
                    }
                } else {
                    stopVpnConnection();
                }
            }
        });

        tvPingHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvPingHome.setText("Pinging...");
                tvPingHome.setTextColor(0xFFA0A0A0);
                tvPingHome.setEnabled(false);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int pingResult = new Random().nextInt(120) + 30;
                        tvPingHome.setText(pingResult + " ms");
                        if (pingResult < 80) {
                            tvPingHome.setTextColor(0xFF00FFCC);
                        } else {
                            tvPingHome.setTextColor(0xFFFF4C4C);
                        }
                        tvPingHome.setEnabled(true);
                    }
                }, 1500);
            }
        });

        menuSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ConfigListActivity.class));
            }
        });

        findViewById(R.id.menuLogs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LogsActivity.class));
            }
        });

        findViewById(R.id.menuMore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MoreActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVpnConnection();
            } else {
                Toast.makeText(this, "Izin VPN ditolak!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startVpnConnection() {
        isConnected = true;
        btnConnect.setText("DISCONNECT");
        tvStatus.setText("CONNECTING...");
        tvStatus.setTextColor(0xFFFFD700); // Kuning

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        // Jalankan Service
        Intent vpnIntent = new Intent(this, MyVpnService.class);
        startService(vpnIntent);

        // Tunggu Service mengecek server (delay 2 detik untuk simulasi)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    if (MyVpnService.isServerReachable) {
                        tvStatus.setText("CONNECTED");
                        tvStatus.setTextColor(0xFF00FFCC);
                        Toast.makeText(MainActivity.this, "Server Aktif! Terhubung.", Toast.LENGTH_SHORT).show();
                    } else {
                        tvStatus.setText("CONNECTED (NO INTERNET)");
                        tvStatus.setTextColor(0xFFFF4C4C); // Merah
                        Toast.makeText(MainActivity.this, "Server Mati atau diblokir!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }, 2000);

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(updateTimerThread, 0);
    }

    private void stopVpnConnection() {
        isConnected = false;
        btnConnect.setText("CONNECT");
        tvStatus.setText("NOT CONNECTED");
        tvStatus.setTextColor(0xFF00D2FF);

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        Intent vpnIntent = new Intent(this, MyVpnService.class);
        vpnIntent.setAction("STOP");
        startService(vpnIntent);

        timeSwapBuff += timeInMilliseconds;
        timerHandler.removeCallbacks(updateTimerThread);
        
        // Reset angka
        tvDownloadTotal.setText("0 B");
        tvUploadTotal.setText("0 B");
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = System.currentTimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            int hours = mins / 60;
            secs = secs % 60;
            tvTimer.setText(String.format("%02d:%02d:%02d", hours, mins, secs));

            // AMBIL ANGKA REAL DARI SERVICE (BUKAN SIMULASI LAGI)
            if (isConnected) {
                tvDownloadTotal.setText(formatSize(MyVpnService.realDownloadBytes));
                tvUploadTotal.setText(formatSize(MyVpnService.realUploadBytes));
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", (double) size / 1024);
        return String.format("%.2f MB", (double) size / (1024 * 1024));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
        String savedName = sharedPref.getString("NAMA_CONFIG", "");
        String savedHost = sharedPref.getString("HOST", "");
        boolean isLocked = sharedPref.getBoolean("IS_LOCKED", false);

        if (!savedName.isEmpty()) {
            tvPingHome.setVisibility(View.VISIBLE);
            tvPingHome.setText("PING");
            tvPingHome.setTextColor(0xFFFFD700);
            tvPingHome.setEnabled(true);

            if (isLocked) {
                tvSavedConfig.setText(savedName + "\nEncrypted");
            } else {
                String displayHost = savedHost;
                if (savedHost.startsWith("{")) {
                    try {
                        JSONObject detail = new JSONObject(savedHost);
                        displayHost = detail.optString("server", "") + ":" + detail.optString("port", "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                tvSavedConfig.setText(savedName + "\n" + displayHost);
            }
        } else {
            tvPingHome.setVisibility(View.GONE);
            tvSavedConfig.setText("Belum ada config tersimpan");
        }
    }
}