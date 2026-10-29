package com.andrecell.vpn;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class XrayConfigGenerator {
    
    // Fungsi untuk mengubah config kita menjadi JSON Xray
    public static String generate(String protocol, String server, String port, String id, String path, String host, String sni, String security) {
        try {
            JSONObject root = new JSONObject();

            // 1. Log settings
            JSONObject log = new JSONObject();
            log.put("loglevel", "warning");
            root.put("log", log);

            // 2. Inbounds (Pintu masuk untuk Tun2Socks)
            JSONArray inbounds = new JSONArray();
            JSONObject inbound = new JSONObject();
            inbound.put("port", 10808); // Port lokal untuk Tun2Socks
            inbound.put("listen", "127.0.0.1");
            inbound.put("protocol", "socks");
            
            JSONObject settings = new JSONObject();
            settings.put("auth", "noauth");
            settings.put("udp", true);
            inbound.put("settings", settings);
            inbounds.put(inbound);
            root.put("inbounds", inbounds);

            // 3. Outbounds (Koneksi ke Server VPN Anda)
            JSONArray outbounds = new JSONArray();
            JSONObject outbound = new JSONObject();
            outbound.put("protocol", protocol.toLowerCase()); // vless, vmess, trojan

            JSONObject vnext = new JSONObject();
            vnext.put("address", server);
            
            // Pastikan port aman dari error parsing
            try {
                vnext.put("port", Integer.parseInt(port));
            } catch (Exception e) {
                vnext.put("port", 443); // Default jika port kosong
            }

            JSONArray users = new JSONArray();
            JSONObject user = new JSONObject();
            
            if (protocol.equalsIgnoreCase("vmess")) {
                user.put("id", id);
                user.put("alterId", 0);
                user.put("security", "auto");
            } else {
                user.put("id", id);
                if (protocol.equalsIgnoreCase("vless")) {
                    user.put("encryption", "none");
                    user.put("flow", "");
                }
            }
            users.put(user);
            vnext.put("users", users);

            JSONArray servers = new JSONArray();
            servers.put(vnext);

            JSONObject outSettings = new JSONObject();
            outSettings.put("vnext", servers);
            outbound.put("settings", outSettings);

            // Stream Settings (TLS, WebSocket, dll)
            JSONObject streamSettings = new JSONObject();
            streamSettings.put("network", "ws"); 
            streamSettings.put("security", security.toLowerCase()); 
            
            if (security.equalsIgnoreCase("tls")) {
                JSONObject tlsSettings = new JSONObject();
                tlsSettings.put("serverName", sni);
                tlsSettings.put("allowInsecure", true);
                streamSettings.put("tlsSettings", tlsSettings);
            }

            JSONObject wsSettings = new JSONObject();
            wsSettings.put("path", path);
            JSONObject headers = new JSONObject();
            headers.put("Host", host);
            wsSettings.put("headers", headers);
            streamSettings.put("wsSettings", wsSettings);

            outbound.put("streamSettings", streamSettings);
            outbounds.put(outbound);
            root.put("outbounds", outbounds);

            return root.toString();

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
        }
