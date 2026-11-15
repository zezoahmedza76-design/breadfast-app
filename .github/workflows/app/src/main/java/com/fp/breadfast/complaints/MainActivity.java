package com.fp.breadfast.complaints;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.messaging.FirebaseMessaging;

import java.net.URLEncoder;

/** بيفتح صفحة المندوب/البيكر جوه التطبيق، وبيبعت للصفحة رقم الموبايل في الإشعارات. */
public class MainActivity extends Activity {
    private static final int PICK_FILE = 7;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createChannel();
        askNotificationPermission();

        getWindow().setStatusBarColor(Color.parseColor("#A7077F"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F4F1F4"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String host = u.getHost() == null ? "" : u.getHost();
                if (host.endsWith("google.com") || host.endsWith("googleusercontent.com") || host.endsWith("gstatic.com")) {
                    return false;   // صفحة الشكاوى نفسها
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;        // أي لينك تاني يفتح برا التطبيق
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), PICK_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
            loaded = true;
            return;
        }

        // رقم الموبايل في الإشعارات، وبعدين نفتح الصفحة. لو اتأخر أكتر من 6 ثواني نفتح من غيره.
        new Handler(Looper.getMainLooper()).postDelayed(() -> load(""), 6000);
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task ->
                    load(task.isSuccessful() && task.getResult() != null ? task.getResult() : ""));
        } catch (Exception e) {
            load("");
        }
    }

    private void load(String pushToken) {
        if (loaded) return;
        loaded = true;
        String url = getString(R.string.page_url);
        if (pushToken != null && !pushToken.isEmpty()) {
            try {
                url += (url.contains("?") ? "&" : "?") + "push=" + URLEncoder.encode(pushToken, "UTF-8");
            } catch (Exception ignored) { }
        }
        web.loadUrl(url);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("complaints", getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            ch.enableLights(true);
            ch.setLightColor(Color.parseColor("#A7077F"));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
