package com.blissless.tensei.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.blissless.tensei.data.models.LocalAnimeEntry
import com.blissless.tensei.data.models.StoredFavorite
import com.blissless.tensei.data.models.SubtitleProfileData
import com.blissless.tensei.data.models.SubtitleSettings
import com.blissless.tensei.ui.theme.ColorMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import androidx.core.content.edit
import com.blissless.tensei.data.anime.json

/**
 * Manages user preferences and settings
 */
class UserPreferences(context: Context) {

    companion object {
        internal const val PREFS_NAME = "anilist_prefs"
        internal const val TOKEN_KEY = "auth_token"

        // Preference keys
        internal const val KEY_THEME_MODE = "theme_mode"
        internal const val KEY_DISABLE_MATERIAL_COLORS = "disable_material_colors"
        internal const val KEY_COLOR_MODE = "color_mode"
        internal const val KEY_PREFERRED_CATEGORY = "preferred_category"
        internal const val KEY_SHOW_STATUS_COLORS = "show_status_colors"
        internal const val KEY_SHOW_ANIME_CARD_BUTTONS = "show_anime_card_buttons"
        internal const val KEY_SHOW_MANGA_CARD_BUTTONS = "show_manga_card_buttons"
        internal const val KEY_SHOW_MANGA_STATUS_COLORS = "show_manga_status_colors"
        internal const val KEY_PREFER_ENGLISH_TITLES = "prefer_english_titles"
        internal const val KEY_ADVANCED_SEARCH = "advanced_search"
        internal const val KEY_TRACKING_PERCENTAGE = "tracking_percentage"
        internal const val KEY_FORWARD_SKIP_SECONDS = "forward_skip_seconds"
        internal const val KEY_BACKWARD_SKIP_SECONDS = "backward_skip_seconds"
        internal const val KEY_HIDE_NAVBAR_TEXT = "hide_navbar_text"
        internal const val KEY_SIMPLIFY_ANIME_DETAILS = "simplify_anime_details"
        internal const val KEY_AUTO_SKIP_OPENING = "auto_skip_opening"
        internal const val KEY_AUTO_SKIP_ENDING = "auto_skip_ending"
        internal const val KEY_AUTO_PLAY_NEXT_EPISODE = "auto_play_next_episode"
        internal const val KEY_ENABLE_THUMBNAIL_PREVIEW = "enable_thumbnail_preview"
        internal const val KEY_LOCAL_FAVORITES_V2 = "local_favorites_v2"
        internal const val KEY_LOCAL_FAVORITES = "local_favorites"
        internal const val KEY_ANILIST_FAVORITES = "anilist_favorites"
        internal const val KEY_PREFERRED_SCRAPER = "preferred_scraper"
        internal const val KEY_DEFAULT_EXTENSION = "default_extension_package"
        internal const val KEY_STREAM_PROVIDER = "stream_provider"
        internal const val KEY_STARTUP_SCREEN = "startup_screen"
        internal const val KEY_APP_LANGUAGE = "app_language"
        internal const val KEY_BUFFER_AHEAD_SECONDS = "buffer_ahead_seconds"
        internal const val KEY_BUFFER_SIZE_MB = "buffer_size_mb"
        internal const val KEY_SHOW_BUFFER_INDICATOR = "show_buffer_indicator"
        internal const val KEY_LAST_HOME_REFRESH = "last_home_refresh_time"
        internal const val KEY_LAST_EXPLORE_REFRESH = "last_explore_refresh_time"
        internal const val KEY_LAST_SCHEDULE_REFRESH = "last_schedule_refresh_time"
        internal const val KEY_LOCAL_ANIME_STATUS = "local_anime_status"
        internal const val KEY_DEFAULT_SUBTITLE_LANG = "default_subtitle_lang"
        internal const val KEY_MAL_FAVORITES = "mal_favorites"
        internal const val KEY_MAL_MANGA_FAVORITES = "mal_manga_favorites"
        internal const val KEY_CROSS_PROVIDER_COPY_DONE = "cross_provider_copy_done"
        internal const val KEY_MAL_AS_MAIN_PROVIDER = "mal_as_main_provider"
        internal const val KEY_CHECK_UPDATES_ON_START = "check_updates_on_start"
        internal const val KEY_SWIPE_VOLUME = "swipe_volume"
        internal const val KEY_SWIPE_BRIGHTNESS = "swipe_brightness"
        internal const val KEY_SWIPE_SWAP = "swipe_swap"
        internal const val KEY_AUTO_UPDATE_EXTENSIONS = "auto_update_extensions"
        internal const val KEY_HIDE_EPISODE_DESCRIPTION = "hide_episode_description"
        internal const val KEY_SUBTITLE_ACTIVE_PROFILE = "subtitle_active_profile"
        internal const val KEY_SUBTITLE_PROFILES = "subtitle_profiles"
        internal const val KEY_STREAM_METHOD = "stream_method"
        internal const val KEY_DEFAULT_MAGNET_EXTENSION = "default_magnet_extension"
        internal const val KEY_DEFAULT_STREAM_EXTENSION = "default_stream_extension"
        internal const val KEY_MANGA_READER_MODE = "manga_reader_mode"
        internal const val KEY_MANGA_DATA_SAVER = "manga_data_saver"
        internal const val KEY_MANGA_PAGE_LAYOUT = "manga_page_layout"
        internal const val KEY_MANGA_IMAGE_SCALING = "manga_image_scaling"
        internal const val KEY_MANGA_PAGE_INDICATOR = "manga_page_indicator"
        internal const val KEY_SELECTED_MANGA_EXTENSION = "selected_manga_extension_authority"
        internal const val KEY_MANGA_SOURCE_METHOD = "manga_source_method"
        internal const val KEY_DEFAULT_EXTERNAL_MANGA_EXTENSION = "default_external_manga_extension"
        internal const val KEY_MANGA_EXTENSION_LANGUAGES = "manga_extension_languages"
        internal const val KEY_MANGA_SYNC_THRESHOLD = "manga_anilist_sync_threshold"
        internal const val KEY_MANGA_LOCK_ROTATION = "manga_lock_rotation"
        internal const val KEY_MANGA_FULLSCREEN = "manga_fullscreen"
        internal const val KEY_MANGA_AUTO_ADVANCE = "manga_auto_advance"
        internal const val KEY_APP_ICON = "app_icon"
        internal const val KEY_MAX_PERFORMANCE = "max_performance"
        internal const val KEY_SUPPORTS_PIP = "supports_pip"
        internal const val KEY_DISCORD_RICH_PRESENCE = "discord_rich_presence"
    }

    internal val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Auth token
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    // UI Preferences
    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isOled = MutableStateFlow(false)
    val isOled: StateFlow<Boolean> = _isOled.asStateFlow()

    private val _appIcon = MutableStateFlow("default")
    val appIcon: StateFlow<String> = _appIcon.asStateFlow()

    private val _maxPerformance = MutableStateFlow(false)
    val maxPerformance: StateFlow<Boolean> = _maxPerformance.asStateFlow()

    private val _disableMaterialColors = MutableStateFlow(true)
    val disableMaterialColors: StateFlow<Boolean> = _disableMaterialColors.asStateFlow()

    /** One of [ColorMode] values: "material", "semi_monochrome" or "monochrome". */
    private val _colorMode = MutableStateFlow(ColorMode.SEMI_MONOCHROME.value)
    val colorMode: StateFlow<String> = _colorMode.asStateFlow()

    private val _hideEpisodeDescription = MutableStateFlow(true)
    val hideEpisodeDescription: StateFlow<Boolean> = _hideEpisodeDescription.asStateFlow()

    private val _preferredCategory = MutableStateFlow("sub")
    val preferredCategory: StateFlow<String> = _preferredCategory.asStateFlow()

    private val _showStatusColors = MutableStateFlow(true)
    val showStatusColors: StateFlow<Boolean> = _showStatusColors.asStateFlow()

    private val _showAnimeCardButtons = MutableStateFlow(false)
    val showAnimeCardButtons: StateFlow<Boolean> = _showAnimeCardButtons.asStateFlow()

    private val _showMangaCardButtons = MutableStateFlow(false)
    val showMangaCardButtons: StateFlow<Boolean> = _showMangaCardButtons.asStateFlow()

    private val _showMangaStatusColors = MutableStateFlow(false)
    val showMangaStatusColors: StateFlow<Boolean> = _showMangaStatusColors.asStateFlow()

    private val _preferEnglishTitles = MutableStateFlow(true)
    val preferEnglishTitles: StateFlow<Boolean> = _preferEnglishTitles.asStateFlow()

    private val _advancedSearch = MutableStateFlow(false)
    val advancedSearch: StateFlow<Boolean> = _advancedSearch.asStateFlow()

    private val _trackingPercentage = MutableStateFlow(85)
    val trackingPercentage: StateFlow<Int> = _trackingPercentage.asStateFlow()

    private val _forwardSkipSeconds = MutableStateFlow(10)
    val forwardSkipSeconds: StateFlow<Int> = _forwardSkipSeconds.asStateFlow()

    private val _backwardSkipSeconds = MutableStateFlow(10)
    val backwardSkipSeconds: StateFlow<Int> = _backwardSkipSeconds.asStateFlow()

    private val _hideNavbarText = MutableStateFlow(false)
    val hideNavbarText: StateFlow<Boolean> = _hideNavbarText.asStateFlow()

    private val _simplifyAnimeDetails = MutableStateFlow(true)
    val simplifyAnimeDetails: StateFlow<Boolean> = _simplifyAnimeDetails.asStateFlow()

    private val _autoSkipOpening = MutableStateFlow(false)
    val autoSkipOpening: StateFlow<Boolean> = _autoSkipOpening.asStateFlow()

    private val _autoSkipEnding = MutableStateFlow(false)
    val autoSkipEnding: StateFlow<Boolean> = _autoSkipEnding.asStateFlow()

    private val _autoPlayNextEpisode = MutableStateFlow(false)
    val autoPlayNextEpisode: StateFlow<Boolean> = _autoPlayNextEpisode.asStateFlow()

    private val _supportsPiP = MutableStateFlow(false)
    val supportsPiP: StateFlow<Boolean> = _supportsPiP.asStateFlow()

    private val _discordRichPresence = MutableStateFlow(false)
    val discordRichPresence: StateFlow<Boolean> = _discordRichPresence.asStateFlow()

    // Thumbnail extraction for seekbar preview
    private val _enableThumbnailPreview = MutableStateFlow(false)

    // Preferred Scraper
    private val _preferredScraper = MutableStateFlow("Animekai")
    val preferredScraper: StateFlow<String> = _preferredScraper.asStateFlow()

    // Default Extension
    private val _defaultExtensionPackage = MutableStateFlow("")
    val defaultExtensionPackage: StateFlow<String> = _defaultExtensionPackage.asStateFlow()

    // Default Subtitle Language
    private val _defaultSubtitleLang = MutableStateFlow("English")
    val defaultSubtitleLang: StateFlow<String> = _defaultSubtitleLang.asStateFlow()

    // Stream Provider (1 = Miruro, 2 = Animekai)
    private val _streamProvider = MutableStateFlow(1)
    val streamProvider: StateFlow<Int> = _streamProvider.asStateFlow()

    // Stream Method (direct or magnet)
    private val _streamMethod = MutableStateFlow("magnet")
    val streamMethod: StateFlow<String> = _streamMethod.asStateFlow()

    // Default Magnet Extension
    private val _defaultMagnetExtension = MutableStateFlow<String?>(null)
    val defaultMagnetExtension: StateFlow<String?> = _defaultMagnetExtension.asStateFlow()

    // Default Stream Extension (tensei stream — ContentProvider-based)
    private val _defaultStreamExtension = MutableStateFlow<String?>(null)
    val defaultStreamExtension: StateFlow<String?> = _defaultStreamExtension.asStateFlow()

    // Manga source method: "tensei" (Blissless ContentProvider extensions) or
    // "external" (installed Mihon-style manga extension APKs).
    private val _mangaSourceMethod = MutableStateFlow("tensei")
    val mangaSourceMethod: StateFlow<String> = _mangaSourceMethod.asStateFlow()

    // Default external manga extension (package name) used when mangaSourceMethod == "external".
    private val _defaultExternalMangaExtension = MutableStateFlow<String?>(null)
    val defaultExternalMangaExtension: StateFlow<String?> = _defaultExternalMangaExtension.asStateFlow()

    // Per-extension preferred language for external manga extensions (pkgName -> language).
    private val _mangaExtensionLanguages = MutableStateFlow<Map<String, String>>(emptyMap())
    val mangaExtensionLanguages: StateFlow<Map<String, String>> = _mangaExtensionLanguages.asStateFlow()

    // Manga Reader Preferences
    private val _mangaReaderMode = MutableStateFlow("vertical_scroll")
    val mangaReaderMode: StateFlow<String> = _mangaReaderMode.asStateFlow()

    private val _mangaDataSaver = MutableStateFlow(false)
    val mangaDataSaver: StateFlow<Boolean> = _mangaDataSaver.asStateFlow()

    private val _mangaPageLayout = MutableStateFlow("single_page")
    val mangaPageLayout: StateFlow<String> = _mangaPageLayout.asStateFlow()

    private val _mangaImageScaling = MutableStateFlow("fit_width")
    val mangaImageScaling: StateFlow<String> = _mangaImageScaling.asStateFlow()

    private val _mangaPageIndicator = MutableStateFlow(true)
    val mangaPageIndicator: StateFlow<Boolean> = _mangaPageIndicator.asStateFlow()

    // AniList sync threshold (75-100%) — the scroll percentage at which a chapter is
    // considered "read" and progress is pushed to AniList. Default 90% (matching oni).
    private val _mangaSyncThreshold = MutableStateFlow(90)
    val mangaSyncThreshold: StateFlow<Int> = _mangaSyncThreshold.asStateFlow()

    // Lock screen rotation to the current orientation while reading manga.
    private val _mangaLockRotation = MutableStateFlow(false)
    val mangaLockRotation: StateFlow<Boolean> = _mangaLockRotation.asStateFlow()

    // Hide system bars (status bar + navigation) while reading manga.
    private val _mangaFullscreen = MutableStateFlow(true)
    val mangaFullscreen: StateFlow<Boolean> = _mangaFullscreen.asStateFlow()

    // Automatically open the next chapter when the reader reaches the end of a chapter.
    private val _mangaAutoAdvance = MutableStateFlow(true)
    val mangaAutoAdvance: StateFlow<Boolean> = _mangaAutoAdvance.asStateFlow()

    // Startup Screen
    private val _startupScreen = MutableStateFlow(2)
    val startupScreen: StateFlow<Int> = _startupScreen.asStateFlow()

    // Interface language: "system" (device language), "en" or "de".
    private val _appLanguage = MutableStateFlow("system")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    // Buffer Settings
    private val _bufferAheadSeconds = MutableStateFlow(30)
    val bufferAheadSeconds: StateFlow<Int> = _bufferAheadSeconds.asStateFlow()
    
    private val _bufferSizeMb = MutableStateFlow(100)
    val bufferSizeMb: StateFlow<Int> = _bufferSizeMb.asStateFlow()
    
    private val _showBufferIndicator = MutableStateFlow(true)
    val showBufferIndicator: StateFlow<Boolean> = _showBufferIndicator.asStateFlow()

    // Local favorites
    internal val _localFavorites = MutableStateFlow<Map<Int, StoredFavorite>>(emptyMap())
    val localFavorites: StateFlow<Map<Int, StoredFavorite>> = _localFavorites.asStateFlow()

    val localFavoriteIds: Set<Int> get() = _localFavorites.value.keys

    // AniList favorites (stored locally, synced to API)
    internal val _aniListFavorites = MutableStateFlow<Set<Int>>(emptySet())
    val aniListFavorites: StateFlow<Set<Int>> = _aniListFavorites.asStateFlow()





    internal val _localAnimeStatus = MutableStateFlow<Map<Int, LocalAnimeEntry>>(emptyMap())
    val localAnimeStatus: StateFlow<Map<Int, LocalAnimeEntry>> = _localAnimeStatus.asStateFlow()

    /**
     * Load all preferences from SharedPreferences.
     * @param hasToken Whether to attempt loading the auth token
     */
    fun loadPreferences(hasToken: Boolean) {
        // Load auth token if requested
        if (hasToken) {
            val token = sharedPreferences.getString(TOKEN_KEY, null)
            _authToken.value = token
        }

        // Load UI preferences
        _themeMode.value = sharedPreferences.getString(KEY_THEME_MODE, "system") ?: "system"
        _isOled.value = _themeMode.value == "oled"
        _appIcon.value = sharedPreferences.getString(KEY_APP_ICON, "default") ?: "default"
        _maxPerformance.value = sharedPreferences.getBoolean(KEY_MAX_PERFORMANCE, false)
        _disableMaterialColors.value = sharedPreferences.getBoolean(KEY_DISABLE_MATERIAL_COLORS, true)
        // Color mode supersedes the old boolean. Fall back to it on first run so
        // existing installs keep whatever they had chosen; the neutral legacy
        // value maps to the new default (semi monochrome).
        _colorMode.value = sharedPreferences.getString(
            KEY_COLOR_MODE,
            if (_disableMaterialColors.value) ColorMode.SEMI_MONOCHROME.value else ColorMode.MATERIAL.value
        ) ?: ColorMode.SEMI_MONOCHROME.value
        _preferredCategory.value = sharedPreferences.getString(KEY_PREFERRED_CATEGORY, "sub") ?: "sub"
        // Status color badges default to ON so users see their list status (Watching / Planning
        // / Completed / On Hold / Dropped) on anime and manga cards out of the box. Users
        // who prefer a cleaner look can still turn them OFF in Settings → Display.
        _showStatusColors.value = sharedPreferences.getBoolean(KEY_SHOW_STATUS_COLORS, true)
        _showAnimeCardButtons.value = sharedPreferences.getBoolean(KEY_SHOW_ANIME_CARD_BUTTONS, false)
        _showMangaCardButtons.value = sharedPreferences.getBoolean(KEY_SHOW_MANGA_CARD_BUTTONS, false)
        _showMangaStatusColors.value = sharedPreferences.getBoolean(KEY_SHOW_MANGA_STATUS_COLORS, true)
        _preferEnglishTitles.value = sharedPreferences.getBoolean(KEY_PREFER_ENGLISH_TITLES, true)
        _advancedSearch.value = sharedPreferences.getBoolean(KEY_ADVANCED_SEARCH, false)

        _trackingPercentage.value = sharedPreferences.getInt(KEY_TRACKING_PERCENTAGE, 85)
        _forwardSkipSeconds.value = sharedPreferences.getInt(KEY_FORWARD_SKIP_SECONDS, 10)
        _backwardSkipSeconds.value = sharedPreferences.getInt(KEY_BACKWARD_SKIP_SECONDS, 10)
        _hideNavbarText.value = sharedPreferences.getBoolean(KEY_HIDE_NAVBAR_TEXT, false)
        _simplifyAnimeDetails.value = sharedPreferences.getBoolean(KEY_SIMPLIFY_ANIME_DETAILS, false)
        _autoSkipOpening.value = sharedPreferences.getBoolean(KEY_AUTO_SKIP_OPENING, false)
        _autoSkipEnding.value = sharedPreferences.getBoolean(KEY_AUTO_SKIP_ENDING, false)
        _autoPlayNextEpisode.value = sharedPreferences.getBoolean(KEY_AUTO_PLAY_NEXT_EPISODE, true)
        _supportsPiP.value = sharedPreferences.getBoolean(KEY_SUPPORTS_PIP, true)
        _discordRichPresence.value = sharedPreferences.getBoolean(KEY_DISCORD_RICH_PRESENCE, false)
        _enableThumbnailPreview.value = sharedPreferences.getBoolean(KEY_ENABLE_THUMBNAIL_PREVIEW, false)
        _preferredScraper.value = sharedPreferences.getString(KEY_PREFERRED_SCRAPER, "Animekai") ?: "Animekai"
        _defaultExtensionPackage.value = sharedPreferences.getString(KEY_DEFAULT_EXTENSION, "") ?: ""
        _defaultSubtitleLang.value = sharedPreferences.getString(KEY_DEFAULT_SUBTITLE_LANG, "English") ?: "English"
        _streamProvider.value = sharedPreferences.getInt(KEY_STREAM_PROVIDER, 1)
        _streamMethod.value = sharedPreferences.getString(KEY_STREAM_METHOD, "magnet") ?: "magnet"
        val savedMagnetExt = sharedPreferences.getString(KEY_DEFAULT_MAGNET_EXTENSION, null)
        val migratedMagnetExt = migrateStreamAuthority(savedMagnetExt)
        if (migratedMagnetExt != savedMagnetExt && savedMagnetExt != null) {
            sharedPreferences.edit { putString(KEY_DEFAULT_MAGNET_EXTENSION, migratedMagnetExt) }
        }
        _defaultMagnetExtension.value = migratedMagnetExt
        val savedStreamExt = sharedPreferences.getString(KEY_DEFAULT_STREAM_EXTENSION, null)
        val migratedStreamExt = migrateStreamAuthority(savedStreamExt)
        if (migratedStreamExt != savedStreamExt && savedStreamExt != null) {
            sharedPreferences.edit { putString(KEY_DEFAULT_STREAM_EXTENSION, migratedStreamExt) }
        }
        _defaultStreamExtension.value = migratedStreamExt
        _mangaSourceMethod.value = sharedPreferences.getString(KEY_MANGA_SOURCE_METHOD, "tensei") ?: "tensei"
        _defaultExternalMangaExtension.value = sharedPreferences.getString(KEY_DEFAULT_EXTERNAL_MANGA_EXTENSION, null)
        _mangaExtensionLanguages.value = loadMangaExtensionLanguages()
        _startupScreen.value = sharedPreferences.getInt(KEY_STARTUP_SCREEN, 2)
        _appLanguage.value = sharedPreferences.getString(KEY_APP_LANGUAGE, "system") ?: "system"
        _bufferAheadSeconds.value = sharedPreferences.getInt(KEY_BUFFER_AHEAD_SECONDS, 30)
        _bufferSizeMb.value = sharedPreferences.getInt(KEY_BUFFER_SIZE_MB, 200)
        _showBufferIndicator.value = sharedPreferences.getBoolean(KEY_SHOW_BUFFER_INDICATOR, true)
        _checkUpdatesOnStart.value = sharedPreferences.getBoolean(KEY_CHECK_UPDATES_ON_START, true)
        _malAsMainProvider.value = sharedPreferences.getBoolean(KEY_MAL_AS_MAIN_PROVIDER, false)
        _swipeVolume.value = sharedPreferences.getBoolean(KEY_SWIPE_VOLUME, false)
        _swipeBrightness.value = sharedPreferences.getBoolean(KEY_SWIPE_BRIGHTNESS, false)
        _swipeSwap.value = sharedPreferences.getBoolean(KEY_SWIPE_SWAP, false)
        _autoUpdateExtensions.value = sharedPreferences.getBoolean(KEY_AUTO_UPDATE_EXTENSIONS, true)
        _hideEpisodeDescription.value = sharedPreferences.getBoolean(KEY_HIDE_EPISODE_DESCRIPTION, true)
        _mangaReaderMode.value = sharedPreferences.getString(KEY_MANGA_READER_MODE, "vertical_scroll") ?: "vertical_scroll"
        _mangaDataSaver.value = sharedPreferences.getBoolean(KEY_MANGA_DATA_SAVER, false)
        _mangaPageLayout.value = sharedPreferences.getString(KEY_MANGA_PAGE_LAYOUT, "single_page") ?: "single_page"
        _mangaImageScaling.value = sharedPreferences.getString(KEY_MANGA_IMAGE_SCALING, "fit_width") ?: "fit_width"
        _mangaPageIndicator.value = sharedPreferences.getBoolean(KEY_MANGA_PAGE_INDICATOR, true)
        _mangaSyncThreshold.value = sharedPreferences.getInt(KEY_MANGA_SYNC_THRESHOLD, 90).coerceIn(75, 100)
        _mangaLockRotation.value = sharedPreferences.getBoolean(KEY_MANGA_LOCK_ROTATION, false)
        _mangaFullscreen.value = sharedPreferences.getBoolean(KEY_MANGA_FULLSCREEN, true)
        _mangaAutoAdvance.value = sharedPreferences.getBoolean(KEY_MANGA_AUTO_ADVANCE, true)

        // Load local favorites
        loadLocalFavorites()

        // Load AniList favorites (stored locally, synced to API)
        loadAniListFavorites()

        // Load local anime status
        loadLocalAnimeStatus()

        // Load subtitle profiles
        loadSubtitleProfiles()
    }

    // ============================================
    // Auth Methods
    // ============================================

    fun saveToken(token: String) {
        sharedPreferences.edit { putString(TOKEN_KEY, token) }
        _authToken.value = token
    }

    fun clearToken() {
        _authToken.value = null
        sharedPreferences.edit { remove(TOKEN_KEY) }
    }

    // ============================================
    // UI Preference Setters
    // ============================================

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        _isOled.value = mode == "oled"
        sharedPreferences.edit { putString(KEY_THEME_MODE, mode) }
    }

    fun setAppIcon(key: String) {
        _appIcon.value = key
        sharedPreferences.edit { putString(KEY_APP_ICON, key) }
    }

    fun setMaxPerformance(enabled: Boolean) {
        _maxPerformance.value = enabled
        sharedPreferences.edit { putBoolean(KEY_MAX_PERFORMANCE, enabled) }
    }

    fun setDisableMaterialColors(enabled: Boolean) {
        setColorMode(if (enabled) ColorMode.MONOCHROME.value else ColorMode.MATERIAL.value)
    }

    fun setColorMode(mode: String) {
        val resolved = ColorMode.fromValue(mode)
        _colorMode.value = resolved.value
        // Legacy mirrors: neutral surfaces in every non-Material mode.
        val neutral = resolved.usesNeutralSurfaces
        _disableMaterialColors.value = neutral
        sharedPreferences.edit {
            putString(KEY_COLOR_MODE, resolved.value)
            putBoolean(KEY_DISABLE_MATERIAL_COLORS, neutral)
        }
    }

    fun setHideEpisodeDescription(enabled: Boolean) {
        _hideEpisodeDescription.value = enabled
        sharedPreferences.edit { putBoolean(KEY_HIDE_EPISODE_DESCRIPTION, enabled) }
    }

    fun setPreferredCategory(category: String) {
        _preferredCategory.value = category
        sharedPreferences.edit { putString(KEY_PREFERRED_CATEGORY, category) }
    }

    fun setShowStatusColors(enabled: Boolean) {
        _showStatusColors.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SHOW_STATUS_COLORS, enabled) }
    }

    fun setShowAnimeCardButtons(enabled: Boolean) {
        _showAnimeCardButtons.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SHOW_ANIME_CARD_BUTTONS, enabled) }
    }

    fun setShowMangaCardButtons(enabled: Boolean) {
        _showMangaCardButtons.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SHOW_MANGA_CARD_BUTTONS, enabled) }
    }

    fun setShowMangaStatusColors(enabled: Boolean) {
        _showMangaStatusColors.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SHOW_MANGA_STATUS_COLORS, enabled) }
    }

    fun setPreferEnglishTitles(enabled: Boolean) {
        _preferEnglishTitles.value = enabled
        sharedPreferences.edit { putBoolean(KEY_PREFER_ENGLISH_TITLES, enabled) }
    }

    fun setAdvancedSearch(enabled: Boolean) {
        _advancedSearch.value = enabled
        sharedPreferences.edit { putBoolean(KEY_ADVANCED_SEARCH, enabled) }
    }

    fun setTrackingPercentage(percentage: Int) {
        val validPercentage = percentage.coerceIn(50, 100)
        _trackingPercentage.value = validPercentage
        sharedPreferences.edit { putInt(KEY_TRACKING_PERCENTAGE, validPercentage) }
    }

    fun setForwardSkipSeconds(seconds: Int) {
        val validSeconds = seconds.coerceIn(5, 30)
        _forwardSkipSeconds.value = validSeconds
        sharedPreferences.edit {putInt(KEY_FORWARD_SKIP_SECONDS, validSeconds) }
    }

    fun setBackwardSkipSeconds(seconds: Int) {
        val validSeconds = seconds.coerceIn(5, 30)
        _backwardSkipSeconds.value = validSeconds
        sharedPreferences.edit { putInt(KEY_BACKWARD_SKIP_SECONDS, validSeconds) }
    }

    fun setHideNavbarText(enabled: Boolean) {
        _hideNavbarText.value = enabled
        sharedPreferences.edit {putBoolean(KEY_HIDE_NAVBAR_TEXT, enabled) }
    }

    fun setSimplifyAnimeDetails(enabled: Boolean) {
        _simplifyAnimeDetails.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SIMPLIFY_ANIME_DETAILS, enabled) }
    }

    fun setAutoSkipOpening(enabled: Boolean) {
        _autoSkipOpening.value = enabled
        sharedPreferences.edit {putBoolean(KEY_AUTO_SKIP_OPENING, enabled) }
    }

    fun setAutoSkipEnding(enabled: Boolean) {
        _autoSkipEnding.value = enabled
        sharedPreferences.edit {putBoolean(KEY_AUTO_SKIP_ENDING, enabled) }
    }

    fun setAutoPlayNextEpisode(enabled: Boolean) {
        _autoPlayNextEpisode.value = enabled
        sharedPreferences.edit {putBoolean(KEY_AUTO_PLAY_NEXT_EPISODE, enabled) }
    }

    fun setSupportsPiP(enabled: Boolean) {
        _supportsPiP.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SUPPORTS_PIP, enabled) }
    }

    fun setDiscordRichPresence(enabled: Boolean) {
        _discordRichPresence.value = enabled
        sharedPreferences.edit { putBoolean(KEY_DISCORD_RICH_PRESENCE, enabled) }
    }

    /**
     * Enable or disable thumbnail preview for video scrubbing.
     * This is a resource-intensive feature that extracts video frames.
     */
    fun setEnableThumbnailPreview(enabled: Boolean) {
        _enableThumbnailPreview.value = enabled
        sharedPreferences.edit {putBoolean(KEY_ENABLE_THUMBNAIL_PREVIEW, enabled) }
    }

    fun setPreferredScraper(scraper: String) {
        _preferredScraper.value = scraper
        sharedPreferences.edit { putString(KEY_PREFERRED_SCRAPER, scraper) }
    }

    fun setDefaultExtensionPackage(packageName: String) {
        _defaultExtensionPackage.value = packageName
        sharedPreferences.edit { putString(KEY_DEFAULT_EXTENSION, packageName) }
    }

    fun setDefaultSubtitleLang(lang: String) {
        _defaultSubtitleLang.value = lang
        sharedPreferences.edit { putString(KEY_DEFAULT_SUBTITLE_LANG, lang) }
    }

    fun setStreamProvider(provider: Int) {
        _streamProvider.value = provider
        sharedPreferences.edit { putInt(KEY_STREAM_PROVIDER, provider) }
    }

    fun setStreamMethod(method: String) {
        _streamMethod.value = method
        sharedPreferences.edit { putString(KEY_STREAM_METHOD, method) }
    }

    fun setDefaultMagnetExtension(authority: String?) {
        _defaultMagnetExtension.value = authority
        sharedPreferences.edit { putString(KEY_DEFAULT_MAGNET_EXTENSION, authority) }
    }

    fun setDefaultStreamExtension(authority: String?) {
        _defaultStreamExtension.value = authority
        sharedPreferences.edit { putString(KEY_DEFAULT_STREAM_EXTENSION, authority) }
    }

    fun setMangaReaderMode(mode: String) {
        _mangaReaderMode.value = mode
        sharedPreferences.edit { putString(KEY_MANGA_READER_MODE, mode) }
    }

    fun setMangaDataSaver(enabled: Boolean) {
        _mangaDataSaver.value = enabled
        sharedPreferences.edit { putBoolean(KEY_MANGA_DATA_SAVER, enabled) }
    }

    fun setMangaPageLayout(layout: String) {
        _mangaPageLayout.value = layout
        sharedPreferences.edit { putString(KEY_MANGA_PAGE_LAYOUT, layout) }
    }

    fun setMangaImageScaling(scaling: String) {
        _mangaImageScaling.value = scaling
        sharedPreferences.edit { putString(KEY_MANGA_IMAGE_SCALING, scaling) }
    }

    fun setMangaPageIndicator(enabled: Boolean) {
        _mangaPageIndicator.value = enabled
        sharedPreferences.edit { putBoolean(KEY_MANGA_PAGE_INDICATOR, enabled) }
    }

    fun setMangaLockRotation(enabled: Boolean) {
        _mangaLockRotation.value = enabled
        sharedPreferences.edit { putBoolean(KEY_MANGA_LOCK_ROTATION, enabled) }
    }

    fun setMangaFullscreen(enabled: Boolean) {
        _mangaFullscreen.value = enabled
        sharedPreferences.edit { putBoolean(KEY_MANGA_FULLSCREEN, enabled) }
    }

    fun setMangaAutoAdvance(enabled: Boolean) {
        _mangaAutoAdvance.value = enabled
        sharedPreferences.edit { putBoolean(KEY_MANGA_AUTO_ADVANCE, enabled) }
    }

    fun setStartupScreen(screen: Int) {
        _startupScreen.value = screen
        sharedPreferences.edit { putInt(KEY_STARTUP_SCREEN, screen) }
    }

    fun setAppLanguage(language: String) {
        _appLanguage.value = language
        sharedPreferences.edit { putString(KEY_APP_LANGUAGE, language) }
    }

    // ============================================
    // Buffer Settings
    // ============================================
    
    fun setBufferAheadSeconds(seconds: Int) {
        _bufferAheadSeconds.value = seconds
        sharedPreferences.edit { putInt(KEY_BUFFER_AHEAD_SECONDS, seconds) }
    }
    
    fun setBufferSizeMb(sizeMb: Int) {
        _bufferSizeMb.value = sizeMb
        sharedPreferences.edit { putInt(KEY_BUFFER_SIZE_MB, sizeMb) }
    }
    
    fun setShowBufferIndicator(show: Boolean) {
        _showBufferIndicator.value = show
        sharedPreferences.edit { putBoolean(KEY_SHOW_BUFFER_INDICATOR, show) }
    }

    // ============================================
    // Local Favorites
    // ============================================
















    fun getSharedPreferences(): SharedPreferences = sharedPreferences

    // Refresh timestamp management
    fun getLastHomeRefreshTime(): Long = sharedPreferences.getLong(KEY_LAST_HOME_REFRESH, 0L)
    fun setLastHomeRefreshTime(time: Long) {
        sharedPreferences.edit { putLong(KEY_LAST_HOME_REFRESH, time) }
    }

    fun getLastExploreRefreshTime(): Long = sharedPreferences.getLong(KEY_LAST_EXPLORE_REFRESH, 0L)
    fun setLastExploreRefreshTime(time: Long) {
        sharedPreferences.edit { putLong(KEY_LAST_EXPLORE_REFRESH, time) }
    }

    fun getLastScheduleRefreshTime(): Long = sharedPreferences.getLong(KEY_LAST_SCHEDULE_REFRESH, 0L)
    fun setLastScheduleRefreshTime(time: Long) {
        sharedPreferences.edit { putLong(KEY_LAST_SCHEDULE_REFRESH, time) }
    }

    // Local Anime Status (for offline users)





    
    private val _checkUpdatesOnStart = MutableStateFlow(true)
    val checkUpdatesOnStart: StateFlow<Boolean> = _checkUpdatesOnStart.asStateFlow()

    // Main list provider when both are logged in: false = AniList (default), true = MyAnimeList.
    // The main provider drives which list populates the home screen.
    private val _malAsMainProvider = MutableStateFlow(false)
    val malAsMainProvider: StateFlow<Boolean> = _malAsMainProvider.asStateFlow()

    // Swipe Gesture Controls
    private val _swipeVolume = MutableStateFlow(false)
    val swipeVolume: StateFlow<Boolean> = _swipeVolume.asStateFlow()

    private val _swipeBrightness = MutableStateFlow(false)
    val swipeBrightness: StateFlow<Boolean> = _swipeBrightness.asStateFlow()
    private val _swipeSwap = MutableStateFlow(false)
    val swipeSwap: StateFlow<Boolean> = _swipeSwap.asStateFlow()

    private val _autoUpdateExtensions = MutableStateFlow(true)
    val autoUpdateExtensions: StateFlow<Boolean> = _autoUpdateExtensions.asStateFlow()

    // Subtitle Profiles
    internal var _subtitleProfileData = MutableStateFlow(SubtitleProfileData())
    val subtitleProfileData: StateFlow<SubtitleProfileData> = _subtitleProfileData.asStateFlow()








    fun setCheckUpdatesOnStart(enabled: Boolean) {
        _checkUpdatesOnStart.value = enabled
        sharedPreferences.edit { putBoolean(KEY_CHECK_UPDATES_ON_START, enabled) }
    }

    fun setMalAsMainProvider(enabled: Boolean) {
        _malAsMainProvider.value = enabled
        sharedPreferences.edit { putBoolean(KEY_MAL_AS_MAIN_PROVIDER, enabled) }
    }

    fun setSwipeVolume(enabled: Boolean) {
        _swipeVolume.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SWIPE_VOLUME, enabled) }
    }

    fun setSwipeBrightness(enabled: Boolean) {
        _swipeBrightness.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SWIPE_BRIGHTNESS, enabled) }
    }

    fun setSwipeSwap(enabled: Boolean) {
        _swipeSwap.value = enabled
        sharedPreferences.edit { putBoolean(KEY_SWIPE_SWAP, enabled) }
    }

    fun setAutoUpdateExtensions(enabled: Boolean) {
        _autoUpdateExtensions.value = enabled
        sharedPreferences.edit { putBoolean(KEY_AUTO_UPDATE_EXTENSIONS, enabled) }
    }

    // MAL Favorites
    
    




    fun clearMalMangaFavorites() {
        sharedPreferences.edit {
            remove(KEY_MAL_MANGA_FAVORITES)
        }
    }

    // ─── Cross-provider one-time copy ─────────────────────────────────

    fun isCrossProviderCopyDone(): Boolean =
        sharedPreferences.getBoolean(KEY_CROSS_PROVIDER_COPY_DONE, false)

    fun setCrossProviderCopyDone(done: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_CROSS_PROVIDER_COPY_DONE, done)
        }
    }

    // ─── Manga Extension Selection ────────────────────────────────────

    fun getSelectedMangaExtensionAuthority(): String? {
        return sharedPreferences.getString(KEY_SELECTED_MANGA_EXTENSION, null)
    }

    fun setSelectedMangaExtensionAuthority(authority: String?) {
        sharedPreferences.edit {
            if (authority == null) {
                remove(KEY_SELECTED_MANGA_EXTENSION)
            } else {
                putString(KEY_SELECTED_MANGA_EXTENSION, authority)
            }
        }
    }

    fun setMangaSourceMethod(method: String) {
        _mangaSourceMethod.value = method
        sharedPreferences.edit { putString(KEY_MANGA_SOURCE_METHOD, method) }
    }

    fun setDefaultExternalMangaExtension(pkgName: String?) {
        _defaultExternalMangaExtension.value = pkgName
        sharedPreferences.edit {
            if (pkgName == null) {
                remove(KEY_DEFAULT_EXTERNAL_MANGA_EXTENSION)
            } else {
                putString(KEY_DEFAULT_EXTERNAL_MANGA_EXTENSION, pkgName)
            }
        }
    }

    fun getMangaExtensionLanguage(pkgName: String): String? =
        _mangaExtensionLanguages.value[pkgName]

    fun setMangaExtensionLanguage(pkgName: String, language: String) {
        val next = _mangaExtensionLanguages.value + (pkgName to language)
        _mangaExtensionLanguages.value = next
        saveMangaExtensionLanguages(next)
    }

    private fun loadMangaExtensionLanguages(): Map<String, String> {
        val raw = sharedPreferences.getString(KEY_MANGA_EXTENSION_LANGUAGES, null) ?: return emptyMap()
        return runCatching {
            val json = com.blissless.tensei.util.TenseiJson.default
            json.decodeFromString(languageMapSerializer(), raw)
        }.getOrDefault(emptyMap())
    }

    private fun saveMangaExtensionLanguages(map: Map<String, String>) {
        val json = com.blissless.tensei.util.TenseiJson.default
        sharedPreferences.edit { putString(KEY_MANGA_EXTENSION_LANGUAGES, json.encodeToString(languageMapSerializer(), map)) }
    }

    private fun languageMapSerializer() = MapSerializer(
        String.serializer(),
        String.serializer(),
    )

    // ─── Manga AniList Sync Threshold ─────────────────────────────────

    fun setMangaSyncThreshold(percent: Int) {
        val clamped = percent.coerceIn(75, 100)
        _mangaSyncThreshold.value = clamped
        sharedPreferences.edit { putInt(KEY_MANGA_SYNC_THRESHOLD, clamped) }
    }

    private fun migrateStreamAuthority(authority: String?): String? {
        if (authority == null) return null
        if (authority.endsWith(".provider")) return authority
        if (authority.endsWith(".anime.stream")) return authority.removeSuffix(".anime.stream") + ".provider"
        if (authority.endsWith(".anime.torrent")) return authority.removeSuffix(".anime.torrent") + ".provider"
        if (authority.endsWith(".manga")) return authority.removeSuffix(".manga") + ".provider"
        if (authority.endsWith(".stream")) return authority.removeSuffix(".stream") + ".provider"
        if (authority.endsWith(".torrent")) return authority.removeSuffix(".torrent") + ".provider"
        return authority
    }
}


