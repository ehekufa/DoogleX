package com.dooglex.app.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dooglex.app.R
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
    val context = LocalContext.current
    val initialHost = Uri.parse(initialUrl).host
    val firstTab = remember {
        BrowserTab(FIRST_TAB_ID, initialHost ?: "DoogleX", initialUrl)
    }
    var tabs by remember { mutableStateOf(listOf(firstTab)) }
    var activeTabId by remember { mutableLongStateOf(FIRST_TAB_ID) }
    var nextTabId by remember { mutableLongStateOf(FIRST_TAB_ID + 1) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf(initialHost ?: "DoogleX") }
    var addressInput by remember { mutableStateOf(initialUrl) }
    var addressEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var blockedTrackers by remember { mutableIntStateOf(0) }
    val blockedTrackerCount = remember { AtomicInteger(0) }
    val currentPageHost = remember { AtomicReference(initialHost) }
    // Background request callbacks read only immutable snapshots from these references.
    val currentSettings = remember { AtomicReference(pageSettings) }
    val appliedSettings = remember { AtomicReference<WebPageSettings?>(null) }
    val defaultUserAgent = remember { AtomicReference("") }
    val shownTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    fun currentTab(): BrowserTab? = tabs.firstOrNull { it.id == activeTabId }

    fun updateActiveUrl(url: String) {
        tabs = tabs.map { tab ->
            if (tab.id == activeTabId) tab.copy(url = url) else tab
        }
    }

    fun updateActiveTitle(title: String) {
        tabs = tabs.map { tab ->
            if (tab.id == activeTabId) tab.copy(title = title) else tab
        }
    }

    fun saveActivePage() {
        val tab = currentTab() ?: return
        val browser = webView ?: return
        if (tab.url == null) return
        val history = Bundle()
        browser.saveState(history)
        tabs = tabs.map { item ->
            if (item.id == tab.id) {
                item.copy(title = pageTitle, url = currentUrl, history = history)
            } else {
                item
            }
        }
    }

    fun displayTab(tab: BrowserTab) {
        currentUrl = tab.url.orEmpty()
        pageTitle = tab.title
        addressInput = currentUrl
        addressEditing = false
        currentPageHost.set(tab.url?.let { Uri.parse(it).host })
        blockedTrackerCount.set(0)
        blockedTrackers = 0
        hasError = false

        val browser = webView ?: return
        browser.stopLoading()
        val targetUrl = tab.url
        if (targetUrl == null) {
            browser.clearHistory()
            isLoading = false
            return
        }

        isLoading = true
        val restored = tab.history?.let { browser.restoreState(it) }
        if (restored == null) {
            browser.clearHistory()
            browser.loadUrl(targetUrl)
        }
    }

    fun selectTab(tab: BrowserTab) {
        if (tab.id == activeTabId) return
        saveActivePage()
        activeTabId = tab.id
        displayTab(tab)
    }

    fun openNewTab() {
        if (tabs.size >= MAX_TABS) return
        saveActivePage()
        val newTab = BrowserTab(nextTabId, "Новая вкладка", null)
        nextTabId++
        tabs = tabs + newTab
        activeTabId = newTab.id
        displayTab(newTab)
    }

    fun closeTab(tab: BrowserTab) {
        if (tabs.size <= 1) {
            onClose()
            return
        }
        val index = tabs.indexOfFirst { it.id == tab.id }
        if (index < 0) return
        val remaining = tabs.filterNot { it.id == tab.id }
        if (tab.id != activeTabId) {
            tabs = remaining
            return
        }

        val target = remaining[index.coerceAtMost(remaining.lastIndex)]
        tabs = remaining
        activeTabId = target.id
        displayTab(target)
    }

    fun navigateBack() {
        val tab = currentTab()
        val browser = webView
        if (tab?.url != null && browser?.canGoBack() == true) {
            browser.goBack()
        } else if (tab != null && tabs.size > 1) {
            closeTab(tab)
        } else {
            onClose()
        }
    }

    fun submitAddress() {
        val targetUrl = BrowserAddress.resolve(addressInput)
        if (targetUrl == null) {
            Toast.makeText(
                context,
                "Введите корректный HTTP/HTTPS URL или поисковый запрос",
                Toast.LENGTH_SHORT,
            ).show()
            addressInput = currentUrl
            addressEditing = false
            return
        }
        val tab = currentTab() ?: return
        val host = Uri.parse(targetUrl).host
        tabs = tabs.map { item ->
            if (item.id == tab.id) {
                item.copy(title = host ?: "DoogleX", url = targetUrl, history = null)
            } else {
                item
            }
        }
        currentUrl = targetUrl
        pageTitle = host ?: "DoogleX"
        addressInput = targetUrl
        addressEditing = false
        currentPageHost.set(host)
        blockedTrackerCount.set(0)
        blockedTrackers = 0
        hasError = false
        isLoading = true
        webView?.stopLoading()
        webView?.loadUrl(targetUrl)
    }

    fun resetPrivateSession() {
        val browser = webView ?: return
        val sessionUrl = currentTab()?.url ?: initialUrl
        isLoading = true
        hasError = false
        CookieManager.getInstance().removeAllCookies {
            clearWebViewData(browser)
            val host = Uri.parse(sessionUrl).host
            val newTab = BrowserTab(nextTabId, host ?: "DoogleX", sessionUrl)
            nextTabId++
            tabs = listOf(newTab)
            activeTabId = newTab.id
            currentUrl = sessionUrl
            currentPageHost.set(host)
            pageTitle = host ?: "DoogleX"
            addressInput = sessionUrl
            addressEditing = false
            blockedTrackerCount.set(0)
            blockedTrackers = 0
            browser.loadUrl(sessionUrl)
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
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::navigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                    BrowserAddressBar(
                        value = addressInput,
                        onValueChange = {
                            addressInput = it
                            addressEditing = true
                        },
                        onSubmit = ::submitAddress,
                        onReset = {
                            addressEditing = false
                            addressInput = currentUrl
                        },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            hasError = false
                            webView?.reload()
                        },
                        enabled = shownTab.url != null,
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Обновить страницу")
                    }
                    IconButton(
                        onClick = { onOpenExternal(currentUrl) },
                        enabled = currentUrl.isNotBlank(),
                    ) {
                        Icon(
                            Icons.Outlined.OpenInNew,
                            contentDescription = "Открыть во внешнем браузере",
                        )
                    }
                }
                BrowserTabStrip(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    canAddTab = tabs.size < MAX_TABS,
                    onSelectTab = ::selectTab,
                    onCloseTab = ::closeTab,
                    onNewTab = ::openNewTab,
                )
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

        if (isDuckDuckGoUrl(currentUrl)) {
            DuckDuckGoBrandBar()
        }

        if (shownTab.url != null && isLoading) {
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
                factory = { viewContext ->
                    currentSettings.set(pageSettings)
                    WebView(viewContext).apply {
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
                                if (currentSettings.get().trackerBlockingActive &&
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
                                if (currentTab()?.url == null) {
                                    isLoading = false
                                    return
                                }
                                isLoading = true
                                hasError = false
                                if (!url.isNullOrBlank()) {
                                    currentUrl = url
                                    if (!addressEditing) addressInput = url
                                    currentPageHost.set(Uri.parse(url).host)
                                    updateActiveUrl(url)
                                }
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                if (currentTab()?.url == null) {
                                    isLoading = false
                                    return
                                }
                                isLoading = false
                                blockedTrackers = blockedTrackerCount.get()
                                if (!url.isNullOrBlank()) {
                                    currentUrl = url
                                    if (!addressEditing) addressInput = url
                                    currentPageHost.set(Uri.parse(url).host)
                                    updateActiveUrl(url)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (request.isForMainFrame && currentTab()?.url != null) {
                                    isLoading = false
                                    hasError = true
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView, title: String?) {
                                if (currentTab()?.url != null && !title.isNullOrBlank()) {
                                    pageTitle = title
                                    updateActiveTitle(title)
                                }
                            }

                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                if (currentTab()?.url == null) {
                                    isLoading = false
                                    return
                                }
                                blockedTrackers = blockedTrackerCount.get()
                                if (newProgress == 100) isLoading = false
                            }

                            override fun onPermissionRequest(request: PermissionRequest) {
                                // No camera, microphone, geolocation or other device access by default.
                                request.deny()
                            }
                        }
                        webView = this
                        currentTab()?.url?.let { loadUrl(it) }
                    }
                },
                update = { browser ->
                    currentSettings.set(pageSettings)
                    val previous = appliedSettings.getAndSet(pageSettings)
                    if (previous != pageSettings) {
                        applyWebPageSettings(browser, pageSettings, defaultUserAgent.get())
                        if (previous != null &&
                            previous.requiresReloadFor(pageSettings) &&
                            currentTab()?.url != null
                        ) {
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

            if (shownTab.url == null) {
                NewTabPage()
            } else if (hasError) {
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
                            text = "Проверьте URL и подключение к интернету. Можно повторить загрузку или открыть страницу во внешнем браузере.",
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
                        TextButton(onClick = { onOpenExternal(currentUrl) }) {
                            Text("Открыть во внешнем браузере")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuckDuckGoBrandBar() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.dooglex_official),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp)),
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DoogleX × DuckDuckGo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Наше оформление · поиск от DuckDuckGo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun privacyStatus(settings: WebPageSettings, blockedTrackers: Int): String {
    if (settings.trackerBlockingActive && blockedTrackers > 0) {
        return "Трекеров заблокировано: $blockedTrackers"
    }
    if (settings.compatibilityMode) {
        return "HTTPS · совместимость: ресурсы сайтов не фильтруются"
    }
    val cookies = if (settings.thirdPartyCookiesAllowed) {
        "сторонние cookies разрешены"
    } else {
        "сторонние cookies запрещены"
    }
    val trackers = if (settings.trackerBlockingActive) {
        "трекеры блокируются"
    } else {
        "трекеры разрешены"
    }
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
        pageSettings.thirdPartyCookiesAllowed,
    )
}

private fun WebPageSettings.requiresReloadFor(other: WebPageSettings): Boolean =
    desktopMode != other.desktopMode ||
        javaScriptEnabled != other.javaScriptEnabled ||
        imagesEnabled != other.imagesEnabled ||
        forceDark != other.forceDark ||
        compatibilityMode != other.compatibilityMode ||
        blockTrackers != other.blockTrackers ||
        thirdPartyCookiesEnabled != other.thirdPartyCookiesEnabled

private const val FIRST_TAB_ID = 1L
private const val MAX_TABS = 8
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
