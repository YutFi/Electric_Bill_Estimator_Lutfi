package com.example.electricbillestimator;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    private static final String WEB_URL = "https://yutfi.github.io/Electric_Bill_Estimator_Lutfi/";
    private static final String SOURCE_URL = "https://github.com/YutFi/Electric_Bill_Estimator_Lutfi";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Ui.applySystemBarInsets(findViewById(R.id.main));
        Ui.setupToolbar(this, R.string.title_about);

        findViewById(R.id.btnWeb).setOnClickListener(v -> open(WEB_URL));
        findViewById(R.id.btnSource).setOnClickListener(v -> open(SOURCE_URL));

        if (savedInstanceState == null) {
            Ui.popIn(findViewById(R.id.devCard), 0);
            Ui.popIn(findViewById(R.id.cardHow), 120);
            Ui.popIn(findViewById(R.id.cardTariff), 220);
        }
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show();
        }
    }
}
