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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import libv2ray.CoreController;
import libv2ray.Libv2ray;

public class MyVpnService extends VpnService {
    private static final String TAG = "AndreVPN";
    private ParcelFileDescriptor mInterface;
    private boolean isRunning = false;
    private Process tun2socksProcess;
    private CoreController coreController;

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
        // PERBAIKAN: Gunakan START_NOT_STICKY agar tidak terjadi crash loop
        return START_NOT_STICKY; 
    }

    private void startVpn() {
        if (isRunning) return;

        try {
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

            String xrayConfig = XrayConfigGenerator.generate(protocol, server, port, id, path, hostHeader, sni, security);
            
            Builder builder = new Builder();
            builder.setSession("Andre Cell VPN");
            builder.addAddress(VPN_ADDRESS, 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("8.8.8.8");
            builder.setMtu(VPN_MTU);
            
            mInterface = builder.establish();
            if (mInterface == null) throw new Exception("Gagal membuat TUN Interface");
            
            isRunning = true;
            showNotification();

            // Jalankan Mesin Xray
            Libv2ray.initCoreEnv(getFilesDir().getAbsolutePath(), "");
            coreController = Libv2ray.newCoreController(null);
            coreController.startLoop(xrayConfig, mInterface.getFd());
            Log.d(TAG, "Xray Engine Started");

            // Jalankan Tun2Socks
            startTun2Socks();

        } catch (Exception e) {
            saveErrorToFile(e);
            Log.e(TAG, "Error Start VPN: " + e.getMessage());
            stopVpn();
        }
    }

    private void startTun2Socks() {
        try {
            String nativeLibDir = getApplicationInfo().nativeLibraryDir;
            File sourceFile = new File(nativeLibDir, "libhev-socks5-tunnel.so");
            
            File destFile = new File(getFilesDir(), "tun2socks");
            copyFile(sourceFile, destFile);
            
            destFile.setExecutable(true);

            List<String> cmd = new ArrayList<>();
            cmd.add(destFile.getAbsolutePath());
            cmd.add("-F"); cmd.add(String.valueOf(mInterface.getFd()));
            cmd.add("-M"); cmd.add(String.valueOf(VPN_MTU));
            cmd.add("-s"); cmd.add("127.0.0.1:" + SOCKS_PORT);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            tun2socksProcess = pb.start();
            Log.d(TAG, "Tun2Socks Started");

        } catch (Exception e) {
            saveErrorToFile(e);
            Log.e(TAG, "Gagal menjalankan Tun2Socks: " + e.getMessage());
        }
    }

    private void copyFile(File source, File dest) throws IOException {
        if (!dest.exists()) {
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    private void saveErrorToFile(Exception e) {
        try {
            File logFile = new File(getFilesDir(), "vpn_error.log");
            FileOutputStream fos = new FileOutputStream(logFile, false);
            PrintWriter pw = new PrintWriter(fos);
            e.printStackTrace(pw);
            pw.flush();
            pw.close();
        } catch (Exception ex) {
            Log.e(TAG, "Gagal menyimpan log error");
        }
    }

    private void stopVpn() {
        isRunning = false;
        if (tun2socksProcess != null) { tun2socksProcess.destroy(); tun2socksProcess = null; }
        if (coreController != null) {
            try { coreController.stopLoop(); } catch (Exception e) {}
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
        PendingIntent pendingStop = PendingIntent.getService(this, 0, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("Andre Cell VPN")
                .setContentText("VPN Terhubung")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", pendingStop)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onRevoke() { stopVpn(); super.onRevoke(); }
    @Override
    public void onDestroy() { stopVpn(); super.onDestroy(); }
}
