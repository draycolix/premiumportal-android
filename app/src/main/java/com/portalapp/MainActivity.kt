package com.portalapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.webkit.WebView as AndroidWebView
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var webView: AndroidWebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNav: BottomNavigationView
    private val api = PremiumApi()
    private var accessToken: String? = null
    private var loadingFinished = false

    companion object {
        private const val PP_BASE_URL = "https://premiumportal.id"
        private const val NETFLIX_URL = "https://www.netflix.com"
        private const val TAG = "PortalApp"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        bottomNav = findViewById(R.id.bottomNav)

        setupWebView()
        setupSwipeRefresh()
        setupBottomNav()

        // Check if we already have a token saved
        accessToken = getStoredToken()
        if (accessToken != null) {
            webView.loadUrl(PP_BASE_URL + "/dashboard")
        } else {
            webView.loadUrl(PP_BASE_URL)
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    webView.loadUrl(PP_BASE_URL + "/dashboard")
                    true
                }
                R.id.nav_netflix -> {
                    launchNetflix()
                    true
                }
                R.id.nav_youtube -> {
                    webView.loadUrl(PP_BASE_URL + "/dashboard")
                    // Could add YouTube redirect here
                    true
                }
                R.id.nav_refresh -> {
                    webView.reload()
                    true
                }
                else -> false
            }
        }
    }

    private fun launchNetflix() {
        val token = accessToken ?: getCookieFromWebView("accessToken")
        if (token == null) {
            Toast.makeText(this, "Login dulu di Premium Portal", Toast.LENGTH_SHORT).show()
            webView.loadUrl(PP_BASE_URL + "/auth/login")
            return
        }

        accessToken = token
        saveToken(token)

        Toast.makeText(this, "Memuat Netflix...", Toast.LENGTH_SHORT).show()

        // Fetch Netflix cookies via API
        api.getNetflixCookies(token) { result ->
            runOnUiThread {
                result.onSuccess { cookies ->
                    if (cookies.isEmpty()) {
                        Toast.makeText(this, "Tidak ada cookie Netflix, coba lagi", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    // Inject cookies into WebView for Netflix domain
                    injectCookies(cookies)

                    // Navigate to Netflix
                    webView.loadUrl(NETFLIX_URL)
                }.onFailure { error ->
                    Toast.makeText(this, "Gagal memuat Netflix: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun injectCookies(cookies: List<CookieItem>) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        for (cookie in cookies) {
            val domain = cookie.domain
            val url = if (domain.startsWith(".")) "https://www${domain}" else "https://$domain"

            val cookieStr = buildCookieString(cookie)

            // Remove old cookie first, then set new one
            cookieManager.removeAllCookies(null)
            cookieManager.setCookie(url, cookieStr)
            android.util.Log.d(TAG, "Set cookie: ${cookie.name} for $url")
        }

        // Sync immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush()
        }
    }

    private fun buildCookieString(cookie: CookieItem): String {
        val sb = StringBuilder()
        sb.append(cookie.name).append("=").append(cookie.value)
        sb.append("; path=").append(cookie.path)
        if (cookie.secure) sb.append("; secure")
        if (cookie.httpOnly) sb.append("; httponly")
        sb.append("; domain=").append(cookie.domain)
        return sb.toString()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings

        // --- Core settings ---
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(true)

        // --- Performance ---
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setAppCacheEnabled(true)
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS

        // --- Media / DRM ---
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // --- User-Agent: spoof as real Chrome Android ---
        val defaultUa = settings.userAgentString ?: ""
        val chromeUa = defaultUa
            .replace("wv", "Chrome")
            .replace("Linux; Android 10", "Linux; Android 14")
        settings.userAgentString = chromeUa

        // --- Cookie management ---
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // --- WebViewClient ---
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: Bitmap?) {
                loadingFinished = false
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: AndroidWebView?, url: String?) {
                loadingFinished = true
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                // Extract accessToken cookie when on premiumportal domain
                if (url?.startsWith(PP_BASE_URL) == true) {
                    getAccessTokenFromCookies()
                }

                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: AndroidWebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${error?.description}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: AndroidWebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                // Detect Netflix navigation from premiumportal
                if (url.contains("netflix.com") && !url.contains("premiumportal")) {
                    // Handle through our cookie flow
                    launchNetflix()
                    return true
                }
                return false
            }
        }

        // --- WebChromeClient ---
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: AndroidWebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun getAccessTokenFromCookies() {
        try {
            // Get cookie via JavaScript (works for httpOnly cookies too if the page sets them)
            val js = "document.cookie.split('; ').find(c => c.startsWith('accessToken='))"
            webView.evaluateJavascript(js) { value ->
                if (value != null && value.isNotEmpty() && value != "null" && value.startsWith("\"")) {
                    val clean = value.trim('"')
                    if (clean.startsWith("accessToken=")) {
                        accessToken = clean.substring("accessToken=".length)
                        saveToken(accessToken!!)
                        android.util.Log.d(TAG, "accessToken captured from page")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Could not get accessToken from JS", e)
        }

        // Also try Android's CookieManager
        val cookies = CookieManager.getInstance().getCookie(PP_BASE_URL)
        if (cookies != null && accessToken == null) {
            for (cookie in cookies.split("; ")) {
                val parts = cookie.split("=", limit = 2)
                if (parts.size == 2 && parts[0] == "accessToken") {
                    accessToken = parts[1]
                    saveToken(accessToken!!)
                    android.util.Log.d(TAG, "accessToken captured from CookieManager")
                    break
                }
            }
        }
    }

    private fun getCookieFromWebView(name: String): String? {
        val cookies = CookieManager.getInstance().getCookie(PP_BASE_URL)
        if (cookies != null) {
            for (cookie in cookies.split("; ")) {
                val parts = cookie.split("=", limit = 2)
                if (parts.size == 2 && parts[0] == name) {
                    return parts[1]
                }
            }
        }
        return null
    }

    // Handle back button
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun saveToken(token: String) {
        getSharedPreferences("premium_portal", Context.MODE_PRIVATE)
            .edit()
            .putString("access_token", token)
            .apply()
    }

    private fun getStoredToken(): String? {
        return getSharedPreferences("premium_portal", Context.MODE_PRIVATE)
            .getString("access_token", null)
    }
}
