package com.blissless.tensei.ui.screens.search

import android.content.Context

/**
 * Persists the last few committed search queries so the redesigned search
 * screen can offer a "Recent" row.
 *
 * Queries are stored newest-first in a single SharedPreferences string,
 * newline-delimited (a committed query is always single-line). Entries are
 * de-duplicated case-insensitively and capped at [MAX_ENTRIES].
 */
class RecentSearchStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> =
        prefs.getString(KEY_RECENT, null)
            ?.split('\n')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    /** Adds [query] to the front, de-dupes (case-insensitive) and persists. Returns the new list. */
    fun add(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return load()
        val existing = load()
        val updated = buildList {
            add(trimmed)
            existing.forEach { if (!it.equals(trimmed, ignoreCase = true)) add(it) }
        }.take(MAX_ENTRIES)
        prefs.edit().putString(KEY_RECENT, updated.joinToString("\n")).apply()
        return updated
    }

    fun clear() {
        prefs.edit().remove(KEY_RECENT).apply()
    }

    private companion object {
        const val PREFS_NAME = "search_prefs"
        const val KEY_RECENT = "recent_searches"
        const val MAX_ENTRIES = 10
    }
}
