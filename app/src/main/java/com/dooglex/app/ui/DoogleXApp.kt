package com.dooglex.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.OpenInNew
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
import androidx.compose.ui.graphics.Color
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
import com.dooglex.app.model.AppTab
import com.dooglex.app.model.DoogleXUiState
import com.dooglex.app.model.SearchResult
import com.dooglex.app.model.SearchUiState
import com.dooglex.app.model.ThemeMode
import com.dooglex.app.ui.theme.Ink
import com.dooglex.app.ui.theme.Lime

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
    onThemeModeChange: (ThemeMode) -> Unit,
    onClearHistory: () -> Unit,
) {
    val context = LocalContext.current

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
                            onSearch = onSearch,
                            onSettingsClick = { onTabSelected(AppTab.SETTINGS) },
                            onClearHistory = onClearHistory,
                        )
                    } else {
                        SearchResultsScreen(
                            state = state,
                            onQueryChange = onQueryChange,
                            onSearch = onSearch,
                            onBackToHome = onBackToHome,
                            onOpenResult = { result -> openWebsite(context, result.url) },
                            onToggleSaved = onToggleSaved,
                        )
                    }
                }

                AppTab.SAVED -> SavedScreen(
                    savedResults = state.savedResults,
                    onOpenResult = { result -> openWebsite(context, result.url) },
                    onToggleSaved = onToggleSaved,
                    onExploreClick = { onTabSelected(AppTab.SEARCH) },
                )

                AppTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onHistoryEnabledChange = onHistoryEnabledChange,
                    onSafeSearchChange = onSafeSearchChange,
                    onThemeModeChange = onThemeModeChange,
                    onClearHistory = onClearHistory,
                    onOpenPrivacyPolicy = {
                        openWebsite(context, "https://duckduckgo.com/privacy")
                    },
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
            label = { Text("Поиск") },
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
            label = { Text("Закладки") },
            colors = navigationItemColors(),
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.SETTINGS,
            onClick = { onTabSelected(AppTab.SETTINGS) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text("Настройки") },
            colors = navigationItemColors(),
        )
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Ink,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    indicatorColor = Lime,
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
            Spacer(Modifier.height(8.dp))
            NativeBadge()
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
                text = "Ищите прямо в приложении. Без аккаунтов, рекламы и браузерных вкладок.",
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
private fun NativeBadge() {
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
                text = "НАТИВНО · БЕЗ CHROMIUM",
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
                .background(Lime, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "X",
                color = Ink,
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
                "Что хотите найти?",
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
                        contentDescription = "Найти",
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
        color = Ink,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Lime.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Lime,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Только необходимое",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
            Text(
                text = "История и закладки хранятся только на телефоне. У DoogleX нет собственного сервера с вашими запросами.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Text(
                text = "Выдачу предоставляет DuckDuckGo. При поиске применяется его политика конфиденциальности.",
                style = MaterialTheme.typography.bodyMedium,
                color = Lime,
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
                            text = "${formatResultCount(searchState.results.size)} · DuckDuckGo",
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
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
            Spacer(Modifier.height(10.dp))
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (result.snippet.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = result.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
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
                selectedContainerColor = Lime,
                selectedLabelColor = Ink,
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
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
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
                    selectedContainerColor = Lime,
                    selectedLabelColor = Ink,
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
    onThemeModeChange: (ThemeMode) -> Unit,
    onClearHistory: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Настройки", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Настройки хранятся только на этом устройстве",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            SettingsSection(title = "Внешний вид") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOption(
                        label = "Система",
                        icon = Icons.Outlined.Settings,
                        selected = state.themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        label = "Светлая",
                        icon = Icons.Outlined.LightMode,
                        selected = state.themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        label = "Тёмная",
                        icon = Icons.Outlined.DarkMode,
                        selected = state.themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f),
                    )
                }
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
                        "Без WebView и Chromium",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "DoogleX — нативный Android-клиент. Поиск и результаты отображаются средствами системы, без встроенного браузера.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }

        item {
            SettingsSection(title = "О приложении") {
                InfoRow(label = "Версия", value = "1.0.0")
                InfoRow(label = "Источник выдачи", value = "DuckDuckGo")
                TextButton(
                    onClick = onOpenPrivacyPolicy,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("Политика конфиденциальности источника")
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
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
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Lime else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun SettingsSwitchRow(
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
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Не найдено приложение для открытия ссылки", Toast.LENGTH_SHORT).show()
    }
}
