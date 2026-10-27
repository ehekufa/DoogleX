package com.dooglex.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dooglex.app.data.DoogleXMetaSearchRepository
import com.dooglex.app.data.PreferencesStore
import com.dooglex.app.model.AccentPalette
import com.dooglex.app.model.AppTab
import com.dooglex.app.model.DohProvider
import com.dooglex.app.model.DoogleXUiState
import com.dooglex.app.model.InterfaceStyle
import com.dooglex.app.model.SearchResult
import com.dooglex.app.model.SearchUiState
import com.dooglex.app.model.ThemeMode
import com.dooglex.app.model.WebPageSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoogleXViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DoogleXMetaSearchRepository()
    private val preferencesStore = PreferencesStore(application.applicationContext)
    private val mutableUiState = MutableStateFlow(DoogleXUiState())
    private var searchJob: Job? = null

    val uiState: StateFlow<DoogleXUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesStore.preferences.collect { preferences ->
                mutableUiState.update { state ->
                    state.copy(
                        history = preferences.history,
                        savedResults = preferences.savedResults,
                        historyEnabled = preferences.historyEnabled,
                        safeSearchEnabled = preferences.safeSearchEnabled,
                        dohEnabled = preferences.dohEnabled,
                        dohProvider = preferences.dohProvider,
                        themeMode = preferences.themeMode,
                        interfaceStyle = preferences.interfaceStyle,
                        accentPalette = preferences.accentPalette,
                        textScale = preferences.textScale,
                        compactResults = preferences.compactResults,
                        showSuggestions = preferences.showSuggestions,
                        webSettings = preferences.webSettings,
                    )
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        mutableUiState.update { it.copy(query = query) }
    }

    fun selectTab(tab: AppTab) {
        mutableUiState.update { it.copy(selectedTab = tab) }
    }

    fun search(rawQuery: String = mutableUiState.value.query) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return

        searchJob?.cancel()
        val safeSearch = mutableUiState.value.safeSearchEnabled
        val useDoh = mutableUiState.value.dohEnabled
        val dohProvider = mutableUiState.value.dohProvider
        mutableUiState.update {
            it.copy(
                query = query,
                activeQuery = query,
                selectedTab = AppTab.SEARCH,
                searchState = SearchUiState.Loading,
            )
        }
        if (mutableUiState.value.historyEnabled) {
            viewModelScope.launch { preferencesStore.addHistory(query) }
        }

        searchJob = viewModelScope.launch {
            try {
                val results = repository.search(query, safeSearch, useDoh, dohProvider)
                mutableUiState.update { it.copy(searchState = SearchUiState.Success(results)) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(
                        searchState = SearchUiState.Error(
                            "Не удалось получить результаты. Проверьте подключение к интернету и попробуйте ещё раз.",
                        ),
                    )
                }
            }
        }
    }

    fun backToSearchHome() {
        searchJob?.cancel()
        mutableUiState.update {
            it.copy(
                query = "",
                activeQuery = "",
                searchState = SearchUiState.Idle,
            )
        }
    }

    fun toggleSaved(result: SearchResult) {
        val current = mutableUiState.value.savedResults
        val updated = if (current.any { it.url == result.url }) {
            current.filterNot { it.url == result.url }
        } else {
            listOf(result) + current
        }
        mutableUiState.update { it.copy(savedResults = updated) }
        viewModelScope.launch { preferencesStore.setSavedResults(updated) }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        mutableUiState.update { it.copy(historyEnabled = enabled) }
        viewModelScope.launch { preferencesStore.setHistoryEnabled(enabled) }
    }

    fun setSafeSearchEnabled(enabled: Boolean) {
        mutableUiState.update { it.copy(safeSearchEnabled = enabled) }
        viewModelScope.launch { preferencesStore.setSafeSearchEnabled(enabled) }
    }

    fun setDohEnabled(enabled: Boolean) {
        mutableUiState.update { it.copy(dohEnabled = enabled) }
        viewModelScope.launch { preferencesStore.setDohEnabled(enabled) }
    }

    fun setDohProvider(provider: DohProvider) {
        mutableUiState.update { it.copy(dohProvider = provider) }
        viewModelScope.launch { preferencesStore.setDohProvider(provider) }
    }

    fun setThemeMode(mode: ThemeMode) {
        mutableUiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { preferencesStore.setThemeMode(mode) }
    }

    fun setInterfaceStyle(style: InterfaceStyle) {
        mutableUiState.update { it.copy(interfaceStyle = style) }
        viewModelScope.launch { preferencesStore.setInterfaceStyle(style) }
    }

    fun setAccentPalette(palette: AccentPalette) {
        mutableUiState.update { it.copy(accentPalette = palette) }
        viewModelScope.launch { preferencesStore.setAccentPalette(palette) }
    }

    fun setTextScale(scale: Float) {
        val safeScale = scale.coerceIn(0.85f, 1.4f)
        mutableUiState.update { it.copy(textScale = safeScale) }
        viewModelScope.launch { preferencesStore.setTextScale(safeScale) }
    }

    fun setCompactResults(enabled: Boolean) {
        mutableUiState.update { it.copy(compactResults = enabled) }
        viewModelScope.launch { preferencesStore.setCompactResults(enabled) }
    }

    fun setShowSuggestions(enabled: Boolean) {
        mutableUiState.update { it.copy(showSuggestions = enabled) }
        viewModelScope.launch { preferencesStore.setShowSuggestions(enabled) }
    }

    fun setWebSettings(settings: WebPageSettings) {
        val safeSettings = settings.copy(textZoom = settings.textZoom.coerceIn(80, 200))
        mutableUiState.update { it.copy(webSettings = safeSettings) }
        viewModelScope.launch { preferencesStore.setWebSettings(safeSettings) }
    }

    fun resetCustomization() {
        mutableUiState.update {
            it.copy(
                themeMode = ThemeMode.SYSTEM,
                interfaceStyle = InterfaceStyle.CLASSIC,
                accentPalette = AccentPalette.LIME,
                textScale = 1f,
                compactResults = false,
                showSuggestions = true,
                webSettings = WebPageSettings(),
            )
        }
        viewModelScope.launch {
            preferencesStore.setThemeMode(ThemeMode.SYSTEM)
            preferencesStore.setInterfaceStyle(InterfaceStyle.CLASSIC)
            preferencesStore.setAccentPalette(AccentPalette.LIME)
            preferencesStore.setTextScale(1f)
            preferencesStore.setCompactResults(false)
            preferencesStore.setShowSuggestions(true)
            preferencesStore.setWebSettings(WebPageSettings())
        }
    }

    fun clearHistory() {
        mutableUiState.update { it.copy(history = emptyList()) }
        viewModelScope.launch { preferencesStore.clearHistory() }
    }
}
