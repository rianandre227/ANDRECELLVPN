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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MyVpnService extends VpnService {
    private static final String TAG = "AndreVPN";
    private ParcelFileDescriptor mInterface;
    private boolean isRunning = false;
    private Process tun2socksProcess;

    public static long realDownloadBytes = 0;
    public static long realUploadBytes = 0;
    public static boolean isServerReachable = true;

    private static final String VPN_ADDRESS = "10.0.0.2";
    private static final int VPN_MTU = 1500;
    private static final int SOCKS_PORT = 10808;

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
            // 1. Ambil Data Config
            SharedPreferences sharedPref = getSharedPreferences("ConfigVPN", MODE_PRIVATE);
            String rawHost = sharedPref.getString("HOST", "");
            String protocol = sharedPref.getString("PROTOCOL", "VLESS");
            
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

            // 2. Buat Config Xray (Format JSON)
            String xrayConfig = XrayConfigGenerator.generate(protocol, server, port, id, path, hostHeader, sni, security);
            
            // 3. Jalankan Xray Core (Mesin)
            // CATATAN: Ini memanggil class dari library libv2ray.aar
            // Jika class name-nya berbeda, Anda harus menyesuaikannya.
            try {
                // Memulai Xray Core (Simulasi pemanggilan standard)
                // Class Libv2ray biasanya ada di dalam .aar
                Class<?> v2rayClass = Class.forName("com.v2ray.core.Libv2ray");
                v2rayClass.getMethod("startXray", String.class).invoke(null, xrayConfig);
                Log.d(TAG, "Xray Engine Started");
            } catch (Exception e) {
                Log.e(TAG, "Gagal menjalankan Xray: " + e.getMessage());
            }

            // 4. Buat VPN Interface (TUN)
            Builder builder = new Builder();
            builder.setSession("Andre Cell VPN");
            builder.addAddress(VPN_ADDRESS, 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("8.8.8.8");
            builder.setMtu(VPN_MTU);

            mInterface = builder.establish();
            isRunning = true;
            
            showNotification();

            // 5. Jalankan Tun2Socks (Jembatan)
            startTun2Socks();

            Log.d(TAG, "VPN Service Started Successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error Start VPN: " + e.getMessage());
            stopVpn();
        }
    }

    private void startTun2Socks() {
        try {
            // Ekstrak file .so dari folder jniLibs ke penyimpanan internal agar bisa dieksekusi
            File tun2socksFile = new File(getFilesDir(), "libhev-socks5-tunnel.so");
            if (!tun2socksFile.exists()) {
                // Di GitHub, file .so ada di dalam APK, kita harus mengekstraknya
                InputStream is = getAssets().open("libhev-socks5-tunnel.so"); // Catatan: Kadang perlu copy manual
                // Jika tidak ada di assets, kita asumsikan sudah ada di nativeLibraryDir
                File nativeLib = new File(getApplicationInfo().nativeLibraryDir, "libhev-socks5-tunnel.so");
                if (nativeLib.exists()) {
                    tun2socksFile = nativeLib;
                }
            }
            
            // Beri izin eksekusi
            tun2socksFile.setExecutable(true);

            // Perintah untuk menjalankan hev-socks5-tunnel
            // Format: ./tun2socks -F <TUN_FD> -M <MTU> -s <SOCKS_ADDR>:<SOCKS_PORT>
            List<String> cmd = new ArrayList<>();
            cmd.add(tun2socksFile.getAbsolutePath());
            cmd.add("-F");
            cmd.add(String.valueOf(mInterface.getFd()));
            cmd.add("-M");
            cmd.add(String.valueOf(VPN_MTU));
            cmd.add("-s");
            cmd.add("127.0.0.1:" + SOCKS_PORT);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            tun2socksProcess = pb.start();

            Log.d(TAG, "Tun2Socks Started");

        } catch (Exception e) {
            Log.e(TAG, "Gagal menjalankan Tun2Socks: " + e.getMessage());
        }
    }

    private void stopVpn() {
        isRunning = false;
        
        if (tun2socksProcess != null) {
            tun2socksProcess.destroy();
            tun2socksProcess = null;
        }

        try {
            Class<?> v2rayClass = Class.forName("com.v2ray.core.Libv2ray");
            v2rayClass.getMethod("stopXray").invoke(null);
            Log.d(TAG, "Xray Engine Stopped");
        } catch (Exception e) {}

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
