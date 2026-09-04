package com.cyberrocket.inventario.ui.knowledgebase;

import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.cyberrocket.inventario.R;

public class KnowbaseDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_knowbase_details);

        com.cyberrocket.inventario.lib.StatusBarHelper.setupStatusBar(this, findViewById(R.id.containerKB), findViewById(R.id.statusBarBackground));

        Toolbar toolbar = findViewById(R.id.toolbarKB);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalhes da Base");
        }

        TextView tvTitle = findViewById(R.id.detailsTitleKB);
        TextView tvDate = findViewById(R.id.detailsDateKB);
        android.webkit.WebView webView = findViewById(R.id.detailsWebKB);
        webView.setBackgroundColor(0); // Transparent

        String title = getIntent().getStringExtra("name");
        String date = getIntent().getStringExtra("date_mod");
        String content = getIntent().getStringExtra("content");

        tvTitle.setText(title);
        tvDate.setText("Modificado em: " + date);
        
        if (content != null) {
            String styledHtml = "<html><head>" +
                "<style>" +
                "body { font-family: sans-serif; font-size: 16px; line-height: 1.6; color: #333333; margin: 0; padding: 0; }" +
                "img { max-width: 100%; height: auto; display: block; margin: 10px 0; }" +
                "table { border-collapse: collapse; width: 100%; margin: 10px 0; }" +
                "th, td { border: 1px solid #dddddd; text-align: left; padding: 8px; }" +
                "tr:nth-child(even) { background-color: #f9f9f9; }" +
                "pre, code { background-color: #f4f4f4; padding: 4px; border-radius: 4px; overflow-x: auto; }" +
                "blockquote { border-left: 4px solid #cccccc; margin: 0; padding-left: 16px; color: #666666; }" +
                "</style></head><body>" + content + "</body></html>";
            
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
