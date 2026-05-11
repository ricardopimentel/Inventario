package com.cyberrocket.inventario;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.android.volley.Request;
import com.cyberrocket.inventario.adapter.ListAdapterEquipamentos;
import com.cyberrocket.inventario.adapter.ListAdapterMonitores;
import com.cyberrocket.inventario.adapter.ListAdapterMudancas;
import com.cyberrocket.inventario.adapter.ListAdapterArmazenamento;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.lib.InfisicalConnect;
import com.cyberrocket.inventario.models.ArmazenamentoLine;
import com.cyberrocket.inventario.models.EquipamentoLine;
import com.cyberrocket.inventario.models.MonitorLine;
import com.cyberrocket.inventario.models.MudancasLine;
import com.cyberrocket.inventario.models.PlacasRedeLine;
import com.cyberrocket.inventario.models.IPLine;
import com.cyberrocket.inventario.adapter.ListAdapterIPs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.cyberrocket.inventario.lib.PasswordVaultHelper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;

import android.widget.ArrayAdapter;
import com.cyberrocket.inventario.lib.PasswordManager;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;

public class ScannerActivity extends AppCompatActivity {
    TextView mTvIdEquipamento;
    CoordinatorLayout mCLayout;
    ImageView mImvImgEquipamento;
    ProgressBar mPgbProgresso;
    RecyclerView mListaEquipamentos;
    RecyclerView mListaMudancas;
    RecyclerView mListaMonitores;
    RecyclerView mListaArmazenamento;
    RecyclerView mListaArmazenamentoFisico;
    RecyclerView mListaIPs;
    Button mBtNovaManutencao;
    ImageButton mBtAddMonitorScanner;
    ImageView mImvSyncDevice;
    FloatingActionButton mBtLerEquipamento;
    LinearLayoutManager linearLayoutManagerEquipamento;
    StaggeredGridLayoutManager gridLayoutManagerEquipamento;
    LinearLayoutManager linearLayoutManagerMudancas;
    StaggeredGridLayoutManager gridLayoutManagerMudancas;
    LinearLayoutManager linearLayoutManagerMonitores;
    StaggeredGridLayoutManager gridLayoutManagerMonitores;
    Crud mCrud;
    ArrayList<EquipamentoLine> listaequipamentos;
    ArrayList<MudancasLine> listamudancas;
    ArrayList<MonitorLine> listamonitores;
    ArrayList<ArmazenamentoLine> listaarmazenamento;
    ArrayList<ArmazenamentoLine> listaarmazenamentofisico;
    ArrayList<IPLine> listaips;
    Boolean existemanutencaoaberta = false;
    ConstraintLayout mLayoutEquipamentos;
    ConstraintLayout mLayoutMonitores;
    ConstraintLayout mLayoutManutencoes;
    ConstraintLayout mLayoutArmazenamento;
    String mIdMonitor;
    SwipeRefreshLayout mSwipeRefreshListEquipamento;
    public String mItemType = "Computer"; // Default

    // Autocomplete for Monitor
    ArrayList<String> mMonitorNamesList;
    ArrayList<String> mMonitorIdsList;
    ArrayAdapter<String> mMonitorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Metodos automáticos
        inicializarViews();
        inicializarListas();
        getParametros();

        //Listeners
        mBtLerEquipamento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inicializarListas();
                IntentIntegrator integrator = new IntentIntegrator(ScannerActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setPrompt("Aponte para o Código");
                integrator.setCameraId(0);  // Use a specific camera of the device
                integrator.setBeepEnabled(true);
                integrator.setBarcodeImageEnabled(true);
                integrator.setOrientationLocked(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });

        mBtNovaManutencao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrPara(CadMudancaActivity.class, false);
            }
        });

        mBtLerEquipamento.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                DigitarManualmente();
                return true;
            }
        });

        mBtAddMonitorScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreencherListaMonitores();
            }
        });

        mSwipeRefreshListEquipamento.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                IrPara(ScannerActivity.class, true);
            }
        });

        // Check for direct actions from Home
        boolean autoScan = getIntent().getBooleanExtra("auto_scan", false);
        boolean autoType = getIntent().getBooleanExtra("auto_type", false);

        if (autoScan) {
            mBtLerEquipamento.performClick();
        } else if (autoType) {
            DigitarManualmente();
        }
    }


    private void DigitarManualmente() {
        View view = LayoutInflater.from(ScannerActivity.this).inflate(R.layout.dialog_input, null);
        TextInputEditText edittext = view.findViewById(R.id.TvNome);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(ScannerActivity.this)
                .setTitle("Digite o patrimônio")
                .setView(view)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mTvIdEquipamento.setText(edittext.getText().toString());
                        dialogInterface.dismiss();
                        GetIdEquipamento();
                    }
                }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        dialog.create();
        dialog.show();
    }

    //Métodos

    private void IrPara(Class para, boolean finalizar) {
        Intent intent = new Intent(ScannerActivity.this, para);
        intent.putExtra("id", mTvIdEquipamento.getText().toString());
        startActivity(intent);
        if(finalizar){
            finish();
        }
    }

    private void getParametros() { //Se a activity for chamada atribuindo como parametro um id de equipamento, realiza a busca pelo equipamento, sem a necessidade de fazer a leitura do qr code
        Intent it = getIntent();
        String id = it.getStringExtra("id");
        String type = it.getStringExtra("item_type");
        if (type != null) {
            mItemType = type;
        }
        
        if (getSupportActionBar() != null) {
            if (mItemType.equals("Monitor")) {
                getSupportActionBar().setTitle("Detalhes do Monitor");
            } else {
                getSupportActionBar().setTitle("Detalhes do Computador");
            }
        }

        if (id != null) {
            mTvIdEquipamento.setText(id);
            inicializarListas();
            BuscarListaEquipamentos(id);
        }
    }

    private void inicializarViews() {
        mCLayout = findViewById(R.id.CntScanner);
        mTvIdEquipamento = findViewById(R.id.TvIdEquipamento);
        mImvImgEquipamento = findViewById(R.id.ImvImgDevice);
        mPgbProgresso = findViewById(R.id.PgbScanner);
        mBtNovaManutencao = findViewById(R.id.BtNovaManutencaoScanner);
        mBtAddMonitorScanner = findViewById(R.id.BtAddMonitorScanner);

        mListaEquipamentos = findViewById(R.id.RvDetalhesEquipamentoScanner);
        mListaMudancas = findViewById(R.id.RvMudancasScanner);
        mListaMonitores = findViewById(R.id.RvMonitoresScanner);
        mListaArmazenamento = findViewById(R.id.RvArmazenamentoScanner);
        mListaArmazenamentoFisico = findViewById(R.id.RvArmazenamentoScannerFisico);

        linearLayoutManagerEquipamento = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        gridLayoutManagerEquipamento = new StaggeredGridLayoutManager(3, LinearLayoutManager.VERTICAL);

        linearLayoutManagerMonitores = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        linearLayoutManagerMudancas = new LinearLayoutManager(this);
        gridLayoutManagerMudancas = new StaggeredGridLayoutManager(1, LinearLayoutManager.HORIZONTAL);


        mListaEquipamentos.setLayoutManager(gridLayoutManagerEquipamento);
        mListaMonitores.setLayoutManager(linearLayoutManagerMonitores);
        mListaMudancas.setLayoutManager(gridLayoutManagerMudancas);
        mListaArmazenamento.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
        mListaArmazenamentoFisico.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        mLayoutEquipamentos = findViewById(R.id.LayoutEquipamentosScanner);
        mLayoutMonitores = findViewById(R.id.LayoutMonitoresScanner);
        mLayoutManutencoes = findViewById(R.id.LayoutManutencoesScanner);
        mLayoutArmazenamento = findViewById(R.id.LayoutArmazenamentoScanner);
        mListaIPs = findViewById(R.id.RvIPsScanner);
        mListaIPs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        mImvSyncDevice = findViewById(R.id.ImvSyncDevice);

        mBtLerEquipamento = findViewById(R.id.BtLerEquipamentoScanner);
        mSwipeRefreshListEquipamento = findViewById(R.id.RefreshEquipamentosScanner);
        mSwipeRefreshListEquipamento = findViewById(R.id.RefreshEquipamentosScanner);
        
        mCrud = new Crud();
    }

    public class CustomGridLayoutManager extends LinearLayoutManager {
        private boolean isScrollEnabled = false;

        public CustomGridLayoutManager(Context context) {
            super(context);
        }

        public void setScrollEnabled(boolean flag) {
            this.isScrollEnabled = flag;
        }

        @Override
        public boolean canScrollVertically() {
            //Similarly you can customize "canScrollHorizontally()" for managing horizontal scroll
            return isScrollEnabled && super.canScrollHorizontally();
        }
    }

    private void inicializarListas() {
        listamudancas = new ArrayList<MudancasLine>();
        listamonitores = new ArrayList<MonitorLine>();
        listaequipamentos = new ArrayList<EquipamentoLine>();
        listaarmazenamento = new ArrayList<ArmazenamentoLine>();
        listaarmazenamentofisico = new ArrayList<ArmazenamentoLine>();
        listaips = new ArrayList<IPLine>();
    }

    private void BuscarListaEquipamentos(String idequipamento) {
        mTvIdEquipamento.setText(idequipamento);
        if(!idequipamento.equals("erro")) {
            GLPIConnect con = new GLPIConnect(this);
            String endpoint = mItemType.equals("Monitor") ? "/apirest.php/Monitor/" : "/apirest.php/Computer/";
            con.GetItem(endpoint + idequipamento + "?expand_dropdowns=true&with_connections=true&with_problems=true&with_softwares=true&with_disks=true&with_devices=true&with_networkports=true&with_networknames=true", new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject = new JSONObject(response);
                    } catch (JSONException err) {
                        Log.d("ParseError", err.toString());
                    }
                    Log.e("jason", jsonObject.toString());

                    //Pegar Versão do Agente do GLPI
                    String versaoagente = "";
                    try {
                        JSONArray softwaresarray = jsonObject.getJSONArray("_softwares");
                        for (int i = 0; i < softwaresarray.length(); i++) {
                            JSONObject software = softwaresarray.getJSONObject(i);
                            if(software.getString("softwares_id").contains("GLPI Agen")){
                                versaoagente = software.getString("softwareversions_id");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        //Pega dados do equipamento
                        int nameVaultType = mItemType.equals("Monitor") ? 0 : 1;
                        int locVaultType = mItemType.equals("Monitor") ? 0 : 2;

                        CriarListaEquipamentos("Nome:", jsonObject.optString("name"), View.GONE, nameVaultType);
                        CriarListaEquipamentos("Localização:", jsonObject.optString("locations_id"), View.VISIBLE, locVaultType);
                        
                        if (mItemType.equals("Computer")) {
                            CriarListaEquipamentos("Agente:", versaoagente, View.GONE, 0);
                        }
                        
                        CriarListaEquipamentos("Nº Série:", jsonObject.optString("serial"), View.GONE, 0);
                        String dateMod = jsonObject.optString("date_mod");
                    if (dateMod != null && !dateMod.isEmpty()) {
                        String[] parts = dateMod.split(" ");
                        if (parts.length >= 1) {
                            String[] dateParts = parts[0].split("-");
                            if (dateParts.length == 3) {
                                String formattedDate = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];
                                CriarListaEquipamentos("Data:", formattedDate, View.GONE, 0);
                            }
                        }
                        if (parts.length >= 2) {
                            String[] timeParts = parts[1].split(":");
                            if (timeParts.length >= 2) {
                                String formattedTime = timeParts[0] + ":" + timeParts[1];
                                CriarListaEquipamentos("Hora:", formattedTime, View.GONE, 0);
                            }
                        }
                    }
                        CriarListaEquipamentos("Estado:", jsonObject.optString("states_id"), View.VISIBLE, 0);
                        CriarListaEquipamentos("Marca:", jsonObject.optString("manufacturers_id"), View.GONE, 0);
                        
                        String tipoLabel = "Tipo:";
                        String tipoValue = "";
                        if (mItemType.equals("Monitor")) {
                            tipoValue = jsonObject.optString("monitortypes_id");
                        } else {
                            tipoValue = jsonObject.optString("computertypes_id");
                        }
                        CriarListaEquipamentos(tipoLabel, tipoValue, View.VISIBLE, 0);
                        
                        // Log focado na busca das placas de rede para depuração
                        try {
                            JSONObject networkPortsLog = jsonObject.optJSONObject("_networkports");
                            if (networkPortsLog != null) {
                                String netJson = networkPortsLog.toString(2);
                                int chunkSize = 3000;
                                for (int i = 0; i < netJson.length(); i += chunkSize) {
                                    Log.d("GLPI_NETWORK_DEBUG", netJson.substring(i, Math.min(netJson.length(), i + chunkSize)));
                                }
                            }
                        } catch (Exception e) {
                            Log.e("IPDebug", "Erro ao logar JSON de rede: " + e.getMessage());
                        }

                        // Pegar Endereço IP do computador (Busca dirigida em campos Nominais de IP do GLPI)
                        try {
                            listaips.clear();
                            String ipRegex = "^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\." +
                                           "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\." +
                                           "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\." +
                                           "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$";

                            JSONObject networkPortsObj = jsonObject.optJSONObject("_networkports");
                            if (networkPortsObj != null) {
                                String[] portTypes = {"NetworkPortEthernet", "NetworkPortWifi", "NetworkPortWireless"};
                                for (String type : portTypes) {
                                    JSONArray ports = networkPortsObj.optJSONArray(type);
                                    if (ports != null) {
                                        for (int i = 0; i < ports.length(); i++) {
                                            JSONObject port = ports.optJSONObject(i);
                                            if (port == null) continue;

                                            // Identifica se a interface é virtual
                                            String portName = port.optString("name").toLowerCase();
                                            String portComment = port.optString("comment").toLowerCase();
                                            boolean isVirtual = portName.contains("virtualbox") || portName.contains("vmware") || 
                                                              portName.contains("vethernet") || portName.contains("vbox") || 
                                                              portName.contains("virtual") || portComment.contains("virtual") ||
                                                              portComment.contains("virtualbox") || portComment.contains("vmware");

                                            int iconResId;
                                            if (isVirtual) {
                                                iconResId = R.drawable.ic_virtual_machine_24dp;
                                            } else {
                                                iconResId = type.contains("Wifi") || type.contains("Wireless") ? 
                                                        R.drawable.wifi_24 : R.drawable.settings_ethernet_24;
                                            }

                                            // 1. Tenta o caminho NetworkName -> IPAddress
                                            JSONObject netName = port.optJSONObject("NetworkName");
                                            if (netName != null) {
                                                JSONArray ipAddresses = netName.optJSONArray("IPAddress");
                                                if (ipAddresses != null) {
                                                    for (int j = 0; j < ipAddresses.length(); j++) {
                                                        String ip = ipAddresses.optJSONObject(j).optString("name");
                                                        if (ip.matches(ipRegex)) {
                                                            if (ip.equals("127.0.0.1") || ip.equals("0.0.0.0") || ip.startsWith("255.")) continue;
                                                            boolean exists = false;
                                                            for(IPLine item : listaips) { if(item.getIp().equals(ip)) { exists = true; break; } }
                                                            if (!exists) listaips.add(new IPLine(ip, iconResId));
                                                        }
                                                    }
                                                }
                                            }
                                            // 2. Tenta _ipaddresses
                                            JSONArray altIps = port.optJSONArray("_ipaddresses");
                                            if (altIps != null) {
                                                for (int j = 0; j < altIps.length(); j++) {
                                                    String ip = altIps.optJSONObject(j).optString("name");
                                                    if (ip.matches(ipRegex)) {
                                                        if (ip.equals("127.0.0.1") || ip.equals("0.0.0.0") || ip.startsWith("255.")) continue;
                                                        boolean exists = false;
                                                        for(IPLine item : listaips) { if(item.getIp().equals(ip)) { exists = true; break; } }
                                                        if (!exists) listaips.add(new IPLine(ip, iconResId));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!listaips.isEmpty()) {
                                mListaIPs.setVisibility(View.VISIBLE);
                                ListAdapterIPs adapter = new ListAdapterIPs(listaips, ScannerActivity.this);
                                mListaIPs.setAdapter(adapter);
                            }
                        } catch (Exception e) {
                            Log.e("IPDebug", "Erro na busca dirigida por IP: " + e.getMessage());
                        }

                        //Seta dados para a lista de detalhes do equipamento
                        ListAdapterEquipamentos equipamentoadapter = new ListAdapterEquipamentos(listaequipamentos, ScannerActivity.this, idequipamento);
                        mListaEquipamentos.setAdapter(equipamentoadapter);
                        mLayoutEquipamentos.setVisibility(View.VISIBLE);

                        if (mItemType.equals("Computer")) {
                            //Pega Monitores vinculados ao computador
                            try {
                                JSONArray monitoresarray = jsonObject.getJSONObject("_connections").getJSONArray("Monitor");
                                for (int i = 0; i < monitoresarray.length(); i++) {
                                    JSONObject monitor = monitoresarray.getJSONObject(i);
                                    CriarListaMonitores(monitor.optString("name"), monitor.optString("manufacturers_id"), monitor.optString("monitormodels_id"), monitor.optString("states_id"), monitor.optString("id"), monitor.optString("serial"));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            //Seta dados para a lista de monitores
                            ListAdapterMonitores adaptermonitores = new ListAdapterMonitores(listamonitores, ScannerActivity.this, idequipamento);
                            mListaMonitores.setAdapter(adaptermonitores);

                            if (!tipoValue.equals("Notebook")) {
                                mLayoutMonitores.setVisibility(View.VISIBLE);
                            } else {
                                mLayoutMonitores.setVisibility(View.GONE);
                            }
                        } else {
                            // É um monitor, esconde a lista de monitores (ele não tem monitores vinculados a ele da mesma forma)
                            mLayoutMonitores.setVisibility(View.GONE);
                            mLayoutArmazenamento.setVisibility(View.GONE);
                        }

                        // Debug keys for storage and partitions finding  
                        Iterator<String> jsonKeys = jsonObject.keys();
                        while(jsonKeys.hasNext()) {
                             String k = jsonKeys.next();
                             Log.d("DiskVolumeDebug", "Main Key in Computer: " + k);
                        }
                        if(jsonObject.has("Item_Disk")) {
                             Log.d("DiskVolumeDebug", "Item_Disk struct: " + jsonObject.opt("Item_Disk").toString());
                        }

                        //Pega Armazenamento
                        try {
                            boolean hasDisks = false;
                            
                            // Em vez de _devices, vamos usar a relação oficial de volumes/discos lógicos (_disks)
                            JSONArray disksArray = jsonObject.optJSONArray("_disks");
                            
                            if (disksArray != null && disksArray.length() > 0) {
                                for (int i = 0; i < disksArray.length(); i++) {
                                    JSONObject diskObj = disksArray.getJSONObject(i);
                                    
                                    // Bizarre GLPI bug: sometimes it returns the actual disk JSON stringIFIED inside the 'name' field
                                    String rawName = diskObj.optString("name", "");
                                    if (rawName.startsWith("{") && rawName.contains("\"id\"")) {
                                        try {
                                            diskObj = new JSONObject(rawName);
                                        } catch (Exception err) {
                                            err.printStackTrace();
                                        }
                                    }
                                    
                                    String name = diskObj.optString("name", "Volume Desconhecido"); // Ex: C:, /home
                                    String type = diskObj.optString("filesystems_id", "Drive"); // Ex: NTFS, ext4
                                    if (type.equals("0") || type.equals("null")) {
                                        type = "Drive";
                                    }
                                    
                                    // Pega tamanho em MB
                                    long totalSize = diskObj.optLong("totalsize", 0);
                                    
                                    // Filtro para não mostrar as partições que tem menos de 1gb
                                    if (totalSize < 1024) {
                                        continue;
                                    }
                                    
                                    long freeSize  = diskObj.optLong("freesize", 0);
                                    long usedSize  = totalSize - freeSize;
                                    
                                    // Calcula progresso (evita divisão por zero)
                                    int progress = 0;
                                    String capacityLabel;
                                    
                                    if (totalSize > 0) {
                                        progress = (int) (((double) usedSize / totalSize) * 100);
                                        long totalGb = totalSize / 1024;
                                        long usedGb  = usedSize / 1024;
                                        capacityLabel = String.format("%d GB / %d GB", usedGb, totalGb);
                                    } else {
                                        capacityLabel = "Capacidade Indisponível no GLPI";
                                    }

                                    ArmazenamentoLine arm = new ArmazenamentoLine();
                                    arm.setNome(name);
                                    arm.setCapacidade(capacityLabel);
                                    arm.setTipo(type);
                                    // Custom properties pra podermos usar na UI nova:
                                    arm.setUsagePercentage(progress);
                                    
                                    listaarmazenamento.add(arm);
                                    hasDisks = true;
                                }
                            }
                            
                            if (hasDisks) {
                                ListAdapterArmazenamento adapterArmazenamento = new ListAdapterArmazenamento(listaarmazenamento, ScannerActivity.this, idequipamento);
                                mListaArmazenamento.setAdapter(adapterArmazenamento);
                                mListaArmazenamento.setVisibility(View.VISIBLE);
                            } else {
                                mListaArmazenamento.setVisibility(View.GONE);
                            }
                            
                            // Agora processa Discos Físicos (Hardware chips)
                            boolean hasPhysicalDisks = false;
                            
                            if (jsonObject.has("_devices") && jsonObject.getJSONObject("_devices").has("Item_DeviceHardDrive")) {
                                JSONObject disksObject = jsonObject.getJSONObject("_devices").getJSONObject("Item_DeviceHardDrive");
                                Iterator<String> diskKeys = disksObject.keys();
                                
                                while (diskKeys.hasNext()) {
                                    String key = diskKeys.next();
                                    JSONObject diskObj = disksObject.getJSONObject(key);
                                    
                                    String designacao = diskObj.optString("designation");
                                    String name = diskObj.optString("deviceharddrives_id", "Drive"); 
                                    long sizeMB = diskObj.optLong("capacity", 0);
                                    
                                    String diskTypeUpper = name.toUpperCase() + designacao.toUpperCase();
                                    String tipoFormatado = "HDD"; // Padrão pessimista

                                    // Heurísticas poderosas para adivinhar SSD/NVMe a partir do código do modelo
                                    if (diskTypeUpper.contains("NVME") || diskTypeUpper.contains("M.2") || 
                                        diskTypeUpper.contains("SSSTC") || diskTypeUpper.contains("SAMSUNG MZ") || 
                                        diskTypeUpper.contains("KINGSTON OM") || diskTypeUpper.contains("KINGSTON SN") ||
                                        diskTypeUpper.startsWith("MZ") || diskTypeUpper.contains("UMIS")) {
                                        tipoFormatado = "NVMe";
                                    } else if (diskTypeUpper.contains("SSD") || diskTypeUpper.contains("FLASH") || 
                                               diskTypeUpper.contains("KINGSTON SA") || diskTypeUpper.contains("WDC WDS") ||
                                               diskTypeUpper.startsWith("CT") || diskTypeUpper.contains("ADATA") || 
                                               diskTypeUpper.contains("CORSAIR") || diskTypeUpper.contains("CRUCIAL")) {
                                        tipoFormatado = "SSD";
                                    }
                                    
                                    // Limpar nomes de modelo muito feios para caber no card
                                    String nomeCurto = name;
                                    if (nomeCurto.length() > 18) {
                                        nomeCurto = nomeCurto.substring(0, 18).trim() + "...";
                                    }
                                    
                                    long totalGb = sizeMB / 1024;
                                    String sizeFormatted = (totalGb > 0) ? (totalGb + " GB") : (sizeMB + " MB");

                                    ArmazenamentoLine armFisico = new ArmazenamentoLine();
                                    armFisico.setNome(nomeCurto);
                                    armFisico.setCapacidade(sizeFormatted);
                                    armFisico.setTipo(tipoFormatado);
                                    listaarmazenamentofisico.add(armFisico);
                                    hasPhysicalDisks = true;
                                }
                            }
                            
                            if (hasPhysicalDisks) {
                                com.cyberrocket.inventario.adapter.ListAdapterArmazenamentoFisico adapterFisico = new com.cyberrocket.inventario.adapter.ListAdapterArmazenamentoFisico(listaarmazenamentofisico, ScannerActivity.this);
                                mListaArmazenamentoFisico.setAdapter(adapterFisico);
                                mListaArmazenamentoFisico.setVisibility(View.VISIBLE);
                            } else {
                                mListaArmazenamentoFisico.setVisibility(View.GONE);
                            }
                            
                            if (hasDisks || hasPhysicalDisks) {
                                mLayoutArmazenamento.setVisibility(View.VISIBLE);
                                if (!hasDisks) mListaArmazenamento.setVisibility(View.GONE);
                            } else {
                                mLayoutArmazenamento.setVisibility(View.GONE);
                                Log.d("ArmazenamentoDebug", "No logical volumes found in _disks array");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            mLayoutArmazenamento.setVisibility(View.GONE);
                            Log.e("ArmazenamentoDebug", "Error parsing logical disks: " + e.toString());
                        }

                        //Pega as problemas
                        try {
                            JSONArray changesarray = jsonObject.getJSONArray("_problems");
                            for (int i = 0; i < changesarray.length(); i++) {
                                JSONObject mudanca = changesarray.getJSONObject(i);
                                int imagem = R.drawable.status_aberto;
                                int botao = View.VISIBLE;
                                if (mudanca.getString("status").equals("6")) {//Status 6 = fechado
                                    imagem = R.drawable.status_fechado;
                                    botao = View.GONE;
                                } else { // marca true caso haja manutencao em aberto
                                    existemanutencaoaberta = true;
                                }
                                CriarListaMudancas(mudanca.getString("name"), mudanca.getString("users_id_recipient"), mudanca.getString("content"), imagem, botao, mudanca.getString("date"), mudanca.getString("closedate"), mudanca.getString("users_id_lastupdater"), mudanca.getString("id"));
                            }

                            //Confere se existe manutenção aberta, caso não exita, abre botão add nova
                            if (existemanutencaoaberta == false) {
                                mBtNovaManutencao.setVisibility(View.VISIBLE);
                            }
                            //Seta dados pata a lista de mudanças
                            ListAdapterMudancas adapter = new ListAdapterMudancas(listamudancas, ScannerActivity.this, idequipamento);
                            mListaMudancas.setAdapter(adapter);
                            //mostra a tela de manutenções
                            mLayoutManutencoes.setVisibility(View.VISIBLE);
                            mImvSyncDevice.setVisibility(View.GONE);
                            mPgbProgresso.setIndeterminate(false);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //mPgbProgresso.setIndeterminate(false);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onVolleyFailure(String url) {
                    Log.d("VolleyFailure", url);
                    Snackbar.make(
                            mCLayout,
                            "Erro de conexão\n" + url,
                            Snackbar.LENGTH_LONG
                    ).show();
                }
            });
        }else{

        }
    }

    private void GetIdEquipamento() {
        mPgbProgresso.setIndeterminate(true);
        //Limpa views
        inicializarViews();
        //Limpa listas
        inicializarListas();
        GLPIConnect con = new GLPIConnect(this);
        String endpoint = mItemType.equals("Monitor") ? "/apirest.php/Monitor" : "/apirest.php/Computer";
        
        // Busca o prefixo configurado (coluna 5 da tabela CONFIG)
        Crud crud = new Crud();
        String prefixo = crud.SelectItem(this, "CONFIG", 1, 5);
        if (prefixo == null) {
            prefixo = ""; // Sem prefixo por padrão se for nulo
        }
        
        con.GetArray(endpoint + "?searchText[name]=" + prefixo + mTvIdEquipamento.getText().toString(), new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = new JSONArray(response);
                } catch (JSONException e) {
                    Log.d("ParseError", e.toString());
                    e.printStackTrace();
                }
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    //Pega dados do equipamento
                    BuscarListaEquipamentos(jsonObject.getString("id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                mTvIdEquipamento.setText("erro");
                Snackbar.make(
                        mCLayout,
                        "Erro de conexão\n" + url,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    private void BuscarListaMudancas() {

    }

    public void CriarListaEquipamentos(String desc, String cont, Integer img, Integer vaultType){
        EquipamentoLine equip = new EquipamentoLine();
        equip.setDescricao(desc);
        equip.setConteudo(cont);
        equip.setBtEditar(img);
        equip.setVaultType(vaultType);
        listaequipamentos.add(equip);
    }

    private void CriarListaMonitores(String nome, String marca, String modelo, String estado, String id, String numeroserie) {
        MonitorLine monitor = new MonitorLine();
        monitor.setNome(nome);
        monitor.setMarca(marca);
        monitor.setModelo(modelo);
        monitor.setEstado(estado);
        monitor.setIdMonitor(id);
        monitor.setNumeroSerie(numeroserie);
        listamonitores.add(monitor);
    }

    public void CriarListaMudancas(String titulo, String nome, String texto, int Image, int visible, String datamanu, String datafinal, String usuariofinal, String Id){
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(Image);
        Button botao = new Button(this);
        botao.setVisibility(visible);

        MudancasLine mud = new MudancasLine();
        mud.setTitulo(titulo);
        mud.setUsuarioCriacao(nome);
        mud.setTexto(texto);
        mud.setDataManutencao(datamanu);
        mud.setDataFinalizacao(datafinal);
        mud.setUsuarioFinalizacao(usuariofinal);
        mud.setIdMudanca(Id);
        mud.setImagemStatus(imageView);
        mud.setBtFinalizarManutencao(botao);
        listamudancas.add(mud);
        Log.d("grub", titulo);
    }

    public void CriarListaPlacasRede(String titulo, String data, String status){
        PlacasRedeLine rede = new PlacasRedeLine();
        rede.setNome(titulo); ;
        rede.setMac(data);
        rede.setIp(status);
        //listaplacasrede.add(rede);
    }

    public void CriarListaPlacasWifi(String titulo, String data, String status){
        PlacasRedeLine wifi = new PlacasRedeLine();
        wifi.setNome(titulo);
        wifi.setMac(data);
        wifi.setIp(status);
        //listaplacaswifi.add(wifi);
    }

    private void PreencherListaMonitores() {
        mPgbProgresso.setIndeterminate(true);
        mMonitorNamesList = new ArrayList<>();
        mMonitorIdsList = new ArrayList<>();
        mMonitorAdapter = new ArrayAdapter<>(ScannerActivity.this, android.R.layout.simple_dropdown_item_1line, mMonitorNamesList);

        GLPIConnect con = new GLPIConnect(this);
        con.GetArray("/apirest.php/Monitor?range=0-1000", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                mPgbProgresso.setIndeterminate(false);
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject monitor = jsonArray.getJSONObject(i);
                        mMonitorNamesList.add(monitor.getString("name"));
                        mMonitorIdsList.add(monitor.getString("id"));
                    }
                    mMonitorAdapter.notifyDataSetChanged();
                    VincularMonitorDialog();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(ScannerActivity.this, "Erro ao processar monitores.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                mPgbProgresso.setIndeterminate(false);
                Toast.makeText(ScannerActivity.this, "Erro de conexão ao buscar monitores:\n" + url, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void VincularMonitorDialog(){
        View view = LayoutInflater.from(ScannerActivity.this).inflate(R.layout.activity_vincular_monitor, null);
        android.widget.AutoCompleteTextView edittext = view.findViewById(R.id.TvNomeMonitorVincularMonitor);
        
        edittext.setAdapter(mMonitorAdapter);
        edittext.setThreshold(1); // Show suggestions after 1 character

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(ScannerActivity.this)
            .setTitle("Nome do Monitor")
            .setView(view);

        AlertDialog dialog = dialogBuilder.create();

        view.findViewById(R.id.BtNovoMonitorVincular).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(ScannerActivity.this, CadMonitorActivity.class);
                intent.putExtra("computers_id", mTvIdEquipamento.getText().toString());
                startActivity(intent);
            }
        });

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String nomeDigitado = edittext.getText().toString();
                    int index = mMonitorNamesList.indexOf(nomeDigitado);
                    if (index != -1) {
                        String idMonitorSelecionado = mMonitorIdsList.get(index);
                        AddConexao("/apirest.php/Computer_Item/", idMonitorSelecionado);
                    } else {
                        Toast.makeText(ScannerActivity.this, "Selecione um monitor válido da lista.", Toast.LENGTH_LONG).show();
                    }
                }
            });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.show();
    }

    private void AddConexao(String url, String idmonitor) {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        try {
            postparams.put("items_id", idmonitor);
            postparams.put("computers_id", mTvIdEquipamento.getText().toString());
            postparams.put("itemtype", "Monitor");
            postparams.put("is_deleted", "0");
            postparams.put("is_dynamic", "1");

            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("sessiontoken", url);
        Log.e("sessiontoken", finalarray.toString());
        //Faz a conexão
        GLPIConnect con = new GLPIConnect(ScannerActivity.this);
        con.InsertItem(url, finalarray, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                IrPara(ScannerActivity.class, true);
            }
            @Override
            public void onVolleyFailure(String errorMsg) {
                Log.d("sessiontoken", errorMsg);
                if(errorMsg != null && errorMsg.contains("ERROR_GLPI_ADD")) {
                    Toast.makeText(ScannerActivity.this, "Erro: Monitor já está vinculado a outro equipamento.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ScannerActivity.this, "Erro: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void GetIdMonitor(String nome) {
        mIdMonitor = "";
        GLPIConnect con = new GLPIConnect(this);
        con.GetArray("/apirest.php/Monitor?searchText[name]="+ nome, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = new JSONArray(response);
                } catch (JSONException e) {
                    Log.d("ParseError", e.toString());
                    e.printStackTrace();
                }
                try {
                    if (jsonArray.length() > 0) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        //Pega dados do equipamento
                        String idmonitor = jsonObject.getString("id");
                        if (!idmonitor.equals("")) {
                            //Vincular monitor
                            AddConexao("/apirest.php/Computer_Item/", idmonitor);
                        }else{
                            Toast.makeText(ScannerActivity.this, "Monitor não encontrado", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ScannerActivity.this, "Monitor não encontrado", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                mTvIdEquipamento.setText("erro");
                Snackbar.make(
                        mCLayout,
                        "Erro de conexão\n" + url,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Escaneamento cancelado");
                Toast.makeText(this, "Escaneamento cancelado", Toast.LENGTH_LONG).show();
            } else {
                Log.d("MainActivity", "Escaneado");
                //Toast.makeText(this, "Escaneado: " + result.getContents(), Toast.LENGTH_LONG).show();
                mTvIdEquipamento.setText(result.getContents());
                GetIdEquipamento();
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
        }
    }

    public void AbrirCofreComputador() {
        AbrirCofreSenhas("Computer", mTvIdEquipamento.getText().toString());
    }

    public void AbrirCofreLocal() {
        mPgbProgresso.setIndeterminate(true);
        GLPIConnect tempCon = new GLPIConnect(ScannerActivity.this);
        tempCon.GetItem("/apirest.php/Computer/" + mTvIdEquipamento.getText().toString(), new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                mPgbProgresso.setIndeterminate(false);
                try {
                    JSONObject rawComp = new JSONObject(response);
                    String rawLocId = rawComp.optString("locations_id", "0");
                    if (rawLocId.equals("0") || rawLocId.isEmpty()) {
                        Toast.makeText(ScannerActivity.this, "Este computador não possui um Local definido.", Toast.LENGTH_SHORT).show();
                    } else {
                        AbrirCofreSenhas("Location", rawLocId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                mPgbProgresso.setIndeterminate(false);
                Toast.makeText(ScannerActivity.this, "Erro ao consultar a localização", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void AbrirCofreSenhas(String itemtype, String itemId) {
        PasswordVaultHelper helper = new PasswordVaultHelper(this);
        helper.showPasswordDialog(itemtype, itemId, new PasswordVaultHelper.VaultOperationListener() {
            @Override
            public void onLoading(boolean loading) {
                mPgbProgresso.setIndeterminate(loading);
            }

            @Override
            public void onVaultConfigError() {
                RedirecionarParaConfigsCofre("Credenciais do Cofre não configuradas.");
            }
        });
    }

    private void RedirecionarParaConfigsCofre(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, VaultSettingsActivity.class);
        startActivity(intent);
    }
}
