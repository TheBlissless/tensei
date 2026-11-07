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

// Manga reader settings: source pickers, reader mode, data saver,
// sync threshold, rotation/fullscreen/indicator/auto-advance.



// â”€â”€â”€ Reader Settings â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun ReaderSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val readerMode by viewModel.mangaReaderMode.collectAsState()
    val showPageIndicator by viewModel.mangaPageIndicator.collectAsState()
    val lockRotation by viewModel.mangaLockRotation.collectAsState()
    val fullscreen by viewModel.mangaFullscreen.collectAsState()
    val autoAdvance by viewModel.mangaAutoAdvance.collectAsState()
    val installedMangaExtensions by viewModel.installedExtensions.collectAsState()
    val selectedMangaExtension by viewModel.selectedExtensionAuthority.collectAsState()
    val mangaSourceMethod by viewModel.mangaSourceMethod.collectAsState()
    val defaultExternalMangaExtension by viewModel.defaultExternalMangaExtension.collectAsState()
    val mangaExtensionLanguages by viewModel.mangaExtensionLanguages.collectAsState()
    var showMangaExtPicker by remember { mutableStateOf(false) }
    var showExternalExtPicker by remember { mutableStateOf(false) }
    var showExternalLangPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val externalManager = remember { MangaExtensionManager.get(context) }
    val externalExtensions by externalManager.extensions.collectAsState()
    val selectedExternalLanguage = defaultExternalMangaExtension?.let { mangaExtensionLanguages[it] }

    LaunchedEffect(Unit) {
        viewModel.discoverExtensions()
    }

    SettingsPageScaffold(title = stringResource(R.string.settings_group_reader), onBack = onBack) {
        SectionHeader(stringResource(R.string.settings_x_manga_source))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsChoiceChip(
                    label = stringResource(R.string.settings_x_tensei),
                    isSelected = mangaSourceMethod != "external",
                    onClick = { viewModel.setMangaSourceMethod("tensei") }
                )
                SettingsChoiceChip(
                    label = stringResource(R.string.settings_x_external),
                    isSelected = mangaSourceMethod == "external",
                    onClick = { viewModel.setMangaSourceMethod("external") }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_x_external_extension_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        if (mangaSourceMethod != "external") {
            SectionHeader(stringResource(R.string.settings_section_default_source))
            SettingsCard {
                ClickableSettingsRow(
                    onClick = { showMangaExtPicker = true },
                    icon = Icons.Default.Extension,
                    title = stringResource(R.string.settings_default_extension),
                    subtitle = installedMangaExtensions.find { it.authority == selectedMangaExtension }?.label
                        ?: selectedMangaExtension?.removeSuffix(".provider")
                        ?: stringResource(R.string.common_none)
                )
            }
        } else {
            SectionHeader(stringResource(R.string.settings_x_external_extension))
            SettingsCard {
                ClickableSettingsRow(
                    onClick = { externalManager.refresh(); showExternalExtPicker = true },
                    icon = Icons.Default.Extension,
                    title = stringResource(R.string.settings_x_default_external_extension),
                    subtitle = externalExtensions.find { it.pkgName == defaultExternalMangaExtension }?.name
                        ?: defaultExternalMangaExtension
                        ?: stringResource(R.string.common_none)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 54.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
                ClickableSettingsRow(
                    onClick = {
                        if (defaultExternalMangaExtension != null) showExternalLangPicker = true
                    },
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_x_language),
                    subtitle = selectedExternalLanguage
                        ?: if (defaultExternalMangaExtension != null) "English (default)" else stringResource(R.string.common_none)
                )
            }
        }

        SectionHeader(stringResource(R.string.settings_section_reading_mode))
        SettingsCard {
            val modes = listOf(
                Triple("vertical_scroll", stringResource(R.string.settings_reading_vertical), stringResource(R.string.settings_reading_vertical_desc)) to Icons.Default.ViewAgenda,
                Triple("left_to_right", stringResource(R.string.settings_reading_ltr), stringResource(R.string.settings_reading_ltr_desc)) to Icons.AutoMirrored.Filled.ArrowForward,
                Triple("right_to_left", stringResource(R.string.settings_reading_rtl), stringResource(R.string.settings_reading_rtl_desc)) to Icons.AutoMirrored.Filled.ArrowBack
            )
            modes.forEachIndexed { index, (triple, icon) ->
                val (key, title, description) = triple
                SettingsRadioItem(
                    selected = readerMode == key,
                    onClick = { viewModel.setMangaReaderMode(key) },
                    icon = icon,
                    title = title,
                    description = description
                )
                if (index < modes.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 54.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                }
            }
        }

        SectionHeader(stringResource(R.string.settings_section_display))
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_page_indicator),
                description = stringResource(R.string.settings_page_indicator_desc),
                checked = showPageIndicator,
                onCheckedChange = { viewModel.setMangaPageIndicator(it) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsToggle(
                title = stringResource(R.string.settings_lock_rotation),
                description = stringResource(R.string.settings_lock_rotation_desc),
                checked = lockRotation,
                onCheckedChange = { viewModel.setMangaLockRotation(it) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsToggle(
                title = stringResource(R.string.settings_fullscreen),
                description = stringResource(R.string.settings_fullscreen_desc),
                checked = fullscreen,
                onCheckedChange = { viewModel.setMangaFullscreen(it) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsToggle(
                title = stringResource(R.string.settings_next_chapter_button),
                description = stringResource(R.string.settings_next_chapter_button_desc),
                checked = autoAdvance,
                onCheckedChange = { viewModel.setMangaAutoAdvance(it) }
            )
        }
    }

    if (showMangaExtPicker) {
        AlertDialog(
            onDismissRequest = { showMangaExtPicker = false },
            title = { Text(stringResource(R.string.settings_default_extension)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (installedMangaExtensions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_no_manga_extensions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    installedMangaExtensions.forEach { ext ->
                        val isSelected = ext.authority == selectedMangaExtension
                        TextButton(
                            onClick = { viewModel.selectExtension(ext.authority); showMangaExtPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ext.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        ext.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMangaExtPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    if (showExternalExtPicker) {
        AlertDialog(
            onDismissRequest = { showExternalExtPicker = false },
            title = { Text(stringResource(R.string.settings_x_default_external_extension)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (externalExtensions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_x_no_external_extensions_installed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    externalExtensions.forEach { ext ->
                        val isSelected = ext.pkgName == defaultExternalMangaExtension
                        TextButton(
                            onClick = { viewModel.setDefaultExternalMangaExtension(ext.pkgName); showExternalExtPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ext.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        "${ext.pkgName}  •  v${ext.versionName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExternalExtPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    if (showExternalLangPicker && defaultExternalMangaExtension != null) {
        val pkg = defaultExternalMangaExtension!!
        val languages = remember(pkg, externalExtensions) { viewModel.externalExtensionLanguages(pkg) }
        AlertDialog(
            onDismissRequest = { showExternalLangPicker = false },
            title = { Text(stringResource(R.string.settings_x_language)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    languages.forEach { language ->
                        val isSelected = (mangaExtensionLanguages[pkg] ?: "en").equals(language, ignoreCase = true)
                        TextButton(
                            onClick = { viewModel.setMangaExtensionLanguage(pkg, language); showExternalLangPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text(language, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExternalLangPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}
