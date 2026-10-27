package com.dooglex.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dooglex.app.model.AccentPalette
import com.dooglex.app.model.DoogleXUiState
import com.dooglex.app.model.InterfaceStyle
import com.dooglex.app.model.ThemeMode
import com.dooglex.app.model.WebPageSettings
import com.dooglex.app.ui.theme.accentColors
import kotlin.math.roundToInt

@Composable
fun CustomizationScreen(
    state: DoogleXUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onInterfaceStyleChange: (InterfaceStyle) -> Unit,
    onAccentPaletteChange: (AccentPalette) -> Unit,
    onTextScaleChange: (Float) -> Unit,
    onCompactResultsChange: (Boolean) -> Unit,
    onShowSuggestionsChange: (Boolean) -> Unit,
    onWebSettingsChange: (WebPageSettings) -> Unit,
    onResetCustomization: () -> Unit,
) {
    val webSettings = state.webSettings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Стиль и управление", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Настрой DoogleX под себя. Всё сохраняется на устройстве.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SettingsSection(title = "Оформление") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOption(
                        label = "Классика",
                        icon = Icons.Outlined.Public,
                        selected = state.interfaceStyle == InterfaceStyle.CLASSIC,
                        onClick = { onInterfaceStyleChange(InterfaceStyle.CLASSIC) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        label = "DoogleX",
                        icon = Icons.Outlined.Palette,
                        selected = state.interfaceStyle == InterfaceStyle.DOOGLEX,
                        onClick = { onInterfaceStyleChange(InterfaceStyle.DOOGLEX) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    if (state.interfaceStyle == InterfaceStyle.CLASSIC) {
                        "Нейтральный фон и синий акцент в стиле классических браузеров."
                    } else {
                        "Фирменный фон DoogleX и выбранная акцентная палитра."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SettingsSection(title = "Тема приложения") {
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

        if (state.interfaceStyle == InterfaceStyle.DOOGLEX) {
            item {
                SettingsSection(title = "Акцентный цвет") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentPalette.entries.chunked(3).forEach { rowPalettes ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowPalettes.forEach { palette ->
                                    AccentChoice(
                                        palette = palette,
                                        selected = state.accentPalette == palette,
                                        onClick = { onAccentPaletteChange(palette) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(3 - rowPalettes.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Размер текста приложения") {
                AppTextScaleChoices(
                    selected = state.textScale,
                    onSelected = onTextScaleChange,
                )
            }
        }

        item {
            SettingsSection(title = "Главная и результаты") {
                SettingsSwitchRow(
                    title = "Подсказки на главной",
                    description = "Показывать примеры поисковых запросов",
                    checked = state.showSuggestions,
                    onCheckedChange = onShowSuggestionsChange,
                )
                SettingsSwitchRow(
                    title = "Компактные результаты",
                    description = "Меньше отступы и короткие описания ссылок",
                    checked = state.compactResults,
                    onCheckedChange = onCompactResultsChange,
                )
            }
        }

        item {
            SettingsSection(title = "Текст на сайтах") {
                WebTextZoomSlider(
                    value = webSettings.textZoom,
                    onChange = { zoom ->
                        onWebSettingsChange(webSettings.copy(textZoom = zoom))
                    },
                )
            }
        }

        item {
            SettingsSection(title = "Режим отображения сайтов") {
                SettingsSwitchRow(
                    title = "Компьютерная версия",
                    description = "Запрашивать desktop-раскладку страниц",
                    checked = webSettings.desktopMode,
                    onCheckedChange = { enabled ->
                        onWebSettingsChange(webSettings.copy(desktopMode = enabled))
                    },
                )
                SettingsSwitchRow(
                    title = "Тёмные страницы",
                    description = "Затемнять светлые сайты, если WebView поддерживает",
                    checked = webSettings.forceDark,
                    onCheckedChange = { enabled ->
                        onWebSettingsChange(webSettings.copy(forceDark = enabled))
                    },
                )
                SettingsSwitchRow(
                    title = "Панель приватной сессии",
                    description = "Показывать статус и кнопку «Новая сессия»",
                    checked = webSettings.showPrivacyBar,
                    onCheckedChange = { enabled ->
                        onWebSettingsChange(webSettings.copy(showPrivacyBar = enabled))
                    },
                )
            }
        }

        item {
            SettingsSection(title = "Содержимое сайтов") {
                SettingsSwitchRow(
                    title = "Изображения",
                    description = "Отключение экономит трафик и ускоряет загрузку",
                    checked = webSettings.imagesEnabled,
                    onCheckedChange = { enabled ->
                        onWebSettingsChange(webSettings.copy(imagesEnabled = enabled))
                    },
                )
                SettingsSwitchRow(
                    title = "JavaScript",
                    description = "Отключение повышает приватность, но может сломать сайты",
                    checked = webSettings.javaScriptEnabled,
                    onCheckedChange = { enabled ->
                        onWebSettingsChange(webSettings.copy(javaScriptEnabled = enabled))
                    },
                )
            }
        }

        item {
            SettingsSection(title = "Совместимость и приватность") {
                SettingsSwitchRow(
                    title = "Совместимость сайтов",
                    description = "Не фильтровать ресурсы и разрешать сторонние cookies для корректной работы GitHub и других сайтов",
                    checked = webSettings.compatibilityMode,
                    onCheckedChange = { enabled ->
                        onWebSettingsChange(webSettings.copy(compatibilityMode = enabled))
                    },
                )
                if (webSettings.compatibilityMode) {
                    Text(
                        "HTTPS, запрет разрешений устройства и очистка новой сессии продолжают работать.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SettingsSwitchRow(
                        title = "Блокировать трекеры",
                        description = "Строгий фильтр рекламы и аналитики; может ломать некоторые сайты",
                        checked = webSettings.blockTrackers,
                        onCheckedChange = { enabled ->
                            onWebSettingsChange(webSettings.copy(blockTrackers = enabled))
                        },
                    )
                    SettingsSwitchRow(
                        title = "Сторонние cookies",
                        description = "Разрешить cookies других доменов — менее приватно",
                        checked = webSettings.thirdPartyCookiesEnabled,
                        onCheckedChange = { enabled ->
                            onWebSettingsChange(webSettings.copy(thirdPartyCookiesEnabled = enabled))
                        },
                    )
                }
                SettingsSwitchRow(
                    title = "Сохранять сессию",
                    description = "Не очищать cookies и кэш при закрытии страницы",
                    checked = webSettings.keepSession,
                    onCheckedChange = { enabled ->
                        onWebSettingsChange(webSettings.copy(keepSession = enabled))
                    },
                )
            }
        }

        item {
            SettingsSection(title = "Вернуть исходный стиль") {
                Text(
                    "Сбрасывает только оформление и параметры страниц. Закладки, история, поиск и DNS не изменяются.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onResetCustomization,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Сбросить кастомизацию")
                }
            }
        }
    }
}

@Composable
private fun AccentChoice(
    palette: AccentPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .background(accentColors(palette, darkTheme = false).primary, CircleShape),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = palette.displayName(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppTextScaleChoices(
    selected: Float,
    onSelected: (Float) -> Unit,
) {
    val choices = listOf(0.9f to "90%", 1f to "100%", 1.15f to "115%", 1.3f to "130%")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.forEach { (scale, label) ->
            FilterChip(
                selected = selected == scale,
                onClick = { onSelected(scale) },
                modifier = Modifier.weight(1f),
                label = {
                    Text(
                        label,
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
}

@Composable
private fun WebTextZoomSlider(
    value: Int,
    onChange: (Int) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Масштаб текста",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${sliderValue.roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onChange(sliderValue.roundToInt()) },
            valueRange = 80f..200f,
            steps = 11,
        )
        Row {
            Text("80%", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text("200%", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun AccentPalette.displayName(): String = when (this) {
    AccentPalette.LIME -> "Лайм"
    AccentPalette.SKY -> "Небо"
    AccentPalette.CORAL -> "Коралл"
    AccentPalette.VIOLET -> "Фиолет"
    AccentPalette.MINT -> "Мята"
    AccentPalette.GRAPHITE -> "Графит"
}
