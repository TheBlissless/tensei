package com.blissless.tensei.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.ui.components.appIconDrawable
import com.blissless.tensei.ui.theme.ColorMode
import com.blissless.tensei.ui.theme.ThemeMode
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.ui.screens.extensions.ExtensionsScreen
import com.blissless.tensei.viewmodel.ExtensionsViewModel
import com.blissless.tensei.update.UpdateViewModel
import kotlin.math.round
import androidx.core.net.toUri
// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)
import com.blissless.tensei.viewmodel.setThemeMode
import com.blissless.tensei.viewmodel.setAppIcon
import com.blissless.tensei.viewmodel.setColorMode
import com.blissless.tensei.viewmodel.setShowStatusColors
import com.blissless.tensei.viewmodel.setShowAnimeCardButtons
import com.blissless.tensei.viewmodel.setShowMangaCardButtons
import com.blissless.tensei.viewmodel.setShowMangaStatusColors
import com.blissless.tensei.viewmodel.setMaxPerformance
import com.blissless.tensei.viewmodel.setPreferEnglishTitles
import com.blissless.tensei.viewmodel.setAdvancedSearch
import com.blissless.tensei.viewmodel.setStartupScreen
import com.blissless.tensei.viewmodel.setAppLanguage
import com.blissless.tensei.viewmodel.setMangaReaderMode
import com.blissless.tensei.viewmodel.setMangaDataSaver
import com.blissless.tensei.viewmodel.setMangaPageIndicator
import com.blissless.tensei.viewmodel.setMangaFullscreen
import com.blissless.tensei.viewmodel.setMangaAutoAdvance
import com.blissless.tensei.viewmodel.setMangaLockRotation
import com.blissless.tensei.viewmodel.setMangaSyncThreshold
import com.blissless.tensei.viewmodel.setStreamMethod
import com.blissless.tensei.viewmodel.setPreferredCategory
import com.blissless.tensei.viewmodel.setBufferAheadSeconds
import com.blissless.tensei.viewmodel.setBufferSizeMb
import com.blissless.tensei.viewmodel.setShowBufferIndicator
import com.blissless.tensei.viewmodel.setDefaultExtensionPackage
import com.blissless.tensei.viewmodel.setDefaultMagnetExtension
import com.blissless.tensei.viewmodel.setDefaultStreamExtension
import com.blissless.tensei.viewmodel.setDefaultSubtitleLang
import com.blissless.tensei.viewmodel.setTrackingPercentage
import com.blissless.tensei.viewmodel.setForwardSkipSeconds
import com.blissless.tensei.viewmodel.setBackwardSkipSeconds
import com.blissless.tensei.viewmodel.setAutoSkipOpening
import com.blissless.tensei.viewmodel.setAutoSkipEnding
import com.blissless.tensei.viewmodel.setAutoPlayNextEpisode
import com.blissless.tensei.viewmodel.setSwipeVolume
import com.blissless.tensei.viewmodel.setSwipeBrightness
import com.blissless.tensei.viewmodel.setSwipeSwap
import com.blissless.tensei.viewmodel.setSupportsPiP
import com.blissless.tensei.viewmodel.setDiscordRichPresence
import com.blissless.tensei.viewmodel.setCheckUpdatesOnStart
import com.blissless.tensei.viewmodel.setAutoUpdateExtensions
import com.blissless.tensei.viewmodel.setHideEpisodeDescription
import com.blissless.tensei.viewmodel.loadAvailableMagnetExtensions
import com.blissless.tensei.viewmodel.loadAvailableStreamExtensions
import com.blissless.tensei.viewmodel.getVideoCacheSize
import com.blissless.tensei.viewmodel.clearNonEssentialCaches
import com.blissless.tensei.viewmodel.discoverExtensions
import com.blissless.tensei.viewmodel.installedExtensions
import com.blissless.tensei.viewmodel.InstalledExtension
import com.blissless.tensei.viewmodel.selectedExtensionAuthority
import com.blissless.tensei.viewmodel.selectExtension
import com.blissless.tensei.viewmodel.mangaSourceMethod
import com.blissless.tensei.viewmodel.defaultExternalMangaExtension
import com.blissless.tensei.viewmodel.mangaExtensionLanguages
import com.blissless.tensei.viewmodel.setMangaSourceMethod
import com.blissless.tensei.viewmodel.setDefaultExternalMangaExtension
import com.blissless.tensei.viewmodel.setMangaExtensionLanguage
import com.blissless.tensei.viewmodel.externalExtensionLanguages
import com.blissless.tensei.extensions.manga.MangaExtensionManager
import com.blissless.tensei.util.AppLocale
import com.blissless.tensei.util.ErrorHandler
import com.blissless.tensei.util.toast
import com.blissless.tensei.util.longToast

// Appearance settings: theme, OLED, app icon, status colors, titles.



// â”€â”€â”€ Appearance â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun AppearanceSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val showStatusColorsState by viewModel.showStatusColors.collectAsState(initial = true)
    val showAnimeCardButtons by viewModel.showAnimeCardButtons.collectAsState(initial = true)
    val showMangaCardButtons by viewModel.showMangaCardButtons.collectAsState(initial = true)
    val showMangaStatusColors by viewModel.showMangaStatusColors.collectAsState(initial = true)
    val advancedSearch by viewModel.advancedSearch.collectAsState(initial = false)

    SettingsPageScaffold(title = stringResource(R.string.settings_group_appearance), onBack = onBack) {
        val currentThemeMode by viewModel.themeMode.collectAsState()
        val colorModeValue by viewModel.colorMode.collectAsState()
        val currentColorMode = remember(colorModeValue) { ColorMode.fromValue(colorModeValue) }

        SectionHeader(stringResource(R.string.settings_section_theme_mode))
        SettingsCard {
            SettingsRadioItem(
                selected = currentThemeMode == ThemeMode.SYSTEM.value,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM.value) },
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_theme_system),
                description = stringResource(R.string.settings_theme_system_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = currentThemeMode == ThemeMode.LIGHT.value,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT.value) },
                icon = Icons.Default.LightMode,
                title = stringResource(R.string.settings_theme_light),
                description = stringResource(R.string.settings_theme_light_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = currentThemeMode == ThemeMode.DARK.value,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK.value) },
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.settings_theme_dark),
                description = stringResource(R.string.settings_theme_dark_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = currentThemeMode == ThemeMode.OLED.value,
                onClick = { viewModel.setThemeMode(ThemeMode.OLED.value) },
                icon = Icons.Default.Storage,
                title = stringResource(R.string.settings_theme_oled),
                description = stringResource(R.string.settings_theme_oled_desc)
            )
        }

        SectionHeader(stringResource(R.string.settings_section_color_mode))
        SettingsCard {
            SettingsRadioItem(
                selected = currentColorMode == ColorMode.MATERIAL,
                onClick = { viewModel.setColorMode(ColorMode.MATERIAL.value) },
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_color_material),
                description = stringResource(R.string.settings_color_material_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = currentColorMode == ColorMode.SEMI_MONOCHROME,
                onClick = { viewModel.setColorMode(ColorMode.SEMI_MONOCHROME.value) },
                icon = Icons.Default.Contrast,
                title = stringResource(R.string.settings_color_semi_mono),
                description = stringResource(R.string.settings_color_semi_mono_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = currentColorMode == ColorMode.MONOCHROME,
                onClick = { viewModel.setColorMode(ColorMode.MONOCHROME.value) },
                icon = Icons.Default.InvertColors,
                title = stringResource(R.string.settings_color_mono),
                description = stringResource(R.string.settings_color_mono_desc)
            )
        }

        val hideEpisodeDescription by viewModel.hideEpisodeDescription.collectAsState(initial = true)

        SectionHeader(stringResource(R.string.settings_section_episodes))
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_hide_episode_desc),
                description = stringResource(R.string.settings_hide_episode_desc_detail),
                checked = hideEpisodeDescription,
                onCheckedChange = { viewModel.setHideEpisodeDescription(it) }
            )
        }

        val appIcon by viewModel.appIcon.collectAsState()

        SectionHeader(stringResource(R.string.settings_section_app_icon))
        SettingsCard {
            AppIconRadioItem(
                selected = appIcon == "default",
                onClick = { viewModel.setAppIcon("default") },
                previewRes = R.drawable.ic_icon_default,
                title = stringResource(R.string.settings_icon_default),
                description = stringResource(R.string.settings_icon_default_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            AppIconRadioItem(
                selected = appIcon == "wob",
                onClick = { viewModel.setAppIcon("wob") },
                previewRes = R.drawable.ic_icon_wob,
                title = stringResource(R.string.settings_icon_wob),
                description = stringResource(R.string.settings_icon_wob_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            AppIconRadioItem(
                selected = appIcon == "bow",
                onClick = { viewModel.setAppIcon("bow") },
                previewRes = R.drawable.ic_icon_bow,
                title = stringResource(R.string.settings_icon_bow),
                description = stringResource(R.string.settings_icon_bow_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    stringResource(R.string.settings_icon_restart_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        SectionHeader(stringResource(R.string.settings_section_display))
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_display_anime_status_colors),
                description = stringResource(R.string.settings_display_anime_status_colors_desc),
                checked = showStatusColorsState,
                onCheckedChange = { viewModel.setShowStatusColors(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_display_anime_card_buttons),
                description = stringResource(R.string.settings_display_anime_card_buttons_desc),
                checked = showAnimeCardButtons,
                onCheckedChange = { viewModel.setShowAnimeCardButtons(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_display_manga_status_colors),
                description = stringResource(R.string.settings_display_manga_status_colors_desc),
                checked = showMangaStatusColors,
                onCheckedChange = { viewModel.setShowMangaStatusColors(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_display_manga_card_buttons),
                description = stringResource(R.string.settings_display_manga_card_buttons_desc),
                checked = showMangaCardButtons,
                onCheckedChange = { viewModel.setShowMangaCardButtons(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_display_advanced_search),
                description = stringResource(R.string.settings_display_advanced_search_desc),
                checked = advancedSearch,
                onCheckedChange = { viewModel.setAdvancedSearch(it) }
            )
        }
    }
}
