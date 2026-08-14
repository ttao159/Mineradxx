package com.mineradio.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // ====================================================================
    // 远程后端地址（完整桌面版后端，可部署到 Railway / 自有服务器）
    // 部署后替换为你的真实域名，例如 "https://xxx.up.railway.app"
    // 注意：不要带结尾的 "/"
    // ====================================================================
    private static final String API_BASE_URL = "https://mineradxx-production.up.railway.app";

    // 双模式：Mineradio 完整播放器 / Folia 歌词舞台
    private static final String MODE_PLAYER = "player";
    private static final String MODE_FOLIA  = "folia";

    private static final String URL_PLAYER = "https://mineradio.local/index.html";
    private static final String URL_FOLIA  = "https://mineradio.local/folia/index.html";

    private WebView webView;
    private NodeService nodeService;
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

        // 在页面任何脚本执行前注入运行环境（含 fetch 拦截器与 desktopWindow stub），
        // 避免 onPageFinished 注入过晚导致早期登录请求（尤其 POST）在 Java 代理层丢失 body。
        WebViewCompat.addDocumentStartJavaScript(
                webView,
                buildBootScript(),
                Collections.singleton("https://mineradio.local"));

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
                // 原生资源请求（如 <audio src>、<img src>）不经过 window.fetch，
                // 其相对 /api/ 路径需在 Java 层代理到远程后端。
                WebResourceResponse api = proxyApiRequest(uri, request);
                if (api != null) return api;
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!url.contains("/folia/")) {
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

    private void loadMode(String mode) {
        currentMode = mode;
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
    // /api/ 代理：原生资源请求（<audio src>、<img src>、<video> 等）不经过
    // window.fetch，无法被 JS 拦截器重写。这里把 mineradio.local 虚拟域下的
    // 相对 /api/ 请求转发到远程后端，保证音频、封面等能正常加载。
    // ====================================================================
    private WebResourceResponse proxyApiRequest(Uri uri, WebResourceRequest request) {
        String url = uri.toString();
        String path = null;
        if (url.startsWith("https://mineradio.local/")) {
            path = url.substring("https://mineradio.local".length());
        } else if (url.startsWith("http://127.0.0.1")) {
            int idx = url.indexOf('/', "http://127.0.0.1".length());
            path = idx >= 0 ? url.substring(idx) : "/";
        }
        if (path == null || !path.startsWith("/api/")) return null;

        HttpURLConnection conn = null;
        try {
            String target = API_BASE_URL + path;
            conn = (HttpURLConnection) new URL(target).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            String method = request.getMethod();
            conn.setRequestMethod(method == null || method.isEmpty() ? "GET" : method);
            for (Map.Entry<String, String> e : request.getRequestHeaders().entrySet()) {
                String k = e.getKey();
                if (k == null) continue;
                String lk = k.toLowerCase(Locale.ROOT);
                if (lk.equals("host") || lk.equals("connection") || lk.equals("accept-encoding")
                        || lk.equals("content-length") || lk.equals("keep-alive")) continue;
                conn.setRequestProperty(k, e.getValue());
            }
            conn.setRequestProperty("Accept-Encoding", "identity");

            int status = conn.getResponseCode();
            String contentType = conn.getContentType();
            String mime = "application/octet-stream";
            if (contentType != null) {
                int sc = contentType.indexOf(';');
                String base = (sc > 0 ? contentType.substring(0, sc) : contentType).trim();
                if (!base.isEmpty()) mime = base;
            }
            Map<String, String> headers = new HashMap<>();
            copyHeader(conn, headers, "Content-Range");
            copyHeader(conn, headers, "Accept-Ranges");
            copyHeader(conn, headers, "Content-Length");

            InputStream raw = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (raw == null) raw = new ByteArrayInputStream(new byte[0]);
            InputStream body = new ProxyStream(raw, conn);
            conn = null; // 所有权交给 body
            return new WebResourceResponse(mime, "utf-8", status, "OK", headers, body);
        } catch (Exception e) {
            if (conn != null) conn.disconnect();
            return new WebResourceResponse("text/plain", "utf-8", 502, "Bad Gateway", null,
                    new ByteArrayInputStream("proxy error".getBytes()));
        }
    }

    private void copyHeader(HttpURLConnection conn, Map<String, String> out, String name) {
        String v = conn.getHeaderField(name);
        if (v != null) out.put(name, v);
    }

    private static final class ProxyStream extends FilterInputStream {
        private final HttpURLConnection conn;
        ProxyStream(InputStream in, HttpURLConnection conn) { super(in); this.conn = conn; }
        @Override
        public void close() throws java.io.IOException {
            try { super.close(); } finally { conn.disconnect(); }
        }
    }

    // 构建可在文档起始注入的运行环境脚本（幂等）。
    // 播放器模式注入 desktopWindow stub + fetch 拦截器；Folia 模式只注入 __NCM_API_BASE。
    private String buildBootScript() {
        String apiBase = API_BASE_URL;
        boolean configured = !API_BASE_URL.contains("YOUR-RAILWAY");
        return "(function(){" +
            "window.__NCM_API_BASE='" + apiBase + "';" +
            "if(location.pathname.indexOf('/folia/')===0)return;" +
            "window.__apiBase='" + apiBase + "';" +
            "window.__apiBaseConfigured=" + (configured ? "true" : "false") + ";" +
            "if(!window.desktopWindow){window.desktopWindow={" +
            "apiBase:'" + apiBase + "',isDesktop:true," +
            "minimize:function(){return Promise.resolve();}," +
            "toggleMaximize:function(){return Promise.resolve();}," +
            "toggleFullscreen:function(){if(document.documentElement.requestFullscreen&&!document.fullscreenElement)document.documentElement.requestFullscreen();else if(document.exitFullscreen)document.exitFullscreen();return Promise.resolve();}," +
            "exitFullscreenWindowed:function(){if(document.fullscreenElement)document.exitFullscreen();return Promise.resolve();}," +
            "getState:function(){return Promise.resolve({isMaximized:false,isMinimized:false,isFullscreen:!!document.fullscreenElement});}," +
            "close:function(){return Promise.resolve();}," +
            "clearNeteaseMusicLogin:function(){return Promise.resolve();}," +
            "clearQQMusicLogin:function(){return Promise.resolve();}," +
            "openUpdateInstaller:function(){return Promise.resolve();}," +
            "restartApp:function(){return Promise.resolve();}," +
            "configureGlobalHotkeys:function(){return Promise.resolve();}," +
            "exportJsonFile:function(){return Promise.resolve();}," +
            "importJsonFile:function(){return Promise.resolve();}," +
            "setDesktopLyricsEnabled:function(){return Promise.resolve();}," +
            "updateDesktopLyrics:function(){return Promise.resolve();}," +
            "setWallpaperMode:function(){return Promise.resolve();}," +
            "updateWallpaperMode:function(){return Promise.resolve();}," +
            "onGlobalHotkey:function(){return function(){};}," +
            "onDesktopLyricsLockState:function(){return function(){};}," +
            "onDesktopLyricsEnabledState:function(){return function(){};}," +
            "onStateChange:function(){return function(){};}" +
            "};}" +
            "if(!window.__apiBaseHooked){window.__apiBaseHooked=true;" +
            "var _origFetch=window.fetch;" +
            "window.fetch=function(url,opts){" +
            "  if(typeof url==='string'&&url.indexOf('/api/')===0){" +
            "    if(!window.__apiBaseConfigured){" +
            "      if(window.AndroidBridge)window.AndroidBridge.showToast('后端未配置，请修改 MainActivity 中的 API_BASE_URL');" +
            "      return Promise.resolve({json:function(){return Promise.resolve({error:'backend not available',loggedIn:false,playlists:[],tracks:[],songs:[]});}});" +
            "    }" +
            "    url=window.__apiBase+url;" +
            "  }" +
            "  return _origFetch.call(window,url,opts);" +
            "};}" +
            "if(document.documentElement)document.documentElement.classList.add('simple-mode-preload');" +
            "function _addAndroidShell(){if(document.body)document.body.classList.add('android-shell');else document.addEventListener('DOMContentLoaded',function(){if(document.body)document.body.classList.add('android-shell');});}" +
            "_addAndroidShell();" +
            "})();";
    }

    // onPageFinished 兜底注入（幂等）：当 DocumentStart 脚本因 WebView 版本未生效时补救。
    private void injectDesktopStubs() {
        webView.evaluateJavascript(buildBootScript(), null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 歌词舞台模式下，返回键先切回播放器，避免直接退出应用
            if (MODE_FOLIA.equals(currentMode)) {
                loadMode(MODE_PLAYER);
                return true;
            }
            if (webView != null && webView.canGoBack()) {
                webView.evaluateJavascript("window.history.back();", null);
                return true;
            }
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
