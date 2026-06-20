package com.portalapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.webkit.WebView as AndroidWebView

class MainActivity : AppCompatActivity() {

    private lateinit var webView: AndroidWebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private var loadingFinished = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        setupSwipeRefresh()

        webView.loadUrl("https://premiumportal.id")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings

        // --- Essential WebView settings ---
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
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)

        // --- Media / Netflix / DRM ---
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // --- User Agent: spoof as real Chrome Android to avoid Cloudflare ---
        val ua = webView.settings.userAgentString
        val mobileUa = ua?.replace(
            "Linux; Android 10",
            "Linux; Android 14"
        )?.replace(
            "wv",
            "Chrome"
        ) ?: "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
        settings.userAgentString = mobileUa

        // --- Allow file access for local storage ---
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // --- Cookie handling ---
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        // --- WebViewClient ---
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: Bitmap?) {
                loadingFinished = false
                progressBar.visibility = android.view.View.VISIBLE
                progressBar.progress = 0
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: AndroidWebView?, url: String?) {
                loadingFinished = true
                progressBar.visibility = android.view.View.GONE
                swipeRefresh.isRefreshing = false
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: AndroidWebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Don't show error for subresources
                if (request?.isForMainFrame == true) {
                    Toast.makeText(
                        this@MainActivity,
                        "Gagal memuat halaman: ${error?.description}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: AndroidWebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                // Stay within premiumportal.id
                if (url.isNotEmpty()) {
                    view?.loadUrl(url)
                }
                return true
            }
        }

        // --- WebChromeClient for progress ---
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: AndroidWebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_dark,
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark,
            android.R.color.holo_red_dark
        )
    }

    // Handle back button to go to previous page
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}
