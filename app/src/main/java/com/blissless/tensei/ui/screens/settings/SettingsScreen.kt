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
import com.blissless.tensei.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.ui.components.appIconDrawable
import com.blissless.tensei.ui.theme.ColorMode
import com.blissless.tensei.ui.theme.ThemeMode
import com.blissless.tensei.api.myanimelist.LoginProvider
import com.blissless.tensei.extensions.ExtensionsScreen
import com.blissless.tensei.extensions.ExtensionsViewModel
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

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    autoSkipOpening: Boolean = false,
    autoSkipEnding: Boolean = false,
    autoPlayNextEpisode: Boolean = true,
    preferredCategory: String = "sub",
    initialGroup: String? = null,
    onBack: () -> Unit = {}
) {
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialGroup) {
        if (initialGroup != null) {
            selectedGroup = initialGroup
        }
    }

    LaunchedEffect(selectedGroup) {
        viewModel.setHideNavbar(selectedGroup != null)
    }

    val appIcon by viewModel.appIcon.collectAsState()

    val groups = listOf(
        SettingsGroup("appearance", stringResource(R.string.settings_group_appearance), stringResource(R.string.settings_group_appearance_desc), Icons.Default.Palette),
        SettingsGroup("account", stringResource(R.string.settings_group_account), stringResource(R.string.settings_group_account_desc), Icons.Default.Person),
        SettingsGroup("general", stringResource(R.string.settings_group_general), stringResource(R.string.settings_group_general_desc), Icons.Default.Settings),
        SettingsGroup("stream", stringResource(R.string.settings_group_stream), stringResource(R.string.settings_group_stream_desc), Icons.Default.PlayArrow),
        SettingsGroup("player", stringResource(R.string.settings_group_player), stringResource(R.string.settings_group_player_desc), Icons.Default.Subscriptions),
        SettingsGroup("reader", stringResource(R.string.settings_group_reader), stringResource(R.string.settings_group_reader_desc), Icons.Default.Bookmark),
        SettingsGroup("extensions", stringResource(R.string.settings_group_extensions), stringResource(R.string.settings_group_extensions_desc), Icons.Default.Extension),
        SettingsGroup("cache", stringResource(R.string.settings_group_cache), stringResource(R.string.settings_group_cache_desc), Icons.Default.Storage),
        SettingsGroup("about", stringResource(R.string.settings_group_about), stringResource(R.string.settings_group_about_desc), Icons.Default.Info)
    )

    AnimatedContent(
        targetState = selectedGroup,
        transitionSpec = {
            if (targetState == null) {
                (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { -it / 8 })
                    .togetherWith(fadeOut(animationSpec = tween(220)) + slideOutHorizontally(animationSpec = tween(220)) { it / 8 })
            } else {
                (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(220)) + slideOutHorizontally(animationSpec = tween(220)))
            }
        },
        label = "settingsNavigation"
    ) { targetGroup ->
        if (targetGroup == null) {
            SettingsLandingPage(
                groups = groups,
                appIcon = appIcon,
                onBack = onBack,
                onGroupClick = { selectedGroup = it }
            )
        } else {
            BackHandler { selectedGroup = null }
            when (targetGroup) {
                "appearance" -> AppearanceSettingsPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "account" -> AccountSettingsPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "general" -> GeneralSettingsPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "stream" -> StreamSettingsPage(viewModel = viewModel,
                    preferredCategory = preferredCategory, onNavigateToExtensions = { selectedGroup = "extensions" }, onBack = { selectedGroup = null })
                "player" -> PlayerSettingsPage(viewModel = viewModel, autoSkipOpening = autoSkipOpening, autoSkipEnding = autoSkipEnding, autoPlayNextEpisode = autoPlayNextEpisode, onBack = { selectedGroup = null })
                "reader" -> ReaderSettingsPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "extensions" -> ExtensionsSettingsPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "cache" -> CacheSettingsPage(viewModel = viewModel, context = LocalContext.current, onBack = { selectedGroup = null })
                "about" -> AboutSettingsPage(viewModel = viewModel, onBack = { selectedGroup = null })
            }
        }
    }
}



// â”€â”€â”€ Account â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AccountSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var showAniListLogoutConfirmation by remember { mutableStateOf(false) }
    var showMalLogoutConfirmation by remember { mutableStateOf(false) }
    val loginProvider by viewModel.loginProvider.collectAsState(initial = LoginProvider.NONE)
    val trackingPercentage by viewModel.trackingPercentage.collectAsState(initial = 85)
    val mangaSyncThreshold by viewModel.mangaSyncThreshold.collectAsState()
    val discordRichPresence by viewModel.discordRichPresence.collectAsState(initial = false)

    val userName by viewModel.userName.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val malUsername by viewModel.malUsernameFlow.collectAsState()
    val malAvatar by viewModel.malAvatar.collectAsState()

    SettingsPageScaffold(title = stringResource(R.string.settings_group_account), onBack = onBack) {
        val anilistLogged = loginProvider == LoginProvider.ANILIST || loginProvider == LoginProvider.BOTH
        val malLogged = loginProvider == LoginProvider.MAL || loginProvider == LoginProvider.BOTH

        if (anilistLogged) {
            SectionHeader("ANILIST")
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (userAvatar != null) {
                        AsyncImage(
                            model = userAvatar,
                            contentDescription = stringResource(R.string.settings_x_cd_anilist_avatar),
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        }
                    }
                    Column {
                        Text(userName ?: stringResource(R.string.settings_logged_in), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.settings_via_anilist), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAniListLogoutConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.settings_logout_anilist), color = Color.White)
                }
            }

            // When only AniList is logged in, offer adding MAL so the user can reach BOTH.
            if (loginProvider == LoginProvider.ANILIST) {
                Spacer(modifier = Modifier.height(8.dp))
                MalLoginButton(viewModel)
            }
        }

        if (malLogged) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("MYANIMELIST")
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (malAvatar != null) {
                        AsyncImage(
                            model = malAvatar,
                            contentDescription = stringResource(R.string.settings_x_cd_mal_avatar),
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
                        }
                    }
                    Column {
                        Text(malUsername ?: stringResource(R.string.settings_logged_in), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.settings_via_mal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showMalLogoutConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.settings_logout_mal), color = Color.White)
                }
            }

            // When only MAL is logged in, offer adding AniList so the user can reach BOTH.
            if (loginProvider == LoginProvider.MAL) {
                Spacer(modifier = Modifier.height(8.dp))
                AniListLoginButton(viewModel)
            }
        }

        if (loginProvider == LoginProvider.NONE) {
            SectionHeader(stringResource(R.string.settings_section_sign_in))
            SettingsCard {
                Spacer(modifier = Modifier.height(4.dp))
                AniListLoginButton(viewModel)
                Spacer(modifier = Modifier.height(8.dp))
                MalLoginButton(viewModel)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(stringResource(R.string.settings_section_tracking_sync))
        SettingsCard {
            SettingsSliderRow(
                title = stringResource(R.string.settings_tracking_episode),
                description = stringResource(R.string.settings_tracking_episode_desc),
                value = trackingPercentage.toFloat(),
                valueRange = 50f..100f,
                valueLabel = stringResource(R.string.settings_value_percent, trackingPercentage),
                onValueChange = { viewModel.setTrackingPercentage((round(it / 5f) * 5f).toInt()) },
                minLabel = stringResource(R.string.settings_value_percent, 50),
                maxLabel = stringResource(R.string.settings_value_percent, 100)
            )
        }
        SettingsCard {
            SettingsSliderRow(
                title = stringResource(R.string.settings_tracking_manga),
                description = stringResource(R.string.settings_tracking_manga_desc),
                value = mangaSyncThreshold.toFloat(),
                valueRange = 75f..100f,
                valueLabel = stringResource(R.string.settings_value_percent, mangaSyncThreshold),
                onValueChange = { viewModel.setMangaSyncThreshold(it.toInt()) },
                minLabel = stringResource(R.string.settings_value_percent, 75),
                maxLabel = stringResource(R.string.settings_value_percent, 100)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader("DISCORD")
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_discord_title),
                description = stringResource(R.string.settings_discord_desc),
                checked = discordRichPresence,
                onCheckedChange = { viewModel.setDiscordRichPresence(it) }
            )
        }
    }

    if (showAniListLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showAniListLogoutConfirmation = false },
            title = { Text(stringResource(R.string.settings_logout_anilist)) },
            text = { Text(stringResource(R.string.settings_logout_anilist_msg)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.logoutProvider(LoginProvider.ANILIST); showAniListLogoutConfirmation = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.settings_logout)) }
            },
            dismissButton = {
                TextButton(onClick = { showAniListLogoutConfirmation = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showMalLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showMalLogoutConfirmation = false },
            title = { Text(stringResource(R.string.settings_logout_mal)) },
            text = { Text(stringResource(R.string.settings_logout_mal_msg)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.logoutProvider(LoginProvider.MAL); showMalLogoutConfirmation = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.settings_logout)) }
            },
            dismissButton = {
                TextButton(onClick = { showMalLogoutConfirmation = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun AniListLoginButton(viewModel: MainViewModel) {
    Button(
        onClick = { viewModel.loginWithAniList() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(com.blissless.tensei.network.Endpoints.AniList.FAVICON)
                .crossfade(true).build(),
            contentDescription = stringResource(R.string.settings_x_cd_anilist),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(stringResource(R.string.settings_login_anilist))
    }
}

@Composable
private fun MalLoginButton(viewModel: MainViewModel) {
    Button(
        onClick = { viewModel.loginWithMal() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(com.blissless.tensei.network.Endpoints.Mal.FAVICON)
                .crossfade(true).build(),
            contentDescription = stringResource(R.string.settings_x_cd_mal),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(stringResource(R.string.settings_login_mal))
    }
}

// â”€â”€â”€ Appearance â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AppearanceSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val showStatusColorsState by viewModel.showStatusColors.collectAsState(initial = true)
    val showAnimeCardButtons by viewModel.showAnimeCardButtons.collectAsState(initial = true)
    val showMangaCardButtons by viewModel.showMangaCardButtons.collectAsState(initial = true)
    val showMangaStatusColors by viewModel.showMangaStatusColors.collectAsState(initial = true)

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
        }
    }
}

// â”€â”€â”€ General â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun GeneralSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val startupScreenState by viewModel.startupScreen.collectAsState()
    val preferEnglishTitles by viewModel.preferEnglishTitles.collectAsState(initial = true)
    val appLanguage by viewModel.appLanguage.collectAsState()
    var showLanguagePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    SettingsPageScaffold(title = stringResource(R.string.settings_group_general), onBack = onBack) {
        SectionHeader(stringResource(R.string.settings_language_header))
        Text(
            text = stringResource(R.string.settings_language_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
        )
        SettingsCard {
            ClickableSettingsRow(
                onClick = { showLanguagePicker = true },
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language_title),
                subtitle = when (appLanguage) {
                    "en" -> stringResource(R.string.settings_language_en)
                    "de" -> stringResource(R.string.settings_language_de)
                    else -> stringResource(R.string.settings_language_system)
                }
            )
        }

        SectionHeader(stringResource(R.string.settings_section_launch))
        Text(
            text = stringResource(R.string.settings_launch_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
        )
        SettingsCard {
            SettingsRadioItem(
                selected = startupScreenState == 0,
                onClick = { viewModel.setStartupScreen(0) },
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.settings_startup_schedule),
                description = stringResource(R.string.settings_startup_schedule_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = startupScreenState == 1,
                onClick = { viewModel.setStartupScreen(1) },
                icon = Icons.Default.Home,
                title = stringResource(R.string.settings_startup_home),
                description = stringResource(R.string.settings_startup_home_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = startupScreenState == 2,
                onClick = { viewModel.setStartupScreen(2) },
                icon = Icons.Default.Explore,
                title = stringResource(R.string.settings_startup_anime),
                description = stringResource(R.string.settings_startup_anime_desc)
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = startupScreenState == 3,
                onClick = { viewModel.setStartupScreen(3) },
                icon = Icons.Default.MenuBook,
                title = stringResource(R.string.settings_startup_manga),
                description = stringResource(R.string.settings_startup_manga_desc)
            )
        }
        SectionHeader(stringResource(R.string.settings_section_content))
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_english_titles),
                description = stringResource(R.string.settings_english_titles_desc),
                checked = preferEnglishTitles,
                onCheckedChange = { viewModel.setPreferEnglishTitles(it) }
            )
        }

        SectionHeader(stringResource(R.string.settings_section_performance))
        SettingsCard {
            val maxPerformanceState by viewModel.maxPerformance.collectAsState(initial = false)
            SettingsToggle(
                title = stringResource(R.string.settings_max_performance),
                description = stringResource(R.string.settings_max_performance_desc),
                checked = maxPerformanceState,
                onCheckedChange = { viewModel.setMaxPerformance(it) }
            )
        }
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(stringResource(R.string.settings_language_title)) },
            text = {
                Column {
                    listOf(
                        AppLocale.SYSTEM to stringResource(R.string.settings_language_system),
                        "en" to "English",
                        "de" to "Deutsch"
                    ).forEach { (value, label) ->
                        val isSelected = value == appLanguage
                        TextButton(
                            onClick = {
                                viewModel.setAppLanguage(value)
                                AppLocale.onChange(context, value)
                                showLanguagePicker = false
                            },
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
                                    label,
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
            confirmButton = { TextButton(onClick = { showLanguagePicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

// â”€â”€â”€ Stream Settings â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun StreamSettingsPage(
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

// â”€â”€â”€ Player Settings â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PlayerSettingsPage(
    viewModel: MainViewModel,
    autoSkipOpening: Boolean,
    autoSkipEnding: Boolean,
    autoPlayNextEpisode: Boolean,
    onBack: () -> Unit
) {
    val forwardSkipSeconds by viewModel.forwardSkipSeconds.collectAsState(initial = 10)
    val backwardSkipSeconds by viewModel.backwardSkipSeconds.collectAsState(initial = 10)
    val swipeVolume by viewModel.swipeVolume.collectAsState(initial = false)
    val swipeBrightness by viewModel.swipeBrightness.collectAsState(initial = false)
    val swipeSwap by viewModel.swipeSwap.collectAsState(initial = false)
    val supportsPiP by viewModel.supportsPiP.collectAsState(initial = false)
    val context = LocalContext.current

    SettingsPageScaffold(title = stringResource(R.string.settings_group_player), onBack = onBack) {
        SectionHeader(stringResource(R.string.settings_section_skip_controls))
        SettingsCard {
            SettingsSliderRow(
                title = stringResource(R.string.settings_skip_forward),
                description = stringResource(R.string.settings_skip_forward_desc),
                value = forwardSkipSeconds.toFloat(),
                valueRange = 5f..30f,
                valueLabel = stringResource(R.string.settings_value_seconds, forwardSkipSeconds),
                onValueChange = { viewModel.setForwardSkipSeconds((round(it / 5f) * 5f).toInt()) },
                minLabel = stringResource(R.string.settings_value_seconds, 5),
                maxLabel = stringResource(R.string.settings_value_seconds, 30),
                leadingIcon = Icons.Default.FastForward
            )
        }
        SettingsCard {
            SettingsSliderRow(
                title = stringResource(R.string.settings_skip_backward),
                description = stringResource(R.string.settings_skip_backward_desc),
                value = backwardSkipSeconds.toFloat(),
                valueRange = 5f..30f,
                valueLabel = stringResource(R.string.settings_value_seconds, backwardSkipSeconds),
                onValueChange = { viewModel.setBackwardSkipSeconds((round(it / 5f) * 5f).toInt()) },
                minLabel = stringResource(R.string.settings_value_seconds, 5),
                maxLabel = stringResource(R.string.settings_value_seconds, 30),
                leadingIcon = Icons.Default.FastRewind
            )
        }

        SectionHeader(stringResource(R.string.settings_section_automation))
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_auto_skip_opening),
                description = stringResource(R.string.settings_auto_skip_opening_desc),
                checked = autoSkipOpening,
                onCheckedChange = { viewModel.setAutoSkipOpening(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_auto_skip_ending),
                description = stringResource(R.string.settings_auto_skip_ending_desc),
                checked = autoSkipEnding,
                onCheckedChange = { viewModel.setAutoSkipEnding(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_auto_play_next),
                description = stringResource(R.string.settings_auto_play_next_desc),
                checked = autoPlayNextEpisode,
                onCheckedChange = { viewModel.setAutoPlayNextEpisode(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_pip),
                description = stringResource(R.string.settings_pip_desc),
                checked = supportsPiP,
                onCheckedChange = { viewModel.setSupportsPiP(it) }
            )
        }

        SectionHeader(stringResource(R.string.settings_section_swipe))
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_swipe_volume),
                description = stringResource(R.string.settings_swipe_volume_desc),
                checked = swipeVolume,
                onCheckedChange = { viewModel.setSwipeVolume(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_swipe_brightness),
                description = stringResource(R.string.settings_swipe_brightness_desc),
                checked = swipeBrightness,
                onCheckedChange = { viewModel.setSwipeBrightness(it) }
            )
            SettingsToggle(
                title = stringResource(R.string.settings_swipe_swap),
                description = stringResource(R.string.settings_swipe_swap_desc),
                checked = swipeSwap,
                onCheckedChange = { viewModel.setSwipeSwap(it) }
            )
        }
    }
}

// â”€â”€â”€ Reader Settings â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ReaderSettingsPage(
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

// â”€â”€â”€ Cache Management â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun CacheSettingsPage(
    viewModel: MainViewModel,
    context: Context,
    onBack: () -> Unit
) {
    var videoCacheSize by remember { mutableLongStateOf(0L) }
    var showClearCacheConfirmation by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        videoCacheSize = viewModel.getVideoCacheSize(context)
    }

    SettingsPageScaffold(title = stringResource(R.string.settings_group_cache), onBack = onBack) {
        SectionHeader(stringResource(R.string.settings_section_storage))
        SettingsCard {
            CacheRow(
                icon = Icons.Default.PlayArrow,
                title = stringResource(R.string.settings_video_cache),
                size = formatFileSize(videoCacheSize),
                onClear = { showClearCacheConfirmation = "video" }
            )
        }
    }

    if (showClearCacheConfirmation != null) {
        val isVideo = showClearCacheConfirmation == "video"
        if (isVideo) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirmation = null },
                title = { Text(stringResource(R.string.settings_clear_video_cache)) },
                text = {
                    Text(stringResource(R.string.settings_clear_video_cache_msg))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearNonEssentialCaches(context)
                            videoCacheSize = 0L
                            showClearCacheConfirmation = null
                            context.toast(context.getString(R.string.settings_cache_cleared))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.common_clear)) }
                },
                dismissButton = { TextButton(onClick = { showClearCacheConfirmation = null }) { Text(stringResource(R.string.common_cancel)) } }
            )
        }
    }
}

@Composable
private fun CacheRow(
    icon: ImageVector,
    title: String,
    size: String,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        Button(
            onClick = onClear,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) { Text(stringResource(R.string.common_clear)) }
    }
}

// â”€â”€â”€ Extensions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ExtensionsSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val extViewModel: ExtensionsViewModel = viewModel()
    var selectedRepoUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val mangaManager = remember { MangaExtensionManager.get(context) }

    SettingsPageScaffold(
        title = stringResource(R.string.settings_group_extensions),
        onBack = onBack,
        scrollable = false,
        navigationIcon = if (selectedRepoUrl != null) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
        actions = {
            IconButton(onClick = {
                extViewModel.checkForUpdatesNow()
                mangaManager.refresh()
                mangaManager.fetchAvailable()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.settings_refresh))
            }
        }
    ) {
        ExtensionsScreen(
            viewModel = extViewModel,
            selectedRepoUrl = selectedRepoUrl,
            onSelectRepo = { selectedRepoUrl = it }
        )
    }
}

// â”€â”€â”€ About â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AboutSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val updateViewModel: UpdateViewModel = viewModel()
    val updateState by updateViewModel.uiState.collectAsState()
    val checkOnStart by viewModel.checkUpdatesOnStart.collectAsState()
    val appIcon by viewModel.appIcon.collectAsState()
    val packageInfo = remember {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val currentVersion = packageInfo.versionName ?: ""

    SettingsPageScaffold(title = stringResource(R.string.settings_group_about), onBack = onBack) {
        SectionHeader(stringResource(R.string.settings_section_app))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = appIconDrawable(appIcon),
                        contentDescription = stringResource(R.string.settings_a11y_app),
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tensei", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    @Suppress("DEPRECATION")
                    Text(
                        stringResource(R.string.settings_version, currentVersion, packageInfo.versionCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        SectionHeader(stringResource(R.string.settings_section_updates))
        SettingsCard {
            val release = updateState.release
            val isChecking = updateState.isChecking
            val isDownloading = updateState.isDownloading
            val error = updateState.error
            val statusText = when {
                isChecking -> stringResource(R.string.settings_update_checking)
                isDownloading -> stringResource(R.string.settings_update_downloading, (updateState.downloadProgress * 100).toInt())
                error != null -> error
                release != null -> {
                    val tag = release.tagName.removePrefix("v")
                    if (compareVersions(tag, currentVersion) > 0) stringResource(R.string.settings_update_available, tag)
                    else stringResource(R.string.settings_update_uptodate, currentVersion)
                }
                else -> stringResource(R.string.settings_update_tap)
            }
            val hasUpdate = release != null && compareVersions(
                release.tagName.removePrefix("v"), currentVersion
            ) > 0

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_check_updates), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (isDownloading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { updateState.downloadProgress },
                            modifier = Modifier.width(80.dp).height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.settings_value_percent, (updateState.downloadProgress * 100).toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (hasUpdate) updateViewModel.downloadUpdate()
                            else updateViewModel.checkForUpdates()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (hasUpdate) stringResource(R.string.settings_update_action) else stringResource(R.string.settings_check_action),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        SectionHeader(stringResource(R.string.settings_section_automation))
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_check_updates_start),
                description = stringResource(R.string.settings_check_updates_start_desc),
                checked = checkOnStart,
                onCheckedChange = { viewModel.setCheckUpdatesOnStart(it) }
            )
        }

        val autoUpdateExts by viewModel.autoUpdateExtensions.collectAsState()
        SettingsCard {
            SettingsToggle(
                title = stringResource(R.string.settings_auto_update_exts),
                description = stringResource(R.string.settings_auto_update_exts_desc),
                checked = autoUpdateExts,
                onCheckedChange = { viewModel.setAutoUpdateExtensions(it) }
            )
        }

        SectionHeader(stringResource(R.string.settings_section_links))
        SettingsCard {
            val githubUrl = com.blissless.tensei.network.Endpoints.GitHub.TENSEI_REPO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, githubUrl.toUri())
                            context.startActivity(intent)
                        }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_github_repo), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(githubUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                Text(
                    stringResource(R.string.settings_open),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

