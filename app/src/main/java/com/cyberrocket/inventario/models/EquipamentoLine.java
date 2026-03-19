package com.cyberrocket.inventario.models;

import android.widget.ImageButton;

public class EquipamentoLine {
    private String Descricao;
    private String Conteudo;
    private Integer BtEditar;
    private Integer vaultType = 0; // 0: None, 1: Computer, 2: Location

    public Integer getVaultType() {
        return vaultType;
    }

    public void setVaultType(Integer vaultType) {
        this.vaultType = vaultType;
    }

    public String getDescricao() {
        return Descricao;
    }

    public void setDescricao(String descricao) {
        Descricao = descricao;
    }

    public String getConteudo() {
        return Conteudo;
    }

    public void setConteudo(String conteudo) {
        Conteudo = conteudo;
    }

    public Integer getBtEditar() {
        return BtEditar;
    }

    public void setBtEditar(Integer btEditar) {
        BtEditar = btEditar;
    }
}
