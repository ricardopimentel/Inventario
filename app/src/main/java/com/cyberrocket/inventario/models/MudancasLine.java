package com.cyberrocket.inventario.models;

import android.widget.Button;
import android.widget.ImageView;

public class MudancasLine {
    private String Titulo;
    private String UsuarioCriacao;
    private String Texto;
    private ImageView ImagemStatus;
    private Button BtFinalizarManutencao;
    private String DataManutencao;
    private String DataFinalizacao;
    private String UsuarioFinalizacao;
    private String IdMudanca;

    public String getTitulo() {
        return Titulo;
    }

    public void setTitulo(String titulo) {
        Titulo = titulo;
    }

    public ImageView getImagemStatus() {
        return ImagemStatus;
    }

    public void setImagemStatus(ImageView imagemStatus) {
        ImagemStatus = imagemStatus;
    }

    public Button getBtFinalizarManutencao() {
        return BtFinalizarManutencao;
    }

    public void setBtFinalizarManutencao(Button btFinalizarManutencao) {
        BtFinalizarManutencao = btFinalizarManutencao;
    }

    public String getDataManutencao() {
        return DataManutencao;
    }

    public void setDataManutencao(String dataManutencao) {
        DataManutencao = dataManutencao;
    }

    public String getDataFinalizacao() {
        return DataFinalizacao;
    }

    public void setDataFinalizacao(String dataFinalizacao) {
        DataFinalizacao = dataFinalizacao;
    }

    public String getUsuarioCriacao() {
        return UsuarioCriacao;
    }

    public void setUsuarioCriacao(String usuariocriacao) {
        UsuarioCriacao = usuariocriacao;
    }

    public String getTexto() {
        return Texto;
    }

    public void setTexto(String texto) {
        Texto = texto;
    }

    public String getUsuarioFinalizacao() {
        return UsuarioFinalizacao;
    }

    public void setUsuarioFinalizacao(String usuarioFinalizacao) {
        UsuarioFinalizacao = usuarioFinalizacao;
    }

    public String getIdMudanca() {
        return IdMudanca;
    }

    public void setIdMudanca(String idMudanca) {
        IdMudanca = idMudanca;
    }
}
