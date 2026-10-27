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

            DoogleXTheme(themeMode = uiState.themeMode) {
                DoogleXApp(
                    state = uiState,
                    onQueryChange = doogleXViewModel::onQueryChange,
                    onSearch = doogleXViewModel::search,
                    onBackToHome = doogleXViewModel::backToSearchHome,
                    onTabSelected = doogleXViewModel::selectTab,
                    onToggleSaved = doogleXViewModel::toggleSaved,
                    onHistoryEnabledChange = doogleXViewModel::setHistoryEnabled,
                    onSafeSearchChange = doogleXViewModel::setSafeSearchEnabled,
                    onThemeModeChange = doogleXViewModel::setThemeMode,
                    onClearHistory = doogleXViewModel::clearHistory,
                )
            }
        }
    }
}
