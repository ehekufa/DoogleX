package com.dooglex.app.model

data class SearchResult(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppTab {
    SEARCH,
    SAVED,
    SETTINGS,
}

sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class Success(val results: List<SearchResult>) : SearchUiState

    data class Error(val message: String) : SearchUiState
}

data class DoogleXUiState(
    val query: String = "",
    val activeQuery: String = "",
    val selectedTab: AppTab = AppTab.SEARCH,
    val searchState: SearchUiState = SearchUiState.Idle,
    val history: List<String> = emptyList(),
    val savedResults: List<SearchResult> = emptyList(),
    val historyEnabled: Boolean = false,
    val safeSearchEnabled: Boolean = true,
    val dohEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
