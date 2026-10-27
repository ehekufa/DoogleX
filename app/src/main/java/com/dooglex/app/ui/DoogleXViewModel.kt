package com.dooglex.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dooglex.app.data.DuckDuckGoSearchRepository
import com.dooglex.app.data.PreferencesStore
import com.dooglex.app.model.AppTab
import com.dooglex.app.model.DoogleXUiState
import com.dooglex.app.model.SearchResult
import com.dooglex.app.model.SearchUiState
import com.dooglex.app.model.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoogleXViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DuckDuckGoSearchRepository()
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
                        themeMode = preferences.themeMode,
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
                val results = repository.search(query, safeSearch)
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

    fun setThemeMode(mode: ThemeMode) {
        mutableUiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { preferencesStore.setThemeMode(mode) }
    }

    fun clearHistory() {
        mutableUiState.update { it.copy(history = emptyList()) }
        viewModelScope.launch { preferencesStore.clearHistory() }
    }
}
