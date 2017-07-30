package com.patientbot;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by Jayanthi Venkat on 7/30/2017.
 */

public class PatientInfoDetailActivity extends AppCompatActivity {
    private WebView webview;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.load_webpage);
        Intent intent = getIntent();
        webview = (WebView) findViewById(R.id.webview);
        if(intent!=null) {
            Log.d("TAG","--intent.getStringExtra(\"search_item_url\")"+intent.getStringExtra("search_item_url"));
            loadPage(intent.getStringExtra("search_item_url"));
        }
    }

    private void loadPage(String url) {
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl(url);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
}
