package com.dooglex.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dooglex.app.WebActivity
import com.dooglex.app.model.AccentPalette
import com.dooglex.app.model.AppTab
import com.dooglex.app.model.DohProvider
import com.dooglex.app.model.DoogleXUiState
import com.dooglex.app.model.InterfaceStyle
import com.dooglex.app.model.SearchResult
import com.dooglex.app.model.SearchUiState
import com.dooglex.app.model.ThemeMode
import com.dooglex.app.model.WebPageSettings

@Composable
fun DoogleXApp(
    state: DoogleXUiState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBackToHome: () -> Unit,
    onTabSelected: (AppTab) -> Unit,
    onToggleSaved: (SearchResult) -> Unit,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onSafeSearchChange: (Boolean) -> Unit,
    onDohEnabledChange: (Boolean) -> Unit,
    onDohProviderChange: (DohProvider) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onInterfaceStyleChange: (InterfaceStyle) -> Unit,
    onAccentPaletteChange: (AccentPalette) -> Unit,
    onTextScaleChange: (Float) -> Unit,
    onCompactResultsChange: (Boolean) -> Unit,
    onShowSuggestionsChange: (Boolean) -> Unit,
    onWebSettingsChange: (WebPageSettings) -> Unit,
    onResetCustomization: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val context = LocalContext.current

    fun submitSearchOrAddress(value: String) {
        val directUrl = BrowserAddress.directUrlOrNull(value)
        if (directUrl != null) {
            openWebsite(context, directUrl)
        } else {
            onSearch(value)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DoogleXBottomBar(
                selectedTab = state.selectedTab,
                onTabSelected = onTabSelected,
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (state.selectedTab) {
                AppTab.SEARCH -> {
                    if (state.searchState == SearchUiState.Idle) {
                        SearchHomeScreen(
                            state = state,
                            onQueryChange = onQueryChange,
                            onSearch = ::submitSearchOrAddress,
                            onSettingsClick = { onTabSelected(AppTab.SETTINGS) },
                            onClearHistory = onClearHistory,
                        )
                    } else {
                        SearchResultsScreen(
                            state = state,
                            onQueryChange = onQueryChange,
                            onSearch = ::submitSearchOrAddress,
                            onBackToHome = onBackToHome,
                            onOpenResult = { result -> openWebsite(context, result.url) },
                            onToggleSaved = onToggleSaved,
                            compact = state.compactResults,
                        )
                    }
                }

                AppTab.SAVED -> SavedScreen(
                    savedResults = state.savedResults,
                    onOpenResult = { result -> openWebsite(context, result.url) },
                    onToggleSaved = onToggleSaved,
                    onExploreClick = { onTabSelected(AppTab.SEARCH) },
                    compact = state.compactResults,
                )

                AppTab.STYLE -> CustomizationScreen(
                    state = state,
                    onThemeModeChange = onThemeModeChange,
                    onInterfaceStyleChange = onInterfaceStyleChange,
                    onAccentPaletteChange = onAccentPaletteChange,
                    onTextScaleChange = onTextScaleChange,
                    onCompactResultsChange = onCompactResultsChange,
                    onShowSuggestionsChange = onShowSuggestionsChange,
                    onWebSettingsChange = onWebSettingsChange,
                    onResetCustomization = onResetCustomization,
                )

                AppTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onHistoryEnabledChange = onHistoryEnabledChange,
                    onSafeSearchChange = onSafeSearchChange,
                    onDohEnabledChange = onDohEnabledChange,
                    onDohProviderChange = onDohProviderChange,
                    onClearHistory = onClearHistory,
                    onOpenPrivacyPolicy = { url -> openWebsite(context, url) },
                )
            }
        }
    }
}

@Composable
private fun DoogleXBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        ),
    ) {
        NavigationBarItem(
            selected = selectedTab == AppTab.SEARCH,
            onClick = { onTabSelected(AppTab.SEARCH) },
            icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            label = { Text(
                "Поиск",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            ) },
            colors = navigationItemColors(),
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.SAVED,
            onClick = { onTabSelected(AppTab.SAVED) },
            icon = {
                Icon(
                    if (selectedTab == AppTab.SAVED) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                )
            },
            label = { Text(
                "Закладки",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            ) },
            colors = navigationItemColors(),
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.STYLE,
            onClick = { onTabSelected(AppTab.STYLE) },
            icon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
            label = { Text(
                "Стиль",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            ) },
            colors = navigationItemColors(),
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.SETTINGS,
            onClick = { onTabSelected(AppTab.SETTINGS) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text(
                "Настройки",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            ) },
            colors = navigationItemColors(),
        )
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    indicatorColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHomeScreen(
    state: DoogleXUiState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val suggestions = listOf(
        "Новости технологий",
        "Полезные нейросети",
        "Рецепт домашней пасты",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DoogleXLogo()
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Открыть настройки")
                }
            }
        }

        item {
            Column {
                Spacer(Modifier.height(8.dp))
                NativeBadge(dohEnabled = state.dohEnabled)
            }
        }

        item {
            Column {
                Text(
                    text = "Ваш поиск.",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Ваши правила.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        item {
            Text(
                text = "Ищите или открывайте URL прямо в приложении. Сайты теперь можно просматривать во вкладках.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            SearchField(
                value = state.query,
                onValueChange = onQueryChange,
                onSearch = { onSearch(state.query) },
            )
        }

        if (state.showSuggestions) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ПОПРОБУЙТЕ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        suggestions.forEach { suggestion ->
                            SuggestionChip(
                                text = suggestion,
                                onClick = { onSearch(suggestion) },
                            )
                        }
                    }
                }
            }
        }

        if (state.history.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Недавние запросы",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onClearHistory) {
                            Text("Очистить")
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        state.history.take(6).forEach { query ->
                            FilterChip(
                                selected = false,
                                onClick = { onSearch(query) },
                                label = {
                                    Text(
                                        query,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                ),
                            )
                        }
                    }
                }
            }
        }

        item {
            NativePrivacyCard()
        }
    }
}

@Composable
private fun NativeBadge(dohEnabled: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (dohEnabled) "HTTPS + DOH · DOOGLEX META" else "HTTPS · DOOGLEX META",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DoogleXLogo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "X",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "doogle",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "x",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        if (value.isNotBlank()) {
            focusManager.clearFocus()
            onSearch()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        placeholder = {
            Text(
                "Поиск или URL",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Очистить запрос",
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                IconButton(
                    onClick = submit,
                    enabled = value.isNotBlank(),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(42.dp)
                        .background(
                            if (value.isNotBlank()) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                ) {
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = "Найти или открыть URL",
                        tint = if (value.isNotBlank()) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun NativePrivacyCard() {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Только необходимое",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "История и закладки хранятся только на телефоне. У DoogleX нет собственного сервера с вашими запросами.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = "DoogleX Meta сам объединяет и ранжирует результаты. Запросы отправляются в Mojeek, Wikipedia и GitHub; DuckDuckGo не используется.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SearchResultsScreen(
    state: DoogleXUiState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBackToHome: () -> Unit,
    onOpenResult: (SearchResult) -> Unit,
    onToggleSaved: (SearchResult) -> Unit,
    compact: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToHome) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Вернуться на главную")
                }
                Column {
                    Text("Результаты", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = state.activeQuery,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        item {
            SearchField(
                value = state.query,
                onValueChange = onQueryChange,
                onSearch = { onSearch(state.query) },
            )
        }

        when (val searchState = state.searchState) {
            SearchUiState.Loading -> {
                item { LoadingResults() }
            }

            is SearchUiState.Success -> {
                if (searchState.results.isEmpty()) {
                    item { EmptyResults(query = state.activeQuery) }
                } else {
                    item {
                        Text(
                            text = "${formatResultCount(searchState.results.size)} · DoogleX Meta · HTTPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(searchState.results, key = { it.url }) { result ->
                        ResultCard(
                            result = result,
                            saved = state.savedResults.any { it.url == result.url },
                            onClick = { onOpenResult(result) },
                            onToggleSaved = { onToggleSaved(result) },
                            compact = compact,
                        )
                    }
                }
            }

            is SearchUiState.Error -> {
                item {
                    ErrorState(
                        message = searchState.message,
                        onRetry = { onSearch(state.activeQuery) },
                    )
                }
            }

            SearchUiState.Idle -> Unit
        }
    }
}

@Composable
private fun LoadingResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
        )
        Text(
            text = "Ищем в открытом вебе…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(3) { ResultSkeleton() }
    }
}

@Composable
private fun ResultSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonLine(widthFraction = 0.32f, height = 10.dp)
            SkeletonLine(widthFraction = 0.9f, height = 16.dp)
            SkeletonLine(widthFraction = 0.72f, height = 10.dp)
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
    )
}

@Composable
private fun ResultCard(
    result: SearchResult,
    saved: Boolean,
    onClick: () -> Unit,
    onToggleSaved: () -> Unit,
    compact: Boolean,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 27.dp else 32.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = result.domain.firstOrNull()?.uppercase() ?: "D",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = result.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleSaved, modifier = Modifier.size(38.dp)) {
                    Icon(
                        if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (saved) "Удалить из закладок" else "Сохранить",
                        tint = if (saved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            Text(
                text = result.title,
                style = if (compact) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (result.snippet.isNotBlank()) {
                Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
                Text(
                    text = result.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compact) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Открыть сайт",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(5.dp))
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = result.source,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyResults(query: String) {
    EmptyState(
        icon = Icons.Outlined.Search,
        title = "Ничего не найдено",
        description = "По запросу «$query» нет результатов. Попробуйте изменить формулировку.",
    )
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("Не получилось найти", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        FilterChip(
            selected = true,
            onClick = onRetry,
            label = { Text("Повторить") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }
}

@Composable
private fun SavedScreen(
    savedResults: List<SearchResult>,
    onOpenResult: (SearchResult) -> Unit,
    onToggleSaved: (SearchResult) -> Unit,
    onExploreClick: () -> Unit,
    compact: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Закладки", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (savedResults.isEmpty()) "Здесь будут ваши находки"
                    else "${formatResultCount(savedResults.size)} на устройстве",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        if (savedResults.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = "Пока ничего нет",
                    description = "Нажмите на закладку у результата поиска, чтобы сохранить его здесь.",
                    actionLabel = "Перейти к поиску",
                    onAction = onExploreClick,
                )
            }
        } else {
            items(savedResults, key = { it.url }) { result ->
                ResultCard(
                    result = result,
                    saved = true,
                    onClick = { onOpenResult(result) },
                    onToggleSaved = { onToggleSaved(result) },
                    compact = compact,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            FilterChip(
                selected = true,
                onClick = onAction,
                label = { Text(actionLabel) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    state: DoogleXUiState,
    onHistoryEnabledChange: (Boolean) -> Unit,
    onSafeSearchChange: (Boolean) -> Unit,
    onDohEnabledChange: (Boolean) -> Unit,
    onDohProviderChange: (DohProvider) -> Unit,
    onClearHistory: () -> Unit,
    onOpenPrivacyPolicy: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Настройки", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Настройки хранятся только на этом устройстве",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SettingsSection(title = "Защита соединения") {
                SettingsSwitchRow(
                    title = "DNS over HTTPS",
                    description = "DNS нативного поиска через ${state.dohProvider.displayName()}, без обычного DNS провайдера",
                    checked = state.dohEnabled,
                    onCheckedChange = onDohEnabledChange,
                )
                Text(
                    "Провайдер DoH",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DohProvider.entries.forEach { provider ->
                        FilterChip(
                            selected = state.dohProvider == provider,
                            onClick = { onDohProviderChange(provider) },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    provider.displayName(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                Text(
                    text = state.dohProvider.privacyNote(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                InfoRow(label = "Шифрование запросов", value = "TLS / HTTPS")
                InfoRow(label = "Обычный HTTP", value = "Запрещён")
                Text(
                    text = "Текст запросов зашифрован. DoH работает для поиска; WebView использует системный DNS, поэтому домены сайтов могут быть видны провайдеру.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        item {
            SettingsSection(title = "Поиск и данные") {
                SettingsSwitchRow(
                    title = "Безопасный поиск",
                    description = "Скрывать материалы для взрослых",
                    checked = state.safeSearchEnabled,
                    onCheckedChange = onSafeSearchChange,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                SettingsSwitchRow(
                    title = "Локальная история",
                    description = "Сохранять запросы на телефоне",
                    checked = state.historyEnabled,
                    onCheckedChange = onHistoryEnabledChange,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.history.isNotEmpty(), onClick = onClearHistory)
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = if (state.history.isNotEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Очистить историю",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.history.isNotEmpty()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                        Text(
                            if (state.history.isNotEmpty()) "Удалить ${formatResultCount(state.history.size)}"
                            else "История уже пуста",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Текущая защита страниц") {
                InfoRow(
                    label = "Совместимость сайтов",
                    value = if (state.webSettings.compatibilityMode) "Включена" else "Строгий режим",
                )
                InfoRow(
                    label = "Сторонние cookies",
                    value = if (state.webSettings.thirdPartyCookiesAllowed) "Разрешены" else "Заблокированы",
                )
                InfoRow(
                    label = "Известные трекеры",
                    value = if (state.webSettings.trackerBlockingActive) "Блокируются" else "Не блокируются",
                )
                InfoRow(label = "HTTPS-only", value = "Всегда")
                InfoRow(
                    label = "Данные при закрытии",
                    value = if (state.webSettings.keepSession) "Сохраняются" else "Удаляются",
                )
                InfoRow(label = "Демонстрация экрана", value = "Разрешена")
                Text(
                    text = "Параметры страниц меняются во вкладке «Стиль». «Новая сессия» очищает данные и загружает сайт заново. Это не смена IP-адреса и не полная защита от fingerprinting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Сайты открываются внутри",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Поиск, результаты и страницы остаются в DoogleX. Для просмотра сайтов используется системный WebView на Chromium; внешний браузер запускается только по отдельной кнопке.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }

        item {
            SettingsSection(title = "О приложении") {
                InfoRow(label = "Версия", value = "1.5.1")
                InfoRow(label = "Поиск", value = "DoogleX Meta")
                Text(
                    "Собственное ранжирование результатов Mojeek, Wikipedia и GitHub. Это метапоиск, а не собственный индекс интернета.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SourcePrivacyLink(
                    label = "Конфиденциальность Mojeek",
                    url = "https://www.mojeek.com/about/privacy/",
                    onClick = onOpenPrivacyPolicy,
                )
                SourcePrivacyLink(
                    label = "Конфиденциальность Wikimedia",
                    url = "https://foundation.wikimedia.org/wiki/Privacy_policy",
                    onClick = onOpenPrivacyPolicy,
                )
                SourcePrivacyLink(
                    label = "Конфиденциальность GitHub",
                    url = "https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement",
                    onClick = onOpenPrivacyPolicy,
                )
            }
        }
    }
}

@Composable
private fun SourcePrivacyLink(
    label: String,
    url: String,
    onClick: (String) -> Unit,
) {
    TextButton(
        onClick = { onClick(url) },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, maxLines = 2)
        Spacer(Modifier.width(5.dp))
        Icon(
            Icons.Outlined.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 2.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
internal fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun DohProvider.displayName(): String = when (this) {
    DohProvider.CLOUDFLARE -> "Cloudflare"
    DohProvider.GOOGLE -> "Google"
    DohProvider.QUAD9 -> "Quad9"
}

private fun DohProvider.privacyNote(): String =
    "${displayName()} обрабатывает DNS-запросы нативного поиска. При ошибке DoH поиск останавливается: скрытого возврата к обычному DNS нет."

private fun formatResultCount(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    val word = when {
        lastTwo in 11..14 -> "записей"
        last == 1 -> "запись"
        last in 2..4 -> "записи"
        else -> "записей"
    }
    return "$count $word"
}

private fun openWebsite(context: Context, url: String) {
    try {
        context.startActivity(WebActivity.createIntent(context, url))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Не удалось открыть страницу в DoogleX", Toast.LENGTH_SHORT).show()
    }
}
