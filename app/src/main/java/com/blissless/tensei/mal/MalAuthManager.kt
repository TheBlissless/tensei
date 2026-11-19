package com.blissless.tensei.mal

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

enum class LoginProvider {
    NONE, ANILIST, MAL, BOTH
}

@Serializable
data class MalUserInfo(
    val id: Int = 0,
    val name: String = "",
    val picture: String? = null
)

@Serializable
data class MalAnimeListEntry(
    val node: MalAnimeNode = MalAnimeNode(),
    val list_status: MalListStatus? = null
)

@Serializable
data class MalAnimeNode(
    val id: Int = 0,
    val title: String = "",
    val main_picture: MalPicture? = null,
    val num_episodes: Int = 0,
    val alternative_titles: MalAlternativeTitles? = null
)

@Serializable
data class MalAlternativeTitles(
    val en: String? = null,
    val ja: String? = null
)

@Serializable
data class MalPicture(
    val medium: String? = null,
    val large: String? = null
)

@Serializable
data class MalListStatus(
    val status: String? = null,
    val score: Int = 0,
    val num_episodes_watched: Int = 0,
    val updated_at: String? = null
)

@Serializable
data class MalMangaListEntry(
    val node: MalMangaNode = MalMangaNode(),
    val list_status: MalMangaListStatus? = null
)

@Serializable
data class MalMangaNode(
    val id: Int = 0,
    val title: String = "",
    val main_picture: MalPicture? = null,
    val num_chapters: Int = 0,
    val num_volumes: Int = 0,
    val alternative_titles: MalAlternativeTitles? = null,
    val status: String? = null
)

@Serializable
data class MalMangaListStatus(
    val status: String? = null,
    val score: Int = 0,
    val num_chapters_read: Int = 0,
    val num_volumes_read: Int = 0,
    val updated_at: String? = null
)

class MalAuthManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "mal_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        /** When the access token was stored — needed to know if `expires_in` has elapsed. */
        private const val KEY_TOKEN_SAVED_AT = "token_saved_at"
        /** Refresh a little before the token actually expires to avoid failed writes. */
        private const val REFRESH_SKEW_MS = 2 * 60 * 1000L
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PICTURE = "user_picture"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_CODE_VERIFIER_TIME = "code_verifier_time"
        private const val CODE_VERIFIER_EXPIRY_MS = 10 * 60 * 1000L // 10 minutes

    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _accessToken = MutableStateFlow(prefs.getString(KEY_ACCESS_TOKEN, null))

    private val _userInfo = MutableStateFlow<MalUserInfo?>(null)
    val userInfo: StateFlow<MalUserInfo?> = _userInfo.asStateFlow()
    
    val isLoggedIn: Boolean get() = _accessToken.value != null
    
    init {
        loadUserInfo()
    }
    
    fun saveToken(token: String, tokenType: String = "Bearer", expiresIn: Int = 0, refreshToken: String? = null) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, token)
            putString(KEY_TOKEN_TYPE, tokenType)
            putInt(KEY_EXPIRES_IN, expiresIn)
            putLong(KEY_TOKEN_SAVED_AT, System.currentTimeMillis())
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
        }
        _accessToken.value = token
    }
    
    fun clearToken() {
        prefs.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_TOKEN_TYPE)
            remove(KEY_EXPIRES_IN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_SAVED_AT)
            remove(KEY_USER_INFO)
            remove(KEY_USER_NAME)
            remove(KEY_USER_PICTURE)
        }
        _accessToken.value = null
        _userInfo.value = null
    }
    
    fun saveUserInfo(name: String, picture: String?) {
        prefs.edit {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PICTURE, picture)
        }
        _userInfo.value = MalUserInfo(name = name, picture = picture)
    }
    
    private fun loadUserInfo() {
        val name = prefs.getString(KEY_USER_NAME, null)
        val picture = prefs.getString(KEY_USER_PICTURE, null)
        if (name != null) {
            _userInfo.value = MalUserInfo(name = name, picture = picture)
        }
    }
    
    fun getAuthHeader(): String? {
        val token = _accessToken.value ?: return null
        val tokenType = prefs.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
        return "$tokenType $token"
    }

    /** Stored OAuth refresh token, if the user authorized MAL with one. */
    val refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }

    /**
     * True when a refresh should be attempted before making a request. Older installs never
     * stored the issue time, so they return false here and rely on the reactive 401 retry in
     * [MalApiService] instead.
     */
    fun needsRefresh(now: Long = System.currentTimeMillis()): Boolean {
        if (_accessToken.value == null || refreshToken == null) return false
        val expiresIn = prefs.getInt(KEY_EXPIRES_IN, 0)
        val savedAt = prefs.getLong(KEY_TOKEN_SAVED_AT, 0L)
        if (expiresIn <= 0 || savedAt <= 0L) return false
        return now >= savedAt + expiresIn * 1000L - REFRESH_SKEW_MS
    }
    
    fun saveCodeVerifier(verifier: String) {
        prefs.edit {
            putString(KEY_CODE_VERIFIER, verifier)
            putLong(KEY_CODE_VERIFIER_TIME, System.currentTimeMillis())
        }
    }
    
    fun getCodeVerifier(): String? {
        val verifier = prefs.getString(KEY_CODE_VERIFIER, null) ?: return null
        val timestamp = prefs.getLong(KEY_CODE_VERIFIER_TIME, 0)
        
        // Check if expired (10-minute window)
        if (System.currentTimeMillis() - timestamp > CODE_VERIFIER_EXPIRY_MS) {
            clearCodeVerifier()
            return null
        }
        
        return verifier
    }
    
    fun clearCodeVerifier() {
        prefs.edit {
            remove(KEY_CODE_VERIFIER)
            remove(KEY_CODE_VERIFIER_TIME)
        }
    }
}


