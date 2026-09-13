package com.mathcoach.app.core.katex

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/**
 * KaTeX WebView：渲染含 $...$ / $$...$$ 的混合 Markdown 文本。
 *
 * 依赖 app/src/main/assets/katex/ 下的：
 *   - render.html
 *   - katex.min.css / katex.min.js / fonts/
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KatexText(
    text: String,
    modifier: Modifier = Modifier,
    onHeightChanged: ((Int) -> Unit)? = null
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRendered(height: Int) {
                        onHeightChanged?.invoke(height)
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        renderText(view, text)
                    }
                }

                loadUrl("file:///android_asset/katex/render.html")
                webView = this
            }
        },
        update = { view ->
            renderText(view, text)
        },
        modifier = modifier
    )
}

private fun renderText(view: WebView, text: String) {
    val jsonStr = JSONObject.quote(text)
    view.evaluateJavascript("window.renderContent($jsonStr)", null)
}
