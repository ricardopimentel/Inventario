package com.cyberrocket.inventario.models;

import android.widget.ImageView;

public class Chamado {
    private String id;
    private String titulo;
    private String descricao;
    private String dataCriacao;
    private String dataFechamento;
    private String usuarioRequerente;
    private String usuarioAtribuido;
    private String tipo;
    private String statusInfo;
    private ImageView imagemStatus;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(String dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public String getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(String dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public String getUsuarioRequerente() {
        return usuarioRequerente;
    }

    public void setUsuarioRequerente(String usuarioRequerente) {
        this.usuarioRequerente = usuarioRequerente;
    }

    public String getUsuarioAtribuido() {
        return usuarioAtribuido;
    }

    public void setUsuarioAtribuido(String usuarioAtribuido) {
        this.usuarioAtribuido = usuarioAtribuido;
    }

    public ImageView getImagemStatus() {
        return imagemStatus;
    }

    public void setImagemStatus(ImageView imagemStatus) {
        this.imagemStatus = imagemStatus;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(String statusInfo) {
        this.statusInfo = statusInfo;
    }
}
