package com.example.productmanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class VnpayWebViewActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        setContentView(webView);

        String paymentUrl = getIntent().getStringExtra("paymentUrl");

        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                String url = request.getUrl().toString();

                if (url.startsWith("myapp://payment-success")) {

                    Intent intent = new Intent(
                            VnpayWebViewActivity.this,
                            BillingSuccessActivity.class
                    );

                    intent.setData(Uri.parse(url));

                    startActivity(intent);
                    finish();

                    return true;
                }

                return false;
            }
        });

        assert paymentUrl != null;
        webView.loadUrl(paymentUrl);
    }
}
