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

        Toolbar toolbar = findViewById(R.id.toolbarKB);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalhes da Base");
        }

        TextView tvTitle = findViewById(R.id.detailsTitleKB);
        TextView tvDate = findViewById(R.id.detailsDateKB);
        TextView tvContent = findViewById(R.id.detailsContentKB);

        String title = getIntent().getStringExtra("name");
        String date = getIntent().getStringExtra("date_mod");
        String content = getIntent().getStringExtra("content");

        tvTitle.setText(title);
        tvDate.setText("Modificado em: " + date);
        
        if (content != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                tvContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT));
            } else {
                tvContent.setText(Html.fromHtml(content));
            }
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
