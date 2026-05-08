package com.johncross.jai;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.JavascriptInterface;
import android.net.Uri;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.graphics.Color;
import android.content.Intent;
import android.os.Build;
import android.app.AlertDialog;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen, no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#0C0E15"));
        }

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(false);

        // Keyboard fix — input box above keyboard
        getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        webView.setBackgroundColor(Color.parseColor("#0C0E15"));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://localhost") || url.startsWith("http://127.0.0.1")) {
                    return false;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Show offline page if server not running
                view.loadData(getOfflinePage(), "text/html", "UTF-8");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        // Add JavaScript interface
        webView.addJavascriptInterface(new JaiInterface(), "Android");

        // Load J-AI server
        webView.loadUrl("http://localhost:8080");
    }

    private String getOfflinePage() {
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>*{margin:0;padding:0;box-sizing:border-box;}"
            + "body{background:#0C0E15;color:#EDEDED;font-family:sans-serif;"
            + "display:flex;flex-direction:column;align-items:center;justify-content:center;"
            + "height:100vh;text-align:center;padding:20px;}"
            + ".logo{width:72px;height:72px;background:#00C8B4;border-radius:36px;"
            + "display:flex;align-items:center;justify-content:center;"
            + "font-size:32px;font-weight:bold;color:#000;margin-bottom:20px;}"
            + "h1{color:#00C8B4;font-size:24px;margin-bottom:10px;}"
            + "p{color:#8A8E9A;font-size:14px;margin-bottom:20px;line-height:1.6;}"
            + "button{background:#00C8B4;color:#000;border:none;padding:14px 28px;"
            + "border-radius:12px;font-size:16px;font-weight:bold;cursor:pointer;}"
            + "</style></head><body>"
            + "<div class='logo'>J</div>"
            + "<h1>J-AI is Starting...</h1>"
            + "<p>The J-AI server is loading.<br>Please wait a moment and try again.</p>"
            + "<button onclick='location.reload()'>Retry</button>"
            + "</body></html>";
    }

    public class JaiInterface {
        @JavascriptInterface
        public String getDeviceInfo() {
            return Build.MODEL + " / Android " + Build.VERSION.RELEASE;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }
}
