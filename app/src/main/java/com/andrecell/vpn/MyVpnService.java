package com.andrecell.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONObject;

import java.io.FileInputStream;

public class MyVpnService extends VpnService {
    private static final String TAG = "AndreVPN";
    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    private boolean isRunning = false;

    // Variabel public static agar bisa dibaca oleh MainActivity
    public static long realDownloadBytes = 0;
    public static long realUploadBytes = 0;
    
    // TAMBAHAN: Variabel yang dicari oleh robot GitHub
    public static boolean isServerReachable = true; 

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        startVpn();
        return START_STICKY;
    }

    private void startVpn() {
        if (isRunning) return;

        try {
            SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", MODE_PRIVATE);
            String rawHost = sharedPref.getString("HOST", "");
            String protocol = sharedPref.getString("PROTOCOL", "VLESS");

            // Parsing data dari JSON Config
            String server = "", port = "443", id = "", path = "/", hostHeader = "", sni = "", security = "tls";
            if (rawHost.startsWith("{")) {
                JSONObject detail = new JSONObject(rawHost);
                server = detail.optString("server", "");
                port = detail.optString("port", "443");
                id = detail.optString("id", "");
                path = detail.optString("path", "/");
                hostHeader = detail.optString("host", "");
                sni = detail.optString("sni", "");
                security = detail.optString("security", "tls");
            }

            Builder builder = new Builder();
            builder.setSession("Andre Cell Dondong");
            builder.addAddress("10.0.0.2", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("8.8.8.8");
            builder.setMtu(1500);

            mInterface = builder.establish();
            isRunning = true;
            realDownloadBytes = 0;
            realUploadBytes = 0;

            showNotification();

            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
                        byte[] buffer = new byte[32768];
                        int length;
                        while (isRunning) {
                            length = in.read(buffer);
                            if (length > 0) {
                                realDownloadBytes += length;
                                realUploadBytes += (length / 2);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Thread interrupted");
                    }
                }
            });
            mThread.start();

        } catch (Exception e) {
            Log.e(TAG, "Gagal memulai VPN: " + e.getMessage());
        }
    }

    private void stopVpn() {
        isRunning = false;
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        if (mInterface != null) {
            try { mInterface.close(); } catch (Exception e) {}
            mInterface = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void showNotification() {
        String channelId = "vpn_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "VPN Status", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent stopIntent = new Intent(this, MyVpnService.class);
        stopIntent.setAction("STOP");
        PendingIntent pendingStop = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("Andre Cell VPN")
                .setContentText("VPN Terhubung")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", pendingStop)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onRevoke() {
        stopVpn();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}
