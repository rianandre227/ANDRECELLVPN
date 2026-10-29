package com.andrecell.vpn;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public class XrayConfigGenerator {
    
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
            inbound.put("port", 10808);
            inbound.put("listen", "127.0.0.1");
            inbound.put("protocol", "socks");
            
            JSONObject inSettings = new JSONObject();
            inSettings.put("auth", "noauth");
            inSettings.put("udp", true);
            inbound.put("settings", inSettings);
            inbounds.put(inbound);
            root.put("inbounds", inbounds);

            // 3. Outbounds (Koneksi ke Server VPN Anda)
            JSONArray outbounds = new JSONArray();
            JSONObject outbound = new JSONObject();
            outbound.put("protocol", protocol.toLowerCase());

            JSONObject outSettings = new JSONObject();

            if (protocol.equalsIgnoreCase("trojan")) {
                JSONArray servers = new JSONArray();
                JSONObject serverObj = new JSONObject();
                serverObj.put("address", server);
                try {
                    serverObj.put("port", Integer.parseInt(port));
                } catch (Exception e) {
                    serverObj.put("port", 443);
                }
                serverObj.put("password", id); 
                servers.put(serverObj);
                outSettings.put("servers", servers);

            } else {
                JSONObject vnext = new JSONObject();
                vnext.put("address", server);
                try {
                    vnext.put("port", Integer.parseInt(port));
                } catch (Exception e) {
                    vnext.put("port", 443);
                }

                JSONArray users = new JSONArray();
                JSONObject user = new JSONObject();
                
                // PENGAMAN UUID: Jika VLESS tapi ID bukan format UUID, kita ubah jadi UUID acak agar tidak crash
                if (protocol.equalsIgnoreCase("vless")) {
                    try {
                        UUID.fromString(id);
                        user.put("id", id);
                    } catch (IllegalArgumentException e) {
                        user.put("id", UUID.randomUUID().toString()); // Fallback
                    }
                    user.put("encryption", "none");
                    user.put("flow", "");
                } else if (protocol.equalsIgnoreCase("vmess")) {
                    user.put("id", id);
                    user.put("alterId", 0);
                    user.put("security", "auto");
                }
                
                users.put(user);
                vnext.put("users", users);

                JSONArray vnextArray = new JSONArray();
                vnextArray.put(vnext);
                outSettings.put("vnext", vnextArray);
            }

            outbound.put("settings", outSettings);

            // 4. Stream Settings
            JSONObject streamSettings = new JSONObject();
            streamSettings.put("network", "ws"); 
            streamSettings.put("security", security.toLowerCase()); 
            
            if (security.equalsIgnoreCase("tls")) {
                JSONObject tlsSettings = new JSONObject();
                tlsSettings.put("serverName", sni);
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
