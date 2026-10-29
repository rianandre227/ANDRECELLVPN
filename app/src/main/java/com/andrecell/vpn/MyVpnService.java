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
import java.util.ArrayList;
import java.util.List;

// IMPORT DARI LIBV2RAY
import libv2ray.CoreController;
import libv2ray.Libv2ray;

public class MyVpnService extends VpnService {
    private static final String TAG = "AndreVPN";
    private ParcelFileDescriptor mInterface;
    private boolean isRunning = false;
    private Process tun2socksProcess;
    private CoreController coreController; // Objek Xray

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

            // 2. Buat Config Xray (JSON)
            String xrayConfig = XrayConfigGenerator.generate(protocol, server, port, id, path, hostHeader, sni, security);
            
            // 3. Buat VPN Interface (TUN)
            Builder builder = new Builder();
            builder.setSession("Andre Cell VPN");
            builder.addAddress(VPN_ADDRESS, 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("8.8.8.8");
            builder.setMtu(VPN_MTU);
            
            mInterface = builder.establish();
            isRunning = true;
            showNotification();

            // 4. Jalankan Mesin Xray (libv2ray.aar)
            // Perbaikan 1: initCoreEnv membutuhkan 2 parameter String (path dan key)
            Libv2ray.initCoreEnv(getFilesDir().getAbsolutePath(), "");
            
            // Buat instance CoreController
            coreController = Libv2ray.newCoreController(null);
            
            // Perbaikan 2: startLoop membutuhkan (int tunFd, String config)
            // Kita kirimkan File Descriptor dari TUN Interface dan Config JSON-nya
            coreController.startLoop(mInterface.getFd(), xrayConfig);
            Log.d(TAG, "Xray Engine Started");

            // 5. Jalankan Tun2Socks (Jembatan)
            startTun2Socks();

        } catch (Exception e) {
            Log.e(TAG, "Error Start VPN: " + e.getMessage());
            stopVpn();
        }
    }

    private void startTun2Socks() {
        try {
            String nativeLibDir = getApplicationInfo().nativeLibraryDir;
            File tun2socksFile = new File(nativeLibDir, "libhev-socks5-tunnel.so");

            if (!tun2socksFile.exists()) {
                Log.e(TAG, "File libhev-socks5-tunnel.so tidak ditemukan!");
                return;
            }

            // Perintah menjalankan Tun2Socks
            List<String> cmd = new ArrayList<>();
            cmd.add(tun2socksFile.getAbsolutePath());
            cmd.add("-F"); cmd.add(String.valueOf(mInterface.getFd())); // File Descriptor TUN
            cmd.add("-M"); cmd.add(String.valueOf(VPN_MTU)); // MTU
            cmd.add("-s"); cmd.add("127.0.0.1:" + SOCKS_PORT); // Arahkan ke port SOCKS Xray

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

        if (coreController != null) {
            coreController.stopLoop();
            // Perbaikan 3: Hapus baris resetCoreEnv() karena method tersebut tidak ada
            coreController = null;
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
