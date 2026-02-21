package xyz.raidenhub.phim.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages search history using SharedPreferences.
 * Extracted from SearchScreen.kt during TD-4 (God Screen Split).
 */
object SearchHistoryManager {
    private const val PREF_NAME = "search_history"
    private const val KEY = "recent"
    private const val MAX_ITEMS = 15

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history = _history.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _history.value = prefs.getString(KEY, null)?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun add(query: String, context: Context) {
        if (query.isBlank() || query.length < 2) return
        val current = _history.value.toMutableList()
        current.remove(query)
        current.add(0, query.trim())
        val trimmed = current.take(MAX_ITEMS)
        _history.value = trimmed
        save(context, trimmed)
    }

    fun remove(query: String, context: Context) {
        val current = _history.value.toMutableList()
        current.remove(query)
        _history.value = current
        save(context, current)
    }

    fun clearAll(context: Context) {
        _history.value = emptyList()
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun save(context: Context, items: List<String>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY, items.joinToString("|||")).apply()
    }
}
