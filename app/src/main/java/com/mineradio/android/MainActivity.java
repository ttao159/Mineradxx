package com.mineradio.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends AppCompatActivity {

    // ====================================================================
    // 远程后端地址（完整桌面版后端，可部署到 Railway / 自有服务器）
    // 部署后替换为你的真实域名，例如 "https://xxx.up.railway.app"
    // 注意：不要带结尾的 "/"
    // ====================================================================
    private static final String API_BASE_URL = "https://YOUR-RAILWAY-URL.up.railway.app";

    // 双模式：Mineradio 完整播放器 / Folia 歌词舞台
    private static final String MODE_PLAYER = "player";
    private static final String MODE_FOLIA  = "folia";

    private static final String URL_PLAYER = "https://mineradio.local/index.html";
    private static final String URL_FOLIA  = "https://mineradio.local/folia/index.html";

    private WebView webView;
    private NodeService nodeService;
    private Button modeSwitchButton;
    private String currentMode = MODE_PLAYER;

    public class AndroidBridge {
        @JavascriptInterface
        public String getPlatform() { return "android"; }

        @JavascriptInterface
        public boolean isAndroid() { return true; }

        @JavascriptInterface
        public int getServerPort() {
            return (nodeService != null) ? nodeService.getPort() : 0;
        }

        @JavascriptInterface
        public String getApiBase() { return API_BASE_URL; }

        @JavascriptInterface
        public void switchToFolia() {
            runOnUiThread(() -> switchMode(MODE_FOLIA));
        }

        @JavascriptInterface
        public void switchToPlayer() {
            runOnUiThread(() -> switchMode(MODE_PLAYER));
        }

        @JavascriptInterface
        public void openExternalBrowser(String url) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "无法打开外部浏览器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        @JavascriptInterface
        public void showToast(String msg) {
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nodeService = new NodeService();
        nodeService.start(this, () -> runOnUiThread(() -> initWebViewIfNeeded()));

        initWebViewIfNeeded();
        setupModeSwitchButton();
    }

    private void initWebViewIfNeeded() {
        if (webView != null) return;

        // 沉浸式全屏
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        webView = new WebView(this);
        setContentView(webView);
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onCreateWindow(android.webkit.WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                android.webkit.WebView newView = new android.webkit.WebView(MainActivity.this);
                android.webkit.WebView.WebViewTransport transport = (android.webkit.WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/vendor/", new WebViewAssetLoader.AssetsPathHandler(this))
                .setDomain("mineradio.local")
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                WebResourceResponse response = assetLoader.shouldInterceptRequest(uri);
                if (response != null) return response;
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.contains("/folia/")) {
                    injectFoliaBridge();
                } else {
                    injectDesktopStubs();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://mineradio.local/") || url.startsWith("http://127.0.0.1:")) {
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "无法打开链接", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        loadMode(MODE_PLAYER);
    }

    private void setupModeSwitchButton() {
        modeSwitchButton = new Button(this);
        modeSwitchButton.setText("歌词舞台");
        modeSwitchButton.setTextColor(Color.WHITE);
        modeSwitchButton.setTextSize(14);
        modeSwitchButton.setAllCaps(false);
        modeSwitchButton.setGravity(android.view.Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x99000000);
        bg.setCornerRadius(32);
        bg.setStroke(2, 0x55FFFFFF);
        modeSwitchButton.setBackground(bg);

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (20 * density + 0.5f);
        int padV = (int) (13 * density + 0.5f);
        modeSwitchButton.setPadding(padH, padV, padH, padV);

        modeSwitchButton.setOnClickListener(v -> {
            if (MODE_PLAYER.equals(currentMode)) {
                switchMode(MODE_FOLIA);
            } else {
                switchMode(MODE_PLAYER);
            }
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        lp.topMargin = (int) (48 * density + 0.5f);
        lp.rightMargin = (int) (18 * density + 0.5f);
        addContentView(modeSwitchButton, lp);
    }

    private void loadMode(String mode) {
        currentMode = mode;
        if (modeSwitchButton != null) {
            modeSwitchButton.setText(MODE_FOLIA.equals(mode) ? "返回播放器" : "歌词舞台");
        }
        if (webView == null) return;
        int port = (nodeService != null) ? nodeService.getPort() : 0;
        if (port > 0) {
            webView.loadUrl(MODE_FOLIA.equals(mode)
                    ? "http://127.0.0.1:" + port + "/folia/index.html"
                    : "http://127.0.0.1:" + port + "/index.html");
        } else {
            webView.loadUrl(MODE_FOLIA.equals(mode) ? URL_FOLIA : URL_PLAYER);
        }
    }

    private void switchMode(String mode) {
        if (mode.equals(currentMode)) return;
        loadMode(mode);
    }

    // ====================================================================
    // Folia 歌词舞台桥：注入 window.__NCM_API_BASE（Folia 运行时优先读取）。
    // 注意：故意不注入 window.electron——Folia 通过 window.electron 是否存在
    // 判断 Electron 运行时，注入会导致其走 localhost:port 而非 __NCM_API_BASE。
    // Folia 对 window.electron 的全部调用都是可选链（?.），无桩时自然跳过。
    // ====================================================================
    private void injectFoliaBridge() {
        String js = "javascript:(function() {" +
            "if (window.__FOLIA_BRIDGE_INJECTED__) return;" +
            "window.__FOLIA_BRIDGE_INJECTED__ = true;" +
            "window.__NCM_API_BASE = '" + API_BASE_URL + "';" +
        "})();";
        webView.evaluateJavascript(js, null);
    }

    private void injectDesktopStubs() {
        String apiBase = API_BASE_URL;
        String js = "javascript:(function() {" +
            "if (window.desktopWindow) return;" +
            "window.desktopWindow = { apiBase: '" + apiBase + "'," +
            "  isDesktop: true," +
            "  minimize: function(){return Promise.resolve();}," +
            "  toggleMaximize: function(){return Promise.resolve();}," +
            "  toggleFullscreen: function(){" +
            "    if(document.documentElement.requestFullscreen&&!document.fullscreenElement)" +
            "      document.documentElement.requestFullscreen();" +
            "    else if(document.exitFullscreen) document.exitFullscreen();" +
            "    return Promise.resolve();" +
            "  }," +
            "  exitFullscreenWindowed: function(){" +
            "    if(document.fullscreenElement) document.exitFullscreen();" +
            "    return Promise.resolve();" +
            "  }," +
            "  getState: function(){return Promise.resolve({isMaximized:false,isMinimized:false,isFullscreen:!!document.fullscreenElement});}," +
            "  close: function(){/* no-op */return Promise.resolve();}," +
            "  openNeteaseMusicLogin:function(){" +
            "    if(window.AndroidBridge)window.AndroidBridge.showToast('请先在网易云音乐网页端登录获取cookie');" +
            "    if(window.AndroidBridge)window.AndroidBridge.openExternalBrowser('https://music.163.com/#/login');" +
            "    return Promise.resolve({ok:true,cookie:''});" +
            "  }," +
            "  clearNeteaseMusicLogin:function(){return Promise.resolve();}," +
            "  openQQMusicLogin:function(){" +
            "    if(window.AndroidBridge)window.AndroidBridge.showToast('请先在QQ音乐网页端登录获取cookie');" +
            "    if(window.AndroidBridge)window.AndroidBridge.openExternalBrowser('https://y.qq.com/n/ryqq/profile');" +
            "    return Promise.resolve({ok:true,cookie:''});" +
            "  }," +
            "  clearQQMusicLogin:function(){return Promise.resolve();}," +
            "  openUpdateInstaller:function(){return Promise.resolve();}," +
            "  restartApp:function(){return Promise.resolve();}," +
            "  configureGlobalHotkeys:function(){return Promise.resolve();}," +
            "  exportJsonFile:function(){return Promise.resolve();}," +
            "  importJsonFile:function(){return Promise.resolve();}," +
            "  setDesktopLyricsEnabled:function(){return Promise.resolve();}," +
            "  updateDesktopLyrics:function(){return Promise.resolve();}," +
            "  setWallpaperMode:function(){return Promise.resolve();}," +
            "  updateWallpaperMode:function(){return Promise.resolve();}," +
            "  onGlobalHotkey:function(){return function(){};}," +
            "  onDesktopLyricsLockState:function(){return function(){};}," +
            "  onDesktopLyricsEnabledState:function(){return function(){};}," +
            "  onStateChange:function(){return function(){};}," +
            "};" +
            "document.documentElement.classList.add('simple-mode-preload');" +
            "document.body.classList.add('android-shell');" +

            "var _origFetch = window.fetch;" +
            "window.__apiBase = '" + apiBase + "';" +
            "window.__apiBaseConfigured = " + (API_BASE_URL.contains("YOUR-RAILWAY") ? "false" : "true") + ";" +
            "window.fetch = function(url, opts) {" +
            "  if (typeof url === 'string' && url.startsWith('/api/')) {" +
            "    if (!window.__apiBaseConfigured) {" +
            "      if (window.AndroidBridge) window.AndroidBridge.showToast('后端未配置，请修改 MainActivity 中的 API_BASE_URL');" +
            "      return Promise.resolve({json:function(){return Promise.resolve({error:'backend not available',loggedIn:false,playlists:[],tracks:[],songs:[]});}});" +
            "    }" +
            "    url = window.__apiBase + url;" +
            "  }" +
            "  return _origFetch.call(window, url, opts);" +
            "};" +
        "})();";
        webView.evaluateJavascript(js, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.evaluateJavascript("window.history.back();", null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (nodeService != null) nodeService.stop();
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
