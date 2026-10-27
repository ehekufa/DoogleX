package com.dooglex.app.ui

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dooglex.app.privacy.TrackerBlocklist
import com.dooglex.app.privacy.upgradeToHttps
import java.io.ByteArrayInputStream

@Composable
fun WebPageScreen(
    initialUrl: String,
    onClose: () -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf(Uri.parse(initialUrl).host ?: "DoogleX") }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var blockedTrackers by remember { mutableIntStateOf(0) }

    fun navigateBack() {
        val browser = webView
        if (browser?.canGoBack() == true) browser.goBack() else onClose()
    }

    fun resetPrivateSession() {
        val browser = webView ?: return
        isLoading = true
        hasError = false
        CookieManager.getInstance().removeAllCookies {
            WebStorage.getInstance().deleteAllData()
            WebViewDatabase.getInstance(browser.context).clearFormData()
            browser.clearCache(true)
            browser.clearHistory()
            browser.clearFormData()
            browser.clearSslPreferences()
            currentUrl = initialUrl
            pageTitle = Uri.parse(initialUrl).host ?: "DoogleX"
            blockedTrackers = 0
            browser.loadUrl(initialUrl)
        }
    }

    BackHandler(onBack = ::navigateBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::navigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = currentUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = {
                            hasError = false
                            webView?.reload()
                        },
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Обновить страницу")
                    }
                    IconButton(onClick = { onOpenExternal(currentUrl) }) {
                        Icon(
                            Icons.Outlined.OpenInNew,
                            contentDescription = "Открыть во внешнем браузере",
                        )
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Приватная сессия",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = ::resetPrivateSession,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Новая сессия", maxLines = 1)
                    }
                }
                Text(
                    text = if (blockedTrackers > 0) "Трекеров заблокировано: $blockedTrackers"
                    else "HTTPS · сторонние cookies запрещены",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 26.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        } else {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadsImagesAutomatically = true
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            mediaPlaybackRequiresUserGesture = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            setGeolocationEnabled(false)
                            saveFormData = false
                            safeBrowsingEnabled = true
                        }
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                return when (request.url.scheme?.lowercase()) {
                                    "https" -> false
                                    "http" -> {
                                        view.loadUrl(upgradeToHttps(request.url.toString()))
                                        true
                                    }
                                    // Block local files, content providers and external schemes.
                                    else -> true
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest,
                            ): WebResourceResponse? {
                                val pageHost = view.url?.let { Uri.parse(it).host }
                                if (!request.isForMainFrame &&
                                    TrackerBlocklist.shouldBlock(request.url.host, pageHost)
                                ) {
                                    blockedTrackers++
                                    return WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        204,
                                        "No Content",
                                        emptyMap(),
                                        ByteArrayInputStream(ByteArray(0)),
                                    )
                                }
                                return null
                            }

                            override fun onPageStarted(
                                view: WebView,
                                url: String?,
                                favicon: Bitmap?,
                            ) {
                                isLoading = true
                                hasError = false
                                if (!url.isNullOrBlank()) currentUrl = url
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                isLoading = false
                                if (!url.isNullOrBlank()) currentUrl = url
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (request.isForMainFrame) {
                                    isLoading = false
                                    hasError = true
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView, title: String?) {
                                if (!title.isNullOrBlank()) pageTitle = title
                            }

                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                if (newProgress == 100) isLoading = false
                            }

                            override fun onPermissionRequest(request: PermissionRequest) {
                                // No camera, microphone, geolocation or other device access by default.
                                request.deny()
                            }
                        }
                        webView = this
                        loadUrl(initialUrl)
                    }
                },
                onRelease = { browser ->
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData()
                    WebViewDatabase.getInstance(browser.context).clearFormData()
                    browser.stopLoading()
                    browser.clearCache(true)
                    browser.clearHistory()
                    browser.clearFormData()
                    browser.clearSslPreferences()
                    browser.removeAllViews()
                    browser.destroy()
                },
            )

            if (hasError) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(
                            text = "Страница не открылась",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 20.dp),
                        )
                        Text(
                            text = "Проверьте подключение к интернету и попробуйте ещё раз.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                        )
                        Button(
                            onClick = {
                                hasError = false
                                isLoading = true
                                webView?.reload()
                            },
                        ) {
                            Text("Повторить")
                        }
                    }
                }
            }
        }
    }
}
