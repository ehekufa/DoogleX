package com.dooglex.app.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.viewinterop.AndroidView
import com.dooglex.app.model.WebPageSettings
import com.dooglex.app.privacy.TrackerBlocklist
import com.dooglex.app.privacy.upgradeToHttps
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Composable
fun WebPageScreen(
    initialUrl: String,
    pageSettings: WebPageSettings,
    onClose: () -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf(Uri.parse(initialUrl).host ?: "DoogleX") }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var blockedTrackers by remember { mutableIntStateOf(0) }
    val blockedTrackerCount = remember { AtomicInteger(0) }
    val currentPageHost = remember { AtomicReference(Uri.parse(initialUrl).host) }
    // Background request callbacks read only immutable snapshots from these references.
    val currentSettings = remember { AtomicReference(pageSettings) }
    val appliedSettings = remember { AtomicReference<WebPageSettings?>(null) }
    val defaultUserAgent = remember { AtomicReference("") }

    fun navigateBack() {
        val browser = webView
        if (browser?.canGoBack() == true) browser.goBack() else onClose()
    }

    fun resetPrivateSession() {
        val browser = webView ?: return
        isLoading = true
        hasError = false
        CookieManager.getInstance().removeAllCookies {
            clearWebViewData(browser)
            currentUrl = initialUrl
            currentPageHost.set(Uri.parse(initialUrl).host)
            pageTitle = Uri.parse(initialUrl).host ?: "DoogleX"
            blockedTrackerCount.set(0)
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

        if (pageSettings.showPrivacyBar) {
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
                            text = if (pageSettings.keepSession) "Сессия сохраняется"
                            else "Приватная сессия",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = ::resetPrivateSession,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text("Новая сессия", maxLines = 1)
                        }
                    }
                    Text(
                        text = privacyStatus(pageSettings, blockedTrackers),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 26.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
                    currentSettings.set(pageSettings)
                    WebView(context).apply {
                        defaultUserAgent.set(settings.userAgentString)
                        settings.apply {
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            mediaPlaybackRequiresUserGesture = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            setGeolocationEnabled(false)
                            saveFormData = false
                            safeBrowsingEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        applyWebPageSettings(this, pageSettings, defaultUserAgent.get())
                        appliedSettings.set(pageSettings)
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
                                // This callback runs off the UI thread. Never call view.url here:
                                // WebView.getUrl() enforces its creating thread on Android 14+.
                                if (currentSettings.get().blockTrackers &&
                                    !request.isForMainFrame &&
                                    TrackerBlocklist.shouldBlock(
                                        request.url.host,
                                        currentPageHost.get(),
                                    )
                                ) {
                                    blockedTrackerCount.incrementAndGet()
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
                                if (!url.isNullOrBlank()) {
                                    currentUrl = url
                                    currentPageHost.set(Uri.parse(url).host)
                                }
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                isLoading = false
                                blockedTrackers = blockedTrackerCount.get()
                                if (!url.isNullOrBlank()) {
                                    currentUrl = url
                                    currentPageHost.set(Uri.parse(url).host)
                                }
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
                                blockedTrackers = blockedTrackerCount.get()
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
                update = { browser ->
                    currentSettings.set(pageSettings)
                    val previous = appliedSettings.getAndSet(pageSettings)
                    if (previous != pageSettings) {
                        applyWebPageSettings(browser, pageSettings, defaultUserAgent.get())
                        if (previous != null && previous.requiresReloadFor(pageSettings)) {
                            hasError = false
                            isLoading = true
                            browser.reload()
                        }
                    }
                },
                onRelease = { browser ->
                    browser.stopLoading()
                    if (!currentSettings.get().keepSession) {
                        CookieManager.getInstance().removeAllCookies(null)
                        clearWebViewData(browser)
                    }
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

private fun privacyStatus(settings: WebPageSettings, blockedTrackers: Int): String {
    if (settings.blockTrackers && blockedTrackers > 0) {
        return "Трекеров заблокировано: $blockedTrackers"
    }
    val cookies = if (settings.thirdPartyCookiesEnabled) {
        "сторонние cookies разрешены"
    } else {
        "сторонние cookies запрещены"
    }
    val trackers = if (settings.blockTrackers) "трекеры блокируются" else "трекеры разрешены"
    return "HTTPS · $cookies · $trackers"
}

private fun clearWebViewData(browser: WebView) {
    WebStorage.getInstance().deleteAllData()
    WebViewDatabase.getInstance(browser.context).clearFormData()
    browser.clearCache(true)
    browser.clearHistory()
    browser.clearFormData()
    browser.clearSslPreferences()
}

@Suppress("DEPRECATION")
private fun applyWebPageSettings(
    browser: WebView,
    pageSettings: WebPageSettings,
    defaultUserAgent: String,
) {
    browser.settings.apply {
        javaScriptEnabled = pageSettings.javaScriptEnabled
        loadsImagesAutomatically = pageSettings.imagesEnabled
        blockNetworkImage = !pageSettings.imagesEnabled
        textZoom = pageSettings.textZoom
        userAgentString = if (pageSettings.desktopMode) DESKTOP_USER_AGENT else defaultUserAgent
        useWideViewPort = pageSettings.desktopMode
        loadWithOverviewMode = pageSettings.desktopMode

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setAlgorithmicDarkeningAllowed(pageSettings.forceDark)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            forceDark = if (pageSettings.forceDark) {
                WebSettings.FORCE_DARK_ON
            } else {
                WebSettings.FORCE_DARK_OFF
            }
        }
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(
        browser,
        pageSettings.thirdPartyCookiesEnabled,
    )
}

private fun WebPageSettings.requiresReloadFor(other: WebPageSettings): Boolean =
    desktopMode != other.desktopMode ||
        javaScriptEnabled != other.javaScriptEnabled ||
        imagesEnabled != other.imagesEnabled ||
        forceDark != other.forceDark

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
