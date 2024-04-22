package com.cyberrocket.inventario.lib;

import android.os.AsyncTask;

import com.cyberrocket.inventario.models.Equipamento;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

public class HttpService extends AsyncTask<Void, Void, Equipamento> {
    private final String idEquipamento;

    public HttpService(String idEquipamento) {
        this.idEquipamento = idEquipamento;
    }

    @Override
    protected Equipamento doInBackground(Void... voids) {
        StringBuilder resposta = new StringBuilder();

        if (this.idEquipamento != null) {
            try {
                URL url = new URL("https://campusparaiso.ifto.edu.br/glpi/apirest.php/Computer/" + this.idEquipamento);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-type", "application/json");
                connection.setRequestProperty("Session-Token", "sokjn3mo317qvoej5m8tqg2av3");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.connect();

                Scanner scanner = new Scanner(url.openStream());
                while (scanner.hasNext()) {
                    resposta.append(scanner.next());
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new Gson().fromJson(resposta.toString(), Equipamento.class);
    }
}
