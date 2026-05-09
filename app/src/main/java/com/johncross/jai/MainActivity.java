package com.johncross.jai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.*;
import android.graphics.Color;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1;
    private static final int PERMISSION_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#0C0E15"));
        }

        // Request permissions
        requestPermissions();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        webView.setBackgroundColor(Color.parseColor("#0C0E15"));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                view.loadData(getOfflinePage(), "text/html", "UTF-8");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new JaiInterface(), "Android");
        webView.loadUrl("http://localhost:8080");
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
        };
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }

    public class JaiInterface {
        @JavascriptInterface
        public String getDeviceInfo() {
            return Build.MODEL + " / Android " + Build.VERSION.RELEASE;
        }
    }

    private String getOfflinePage() {
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>*{margin:0;padding:0}body{background:#0C0E15;color:#EDEDED;font-family:sans-serif;"
            + "display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;text-align:center;padding:20px}"
            + ".logo{width:80px;height:80px;background:#00C8B4;border-radius:40px;display:flex;align-items:center;"
            + "justify-content:center;font-size:36px;font-weight:bold;color:#000;margin-bottom:20px}"
            + "h1{color:#00C8B4;font-size:22px;margin-bottom:10px}p{color:#8A8E9A;font-size:13px;margin-bottom:20px;line-height:1.6}"
            + "button{background:#00C8B4;color:#000;border:none;padding:12px 24px;border-radius:10px;font-size:15px;font-weight:bold}"
            + "</style></head><body><div class='logo'>J</div><h1>J-AI Starting...</h1>"
            + "<p>Please open Termux and start the J-AI server,<br>then tap Retry.</p>"
            + "<button onclick='location.reload()'>Retry</button></body></html>";
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() { super.onResume(); webView.onResume(); }

    @Override
    protected void onPause() { super.onPause(); webView.onPause(); }
}
