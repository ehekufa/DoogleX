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

enum class AccentPalette {
    LIME,
    SKY,
    CORAL,
    VIOLET,
    MINT,
    GRAPHITE,
}

enum class DohProvider {
    CLOUDFLARE,
    GOOGLE,
    QUAD9,
}

enum class AppTab {
    SEARCH,
    SAVED,
    STYLE,
    SETTINGS,
}

data class WebPageSettings(
    val textZoom: Int = 100,
    val imagesEnabled: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val forceDark: Boolean = false,
    val showPrivacyBar: Boolean = true,
    val blockTrackers: Boolean = true,
    val thirdPartyCookiesEnabled: Boolean = false,
    val keepSession: Boolean = false,
)

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
    val dohProvider: DohProvider = DohProvider.CLOUDFLARE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.LIME,
    val textScale: Float = 1f,
    val compactResults: Boolean = false,
    val showSuggestions: Boolean = true,
    val webSettings: WebPageSettings = WebPageSettings(),
)
