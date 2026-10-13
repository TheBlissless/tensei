package com.blissless.tensei.extensions.manga

import android.content.Context
import android.util.Log
import com.blissless.tensei.R
import com.blissless.tensei.extensions.manga.index.NetworkExtensionStore
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Owns the lifecycle of Mihon-style manga extensions: loading what's installed, browsing
 * user-defined extension repositories and installing/uninstalling extensions.
 */
class MangaExtensionManager private constructor(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val TAG = "MangaExtMgr"

    private val client = NetworkHelper.getInstance().client
    private val json = Injekt.get<kotlinx.serialization.json.Json>()
    private val protoBuf = ProtoBuf

    private val _extensions = MutableStateFlow<List<LoadedExtension>>(emptyList())
    val extensions: StateFlow<List<LoadedExtension>> = _extensions.asStateFlow()

    private val _loadErrors = MutableStateFlow<List<String>>(emptyList())
    val loadErrors: StateFlow<List<String>> = _loadErrors.asStateFlow()

    private val _repos = MutableStateFlow(readRepos())
    val repos: StateFlow<List<String>> = _repos.asStateFlow()

    private val _available = MutableStateFlow<List<AvailableExtension>?>(null)
    val available: StateFlow<List<AvailableExtension>?> = _available.asStateFlow()

    private val _availableError = MutableStateFlow<String?>(null)
    val availableError: StateFlow<String?> = _availableError.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    init {
        scope.launch { reload() }
    }

    val sources: List<Source>
        get() = _extensions.value.flatMap { it.sources }

    fun source(id: Long): Source? =
        _extensions.value.asSequence().flatMap { it.sources }.firstOrNull { it.id == id }

    fun installed(pkgName: String): LoadedExtension? =
        _extensions.value.firstOrNull { it.pkgName == pkgName }

    /** Latest version in the store, if the extension is listed and newer than installed. */
    fun updateFor(pkgName: String): AvailableExtension? {
        val current = installed(pkgName) ?: return null
        val store = _available.value?.firstOrNull { it.pkgName == pkgName } ?: return null
        return store.takeIf { it.versionCode > current.versionCode }
    }

    fun refresh() {
        scope.launch { reload() }
    }

    suspend fun reload() {
        runCatching { MangaExtensionLoader.loadAll(context) }
            .onSuccess {
                _extensions.value = it.extensions
                _loadErrors.value = it.errors
                Log.d(TAG, "reload ok: ${it.extensions.size} extensions, ${it.errors.size} errors")
            }
            .onFailure {
                _loadErrors.value = listOf(it.message ?: "failed to load extensions")
                Log.e(TAG, "loadAll failed", it)
            }
        _initialized.value = true
    }

    // ------------------------------------------------------------------ repos

    private fun readRepos(): List<String> =
        context.getSharedPreferences("manga_repos", Context.MODE_PRIVATE)
            .getString("urls", null)
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toList()
            ?: emptyList()

    private fun saveRepos(repos: List<String>) {
        context.getSharedPreferences("manga_repos", Context.MODE_PRIVATE)
            .edit()
            .putString("urls", repos.joinToString("\n"))
            .apply()
    }

    fun addRepo(url: String): Boolean {
        val clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) return false
        val current = _repos.value
        val next = if (clean in current) current else current + clean
        _repos.value = next
        saveRepos(next)
        return true
    }

    fun removeRepo(url: String) {
        val next = _repos.value - url
        _repos.value = next
        saveRepos(next)
        fetchAvailable()
    }

    fun fetchAvailable() {
        _availableError.value = null
        scope.launch {
            val repos = _repos.value
            if (repos.isEmpty()) {
                _available.value = emptyList()
                return@launch
            }
            val available = mutableListOf<AvailableExtension>()
            val errors = mutableListOf<String>()
            for (repo in repos) {
                MangaExtensionRepository(client, json, protoBuf, repo).fetch()
                    .onSuccess { available += it }
                    .onFailure { errors += repo.substringBeforeLast('/').orEmpty() + ": " + (it.message ?: "failed") }
            }
            _available.value = available.distinctBy { it.pkgName }.sortedBy { it.name.lowercase() }
            _availableError.value = errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
            if (autoUpdateEnabled()) {
                autoUpdate()
            }
        }
    }

    fun install(pkgName: String, apkUrl: String) {
        scope.launch {
            _busy.value = true
            val result = MangaExtensionInstaller.install(context, client, apkUrl)
            _message.value = result.getOrElse { it.message ?: "install failed" }
            reload()
            _busy.value = false
        }
    }

    fun uninstall(pkgName: String) {
        scope.launch {
            _busy.value = true
            val result = MangaExtensionInstaller.uninstall(context, pkgName)
            _message.value = result.fold(
                onSuccess = {
                    reload()
                    "Uninstalled $pkgName"
                },
                onFailure = { it.message ?: "uninstall failed" },
            )
            _busy.value = false
        }
    }

    /** Installs newer versions of every installed extension present in the fetched store. */
    fun autoUpdate() {
        scope.launch {
            val updates = _extensions.value.mapNotNull { ext ->
                val store = _available.value?.firstOrNull { it.pkgName == ext.pkgName } ?: return@mapNotNull null
                if (store.versionCode > ext.versionCode) store else null
            }
            if (updates.isEmpty()) return@launch
            _busy.value = true
            var installedCount = 0
            for (update in updates) {
                val result = MangaExtensionInstaller.install(context, client, update.apkUrl)
                if (result.isSuccess) installedCount++
            }
            if (installedCount > 0) {
                _message.value = context.getString(R.string.runtime_updated_extensions, installedCount)
                reload()
            }
            _busy.value = false
        }
    }

    private fun autoUpdateEnabled(): Boolean =
        context.getSharedPreferences("anilist_prefs", Context.MODE_PRIVATE)
            .getBoolean("auto_update_extensions", true)

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        @Volatile
        private var instance: MangaExtensionManager? = null

        fun init(context: Context) {
            val appContext = context.applicationContext
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = MangaExtensionManager(appContext)
                    }
                }
            }
        }

        fun get(context: Context): MangaExtensionManager {
            return instance ?: synchronized(this) {
                instance ?: MangaExtensionManager(context.applicationContext).also { instance = it }
            }
        }

        fun getOrNull(): MangaExtensionManager? = instance
    }
}
