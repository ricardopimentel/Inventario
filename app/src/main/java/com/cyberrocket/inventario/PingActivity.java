package com.cyberrocket.inventario;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PingActivity extends AppCompatActivity {

    private TextView mTvPingLog;
    private TextView mTvPingStatus;
    private String ipAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        MaterialToolbar toolbar = findViewById(R.id.toolbarPing);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        mTvPingLog = findViewById(R.id.TvPingLog);
        mTvPingStatus = findViewById(R.id.TvPingStatus);

        ipAddress = getIntent().getStringExtra("IP_ADDRESS");

        if (ipAddress != null && !ipAddress.isEmpty()) {
            setTitle("Ping: " + ipAddress);
            executePingCommand();
        } else {
            mTvPingStatus.setText("Endereço IP não fornecido.");
        }
    }

    private void executePingCommand() {
        mTvPingStatus.setText("Realizando ping para " + ipAddress + "...");
        new Thread(() -> {
            try {
                // Execute ping command
                Process process = Runtime.getRuntime().exec("ping -c 4 " + ipAddress);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                
                StringBuilder logBuilder = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    final String currentLine = line;
                    runOnUiThread(() -> {
                        logBuilder.append(currentLine).append("\n");
                        mTvPingLog.setText(logBuilder.toString());
                    });
                }
                
                while ((line = errorReader.readLine()) != null) {
                    final String currentLine = line;
                    runOnUiThread(() -> {
                        logBuilder.append(currentLine).append("\n");
                        mTvPingLog.setText(logBuilder.toString());
                    });
                }
                
                int exitCode = process.waitFor();
                runOnUiThread(() -> {
                    if (exitCode == 0) {
                        mTvPingStatus.setText("Ping concluído com sucesso.");
                    } else {
                        mTvPingStatus.setText("Ping falhou ou foi incompleto (código " + exitCode + ").");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    mTvPingStatus.setText("Erro ao executar ping.");
                    mTvPingLog.append("\nException: " + e.getMessage());
                });
            }
        }).start();
    }
}
