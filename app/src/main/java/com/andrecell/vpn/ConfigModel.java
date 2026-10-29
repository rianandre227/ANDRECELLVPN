package com.andrecell.vpn;

public class ConfigModel {
    String name;
    String host;
    String protocol;
    boolean isLocked; 

    public ConfigModel(String name, String host, String protocol, boolean isLocked) {
        this.name = name;
        this.host = host;
        this.protocol = protocol;
        this.isLocked = isLocked;
    }

    public String getName() { return name; }
    public String getHost() { return host; }
    public String getProtocol() { return protocol; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
}