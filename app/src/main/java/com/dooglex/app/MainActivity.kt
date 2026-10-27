package com.dooglex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dooglex.app.ui.DoogleXApp
import com.dooglex.app.ui.DoogleXViewModel
import com.dooglex.app.ui.theme.DoogleXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val doogleXViewModel: DoogleXViewModel = viewModel()
            val uiState by doogleXViewModel.uiState.collectAsState()

            DoogleXTheme(
                themeMode = uiState.themeMode,
                interfaceStyle = uiState.interfaceStyle,
                accentPalette = uiState.accentPalette,
                textScale = uiState.textScale,
            ) {
                DoogleXApp(
                    state = uiState,
                    onQueryChange = doogleXViewModel::onQueryChange,
                    onSearch = doogleXViewModel::search,
                    onBackToHome = doogleXViewModel::backToSearchHome,
                    onTabSelected = doogleXViewModel::selectTab,
                    onToggleSaved = doogleXViewModel::toggleSaved,
                    onHistoryEnabledChange = doogleXViewModel::setHistoryEnabled,
                    onSafeSearchChange = doogleXViewModel::setSafeSearchEnabled,
                    onDohEnabledChange = doogleXViewModel::setDohEnabled,
                    onDohProviderChange = doogleXViewModel::setDohProvider,
                    onThemeModeChange = doogleXViewModel::setThemeMode,
                    onInterfaceStyleChange = doogleXViewModel::setInterfaceStyle,
                    onAccentPaletteChange = doogleXViewModel::setAccentPalette,
                    onTextScaleChange = doogleXViewModel::setTextScale,
                    onCompactResultsChange = doogleXViewModel::setCompactResults,
                    onShowSuggestionsChange = doogleXViewModel::setShowSuggestions,
                    onWebSettingsChange = doogleXViewModel::setWebSettings,
                    onResetCustomization = doogleXViewModel::resetCustomization,
                    onClearHistory = doogleXViewModel::clearHistory,
                )
            }
        }
    }
}
