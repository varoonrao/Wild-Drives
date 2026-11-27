package com.wilddrives.privategame;

import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class MainActivity extends Activity {
    private static final int PICK_MODEL = 101;
    private WebView web;
    private PowerManager power;
    private boolean active;
    private ValueCallback<Uri[]> modelPicker;
    private final PowerManager.OnThermalStatusChangedListener thermal = this::applyThermal;

    private void immersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                c.hide(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
    private void applyThermal(int status) {
        if (!active || web == null) return;
        // 60 Hz is requested under thermal pressure; the game additionally limits itself to 30 FPS at severe status.
        float hz = status >= PowerManager.THERMAL_STATUS_MODERATE ? 60f : 120f;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp.preferredRefreshRate != hz) {
            lp.preferredRefreshRate = hz;
            getWindow().setAttributes(lp);
        }
        web.evaluateJavascript("if(window.wildDrivesThermal)window.wildDrivesThermal(" + status + ")", null);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        power = getSystemService(PowerManager.class);
        web = new WebView(this);
        web.setBackgroundColor(0xff101711);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        // User-selected files may be read through the system document picker.
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setBlockNetworkLoads(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return true; }
            @Override public void onPageFinished(WebView v, String u) {
                if (active && power != null) applyThermal(power.getCurrentThermalStatus());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (modelPicker != null) modelPicker.onReceiveValue(null);
                modelPicker = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try { startActivityForResult(intent, PICK_MODEL); }
                catch (ActivityNotFoundException e) { modelPicker.onReceiveValue(null); modelPicker = null; }
                return true;
            }
        });
        setContentView(web);
        immersive();
        web.loadUrl("file:///android_asset/index.html");
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_MODEL && modelPicker != null) {
            modelPicker.onReceiveValue(result == RESULT_OK && data != null && data.getData() != null ? new Uri[]{data.getData()} : null);
            modelPicker = null;
        }
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }
    @Override public void onResume() {
        super.onResume(); active = true;
        if (web != null) { web.onResume(); web.resumeTimers(); }
        if (power != null) { power.addThermalStatusListener(getMainExecutor(), thermal); applyThermal(power.getCurrentThermalStatus()); }
    }
    @Override public void onPause() {
        active = false;
        if (power != null) power.removeThermalStatusListener(thermal);
        if (web != null) {
            web.evaluateJavascript("if(window.wildDrivesSuspend)window.wildDrivesSuspend()", null);
            web.onPause(); web.pauseTimers();
        }
        super.onPause();
    }
    @Override public void onBackPressed() {
        if (web != null) web.evaluateJavascript("var m=document.getElementById('modal'); if(m&&m.classList.contains('show'))document.getElementById('closeModal').click();else document.getElementById('menu').click();", null);
    }
    @Override public void onDestroy() {
        if (modelPicker != null) { modelPicker.onReceiveValue(null); modelPicker = null; }
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
