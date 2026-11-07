package com.blissless.tensei

// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)

import android.Manifest
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.DetailedAnimeData
import com.blissless.tensei.data.models.EpisodeStreams
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.LocalAnimeEntry
import com.blissless.tensei.data.models.QualityOption
import com.blissless.tensei.data.models.ServerInfo
import com.blissless.tensei.data.models.toDetailedAnimeData
import com.blissless.tensei.data.anime.calculateRecursiveOffset
import com.blissless.tensei.viewmodel.ExtensionsViewModel
import com.blissless.tensei.player.playback.buildTenseiServerList
import com.blissless.tensei.player.playback.pickTenseiSubtitleUrl
import com.blissless.tensei.player.playback.selectPreferredTenseiStream
import com.blissless.tensei.player.stream.PlayerData
import com.blissless.tensei.torrent.StreamEntry
import com.blissless.tensei.torrent.TorrentEngine
import com.blissless.tensei.torrent.TorrentStreamServer
import com.blissless.tensei.ui.screens.airing.ScheduleScreen
import com.blissless.tensei.ui.screens.cast.AllCastScreen
import com.blissless.tensei.ui.screens.cast.AllStaffScreen
import com.blissless.tensei.ui.screens.character.CharacterScreen
import com.blissless.tensei.ui.screens.character.StaffScreen
import com.blissless.tensei.ui.screens.details.DetailedAnimeScreen
import com.blissless.tensei.ui.screens.explore.AnimeScreen
import com.blissless.tensei.ui.screens.explore.MangaScreen
import com.blissless.tensei.ui.screens.home.HomeScreen
import com.blissless.tensei.ui.screens.episode.RichEpisodeList
import com.blissless.tensei.ui.screens.player.PlayerScreen
import com.blissless.tensei.ui.screens.profile.UserProfileScreen
import com.blissless.tensei.ui.screens.relations.AllRecommendationsScreen
import com.blissless.tensei.ui.screens.relations.AllRelationsScreen
import com.blissless.tensei.ui.screens.search.SearchScreen
import com.blissless.tensei.ui.screens.settings.SettingsScreen
import com.blissless.tensei.ui.screens.status.StatusListScreen
import com.blissless.tensei.ui.screens.manga.DetailedMangaScreen
import com.blissless.tensei.ui.screens.manga.MangaReaderScreen
import com.blissless.tensei.ui.screens.manga.MangaAllCharactersScreen
import com.blissless.tensei.ui.screens.manga.MangaAllRelationsScreen
import com.blissless.tensei.ui.screens.manga.MangaAllRecommendationsScreen
import com.blissless.tensei.ui.screens.manga.MangaAllStaffScreen
import com.blissless.tensei.ui.theme.AppTheme
import com.blissless.tensei.ui.theme.ColorMode
import com.blissless.tensei.ui.theme.ThemeMode
import com.blissless.tensei.update.UpdateViewModel
import com.blissless.tensei.util.AppLocale
import com.blissless.tensei.util.toast
import com.blissless.tensei.viewmodel.clearAnimeExtensionStreamCaches
import com.blissless.tensei.viewmodel.clearPlaybackPosition
import com.blissless.tensei.viewmodel.fetchExtensionHosterVideos
import com.blissless.tensei.viewmodel.fetchMagnetForEpisode
import com.blissless.tensei.viewmodel.fetchStreamUrlForEpisode
import com.blissless.tensei.viewmodel.getCacheDataSourceFactory
import com.blissless.tensei.viewmodel.getMagnetForEpisode
import com.blissless.tensei.viewmodel.getPlaybackPosition
import com.blissless.tensei.viewmodel.invalidateStreamCache
import com.blissless.tensei.viewmodel.playEpisodeWithExtension
import com.blissless.tensei.viewmodel.removeFromVideoCache
import com.blissless.tensei.viewmodel.savePlaybackPosition
import com.blissless.tensei.viewmodel.setAutoPlayNextEpisode
import com.blissless.tensei.viewmodel.setSwipeBrightness
import com.blissless.tensei.viewmodel.setSwipeSwap
import com.blissless.tensei.viewmodel.setSwipeVolume
import com.blissless.tensei.viewmodel.updateMangaStatus
import com.blissless.tensei.viewmodel.updateMangaProgress
import com.blissless.tensei.viewmodel.removeMangaTracking
import com.blissless.tensei.viewmodel.dismissMangaContinueReading
import com.blissless.tensei.viewmodel.isMangaFavorited
import com.blissless.tensei.viewmodel.clearMangaDetail
import com.blissless.tensei.viewmodel.selectedExtensionAuthority
import com.blissless.tensei.viewmodel.mangaSourceMethod
import com.blissless.tensei.viewmodel.defaultExternalMangaExtension
import com.blissless.tensei.viewmodel.setSupportsPiP
import com.blissless.tensei.viewmodel.crossProviderCopyPrompt
import com.blissless.tensei.viewmodel.applyCrossProviderCopy
import com.blissless.tensei.viewmodel.dismissCrossProviderCopyPrompt
import com.blissless.tensei.viewmodel.lastExtensionPlaybackError
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.MangaMedia
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import com.blissless.tensei.ui.navigation.MangaOverlay
import com.blissless.tensei.ui.navigation.OverlayState
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.viewmodel.discardLocalChanges
import com.blissless.tensei.viewmodel.addLocalToAniListOnlyNew
import com.blissless.tensei.viewmodel.overwriteAniListWithLocal
import com.blissless.tensei.viewmodel.handleMalAuthAuthCode
import com.blissless.tensei.viewmodel.handleAuthRedirect

@UnstableApi
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val _widgetClicks = kotlinx.coroutines.flow.MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 1)
    val widgetClicks: kotlinx.coroutines.flow.SharedFlow<Int> = _widgetClicks

    var isInPiPMode = mutableStateOf(false)
        private set

    var shouldAutoEnterPiP = false
    var wasFullscreenBeforePiP = false
    var playerViewBounds = android.graphics.Rect()
    private var pipReceiver: android.content.BroadcastReceiver? = null
    private var pipMediaSession: android.media.session.MediaSession? = null

    companion object {
        const val PREFS_NAME = "anilist_prefs"
        const val TOKEN_KEY = "auth_token"
        private const val TAG_TORRENT = "MainActivity.Torrent"
        const val ACTION_PIP_PLAY_PAUSE = "com.blissless.tensei.PIP_PLAY_PAUSE"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    private fun getPipPlayPauseIcon(isPlaying: Boolean): Icon {
        val sizePx = (256 * resources.displayMetrics.density).toInt()
        val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        if (isPlaying) {
            val barWidth = sizePx * 0.16f
            val gap = sizePx * 0.08f
            val barHeight = sizePx * 0.48f
            val top = cy - barHeight / 2f
            val radius = barWidth / 2.5f
            canvas.drawRoundRect(cx - gap / 2f - barWidth, top, cx - gap / 2f, top + barHeight, radius, radius, paint)
            canvas.drawRoundRect(cx + gap / 2f, top, cx + gap / 2f + barWidth, top + barHeight, radius, radius, paint)
        } else {
            val triSize = sizePx * 0.52f
            val path = android.graphics.Path()
            path.moveTo(cx - triSize * 0.42f, cy - triSize / 2f)
            path.lineTo(cx + triSize * 0.42f, cy)
            path.lineTo(cx - triSize * 0.42f, cy + triSize / 2f)
            path.close()
            canvas.drawPath(path, paint)
        }
        return Icon.createWithBitmap(bitmap)
    }

    private fun togglePlayPause() {
        com.blissless.tensei.player.stream.PlayerData.playerEngine?.let { engine ->
            val willBePlaying = !engine.isPlaying
            if (willBePlaying) engine.play() else engine.pause()
            updatePiPPlayPauseIcon(willBePlaying)
        }
    }

    private fun buildPiPParams(autoEnter: Boolean = false, source: String = "?"): PictureInPictureParams {
        val engine = com.blissless.tensei.player.stream.PlayerData.playerEngine
        val isPlaying = engine?.isPlaying == true
        val icon = getPipPlayPauseIcon(isPlaying)
        val intent = Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName)
        val requestCode = if (isPlaying) 0 else 1
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this, requestCode, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
        )
        val action = RemoteAction(icon, getString(R.string.app_pip_play_pause_title), getString(R.string.app_pip_play_pause_description), pendingIntent)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setAutoEnterEnabled(autoEnter)
            .setActions(listOf(action))
        if (!playerViewBounds.isEmpty) {
            builder.setSourceRectHint(playerViewBounds)
        }
        return builder.build()
    }

    private fun ensurePiPMediaSession() {
        if (pipMediaSession == null) {
            pipMediaSession = android.media.session.MediaSession(this, "TenseiPlayer").apply {
                setMetadata(
                    android.media.MediaMetadata.Builder()
                        .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, "Tensei")
                        .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, getString(R.string.app_pip_playing))
                        .build()
                )
                setPlaybackState(
                    android.media.session.PlaybackState.Builder()
                        .setState(
                            android.media.session.PlaybackState.STATE_PLAYING,
                            0, 1f
                        )
                        .build()
                )
                isActive = true
            }
        }
    }

    fun updatePiPParams(autoEnter: Boolean) {
        ensurePiPMediaSession()
        setPictureInPictureParams(buildPiPParams(autoEnter, "updatePiPParams"))
    }

    fun refreshAutoEnterPiPSnapshot() {
        if (!shouldAutoEnterPiP) return
        ensurePiPMediaSession()
        setPictureInPictureParams(buildPiPParams(true, "refreshAutoEnter"))
    }

    fun enterPiPMode() {
        registerPiPReceiver()
        ensurePiPMediaSession()
        enterPictureInPictureMode(buildPiPParams(source = "enterPiPMode"))
    }

    fun releasePiPMediaSession() {
        pipMediaSession?.release()
        pipMediaSession = null
    }

    fun saveFullscreenState(fullscreen: Boolean) {
        wasFullscreenBeforePiP = fullscreen
    }

    private fun registerPiPReceiver() {
        if (pipReceiver != null) return
        pipReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: Intent) {
                if (intent.action == ACTION_PIP_PLAY_PAUSE) {
                    togglePlayPause()
                }
            }
        }
        val filter = android.content.IntentFilter(ACTION_PIP_PLAY_PAUSE)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipReceiver, filter)
        }
    }

    private fun unregisterPiPReceiver() {
        pipReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        pipReceiver = null
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldAutoEnterPiP) {
            enterPiPMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPiPMode.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            registerPiPReceiver()
        } else {
            unregisterPiPReceiver()
        }
    }

    fun updatePiPPlayPauseIcon(isPlaying: Boolean = com.blissless.tensei.player.stream.PlayerData.playerEngine?.isPlaying == true) {
        val icon = getPipPlayPauseIcon(isPlaying)
        val intent = Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName)
        val requestCode = if (isPlaying) 0 else 1
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this, requestCode, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
        )
        val action = RemoteAction(icon, getString(R.string.app_pip_play_pause_title), getString(R.string.app_pip_play_pause_description), pendingIntent)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(action))
            .build()
        setPictureInPictureParams(params)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // DEBUG: log any uncaught crash to logcat so "screen just closes" issues are traceable.
        val previousCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("AppCrash", "UNCAUGHT EXCEPTION on thread '${thread.name}':", throwable)
            previousCrashHandler?.uncaughtException(thread, throwable)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        initDiscordSdk()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val hasToken = prefs.getString(TOKEN_KEY, null) != null
        val savedToken = prefs.getString(TOKEN_KEY, null)

        mainViewModel.init(applicationContext, hasToken)

        intent.getIntExtra("widget_anime_id", 0).let { if (it > 0) _widgetClicks.tryEmit(it) }
        if (intent.getBooleanExtra("open_extensions", false)) {
            mainViewModel.requestOpenExtensions()
        }
        handleAuthCallback(intent)

        // Auto-connect Discord Rich Presence if previously enabled
        if (mainViewModel.discordRichPresence.value) {
            com.blissless.tensei.discord.DiscordRichPresence.connect()
        }

        setContent {
            val themeModeStr by mainViewModel.themeMode.collectAsState()
            val isOled by mainViewModel.isOled.collectAsState()
            val disableMaterialColors by mainViewModel.disableMaterialColors.collectAsState()
            val colorMode by mainViewModel.colorMode.collectAsState()
            val resolvedColorMode = remember(colorMode) { ColorMode.fromValue(colorMode) }
            val showStatusColors by mainViewModel.showStatusColors.collectAsState()
            val showAnimeCardButtons by mainViewModel.showAnimeCardButtons.collectAsState()
            val showMangaCardButtons by mainViewModel.showMangaCardButtons.collectAsState()
            val showMangaStatusColors by mainViewModel.showMangaStatusColors.collectAsState()
            val preferEnglishTitles by mainViewModel.preferEnglishTitles.collectAsState()
            val advancedSearch by mainViewModel.advancedSearch.collectAsState()

            var isLoggedIn by remember { mutableStateOf(savedToken != null) }
            val token by mainViewModel.authToken.collectAsState()
            val loginProvider by mainViewModel.loginProvider.collectAsState()
            var showLocalSyncDialog by remember { mutableStateOf(false) }
            val localAnimeStatus by mainViewModel.localAnimeStatus.collectAsState()
            val crossCopyPrompt by mainViewModel.crossProviderCopyPrompt.collectAsState()
            
            LaunchedEffect(token, loginProvider) {
                val isAnyLoggedIn = token != null || loginProvider != LoginProvider.NONE
                if (isAnyLoggedIn && !isLoggedIn && localAnimeStatus.isNotEmpty()) {
                    showLocalSyncDialog = true
                }
                isLoggedIn = isAnyLoggedIn
            }

            val maxPerformance by mainViewModel.maxPerformance.collectAsState()

            LaunchedEffect(maxPerformance) {
                if (maxPerformance) {
                    enableHighRefreshRate()
                }
            }

            val notifPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            
            val toastContext = LocalContext.current
            LaunchedEffect(Unit) {
                mainViewModel.toastMessage.collect { message ->
                    toastContext.toast(message)
                }
            }
            
            LaunchedEffect(Unit) {
                mainViewModel.logoutEvent.collect {
                    (toastContext as? MainActivity)?.resetAuthFlags()
                }
            }

            if (showLocalSyncDialog) {
                com.blissless.tensei.ui.components.LocalSyncDialog(
                    localAnimeCount = localAnimeStatus.size,
                    onDismiss = { showLocalSyncDialog = false },
                    onDiscard = {
                        showLocalSyncDialog = false
                        mainViewModel.discardLocalChanges()
                    },
                    onAddNewOnly = {
                        showLocalSyncDialog = false
                        mainViewModel.addLocalToAniListOnlyNew()
                    },
                    onOverwrite = {
                        showLocalSyncDialog = false
                        mainViewModel.overwriteAniListWithLocal()
                    },
                )
            }

            val dialogThemeMode = remember(themeModeStr) { ThemeMode.fromValue(themeModeStr) }
            AppTheme(themeMode = dialogThemeMode, colorMode = resolvedColorMode) {
            crossCopyPrompt?.takeIf { it.visible }?.let { prompt ->
                com.blissless.tensei.ui.components.CrossProviderSyncDialog(
                    aniToMalAnime = prompt.aniToMalAnime,
                    aniToMalManga = prompt.aniToMalManga,
                    malToAniAnime = prompt.malToAniAnime,
                    malToAniManga = prompt.malToAniManga,
                    useMonochrome = resolvedColorMode == ColorMode.MONOCHROME,
                    isOled = isOled,
                    onDismiss = { mainViewModel.dismissCrossProviderCopyPrompt() },
                    onSyncAniListToMal = { mainViewModel.applyCrossProviderCopy(toMal = true) },
                    onSyncMalToAniList = { mainViewModel.applyCrossProviderCopy(toMal = false) }
                )
            }

            } // end AppTheme wrapping the cross-provider dialogs

            val themeMode = remember(themeModeStr) { ThemeMode.fromValue(themeModeStr) }
            AppTheme(themeMode = themeMode, colorMode = resolvedColorMode) {
                MainScreen(
                    viewModel = mainViewModel,
                    isOled = isOled,
                    showStatusColors = showStatusColors,
                    showAnimeCardButtons = showAnimeCardButtons,
                    showMangaCardButtons = showMangaCardButtons,
                    showMangaStatusColors = showMangaStatusColors,
                    preferEnglishTitles = preferEnglishTitles,
                    advancedSearch = advancedSearch,
                    isLoggedIn = isLoggedIn
                )
            }
        }
    }

    /**
     * Wires this activity into the Discord Social SDK (required for Rich Presence)
     * when the SDK is present. The SDK is proprietary and intentionally not
     * committed to the repository — when app/libs/discord_partner_sdk.aar is
     * missing the app still builds and runs, Discord Rich Presence is simply
     * disabled. See app/build.gradle.kts and the "Building" section in README.md.
     */
    private fun initDiscordSdk() {
        try {
            Class.forName("com.discord.socialsdk.DiscordSocialSdkInit")
                .getMethod("setEngineActivity", android.app.Activity::class.java)
                .invoke(null, this)
        } catch (_: ReflectiveOperationException) {
            // Discord Social SDK not on the classpath — Rich Presence stays off.
        }
    }

    /**
     * Enable the highest supported refresh rate and request high frame rate for animations.
     */
    private fun enableHighRefreshRate() {
        try {
            // Step 1: Set the preferred display mode to the highest refresh rate available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display?.let { disp ->
                    val modes = disp.supportedModes
                    var bestMode: android.view.Display.Mode? = null
                    var highestRefreshRate = 60f

                    modes?.forEach { mode ->
                        if (mode.refreshRate > highestRefreshRate) {
                            highestRefreshRate = mode.refreshRate
                            bestMode = mode
                        }
                    }

                    bestMode?.let { mode ->
                        val params = window.attributes
                        params.preferredDisplayModeId = mode.modeId
                        window.attributes = params
                    }
                }
            }

            // Step 2: Request high frame rate for the window
            // This tells the system we want animations to run at the display's native refresh rate
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.attributes = window.attributes.apply {
                    // Request the surface to render at high frame rate
                    preferredRefreshRate = 120f // Request up to 120Hz
                }
                
                // Try to set frame rate directly on the decor view's surface (API 30+)
                try {
                    val decorView = window.decorView
                    decorView.viewTreeObserver.addOnPreDrawListener {
                        // Keep requesting high frame rate
                        decorView.postInvalidateOnAnimation()
                        true
                    }
                } catch (_: Exception) {
                    // Ignore - this is optional enhancement
                }
            }

        } catch (e: Exception) {
            // Gracefully handle any errors - fallback to system default
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent)
        intent.getIntExtra("widget_anime_id", 0).let { if (it > 0) _widgetClicks.tryEmit(it) }
        if (intent.getBooleanExtra("open_extensions", false)) {
            mainViewModel.requestOpenExtensions()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for any pending auth callback when returning to the app
        handleAuthCallback(intent)
    }

    private var isMalAuthHandled = false
    private var isAniListAuthHandled = false
    
    private fun handleAuthCallback(intent: Intent?) {
        if (intent == null) {
            return
        }

        val uriString = intent.dataString ?: return

        // Check if it's MAL auth (contains code= parameter)
        if (!isMalAuthHandled && uriString.contains("code=") && uriString.startsWith("animescraper://success")) {
            isMalAuthHandled = true
            mainViewModel.handleMalAuthAuthCode(uriString)
        }
        // Check if it's AniList auth (contains access_token=)
        else if (!isAniListAuthHandled && uriString.contains("access_token=") && uriString.startsWith("animescraper://success")) {
            isAniListAuthHandled = true
            mainViewModel.handleAuthRedirect(intent)
        }
    }
    
    fun resetAuthFlags() {
        isMalAuthHandled = false
        isAniListAuthHandled = false
    }
}
