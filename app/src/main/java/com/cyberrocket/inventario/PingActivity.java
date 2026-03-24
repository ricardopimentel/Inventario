package com.cyberrocket.inventario;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PingActivity extends AppCompatActivity {

    private TextInputEditText mEtIpAddress;
    private Button mBtStartPing;
    private TextView mTvPingLog;
    private TextView mTvPingStatus;
    private String passedIp;
    private Process currentProcess;

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

        mEtIpAddress = findViewById(R.id.EtIpAddress);
        mBtStartPing = findViewById(R.id.BtStartPing);
        mTvPingLog = findViewById(R.id.TvPingLog);
        mTvPingStatus = findViewById(R.id.TvPingStatus);

        passedIp = getIntent().getStringExtra("IP_ADDRESS");

        if (passedIp != null && !passedIp.isEmpty()) {
            mEtIpAddress.setText(passedIp);
            executePingCommand(passedIp);
        }

        mBtStartPing.setOnClickListener(v -> {
            String typedIp = mEtIpAddress.getText().toString().trim();
            if (!typedIp.isEmpty()) {
                executePingCommand(typedIp);
            } else {
                mTvPingStatus.setText("Por favor, preencha o IP.");
            }
        });
    }

    private void executePingCommand(String targetIp) {
        if (currentProcess != null) {
            currentProcess.destroy();
        }

        setTitle("Ping: " + targetIp);
        mTvPingLog.setText("");
        mTvPingStatus.setText("Realizando ping para " + targetIp + "...");
        mBtStartPing.setEnabled(false);

        new Thread(() -> {
            try {
                // Execute ping command
                currentProcess = Runtime.getRuntime().exec("ping -c 4 " + targetIp);
                BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(currentProcess.getErrorStream()));
                
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
                
                int exitCode = currentProcess.waitFor();
                runOnUiThread(() -> {
                    mBtStartPing.setEnabled(true);
                    if (exitCode == 0) {
                        mTvPingStatus.setText("Ping concluído com sucesso.");
                    } else {
                        mTvPingStatus.setText("Ping falhou ou foi incompleto (código " + exitCode + ").");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    mBtStartPing.setEnabled(true);
                    mTvPingStatus.setText("Erro ao executar ping.");
                    mTvPingLog.append("\nException: " + e.getMessage());
                });
            }
        }).start();
    }
}
