package com.cyberrocket.inventario.ui.tickets;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.cyberrocket.inventario.R;

public class FormActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> uploadMessage;

    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (uploadMessage == null) return;
                Uri[] results = null;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    } else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                uploadMessage.onReceiveValue(results);
                uploadMessage = null;
            }
    );

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        com.cyberrocket.inventario.lib.StatusBarHelper.setupStatusBar(this, findViewById(R.id.containerForm), findViewById(R.id.statusBarBackground));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Formulário GLPI");
        }

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        String url = getIntent().getStringExtra("url");
        String sessionToken = getIntent().getStringExtra("session_token");
        final String usuario = getIntent().getStringExtra("usuario");
        final String senha = getIntent().getStringExtra("senha");

        if (url != null && sessionToken != null) {
            // Tenta injetar o token na URL caso o GLPI suporte ?session_token=...
            if (url.contains("?")) {
                url += "&session_token=" + sessionToken;
            } else {
                url += "?session_token=" + sessionToken;
            }

            // Configura o CookieManager para o SSO via Cookie
            try {
                android.net.Uri uri = android.net.Uri.parse(url);
                String domain = uri.getHost();
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(webView, true);
                
                // No GLPI 10/11, o token da API as vezes pode ser usado como id de sessao PHP (glpi_session)
                cookieManager.setCookie(domain, "glpi_session=" + sessionToken + "; Path=/; Secure; HttpOnly");
                cookieManager.flush();
                
                android.util.Log.d("FormActivity", "SSO: Injetando cookie para o domínio: " + domain);
            } catch (Exception e) {
                android.util.Log.e("FormActivity", "Erro ao configurar cookies de SSO", e);
            }
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(view.getTitle());
                }

                // Injeção de JS para Auto-Login Ninja (Suporta GLPI 10 e 11)
                if (usuario != null && senha != null && !usuario.isEmpty() && !senha.isEmpty()) {
                    String js = "javascript:(function() {" +
                            "   function tryLogin() {" +
                            "       var inputs = document.getElementsByTagName('input');" +
                            "       var userField = null, passField = null, submitBtn = null;" +
                            "       " +
                            "       for(var i=0; i<inputs.length; i++) {" +
                            "           var inp = inputs[i];" +
                            "           var name = (inp.name || '').toLowerCase();" +
                            "           var id = (inp.id || '').toLowerCase();" +
                            "           if (!userField && (name === 'login_name' || name === 'login_user' || id === 'login_user')) userField = inp;" +
                            "           if (!passField && inp.type === 'password') passField = inp;" +
                            "           if (!submitBtn && (name === 'submit' || inp.type === 'submit')) submitBtn = inp;" +
                            "       }" +
                            "       if (!submitBtn) submitBtn = document.querySelector('button[type=\"submit\"]');" +
                            "       " +
                            "       if (userField && passField) {" +
                            "           userField.value = '" + usuario + "';" +
                            "           passField.value = '" + senha + "';" +
                            "           if (submitBtn) {" +
                            "               setTimeout(function() { submitBtn.click(); }, 300);" +
                            "           } else {" +
                            "               userField.form.submit();" +
                            "           }" +
                            "       }" +
                            "   }" +
                            "   setTimeout(tryLogin, 800);" +
                            "})()";
                    view.loadUrl(js);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                android.util.Log.d("WebViewConsole", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;

                Intent intent = null;
                try {
                    intent = fileChooserParams.createIntent();
                } catch (Exception e) {
                    android.util.Log.e("FormActivity", "Erro ao criar intent do fileChooser", e);
                }

                if (intent == null) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                }

                if (fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }

                try {
                    fileChooserLauncher.launch(Intent.createChooser(intent, "Selecionar Arquivo"));
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    Toast.makeText(FormActivity.this, "Nenhum gerenciador de arquivos encontrado", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        if (url != null) {
            webView.loadUrl(url);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_form, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_open_browser) {
            if (webView != null && webView.getUrl() != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()));
                startActivity(browserIntent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
