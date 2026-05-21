package com.cyberrocket.inventario.models;

public class ComputadorLine {
    private String Nome;
    private String Marca;
    private String Modelo;
    private String Estado;
    private String IdComputador;
    private String NumeroSerie;

    public String getNome() {
        return Nome;
    }

    public void setNome(String nome) {
        Nome = nome;
    }

    public String getMarca() {
        return Marca;
    }

    public void setMarca(String marca) {
        Marca = marca;
    }

    public String getModelo() {
        return Modelo;
    }

    public void setModelo(String modelo) {
        Modelo = modelo;
    }

    public String getEstado() {
        return Estado;
    }

    public void setEstado(String estado) {
        Estado = estado;
    }

    public String getIdComputador() {
        return IdComputador;
    }

    public void setIdComputador(String idComputador) {
        IdComputador = idComputador;
    }

    public String getNumeroSerie() {
        return NumeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        NumeroSerie = numeroSerie;
    }
}
