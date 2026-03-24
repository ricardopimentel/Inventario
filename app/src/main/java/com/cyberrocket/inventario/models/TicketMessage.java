package com.cyberrocket.inventario.models;

public class TicketMessage {
    public static final int TYPE_TICKET = 0; // Green bubble
    public static final int TYPE_REPLY = 1;  // Gray bubble

    private String id; // For followups/tasks
    private int messageType;
    private String authorName;
    private String creationDate;
    private String content;

    // Optional fields for the main ticket
    private String lastUpdater;
    private String updateDate;

    // Optional arrays for image parsing and documents
    private java.util.List<String> inlineImages = new java.util.ArrayList<>();
    private java.util.List<Attachment> attachments = new java.util.ArrayList<>();

    public TicketMessage(int messageType, String authorName, String creationDate, String content) {
        this.messageType = messageType;
        this.authorName = authorName;
        this.creationDate = creationDate;
        this.content = content;
    }

    public TicketMessage(String id, int messageType, String authorName, String creationDate, String content) {
        this.id = id;
        this.messageType = messageType;
        this.authorName = authorName;
        this.creationDate = creationDate;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastUpdater() {
        return lastUpdater;
    }

    public void setLastUpdater(String lastUpdater) {
        this.lastUpdater = lastUpdater;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public java.util.List<String> getInlineImages() {
        return inlineImages;
    }

    public void setInlineImages(java.util.List<String> inlineImages) {
        this.inlineImages = inlineImages;
    }

    public java.util.List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(java.util.List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
