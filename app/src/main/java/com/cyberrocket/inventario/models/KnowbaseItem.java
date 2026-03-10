package com.cyberrocket.inventario.models;

public class KnowbaseItem {
    private String id;
    private String name;
    private String content;
    private String date_mod;
    private String category_name;
    private String category_id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDateMod() {
        return date_mod;
    }

    public void setDateMod(String date_mod) {
        this.date_mod = date_mod;
    }

    public String getCategoryName() {
        return category_name;
    }

    public void setCategoryName(String category_name) {
        this.category_name = category_name;
    }

    public String getCategoryId() {
        return category_id;
    }

    public void setCategoryId(String category_id) {
        this.category_id = category_id;
    }
}
