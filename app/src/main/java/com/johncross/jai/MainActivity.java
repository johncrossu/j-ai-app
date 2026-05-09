package com.johncross.jai;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;
import android.Manifest;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private boolean ttsReady = false;

    private static final int FILE_CHOOSER_REQUEST = 100;
    private static final int SPEECH_REQUEST = 101;
    private static final int PERMISSION_REQUEST = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen setup
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#0C0E15"));
            getWindow().setNavigationBarColor(Color.parseColor("#0C0E15"));
        }

        // Request all permissions upfront
        requestAllPermissions();

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                ttsReady = true;
            }
        });

        // Setup WebView
        webView = new WebView(this);
        setContentView(webView);
        webView.setBackgroundColor(Color.parseColor("#0C0E15"));

        // WebView settings
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setDatabaseEnabled(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        ws.setGeolocationEnabled(false);

        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // JavaScript Interface
        webView.addJavascriptInterface(new JaiAndroidBridge(), "AndroidBridge");

        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                view.loadData(getOfflinePage(), "text/html; charset=utf-8", "UTF-8");
            }
        });

        // WebChromeClient - handles file chooser and permissions
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            // FILE CHOOSER - Critical for uploads
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                // Cancel any pending callback
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                }
                filePathCallback = callback;

                // Build intent for file picker
                Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentIntent.addCategory(Intent.CATEGORY_OPENABLE);

                // Accept all types
                String[] acceptTypes = params.getAcceptTypes();
                if (acceptTypes != null && acceptTypes.length > 0 &&
                    !acceptTypes[0].isEmpty()) {
                    contentIntent.setType(acceptTypes[0]);
                    if (acceptTypes.length > 1) {
                        contentIntent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes);
                    }
                } else {
                    contentIntent.setType("*/*");
                }

                Intent chooserIntent = Intent.createChooser(contentIntent, "Select File");

                // Also allow camera for images
                if (params.isCaptureEnabled()) {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    Intent[] extraIntents = {cameraIntent};
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
                }

                try {
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Cannot open file picker", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        // Load the app
        webView.loadUrl("http://localhost:8080");
    }

    // Handle file picker result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;

            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String dataStr = data.getDataString();
                    if (dataStr != null) {
                        results = new Uri[]{Uri.parse(dataStr)};
                    } else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }

        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                webView.evaluateJavascript(
                    "document.getElementById('messageInput').value='" +
                    text.replace("'", "\\'") + "';", null);
                webView.evaluateJavascript(
                    "if(window.autoSendVoice) sendMessage();", null);
            }
        }
    }

    // Request all permissions at startup
    private void requestAllPermissions() {
        ArrayList<String> perms = new ArrayList<>();
        String[] required = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        };
        for (String p : required) {
            if (ContextCompat.checkSelfPermission(this, p) !=
                PackageManager.PERMISSION_GRANTED) {
                perms.add(p);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_MEDIA_IMAGES) !=
                PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_MEDIA_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_MEDIA_VIDEO) !=
                PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                perms.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }

    // JavaScript Bridge
    public class JaiAndroidBridge {

        // Start native Android speech recognition
        @JavascriptInterface
        public void startSpeechRecognition() {
            runOnUiThread(() -> {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to J-AI...");
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                try {
                    startActivityForResult(intent, SPEECH_REQUEST);
                } catch (Exception e) {
                    webView.evaluateJavascript(
                        "showToast('Voice not available on this device')", null);
                }
            });
        }

        // Native TTS
        @JavascriptInterface
        public void speak(String text) {
            if (ttsReady && tts != null) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JAI_TTS");
            }
        }

        // Stop TTS
        @JavascriptInterface
        public void stopSpeaking() {
            if (tts != null) tts.stop();
        }

        // Get device info
        @JavascriptInterface
        public String getDeviceInfo() {
            return Build.MODEL + " / Android " + Build.VERSION.RELEASE;
        }

        // Check if speech recognition is available
        @JavascriptInterface
        public boolean isSpeechAvailable() {
            return SpeechRecognizer.isRecognitionAvailable(MainActivity.this);
        }

        // Show native toast
        @JavascriptInterface
        public void showNativeToast(String msg) {
            runOnUiThread(() ->
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }

        // Open file chooser from JS
        @JavascriptInterface
        public void openFileChooser(String mimeType) {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(mimeType != null && !mimeType.isEmpty() ? mimeType : "*/*");
                Intent chooser = Intent.createChooser(intent, "Select File");
                try {
                    startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                        "Cannot open file picker", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getOfflinePage() {
        return "<!DOCTYPE html><html><head>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>"
            + "*{margin:0;padding:0;box-sizing:border-box}"
            + "body{background:#0C0E15;color:#EDEDED;font-family:sans-serif;"
            + "display:flex;flex-direction:column;align-items:center;"
            + "justify-content:center;height:100vh;text-align:center;padding:20px}"
            + ".logo{width:90px;height:90px;background:#00C8B4;border-radius:45px;"
            + "display:flex;align-items:center;justify-content:center;"
            + "font-size:40px;font-weight:bold;color:#000;margin-bottom:20px}"
            + "h1{color:#00C8B4;font-size:22px;margin-bottom:12px}"
            + "p{color:#8A8E9A;font-size:13px;line-height:1.7;margin-bottom:24px}"
            + "button{background:#00C8B4;color:#000;border:none;padding:13px 28px;"
            + "border-radius:12px;font-size:15px;font-weight:bold;cursor:pointer}"
            + "</style></head><body>"
            + "<div class='logo'>J</div>"
            + "<h1>J-AI is Starting...</h1>"
            + "<p>Open Termux and make sure<br>the J-AI server is running.<br><br>"
            + "Then tap Retry below.</p>"
            + "<button onclick='location.reload()'>Retry</button>"
            + "</body></html>";
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (webView != null) { webView.destroy(); }
    }
}
