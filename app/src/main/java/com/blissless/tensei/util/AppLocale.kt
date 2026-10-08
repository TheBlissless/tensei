package com.blissless.tensei.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Applies the interface language chosen in Settings.
 *
 * Stored values: "system" (device language), "en" or "de".
 *
 * Android 13+ persists and applies per-app locales through [LocaleManager],
 * so [wrap] is a no-op there. Older versions have no per-app locale support,
 * so the activity base context is wrapped in an overridden configuration and
 * the application resources (toasts, notifications, widgets) are kept in sync.
 * Anything not translated in `values-de` falls back to the default
 * `values/strings.xml` (English) automatically.
 */
object AppLocale {

    const val SYSTEM = "system"

    private const val PREFS_NAME = "anilist_prefs"
    private const val KEY_APP_LANGUAGE = "app_language"

    private val OVERRIDE_LANGUAGES = setOf("en", "de")

    /** The stored choice: "system", "en" or "de". */
    fun read(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_LANGUAGE, SYSTEM) ?: SYSTEM

    private fun localeFor(value: String): Locale? =
        if (value in OVERRIDE_LANGUAGES) Locale.forLanguageTag(value) else null

    /**
     * Called from `attachBaseContext`. Returns the context with the chosen
     * locale applied on pre-13 devices, mirroring it onto the application
     * resources so app-context strings (toasts) match the activity.
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = localeFor(read(base)) ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        syncApplication(base, config)
        return base.createConfigurationContext(config)
    }

    /** Called after the user picks a language in Settings. */
    fun onChange(context: Context, value: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The platform stores the choice and restarts the app's activities
            // itself (locale is not part of either activity's configChanges).
            val locale = localeFor(value)
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (locale == null) LocaleList.getEmptyLocaleList() else LocaleList(locale)
            return
        }
        val config = Configuration(context.applicationContext.resources.configuration)
        config.setLocale(localeFor(value) ?: Locale.getDefault())
        syncApplication(context, config)
        (context as? Activity)?.recreate()
    }

    /** Pre-13 only: keeps the application-level resources on the chosen locale. */
    private fun syncApplication(context: Context, config: Configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        @Suppress("DEPRECATION")
        val resources = context.applicationContext.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
