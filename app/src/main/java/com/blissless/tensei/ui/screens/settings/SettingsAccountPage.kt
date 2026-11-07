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
import com.blissless.tensei.viewmodel.logoutProvider
import com.blissless.tensei.viewmodel.loginWithAniList
import com.blissless.tensei.viewmodel.loginWithMal

// Account settings: AniList / MAL login cards, logout, sync threshold.





// â”€â”€â”€ Account â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun AccountSettingsPage(
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
