package com.cyberrocket.inventario.models;

public class Attachment {
    private String id;
    private String filename;
    private String url;
    private String mimeType;

    public Attachment(String id, String filename, String url, String mimeType) {
        this.id = id;
        this.filename = filename;
        this.url = url;
        this.mimeType = mimeType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
