package com.dooglex.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dooglex.app.data.PreferencesStore
import com.dooglex.app.data.StoredPreferences
import com.dooglex.app.privacy.upgradeToHttps
import com.dooglex.app.ui.WebPageScreen
import com.dooglex.app.ui.theme.DoogleXTheme

/** Displays a selected website without handing it off to the default browser. */
class WebActivity : ComponentActivity() {
    private val preferencesStore by lazy { PreferencesStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUrl = intent.getStringExtra(EXTRA_URL)
        if (!initialUrl.isSupportedWebUrl()) {
            finish()
            return
        }

        WebView.setWebContentsDebuggingEnabled(false)
        enableEdgeToEdge()
        val secureInitialUrl = upgradeToHttps(initialUrl!!)
        setContent {
            val preferences by preferencesStore.preferences.collectAsState(
                initial = StoredPreferences(),
            )
            DoogleXTheme(
                themeMode = preferences.themeMode,
                interfaceStyle = preferences.interfaceStyle,
                accentPalette = preferences.accentPalette,
                textScale = preferences.textScale,
            ) {
                WebPageScreen(
                    initialUrl = secureInitialUrl,
                    pageSettings = preferences.webSettings,
                    onClose = { finish() },
                    onOpenExternal = { url -> openExternal(url) },
                )
            }
        }
    }

    private fun openExternal(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Не найдено приложение для открытия ссылки", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_URL = "com.dooglex.app.extra.URL"

        fun createIntent(context: Context, url: String): Intent =
            Intent(context, WebActivity::class.java).putExtra(EXTRA_URL, url)
    }
}

private fun String?.isSupportedWebUrl(): Boolean {
    if (this.isNullOrBlank()) return false
    return Uri.parse(this).scheme?.lowercase() in setOf("http", "https")
}
