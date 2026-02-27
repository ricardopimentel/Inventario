package com.cyberrocket.inventario.models;

public class SenhaItem {
    private String id; // Opcional, pode ser útil no local
    private String descricao;
    private String senha;

    public SenhaItem() {
    }

    public SenhaItem(String descricao, String senha) {
        this.descricao = descricao;
        this.senha = senha;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
