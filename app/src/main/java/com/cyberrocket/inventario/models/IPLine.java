package com.cyberrocket.inventario.models;

public class IPLine {
    private String ip;
    private int iconResId;

    public IPLine(String ip, int iconResId) {
        this.ip = ip;
        this.iconResId = iconResId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }
}
