package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

class MarkdownWebBridge(
    private val onWikiLinkClick: (String) -> Unit,
    private val onLinkClick: (String) -> Unit,
    private val onHeadingsFound: (String) -> Unit,
    private val onPageLoaded: () -> Unit
) {
    @JavascriptInterface
    fun onWikiLinkClick(target: String) {
        onWikiLinkClick.invoke(target)
    }

    @JavascriptInterface
    fun onLinkClick(target: String) {
        onLinkClick.invoke(target)
    }

    @JavascriptInterface
    fun onHeadingsFound(headingsJson: String) {
        onHeadingsFound.invoke(headingsJson)
    }

    @JavascriptInterface
    fun onPageLoaded() {
        onPageLoaded.invoke()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SuperMarkdownView(
    markdownContent: String,
    customCss: String,
    fontSizeSp: Int,
    isDark: Boolean,
    onWikiLinkClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onHeadingsFound: (String) -> Unit,
    targetHeadingIdToScroll: String? = null,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    val bridge = remember {
        MarkdownWebBridge(
            onWikiLinkClick = onWikiLinkClick,
            onLinkClick = onLinkClick,
            onHeadingsFound = onHeadingsFound,
            onPageLoaded = {
                isPageLoaded = true
            }
        )
    }

    // Function to push markdown update into WebView
    fun pushUpdate() {
        val webView = webViewRef ?: return
        if (!isPageLoaded) return
        val escapedMarkdown = JSONObject.quote(markdownContent)
        val escapedCss = JSONObject.quote(customCss)
        val js = "setMarkdown($escapedMarkdown, $escapedCss, $fontSizeSp, $isDark);"
        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }

    // When content, css, or theme changes, update WebView
    LaunchedEffect(markdownContent, customCss, fontSizeSp, isDark, isPageLoaded) {
        if (isPageLoaded) {
            pushUpdate()
        }
    }

    // Scroll to specific heading if requested
    LaunchedEffect(targetHeadingIdToScroll) {
        if (targetHeadingIdToScroll != null && isPageLoaded) {
            webViewRef?.post {
                val escapedId = JSONObject.quote(targetHeadingIdToScroll)
                webViewRef?.evaluateJavascript("scrollToHeading($escapedId);", null)
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded = true
                        pushUpdate()
                    }
                }
                addJavascriptInterface(bridge, "AndroidBridge")
                loadUrl("file:///android_asset/web/super_markdown.html")
                webViewRef = this
            }
        },
        update = { webView ->
            webViewRef = webView
        }
    )
}
