package xyz.raidenhub.phim.data.local

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.raidenhub.phim.data.db.AppDatabase
import xyz.raidenhub.phim.data.db.entity.ContinueWatchingEntity
import xyz.raidenhub.phim.data.db.entity.WatchedEpisodeEntity

// ═══ UI-facing models — không đổi để UI không bị ảnh hưởng ═══

@Immutable
data class ContinueItem(
    val slug: String,
    val name: String,
    val thumbUrl: String,
    val episodeIdx: Int,
    val episodeName: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val source: String = "ophim",
    val lastWatched: Long = System.currentTimeMillis()
) {
    /** Compat aliases for UI code that uses old field names */
    val episode: Int get() = episodeIdx
    val epName: String get() = episodeName
    val server: Int get() = 0  // Room doesn't track server idx — default 0
    val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
}

/**
 * WatchHistoryManager — migrated to Room (Phase 03).
 * Manages:
 *   • ContinueWatching (per-movie progress)
 *   • WatchedEpisodes  (per-episode completion)
 */
object WatchHistoryManager {
    private const val TAG = "WatchHistoryMgr"
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    fun init(db: AppDatabase) {
        this.db = db
        Log.d(TAG, "init: Room-backed WatchHistoryManager ready")
    }

    // ═══ Continue Watching ═══

    /** Reactive Flow for Home & Continue Watching screen */
    val continueWatching: Flow<List<ContinueItem>>
        get() = db.watchHistoryDao().getContinueWatching().map { list ->
            list.map { it.toContinueItem() }
        }

    /** Compat alias — UI uses continueList.collectAsState() */
    val continueList: Flow<List<ContinueItem>> get() = continueWatching

    fun getContinueItem(slug: String): ContinueItem? = runBlocking(Dispatchers.IO) {
        db.watchHistoryDao().getContinueItem(slug)?.toContinueItem()
    }

    fun updateContinue(
        slug: String,
        name: String,
        thumbUrl: String,
        episodeIdx: Int,
        episodeName: String,
        positionMs: Long,
        durationMs: Long,
        source: String = "ophim"
    ) {
        scope.launch {
            db.watchHistoryDao().upsertContinue(
                ContinueWatchingEntity(
                    slug = slug,
                    name = name,
                    thumbUrl = thumbUrl,
                    episodeIdx = episodeIdx,
                    episodeName = episodeName,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    source = source,
                    lastWatched = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeContinue(slug: String) {
        scope.launch { db.watchHistoryDao().removeContinue(slug) }
    }

    /** IA-2: Pin item to top by refreshing lastWatched timestamp */
    fun pinToTop(slug: String) {
        scope.launch {
            db.watchHistoryDao().getContinueItem(slug)?.let { entity ->
                db.watchHistoryDao().upsertContinue(entity.copy(lastWatched = System.currentTimeMillis()))
            }
        }
    }

    fun clearAllContinue() {
        scope.launch { db.watchHistoryDao().clearAllContinue() }
    }

    // ═══ Watched Episodes ═══

    fun getWatchedEpisodes(slug: String): Flow<List<Int>> =
        db.watchHistoryDao().getWatchedEpisodes(slug)

    fun getWatchedEpisodesSync(slug: String): List<Int> = runBlocking(Dispatchers.IO) {
        db.watchHistoryDao().getWatchedEpisodesOnce(slug)
    }

    fun markWatched(slug: String, episodeIdx: Int) {
        scope.launch {
            db.watchHistoryDao().markWatched(
                WatchedEpisodeEntity(slug = slug, episodeIdx = episodeIdx)
            )
        }
    }

    fun isWatched(slug: String, episodeIdx: Int): Boolean = runBlocking(Dispatchers.IO) {
        db.watchHistoryDao().isWatched(slug, episodeIdx)
    }

    /** For #UX-2 Episode Tracker Badge */
    suspend fun watchedCount(slug: String): Int =
        db.watchHistoryDao().watchedCount(slug)

    fun clearHistory(slug: String) {
        scope.launch {
            db.watchHistoryDao().clearWatchedForSlug(slug)
            db.watchHistoryDao().removeContinue(slug)
        }
    }

    fun clearAll() {
        scope.launch {
            db.watchHistoryDao().clearAllContinue()
            db.watchHistoryDao().clearAllWatched()
        }
    }

    // ═══ Helpers ═══

    private fun ContinueWatchingEntity.toContinueItem() = ContinueItem(
        slug = slug,
        name = name,
        thumbUrl = thumbUrl,
        episodeIdx = episodeIdx,
        episodeName = episodeName,
        positionMs = positionMs,
        durationMs = durationMs,
        source = source,
        lastWatched = lastWatched
    )
}
