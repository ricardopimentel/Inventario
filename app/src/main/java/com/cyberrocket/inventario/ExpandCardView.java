package com.cyberrocket.inventario;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ExpandCardView extends AppCompatActivity {
    TextView mItemDescription;
    ImageView mDescriptionImg;
    CardView mCard;
    ConstraintLayout mRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expand_card_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        mDescriptionImg = findViewById(R.id.item_description_img);
        mItemDescription = findViewById(R.id.item_description);
        mCard = findViewById(R.id.card);
        mRoot = findViewById(R.id.root);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        mCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collapseExpandTextView();
            }
        });
    }

    void collapseExpandTextView() {
        if (mItemDescription.getVisibility() == View.GONE) {
            // it's collapsed - expand it
            TransitionManager.beginDelayedTransition(mRoot, new AutoTransition());
            mItemDescription.setVisibility(View.VISIBLE);
            mDescriptionImg.setImageResource(R.drawable.ic_calendar_24);
        } else {
            // it's expanded - collapse it
            TransitionManager.beginDelayedTransition(mRoot, new AutoTransition());
            mItemDescription.setVisibility(View.GONE);
            mDescriptionImg.setImageResource(R.drawable.ic_home_black_24dp);
        }
    }

}
