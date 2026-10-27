package com.dooglex.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dooglex.app.model.AccentPalette
import com.dooglex.app.model.DohProvider
import com.dooglex.app.model.SearchResult
import com.dooglex.app.model.ThemeMode
import com.dooglex.app.model.WebPageSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.doogleXDataStore by preferencesDataStore(name = "dooglex_preferences")

data class StoredPreferences(
    val history: List<String> = emptyList(),
    val savedResults: List<SearchResult> = emptyList(),
    val historyEnabled: Boolean = false,
    val safeSearchEnabled: Boolean = true,
    val dohEnabled: Boolean = true,
    val dohProvider: DohProvider = DohProvider.CLOUDFLARE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.LIME,
    val textScale: Float = 1f,
    val compactResults: Boolean = false,
    val showSuggestions: Boolean = true,
    val webSettings: WebPageSettings = WebPageSettings(),
)

class PreferencesStore(private val context: Context) {
    private val historyKey = stringPreferencesKey("history")
    private val savedResultsKey = stringPreferencesKey("saved_results")
    private val historyEnabledKey = booleanPreferencesKey("history_enabled")
    private val safeSearchEnabledKey = booleanPreferencesKey("safe_search_enabled")
    private val dohEnabledKey = booleanPreferencesKey("doh_enabled")
    private val dohProviderKey = stringPreferencesKey("doh_provider")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val accentPaletteKey = stringPreferencesKey("accent_palette")
    private val textScaleKey = floatPreferencesKey("text_scale")
    private val compactResultsKey = booleanPreferencesKey("compact_results")
    private val showSuggestionsKey = booleanPreferencesKey("show_suggestions")
    private val webTextZoomKey = intPreferencesKey("web_text_zoom")
    private val webImagesEnabledKey = booleanPreferencesKey("web_images_enabled")
    private val webJavaScriptEnabledKey = booleanPreferencesKey("web_javascript_enabled")
    private val webDesktopModeKey = booleanPreferencesKey("web_desktop_mode")
    private val webForceDarkKey = booleanPreferencesKey("web_force_dark")
    private val webShowPrivacyBarKey = booleanPreferencesKey("web_show_privacy_bar")
    private val webBlockTrackersKey = booleanPreferencesKey("web_block_trackers")
    private val webThirdPartyCookiesKey = booleanPreferencesKey("web_third_party_cookies")
    private val webKeepSessionKey = booleanPreferencesKey("web_keep_session")

    val preferences: Flow<StoredPreferences> = context.doogleXDataStore.data.map { values ->
        StoredPreferences(
            history = decodeHistory(values[historyKey]),
            savedResults = decodeResults(values[savedResultsKey]),
            historyEnabled = values[historyEnabledKey] ?: false,
            safeSearchEnabled = values[safeSearchEnabledKey] ?: true,
            dohEnabled = values[dohEnabledKey] ?: true,
            dohProvider = values[dohProviderKey]
                ?.let { name -> runCatching { DohProvider.valueOf(name) }.getOrNull() }
                ?: DohProvider.CLOUDFLARE,
            themeMode = values[themeModeKey]
                ?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accentPalette = values[accentPaletteKey]
                ?.let { name -> runCatching { AccentPalette.valueOf(name) }.getOrNull() }
                ?: AccentPalette.LIME,
            textScale = (values[textScaleKey] ?: 1f).coerceIn(0.85f, 1.4f),
            compactResults = values[compactResultsKey] ?: false,
            showSuggestions = values[showSuggestionsKey] ?: true,
            webSettings = WebPageSettings(
                textZoom = (values[webTextZoomKey] ?: 100).coerceIn(80, 200),
                imagesEnabled = values[webImagesEnabledKey] ?: true,
                javaScriptEnabled = values[webJavaScriptEnabledKey] ?: true,
                desktopMode = values[webDesktopModeKey] ?: false,
                forceDark = values[webForceDarkKey] ?: false,
                showPrivacyBar = values[webShowPrivacyBarKey] ?: true,
                blockTrackers = values[webBlockTrackersKey] ?: true,
                thirdPartyCookiesEnabled = values[webThirdPartyCookiesKey] ?: false,
                keepSession = values[webKeepSessionKey] ?: false,
            ),
        )
    }

    suspend fun addHistory(query: String) {
        context.doogleXDataStore.edit { values ->
            val updated = buildList {
                add(query)
                addAll(decodeHistory(values[historyKey]).filterNot { it.equals(query, true) })
            }.take(MAX_HISTORY_ITEMS)
            values[historyKey] = JSONArray(updated).toString()
        }
    }

    suspend fun clearHistory() {
        context.doogleXDataStore.edit { values ->
            values[historyKey] = "[]"
        }
    }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        context.doogleXDataStore.edit { values ->
            values[historyEnabledKey] = enabled
        }
    }

    suspend fun setSafeSearchEnabled(enabled: Boolean) {
        context.doogleXDataStore.edit { values ->
            values[safeSearchEnabledKey] = enabled
        }
    }

    suspend fun setDohEnabled(enabled: Boolean) {
        context.doogleXDataStore.edit { values ->
            values[dohEnabledKey] = enabled
        }
    }

    suspend fun setDohProvider(provider: DohProvider) {
        context.doogleXDataStore.edit { values ->
            values[dohProviderKey] = provider.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.doogleXDataStore.edit { values ->
            values[themeModeKey] = mode.name
        }
    }

    suspend fun setAccentPalette(palette: AccentPalette) {
        context.doogleXDataStore.edit { values ->
            values[accentPaletteKey] = palette.name
        }
    }

    suspend fun setTextScale(scale: Float) {
        context.doogleXDataStore.edit { values ->
            values[textScaleKey] = scale.coerceIn(0.85f, 1.4f)
        }
    }

    suspend fun setCompactResults(enabled: Boolean) {
        context.doogleXDataStore.edit { values ->
            values[compactResultsKey] = enabled
        }
    }

    suspend fun setShowSuggestions(enabled: Boolean) {
        context.doogleXDataStore.edit { values ->
            values[showSuggestionsKey] = enabled
        }
    }

    suspend fun setWebSettings(settings: WebPageSettings) {
        context.doogleXDataStore.edit { values ->
            values[webTextZoomKey] = settings.textZoom.coerceIn(80, 200)
            values[webImagesEnabledKey] = settings.imagesEnabled
            values[webJavaScriptEnabledKey] = settings.javaScriptEnabled
            values[webDesktopModeKey] = settings.desktopMode
            values[webForceDarkKey] = settings.forceDark
            values[webShowPrivacyBarKey] = settings.showPrivacyBar
            values[webBlockTrackersKey] = settings.blockTrackers
            values[webThirdPartyCookiesKey] = settings.thirdPartyCookiesEnabled
            values[webKeepSessionKey] = settings.keepSession
        }
    }

    suspend fun setSavedResults(results: List<SearchResult>) {
        val json = JSONArray().apply {
            results.forEach { result ->
                put(
                    JSONObject()
                        .put("title", result.title)
                        .put("url", result.url)
                        .put("domain", result.domain)
                        .put("snippet", result.snippet),
                )
            }
        }
        context.doogleXDataStore.edit { values ->
            values[savedResultsKey] = json.toString()
        }
    }

    private fun decodeHistory(raw: String?): List<String> = runCatching {
        val array = JSONArray(raw ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun decodeResults(raw: String?): List<SearchResult> = runCatching {
        val array = JSONArray(raw ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString("title")
                val url = item.optString("url")
                if (title.isBlank() || url.isBlank()) continue
                add(
                    SearchResult(
                        title = title,
                        url = url,
                        domain = item.optString("domain"),
                        snippet = item.optString("snippet"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX_HISTORY_ITEMS = 12
    }
}
