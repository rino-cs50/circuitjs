package com.lushprojects.circuitjs1.mobile;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.AtomicFile;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import org.json.JSONArray;

public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final int PICK_FILE = 1;
    private WebView browser;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        browser = new WebView(this);
        setContentView(browser);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setAllowFileAccess(false);
        browser.addJavascriptInterface(new ProjectStorage(), "CircuitJSProjects");
        browser.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                if (!isLocal(uri) || path == null || !path.startsWith("/circuit/")) {
                    return null;
                }
                String asset = path.substring(1);
                try {
                    String mime = URLConnection.guessContentTypeFromName(asset);
                    if (asset.endsWith(".js")) {
                        mime = "application/javascript";
                    }
                    if (mime == null) {
                        mime = "application/octet-stream";
                    }
                    WebResourceResponse response = new WebResourceResponse(mime, "UTF-8",
                        getAssets().open(asset, AssetManager.ACCESS_STREAMING));
                    if ("text/html".equals(mime)) {
                        response.setResponseHeaders(Collections.singletonMap("Content-Security-Policy",
                            "script-src 'self' 'unsafe-inline'; frame-src 'self'; object-src 'none'"));
                    }
                    return response;
                } catch (IOException missing) {
                    return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found",
                        Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (isLocal(uri) && uri.getPath() != null && uri.getPath().startsWith("/circuit/")) {
                    return false;
                }
                if (request.isForMainFrame() && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException ignored) {
                    }
                }
                return true;
            }
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                try {
                    startActivityForResult(params.createIntent(), PICK_FILE);
                    fileCallback = callback;
                    return true;
                } catch (ActivityNotFoundException unavailable) {
                    fileCallback = null;
                    return false;
                }
            }
        });
        browser.loadUrl("https://" + HOST + "/circuit/circuitjs.html");
    }

    private static boolean isLocal(Uri uri) {
        return "https".equals(uri.getScheme()) && HOST.equals(uri.getHost())
            && (uri.getPort() == -1 || uri.getPort() == 443);
    }

    public final class ProjectStorage {
        private final File projects = new File(getFilesDir(), "projects");
        private final File session = new File(getFilesDir(), "working-circuit.json");

        @JavascriptInterface
        public synchronized String list() {
            String[] names = projects.list((dir, name) -> projectFile(name) != null
                && new File(dir, name).isFile());
            if (names == null) {
                if (projects.exists()) {
                    return null;
                }
                names = new String[0];
            }
            Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
            return new JSONArray(Arrays.asList(names)).toString();
        }

        @JavascriptInterface
        public synchronized String open(String name) {
            File file = projectFile(name);
            return file == null ? null : read(file);
        }

        @JavascriptInterface
        public synchronized boolean save(String name, String circuit, boolean replace) {
            File file = projectFile(name);
            return file != null && (replace || !file.exists())
                && (projects.isDirectory() || projects.mkdirs()) && write(file, circuit);
        }

        @JavascriptInterface
        public synchronized String restoreSession() {
            return read(session);
        }

        @JavascriptInterface
        public synchronized boolean saveSession(String snapshot) {
            return write(session, snapshot);
        }

        private File projectFile(String name) {
            if (name == null || name.length() <= 4 || name.length() > 80
                || !name.endsWith(".txt") || name.startsWith(".")
                || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*")) {
                return null;
            }
            return new File(projects, name);
        }

        private String read(File file) {
            try (FileInputStream input = new AtomicFile(file).openRead();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            } catch (FileNotFoundException missing) {
                return null;
            } catch (IOException error) {
                Log.w("CircuitJS", "Could not read " + file.getName(), error);
                return null;
            }
        }

        private boolean write(File file, String content) {
            if (content == null) {
                return false;
            }
            AtomicFile target = new AtomicFile(file);
            FileOutputStream output = null;
            try {
                output = target.startWrite();
                output.write(content.getBytes(StandardCharsets.UTF_8));
                output.getFD().sync();
                target.finishWrite(output);
                return true;
            } catch (IOException error) {
                target.failWrite(output);
                Log.w("CircuitJS", "Could not save " + file.getName(), error);
                return false;
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCallback = null;
        }
    }

    @Override
    protected void onPause() {
        browser.evaluateJavascript("if (window.flushCircuitJSSession) window.flushCircuitJSSession()", null);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (browser.canGoBack()) {
            browser.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
