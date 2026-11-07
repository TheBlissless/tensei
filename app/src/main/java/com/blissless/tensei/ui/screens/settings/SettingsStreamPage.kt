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

// Streaming settings: stream method, extension pickers, sub/dub,
// buffering and proxy configuration.



// â”€â”€â”€ Stream Settings â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun StreamSettingsPage(
    viewModel: MainViewModel,
    preferredCategory: String,
    onNavigateToExtensions: () -> Unit,
    onBack: () -> Unit
) {
    val bufferAheadSeconds by viewModel.bufferAheadSeconds.collectAsState(initial = 30)
    val bufferSizeMb by viewModel.bufferSizeMb.collectAsState(initial = 100)
    val showBufferIndicator by viewModel.showBufferIndicator.collectAsState(initial = true)
    val defaultExtPackage by viewModel.defaultExtensionPackage.collectAsState()
    val defaultSubtitleLang by viewModel.defaultSubtitleLang.collectAsState()
    val streamMethod by viewModel.streamMethod.collectAsState()
    val extViewModel: ExtensionsViewModel = viewModel()
    val extUiState by extViewModel.uiState.collectAsState()
    val magnetExtensions by viewModel.availableMagnetExtensions.collectAsState()
    val streamExtensions by viewModel.availableStreamExtensions.collectAsState()
    val defaultMagnetExtension by viewModel.defaultMagnetExtension.collectAsState()
    val defaultStreamExtension by viewModel.defaultStreamExtension.collectAsState()
    var showExtPicker by remember { mutableStateOf(false) }
    var showSubtitleLangPicker by remember { mutableStateOf(false) }
    val subtitleLanguages = listOf("English", "Arabic", "French", "German", "Italian", "Portuguese", "Russian", "Spanish", "Japanese", "Chinese", "Korean")
    LaunchedEffect(Unit) {
        viewModel.loadAvailableMagnetExtensions()
        viewModel.loadAvailableStreamExtensions()
    }

    SettingsPageScaffold(title = stringResource(R.string.settings_group_stream), onBack = onBack) {
        SectionHeader(stringResource(R.string.settings_section_stream_method))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsChoiceChip(label = "Tensei", isSelected = streamMethod == "magnet", onClick = { viewModel.setStreamMethod("magnet") })
                SettingsChoiceChip(label = stringResource(R.string.settings_stream_external), isSelected = streamMethod == "direct", onClick = { viewModel.setStreamMethod("direct") })
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_stream_method_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        SectionHeader(stringResource(R.string.settings_section_extensions))
        SettingsCard {
            ClickableSettingsRow(
                onClick = {
                    if (streamMethod == "direct") {
                        if (defaultExtPackage.isEmpty() && extUiState.extensions.isEmpty()) {
                            onNavigateToExtensions()
                        } else {
                            showExtPicker = true
                        }
                    } else {
                        showExtPicker = true
                    }
                },
                icon = Icons.Default.Extension,
                title = stringResource(R.string.settings_default_extension),
                subtitle = if (streamMethod == "direct") {
                    if (defaultExtPackage.isNotEmpty())
                        extUiState.extensions.find { it.packageName == defaultExtPackage }?.name ?: defaultExtPackage
                    else stringResource(R.string.common_none)
                } else {
                    val streamName = defaultStreamExtension?.takeIf { it.isNotBlank() }?.let { auth -> streamExtensions.find { it.second == auth }?.first }
                    if (streamName != null) streamName
                    else {
                        val name = defaultMagnetExtension?.takeIf { it.isNotBlank() }?.let { auth -> magnetExtensions.find { it.second == auth }?.first }
                        name ?: defaultMagnetExtension?.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_none)
                    }
                }
            )
        }
        SettingsCard {
            ClickableSettingsRow(
                onClick = { showSubtitleLangPicker = true },
                icon = Icons.Default.Subtitles,
                title = stringResource(R.string.settings_default_subtitle_lang),
                subtitle = defaultSubtitleLang
            )
        }

        SectionHeader(stringResource(R.string.settings_section_audio))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsChoiceChip(label = "SUB", isSelected = preferredCategory == "sub", onClick = { viewModel.setPreferredCategory("sub") })
                SettingsChoiceChip(label = "DUB", isSelected = preferredCategory == "dub", onClick = { viewModel.setPreferredCategory("dub") })
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_preferred_audio_category),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.settings_preferred_audio_category_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        SectionHeader(stringResource(R.string.settings_section_buffer))
        SettingsCard {
            SettingsSliderRow(
                title = stringResource(R.string.settings_buffer_ahead),
                description = stringResource(R.string.settings_buffer_ahead_desc),
                value = bufferAheadSeconds.toFloat(),
                valueRange = 0f..300f,
                valueLabel = stringResource(R.string.settings_value_seconds, bufferAheadSeconds),
                onValueChange = { viewModel.setBufferAheadSeconds((round(it / 10f) * 10f).toInt()) },
                minLabel = stringResource(R.string.settings_value_seconds, 0),
                maxLabel = stringResource(R.string.settings_value_seconds, 300),
                leadingIcon = Icons.Default.PlayArrow
            )
        }
        SettingsCard {
            SettingsSliderRow(
                title = stringResource(R.string.settings_buffer_size),
                description = stringResource(R.string.settings_buffer_size_desc),
                value = bufferSizeMb.toFloat(),
                valueRange = 50f..500f,
                valueLabel = stringResource(R.string.settings_value_mb, bufferSizeMb),
                onValueChange = { viewModel.setBufferSizeMb((round(it / 25f) * 25f).toInt()) },
                minLabel = stringResource(R.string.settings_value_mb, 50),
                maxLabel = stringResource(R.string.settings_value_mb, 500),
                leadingIcon = Icons.Default.Memory
            )
        }
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_buffer_indicator),
                description = stringResource(R.string.settings_buffer_indicator_desc),
                checked = showBufferIndicator,
                onCheckedChange = { viewModel.setShowBufferIndicator(it) }
            )
        }
    }

    if (showExtPicker) {
        AlertDialog(
            onDismissRequest = { showExtPicker = false },
            title = { Text(stringResource(R.string.settings_default_extension)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (streamMethod == "direct") {
                        val standardExtensions = extUiState.extensions.filter {
                            !com.blissless.tensei.extensions.ExtensionDetector.isBlisslessStreamExtension(it.packageName) &&
                            !com.blissless.tensei.extensions.ExtensionDetector.isBlisslessTorrentExtension(it.packageName) &&
                            !com.blissless.tensei.extensions.ExtensionDetector.isBlisslessMangaExtension(it.packageName)
                        }
                        if (standardExtensions.isNotEmpty()) {
                            Text(
                                stringResource(R.string.settings_stream_external),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        standardExtensions.forEach { ext ->
                            val isSelected = ext.packageName == defaultExtPackage
                            TextButton(
                                onClick = { viewModel.setDefaultExtensionPackage(ext.packageName); viewModel.setDefaultStreamExtension(null); showExtPicker = false },
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
                                            ext.name,
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
                    } else {
                        if (streamExtensions.isNotEmpty()) {
                            Text(
                                stringResource(R.string.settings_stream_group),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        streamExtensions.forEach { (name, authority) ->
                            val isSelected = authority == defaultStreamExtension
                            TextButton(
                                onClick = { viewModel.setDefaultStreamExtension(authority); viewModel.setDefaultExtensionPackage(""); viewModel.setDefaultMagnetExtension(""); showExtPicker = false },
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
                                            name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        Text(
                                            authority,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        if (magnetExtensions.isNotEmpty()) {
                            if (streamExtensions.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Text(
                                stringResource(R.string.settings_stream_torrent),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        magnetExtensions.forEach { (name, authority) ->
                            val isSelected = authority == defaultMagnetExtension
                            TextButton(
                                onClick = { viewModel.setDefaultMagnetExtension(authority); viewModel.setDefaultExtensionPackage(""); viewModel.setDefaultStreamExtension(null); showExtPicker = false },
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
                                            name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        Text(
                                            authority,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExtPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    if (showSubtitleLangPicker) {
        AlertDialog(
            onDismissRequest = { showSubtitleLangPicker = false },
            title = { Text(stringResource(R.string.settings_default_subtitle_lang)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    subtitleLanguages.forEach { lang ->
                        val isSelected = lang == defaultSubtitleLang
                        TextButton(
                            onClick = { viewModel.setDefaultSubtitleLang(lang); showSubtitleLangPicker = false },
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
                                Text(
                                    lang,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSubtitleLangPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}
