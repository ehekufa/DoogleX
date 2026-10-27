package com.dooglex.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dooglex.app.data.DoogleXMetaSearchRepository
import com.dooglex.app.data.GoogleSearchErrors
import com.dooglex.app.data.MetaSearchResultMerger
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
    private var loadMoreJob: Job? = null
    private var searchGeneration = 0L

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
                        googleApiKey = preferences.googleApiKey,
                        googleSearchEngineId = preferences.googleSearchEngineId,
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
        loadMoreJob?.cancel()
        val generation = ++searchGeneration
        val config = mutableUiState.value
        mutableUiState.update {
            it.copy(
                query = query,
                activeQuery = query,
                selectedTab = AppTab.SEARCH,
                searchState = SearchUiState.Loading,
            )
        }
        if (config.historyEnabled) {
            viewModelScope.launch { preferencesStore.addHistory(query) }
        }

        searchJob = viewModelScope.launch {
            try {
                val response = repository.search(
                    query = query,
                    safeSearch = config.safeSearchEnabled,
                    useDoh = config.dohEnabled,
                    dohProvider = config.dohProvider,
                    googleApiKey = config.googleApiKey,
                    googleSearchEngineId = config.googleSearchEngineId,
                )
                mutableUiState.update { state ->
                    if (generation != searchGeneration) {
                        state
                    } else {
                        state.copy(
                            searchState = SearchUiState.Success(
                                results = response.results,
                                googleNextStart = response.googleNextStart,
                                googleError = response.googleError,
                            ),
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableUiState.update { state ->
                    if (generation != searchGeneration) {
                        state
                    } else {
                        state.copy(
                            searchState = SearchUiState.Error(
                                "Не удалось получить результаты. Проверьте подключение к интернету и попробуйте ещё раз.",
                            ),
                        )
                    }
                }
            }
        }
    }

    fun loadMoreGoogleResults() {
        val config = mutableUiState.value
        val success = config.searchState as? SearchUiState.Success ?: return
        val start = success.googleNextStart ?: return
        if (success.loadingMore) return

        val query = config.activeQuery
        val generation = searchGeneration
        mutableUiState.update { state ->
            val current = state.searchState as? SearchUiState.Success
            if (current == null) {
                state
            } else {
                state.copy(
                    searchState = current.copy(loadingMore = true, loadMoreError = null),
                )
            }
        }
        loadMoreJob = viewModelScope.launch {
            try {
                val page = repository.loadGoogleResults(
                    query = query,
                    start = start,
                    safeSearch = config.safeSearchEnabled,
                    useDoh = config.dohEnabled,
                    dohProvider = config.dohProvider,
                    apiKey = config.googleApiKey,
                    searchEngineId = config.googleSearchEngineId,
                )
                mutableUiState.update { state ->
                    val current = state.searchState as? SearchUiState.Success
                    if (generation != searchGeneration ||
                        state.activeQuery != query ||
                        current == null
                    ) {
                        state
                    } else {
                        state.copy(
                            searchState = current.copy(
                                results = MetaSearchResultMerger.append(current.results, page.results),
                                googleNextStart = page.nextStart?.takeIf { it > start },
                                loadingMore = false,
                                loadMoreError = null,
                            ),
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableUiState.update { state ->
                    val current = state.searchState as? SearchUiState.Success
                    if (generation != searchGeneration ||
                        state.activeQuery != query ||
                        current == null
                    ) {
                        state
                    } else {
                        state.copy(
                            searchState = current.copy(
                                loadingMore = false,
                                loadMoreError = GoogleSearchErrors.message(error),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun backToSearchHome() {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchGeneration++
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

    fun setGoogleSearchConfig(apiKey: String, searchEngineId: String) {
        val cleanKey = apiKey.trim()
        val cleanSearchEngineId = searchEngineId.trim()
        mutableUiState.update {
            it.copy(
                googleApiKey = cleanKey,
                googleSearchEngineId = cleanSearchEngineId,
            )
        }
        viewModelScope.launch {
            preferencesStore.setGoogleSearchConfig(cleanKey, cleanSearchEngineId)
        }
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
