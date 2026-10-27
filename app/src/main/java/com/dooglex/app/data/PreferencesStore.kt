package com.dooglex.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dooglex.app.model.SearchResult
import com.dooglex.app.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.doogleXDataStore by preferencesDataStore(name = "dooglex_preferences")

data class StoredPreferences(
    val history: List<String> = emptyList(),
    val savedResults: List<SearchResult> = emptyList(),
    val historyEnabled: Boolean = true,
    val safeSearchEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class PreferencesStore(private val context: Context) {
    private val historyKey = stringPreferencesKey("history")
    private val savedResultsKey = stringPreferencesKey("saved_results")
    private val historyEnabledKey = booleanPreferencesKey("history_enabled")
    private val safeSearchEnabledKey = booleanPreferencesKey("safe_search_enabled")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val preferences: Flow<StoredPreferences> = context.doogleXDataStore.data.map { values ->
        StoredPreferences(
            history = decodeHistory(values[historyKey]),
            savedResults = decodeResults(values[savedResultsKey]),
            historyEnabled = values[historyEnabledKey] ?: true,
            safeSearchEnabled = values[safeSearchEnabledKey] ?: true,
            themeMode = values[themeModeKey]
                ?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
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

    suspend fun setThemeMode(mode: ThemeMode) {
        context.doogleXDataStore.edit { values ->
            values[themeModeKey] = mode.name
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
