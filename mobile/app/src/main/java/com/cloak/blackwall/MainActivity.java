package com.cloak.blackwall;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Blackwall for Android: tabs, ad blocking and the desktop app's look. Everything stays on this phone.
public class MainActivity extends Activity {
    static final int BG = 0xFF07070A, PANEL = 0xFF0E0E13, SURFACE = 0xFF1A1A21, INK = 0xFFF1F2F6, MUTED = 0xFFA7ABB9, ACCENT = 0xFFE3192F;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    // same built-in list as desktop Blackwall
    static final String[] AD_HOSTS = {"doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com", "pagead2.googlesyndication.com", "googletagservices.com",
        "google-analytics.com", "adnxs.com", "adsrvr.org", "advertising.com", "amazon-adsystem.com", "taboola.com", "outbrain.com", "criteo.com", "criteo.net", "pubmatic.com",
        "rubiconproject.com", "openx.net", "casalemedia.com", "smartadserver.com", "moatads.com", "scorecardresearch.com", "quantserve.com", "adform.net", "yieldmo.com",
        "sharethrough.com", "teads.tv", "media.net", "zedo.com", "propellerads.com", "popads.net", "popcash.net", "adcash.com", "exoclick.com", "juicyads.com",
        "trafficjunky.net", "mgid.com", "revcontent.com", "adroll.com", "bidswitch.net", "33across.com", "indexww.com", "lijit.com", "sovrn.com", "contextweb.com",
        "spotxchange.com", "spotx.tv", "springserve.com", "unrulymedia.com", "yieldlab.net", "adition.com", "adsafeprotected.com", "doubleverify.com", "imasdk.googleapis.com",
        "ads.yahoo.com", "ads.twitter.com", "ads-twitter.com", "static.ads-twitter.com", "ads.linkedin.com", "adsymptotic.com", "hotjar.com", "mouseflow.com", "zergnet.com",
        "onclickads.net", "onclkds.com", "adsterra.com", "hilltopads.net", "clickadu.com", "admaven.com", "a-ads.com", "coinzilla.com", "bitmedia.io", "ad.plus", "adskeeper.com"};
    static final String[][] QUICK = {{"YouTube", "https://www.youtube.com/"}, {"Google", "https://www.google.com/"}, {"Reddit", "https://www.reddit.com/"},
        {"Twitch", "https://www.twitch.tv/"}, {"Gmail", "https://mail.google.com/"}, {"Modrinth", "https://modrinth.com/"}};
    static final String PAUSE_JS = "document.querySelectorAll('video,audio').forEach(function(v){v.pause();})";

    static class Tab { WebView wv; String pending; String title = ""; }

    final List<Tab> tabs = new ArrayList<>();
    final Set<String> adHosts = new HashSet<>(Arrays.asList(AD_HOSTS));
    Tab cur;
    FrameLayout content;
    View home;
    EditText url;
    TextView tabsBtn;
    ProgressBar prog;
    View customView;
    WebChromeClient.CustomViewCallback customCb;
    SharedPreferences prefs;
    volatile boolean adBlock;

    int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics())); }

    GradientDrawable round(int color, float r) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(r)); return g; }

    TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("blackwall", MODE_PRIVATE);
        adBlock = prefs.getBoolean("adBlock", true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, in) -> {
            Insets i = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return WindowInsets.CONSUMED;
        });

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(PANEL);
        bar.setPadding(dp(6), dp(6), dp(4), dp(6));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.blackwall);
        logo.setPadding(dp(6), dp(6), dp(6), dp(6));
        logo.setContentDescription("Home");
        logo.setOnClickListener(v -> goHome());
        bar.addView(logo, new LinearLayout.LayoutParams(dp(40), dp(40)));

        url = new EditText(this);
        url.setSingleLine(true);
        url.setHint("Search or type a web address");
        url.setHintTextColor(MUTED);
        url.setTextColor(INK);
        url.setTextSize(15);
        url.setBackground(round(SURFACE, 20));
        url.setPadding(dp(14), 0, dp(14), 0);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setImeOptions(EditorInfo.IME_ACTION_GO);
        url.setSelectAllOnFocus(true);
        url.setOnEditorActionListener((v, id, ev) -> {
            boolean enter = ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER && ev.getAction() == KeyEvent.ACTION_DOWN;
            if (id == EditorInfo.IME_ACTION_GO || enter) { go(url.getText().toString()); return true; }
            return false;
        });
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(0, dp(40), 1);
        ulp.setMargins(dp(4), 0, dp(4), 0);
        bar.addView(url, ulp);

        tabsBtn = text("0", 13, INK, true);
        tabsBtn.setGravity(Gravity.CENTER);
        GradientDrawable tb = new GradientDrawable();
        tb.setStroke(dp(2), INK);
        tb.setCornerRadius(dp(6));
        tabsBtn.setBackground(tb);
        tabsBtn.setContentDescription("Tabs");
        tabsBtn.setOnClickListener(v -> showTabs());
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(28), dp(28));
        tlp.setMargins(dp(8), 0, dp(6), 0);
        bar.addView(tabsBtn, tlp);

        TextView menu = text("\u22EE", 22, INK, true);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription("Menu");
        menu.setOnClickListener(this::showMenu);
        bar.addView(menu, new LinearLayout.LayoutParams(dp(36), dp(40)));
        root.addView(bar, new LinearLayout.LayoutParams(MATCH, WRAP));

        prog = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        prog.setIndeterminate(false);
        prog.setMax(100);
        prog.setProgressTintList(ColorStateList.valueOf(ACCENT));
        prog.setProgressBackgroundTintList(ColorStateList.valueOf(PANEL));
        prog.setVisibility(View.INVISIBLE);
        root.addView(prog, new LinearLayout.LayoutParams(MATCH, dp(3)));

        content = new FrameLayout(this);
        home = buildHome();
        content.addView(home, new FrameLayout.LayoutParams(MATCH, MATCH));
        root.addView(content, new LinearLayout.LayoutParams(MATCH, 0, 1));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33)
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);

        try {
            JSONArray a = new JSONArray(prefs.getString("tabs", "[]"));
            for (int i = 0; i < a.length(); i++) { Tab t = new Tab(); t.pending = a.getString(i); tabs.add(t); }   // each loads when you open it
        } catch (JSONException ignored) { }
        int ci = prefs.getInt("cur", -1);
        if (!openFromIntent(getIntent())) {
            if (ci >= 0 && ci < tabs.size()) select(tabs.get(ci)); else goHome();
        }
    }

    View buildHome() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Color.BLACK);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(dp(24), dp(24), dp(24), dp(48));

        ImageView big = new ImageView(this);
        big.setImageResource(R.drawable.blackwall_big);
        big.setAdjustViewBounds(true);
        big.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        col.addView(big, new LinearLayout.LayoutParams(dp(220), WRAP));

        TextView word = text("BLACKWALL", 26, ACCENT, true);
        word.setLetterSpacing(.3f);
        word.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        wlp.setMargins(0, dp(16), 0, dp(28));
        col.addView(word, wlp);
        // the name's red glow breathes in and out, like the desktop logo
        ValueAnimator glow = ValueAnimator.ofFloat(dp(2), dp(14));
        glow.setDuration(3500);
        glow.setRepeatMode(ValueAnimator.REVERSE);
        glow.setRepeatCount(ValueAnimator.INFINITE);
        glow.addUpdateListener(a -> word.setShadowLayer((float) a.getAnimatedValue(), 0, 0, 0xCCFF3C50));
        glow.start();

        for (int i = 0; i < QUICK.length; i += 3) {
            LinearLayout row = new LinearLayout(this);
            for (int j = i; j < Math.min(i + 3, QUICK.length); j++) {
                final String u = QUICK[j][1];
                TextView b = text(QUICK[j][0], 13, INK, false);
                b.setGravity(Gravity.CENTER);
                b.setBackground(round(SURFACE, 14));
                b.setPadding(dp(6), dp(14), dp(6), dp(14));
                b.setOnClickListener(v -> go(u));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1);
                lp.setMargins(dp(5), dp(5), dp(5), dp(5));
                row.addView(b, lp);
            }
            col.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
        }
        sv.addView(col, new FrameLayout.LayoutParams(MATCH, WRAP));
        return sv;
    }

    static String toUrl(String q) {
        q = q == null ? "" : q.trim();
        if (q.isEmpty()) return "";
        if (q.matches("(?i)^[a-z][a-z0-9+.-]*://.*") || q.startsWith("about:")) return q;
        if (!q.contains(" ") && (q.matches("(?i)^(localhost|\\d{1,3}(\\.\\d{1,3}){3})(:\\d+)?(/.*)?$") || q.matches("(?i)^[^\\s/]+\\.[a-z]{2,}(:\\d+)?(/\\S*)?$")))
            return "https://" + q;
        return "https://www.google.com/search?q=" + Uri.encode(q);
    }

    boolean blocked(String host) {
        for (String h = host; h.contains("."); h = h.substring(h.indexOf('.') + 1)) if (adHosts.contains(h)) return true;
        return false;
    }

    WebView makeView(Tab t) {
        WebView wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        // look like regular Chrome: Google won't let you sign in from a page that says it's an app's web view
        s.setUserAgentString(s.getUserAgentString().replace("; wv", "").replaceFirst("Version/\\d+\\.\\d+ ", ""));
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);
        wv.setBackgroundColor(Color.WHITE);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String sc = r.getUrl().getScheme();
                if (sc == null || sc.equals("http") || sc.equals("https") || sc.equals("about") || sc.equals("data") || sc.equals("blob")) return false;
                // intent:, market:, tel:, mailto: and so on go to the app that handles them
                try {
                    Intent i = sc.equals("intent") ? Intent.parseUri(r.getUrl().toString(), Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, r.getUrl());
                    i.addCategory(Intent.CATEGORY_BROWSABLE);
                    i.setComponent(null);
                    i.setSelector(null);
                    startActivity(i);
                } catch (Exception e) { toast("No app on this phone can open that link"); }
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (!adBlock || r.isForMainFrame()) return null;
                String h = r.getUrl().getHost();
                if (h == null || !blocked(h.toLowerCase(Locale.ROOT))) return null;
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
            }

            @Override
            public void doUpdateVisitedHistory(WebView v, String u, boolean reload) { if (t == cur) paintUrl(); save(); }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                if (t != cur) return;
                prog.setProgress(p);
                prog.setVisibility(p < 100 ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onReceivedTitle(WebView v, String title) { t.title = title == null ? "" : title; }

            @Override
            public boolean onCreateWindow(WebView v, boolean dialog, boolean userGesture, Message msg) {
                if (!userGesture) return false;   // pop-ups a page opens on its own are blocked
                Tab n = newTab(null, false);
                ((WebView.WebViewTransport) msg.obj).setWebView(n.wv);
                msg.sendToTarget();
                return true;
            }

            @Override
            public void onCloseWindow(WebView v) { content.post(() -> closeTab(t)); }

            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) { enterFull(view, cb); }

            @Override
            public void onHideCustomView() { exitFull(); }
        });

        wv.setDownloadListener((u, ua, cd, mime, len) -> download(u, ua, cd, mime));
        return wv;
    }

    Tab newTab(String u, boolean background) {
        Tab t = new Tab();
        t.pending = u;
        tabs.add(t);
        if (background) { updateTabCount(); save(); } else select(t);
        return t;
    }

    void select(Tab t) {
        if (cur != null && cur != t && cur.wv != null) cur.wv.evaluateJavascript(PAUSE_JS, null);
        cur = t;
        if (t.wv == null) {
            t.wv = makeView(t);
            content.addView(t.wv, new FrameLayout.LayoutParams(MATCH, MATCH));
            if (t.pending != null) t.wv.loadUrl(t.pending);
            t.pending = null;
        }
        for (Tab x : tabs) if (x.wv != null) x.wv.setVisibility(x == t ? View.VISIBLE : View.GONE);
        home.setVisibility(View.GONE);
        prog.setVisibility(t.wv.getProgress() < 100 ? View.VISIBLE : View.INVISIBLE);
        paintUrl();
        updateTabCount();
        save();
    }

    void goHome() {
        if (cur != null && cur.wv != null) cur.wv.evaluateJavascript(PAUSE_JS, null);
        cur = null;
        for (Tab x : tabs) if (x.wv != null) x.wv.setVisibility(View.GONE);
        home.setVisibility(View.VISIBLE);
        prog.setVisibility(View.INVISIBLE);
        url.setText("");
        updateTabCount();
        save();
    }

    void go(String q) {
        String u = toUrl(q);
        if (u.isEmpty()) return;
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(url.getWindowToken(), 0);
        url.clearFocus();
        if (cur == null) newTab(u, false);
        else { cur.wv.loadUrl(u); paintUrl(); }
    }

    void closeTab(Tab t) {
        int i = tabs.indexOf(t);
        if (i < 0) return;
        tabs.remove(i);
        if (t.wv != null) { content.removeView(t.wv); t.wv.destroy(); }
        if (cur == t) {
            cur = null;
            if (tabs.isEmpty()) goHome(); else select(tabs.get(Math.min(i, tabs.size() - 1)));
        } else { updateTabCount(); save(); }
    }

    void closeAll() {
        for (Tab t : tabs) if (t.wv != null) { content.removeView(t.wv); t.wv.destroy(); }
        tabs.clear();
        cur = null;
        goHome();
    }

    void paintUrl() {
        if (url.hasFocus()) return;
        String u = cur != null && cur.wv != null ? cur.wv.getUrl() : null;
        url.setText(u == null ? "" : u);
    }

    void updateTabCount() { tabsBtn.setText(tabs.size() > 99 ? "99+" : String.valueOf(tabs.size())); }

    String label(Tab t) {
        if (!TextUtils.isEmpty(t.title)) return t.title;
        String u = t.wv != null ? t.wv.getUrl() : t.pending;
        return u == null ? "New tab" : u;
    }

    void showTabs() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(8));
        ScrollView sv = new ScrollView(this);
        sv.addView(list);
        AlertDialog d = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(tabs.size() + " tabs")
            .setView(sv)
            .setPositiveButton("New tab", (x, w) -> goHome())
            .setNegativeButton("Done", null)
            .create();
        for (Tab t : new ArrayList<>(tabs)) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(round(t == cur ? 0xFF2A1015 : SURFACE, 12));
            row.setPadding(dp(14), dp(4), dp(4), dp(4));
            TextView name = text(label(t), 15, INK, t == cur);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1));
            TextView x = text("\u2715", 16, MUTED, false);
            x.setPadding(dp(14), dp(12), dp(14), dp(12));
            x.setContentDescription("Close tab");
            x.setOnClickListener(v -> {
                closeTab(t);
                list.removeView(row);
                d.setTitle(tabs.size() + " tabs");
                if (tabs.isEmpty()) d.dismiss();
            });
            row.addView(x);
            row.setOnClickListener(v -> { d.dismiss(); select(t); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.setMargins(0, 0, 0, dp(6));
            list.addView(row, lp);
        }
        if (tabs.isEmpty()) list.addView(text("No tabs open", 14, MUTED, false));
        d.show();
    }

    void showMenu(View anchor) {
        PopupMenu m = new PopupMenu(this, anchor);
        Menu menu = m.getMenu();
        boolean has = cur != null && cur.wv != null;
        menu.add(0, 1, 0, "New tab");
        menu.add(0, 2, 0, "Forward").setEnabled(has && cur.wv.canGoForward());
        menu.add(0, 3, 0, "Reload").setEnabled(has);
        menu.add(0, 4, 0, "Share page").setEnabled(has);
        menu.add(0, 5, 0, "Block ads and trackers").setCheckable(true).setChecked(adBlock);
        menu.add(0, 6, 0, "Close all tabs").setEnabled(!tabs.isEmpty());
        menu.add(0, 7, 0, "Clear cookies, sign-ins and site data");
        m.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1: goHome(); break;
                case 2: cur.wv.goForward(); break;
                case 3: cur.wv.reload(); break;
                case 4: {
                    Intent s = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, cur.wv.getUrl());
                    startActivity(Intent.createChooser(s, "Share page"));
                    break;
                }
                case 5:
                    adBlock = !adBlock;
                    prefs.edit().putBoolean("adBlock", adBlock).apply();
                    toast(adBlock ? "Blocking ads and trackers" : "Ads allowed");
                    break;
                case 6: closeAll(); break;
                case 7:
                    new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                        .setTitle("Clear site data?")
                        .setMessage("You'll be signed out of every site. Your tabs stay open.")
                        .setPositiveButton("Clear", (x, w) -> {
                            CookieManager.getInstance().removeAllCookies(null);
                            WebStorage.getInstance().deleteAllData();
                            for (Tab t : tabs) if (t.wv != null) t.wv.clearCache(true);
                            toast("Cookies and site data cleared");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                    break;
            }
            return true;
        });
        m.show();
    }

    void download(String u, String ua, String cd, String mime) {
        try {
            String name = URLUtil.guessFileName(u, cd, mime);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(u));
            r.setMimeType(mime);
            r.addRequestHeader("User-Agent", ua);
            String c = CookieManager.getInstance().getCookie(u);
            if (c != null) r.addRequestHeader("Cookie", c);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
            toast("Downloading " + name);
        } catch (Exception e) { toast("Couldn't download that file"); }
    }

    void enterFull(View v, WebChromeClient.CustomViewCallback cb) {
        if (customView != null) { cb.onCustomViewHidden(); return; }
        customView = v;
        customCb = cb;
        v.setBackgroundColor(Color.BLACK);
        ((FrameLayout) getWindow().getDecorView()).addView(v, new FrameLayout.LayoutParams(MATCH, MATCH));
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) {
            c.hide(WindowInsets.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    void exitFull() {
        if (customView == null) return;
        ((FrameLayout) getWindow().getDecorView()).removeView(customView);
        customView = null;
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) c.show(WindowInsets.Type.systemBars());
        if (customCb != null) { WebChromeClient.CustomViewCallback cb = customCb; customCb = null; cb.onCustomViewHidden(); }
    }

    void back() {
        if (customView != null) { exitFull(); return; }
        if (cur != null && cur.wv != null && cur.wv.canGoBack()) { cur.wv.goBack(); return; }
        if (cur != null) { goHome(); return; }
        moveTaskToBack(true);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() { back(); }   // Android 12 and older; newer versions use the callback set in onCreate

    boolean openFromIntent(Intent i) {
        if (i == null || !Intent.ACTION_VIEW.equals(i.getAction()) || i.getData() == null) return false;
        String sc = i.getData().getScheme();
        if (!"http".equals(sc) && !"https".equals(sc)) return false;
        newTab(i.getData().toString(), false);
        return true;
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        openFromIntent(i);
    }

    void save() {
        JSONArray a = new JSONArray();
        int ci = -1;
        for (Tab t : tabs) {
            String u = t.wv != null ? t.wv.getUrl() : t.pending;
            if (u == null || !u.startsWith("http")) continue;
            if (t == cur) ci = a.length();
            a.put(u);
        }
        prefs.edit().putString("tabs", a.toString()).putInt("cur", ci).apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        for (Tab t : tabs) if (t.wv != null) t.wv.destroy();
        super.onDestroy();
    }
}
