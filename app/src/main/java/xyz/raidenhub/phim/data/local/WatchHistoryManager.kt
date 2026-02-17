package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.raidenhub.phim.util.Constants

/**
 * Manages watch history: continue watching + watched episodes.
 */
object WatchHistoryManager {
    private const val PREF_NAME = "watch_history"
    private const val KEY_CONTINUE = "continue_list"
    private const val KEY_WATCHED = "watched_eps"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    // Continue watching items (ordered by lastWatched desc)
    private val _continueList = MutableStateFlow<List<ContinueItem>>(emptyList())
    val continueList = _continueList.asStateFlow()

    // Watched episodes: slug -> set of ep indices
    private val _watchedEps = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())
    val watchedEps = _watchedEps.asStateFlow()

    data class ContinueItem(
        val slug: String,
        val name: String,
        val thumbUrl: String = "",
        val source: String = "ophim",
        val server: Int = 0,
        val episode: Int = 0,
        val epName: String = "",
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val lastWatched: Long = System.currentTimeMillis()
    ) {
        val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _continueList.value = loadContinue()
        _watchedEps.value = loadWatched()
    }

    // ═══ Continue Watching ═══

    fun saveProgress(
        slug: String, name: String, thumbUrl: String, source: String,
        server: Int, episode: Int, epName: String,
        positionMs: Long, durationMs: Long
    ) {
        // Consider "watched" if >90% through
        val threshold = durationMs * 0.9
        if (positionMs >= threshold && durationMs > 0) {
            markWatched(slug, episode)
            removeContinue(slug)
            return
        }

        // Only save if > 30s watched (avoid accidental taps)
        if (positionMs < 30_000) return

        val current = _continueList.value.toMutableList()
        current.removeAll { it.slug == slug }
        current.add(0, ContinueItem(slug, name, thumbUrl, source, server, episode, epName, positionMs, durationMs))

        // Limit
        val trimmed = current.take(Constants.MAX_CONTINUE_ITEMS)
        _continueList.value = trimmed
        saveContinue(trimmed)
    }

    fun removeContinue(slug: String) {
        val current = _continueList.value.toMutableList()
        current.removeAll { it.slug == slug }
        _continueList.value = current
        saveContinue(current)
    }

    // ═══ Watched Episodes ═══

    fun markWatched(slug: String, epIdx: Int) {
        val current = _watchedEps.value.toMutableMap()
        val eps = current.getOrDefault(slug, emptySet()).toMutableSet()
        eps.add(epIdx)
        current[slug] = eps
        _watchedEps.value = current
        saveWatched(current)
    }

    fun isWatched(slug: String, epIdx: Int): Boolean {
        return _watchedEps.value[slug]?.contains(epIdx) == true
    }

    fun getWatchedSet(slug: String): Set<Int> {
        return _watchedEps.value[slug] ?: emptySet()
    }

    // ═══ Persistence ═══

    private fun loadContinue(): List<ContinueItem> {
        val json = prefs.getString(KEY_CONTINUE, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<ContinueItem>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    private fun saveContinue(items: List<ContinueItem>) {
        prefs.edit().putString(KEY_CONTINUE, gson.toJson(items)).apply()
    }

    private fun loadWatched(): Map<String, Set<Int>> {
        val json = prefs.getString(KEY_WATCHED, null) ?: return emptyMap()
        return try {
            val raw: Map<String, List<Int>> = gson.fromJson(json, object : TypeToken<Map<String, List<Int>>>() {}.type)
            raw.mapValues { it.value.toSet() }
        } catch (_: Exception) { emptyMap() }
    }

    private fun saveWatched(map: Map<String, Set<Int>>) {
        val raw = map.mapValues { it.value.toList() }
        prefs.edit().putString(KEY_WATCHED, gson.toJson(raw)).apply()
    }

    fun clearAll() {
        _continueList.value = emptyList()
        _watchedEps.value = emptyMap()
        prefs.edit().clear().apply()
    }
}
