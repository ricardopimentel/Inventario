package com.cyberrocket.inventario.models;
import android.widget.ImageButton;

public class MonitorLine {
    private String Nome;
    private String Marca;
    private String Modelo;
    private String Estado;
    private String IdMonitor;
    private String NumeroSerie;
    private ImageButton BtEditNomeMonitor;
    private ImageButton BtRemoverMonitor;
    private ImageButton BtEditEstadoMonitor;

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

    public String getIdMonitor() {
        return IdMonitor;
    }

    public void setIdMonitor(String idMonitor) {
        IdMonitor = idMonitor;
    }

    public ImageButton getBtEditNomeMonitor() {
        return BtEditNomeMonitor;
    }

    public void setBtEditNomeMonitor(ImageButton btEditNomeMonitor) {
        BtEditNomeMonitor = btEditNomeMonitor;
    }

    public ImageButton getBtRemoverMonitor() {
        return BtRemoverMonitor;
    }

    public void setBtRemoverMonitor(ImageButton btRemoverMonitor) {
        BtRemoverMonitor = btRemoverMonitor;
    }

    public ImageButton getBtEditEstadoMonitor() {
        return BtEditEstadoMonitor;
    }

    public void setBtEditEstadoMonitor(ImageButton btEditEstadoMonitor) {
        BtEditEstadoMonitor = btEditEstadoMonitor;
    }

    public String getNumeroSerie() {
        return NumeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        NumeroSerie = numeroSerie;
    }
}
