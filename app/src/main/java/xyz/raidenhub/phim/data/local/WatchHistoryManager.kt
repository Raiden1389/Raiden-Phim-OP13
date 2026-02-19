package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
    private const val TAG = "WatchHistory"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private val _continueList = MutableStateFlow<List<ContinueItem>>(emptyList())
    val continueList = _continueList.asStateFlow()

    private val _watchedEps = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())
    val watchedEps = _watchedEps.asStateFlow()

    data class ContinueItem(
        @SerializedName("slug") val slug: String = "",
        @SerializedName("name") val name: String = "",
        @SerializedName("thumbUrl") val thumbUrl: String = "",
        @SerializedName("source") val source: String = "ophim",
        @SerializedName("server") val server: Int = 0,
        @SerializedName("episode") val episode: Int = 0,
        @SerializedName("epName") val epName: String = "",
        @SerializedName("positionMs") val positionMs: Long = 0,
        @SerializedName("durationMs") val durationMs: Long = 0,
        @SerializedName("lastWatched") val lastWatched: Long = System.currentTimeMillis(),
        // English (Consumet) specific fields
        @SerializedName("episodeId") val episodeId: String = "",
        @SerializedName("filmName") val filmName: String = ""
    ) {
        val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        val isEnglish: Boolean get() = source == "english"
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _continueList.value = loadContinue()
        _watchedEps.value = loadWatched()
        Log.d(TAG, "init: ${_continueList.value.size} continue, ${_watchedEps.value.size} watched")
    }

    // ═══ Continue Watching ═══

    fun saveProgress(
        slug: String, name: String, thumbUrl: String, source: String,
        server: Int, episode: Int, epName: String,
        positionMs: Long, durationMs: Long
    ) {
        val threshold = durationMs * 0.9
        if (positionMs >= threshold && durationMs > 0) {
            markWatched(slug, episode)
            removeContinue(slug)
            return
        }
        if (positionMs < 30_000) return

        val current = _continueList.value.toMutableList()
        current.removeAll { it.slug == slug }
        current.add(0, ContinueItem(slug, name, thumbUrl, source, server, episode, epName, positionMs, durationMs))
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

    // ═══ English Continue Watching ═══

    fun saveEnglishProgress(
        mediaId: String, name: String, thumbUrl: String,
        episodeId: String, filmName: String, epName: String,
        positionMs: Long, durationMs: Long
    ) {
        val threshold = durationMs * 0.9
        if (positionMs >= threshold && durationMs > 0) {
            removeContinue(mediaId)
            return
        }
        if (positionMs < 30_000) return

        val current = _continueList.value.toMutableList()
        current.removeAll { it.slug == mediaId }
        current.add(0, ContinueItem(
            slug = mediaId, name = name, thumbUrl = thumbUrl,
            source = "english", epName = epName,
            positionMs = positionMs, durationMs = durationMs,
            episodeId = episodeId, filmName = filmName
        ))
        val trimmed = current.take(Constants.MAX_CONTINUE_ITEMS)
        _continueList.value = trimmed
        saveContinue(trimmed)
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
        val json = prefs.getString(KEY_CONTINUE, null)
        if (json.isNullOrBlank()) return emptyList()
        return try {
            // Array::class.java — tránh R8 strip TypeToken generic
            val arr = gson.fromJson(json, Array<ContinueItem>::class.java)
            arr?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "loadContinue FAILED: ${e.message}")
            emptyList()
        }
    }

    private fun saveContinue(items: List<ContinueItem>) {
        prefs.edit().putString(KEY_CONTINUE, gson.toJson(items)).commit()
    }

    private fun loadWatched(): Map<String, Set<Int>> {
        val json = prefs.getString(KEY_WATCHED, null)
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            // TypeToken OK cho primitive types (Map<String, List<Int>>)
            val raw: Map<String, List<Int>> = gson.fromJson(json, object : TypeToken<Map<String, List<Int>>>() {}.type)
            raw.mapValues { it.value.toSet() }
        } catch (e: Exception) {
            Log.e(TAG, "loadWatched FAILED: ${e.message}")
            emptyMap()
        }
    }

    private fun saveWatched(map: Map<String, Set<Int>>) {
        val raw = map.mapValues { it.value.toList() }
        prefs.edit().putString(KEY_WATCHED, gson.toJson(raw)).commit()
    }

    fun clearAll() {
        _continueList.value = emptyList()
        _watchedEps.value = emptyMap()
        prefs.edit().clear().commit()
    }
}
