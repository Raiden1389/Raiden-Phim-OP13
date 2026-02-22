package xyz.raidenhub.phim.data.local

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.db.AppDatabase

/**
 * SearchHistoryManager — migrated to Room (Phase 03).
 * Bonus: count field enables #S-5 Dynamic Trending.
 */
object SearchHistoryManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    fun init(db: AppDatabase) {
        this.db = db
    }

    /** Reactive list — 15 most recent, descending */
    val history: Flow<List<String>>
        get() = db.searchHistoryDao().getRecent(15).map { list -> list.map { it.query } }

    /** Add search, update count if exists (upsert) — no Context needed */
    fun add(query: String) {
        if (query.isBlank() || query.length < 2) return
        scope.launch { db.searchHistoryDao().addSearch(query.trim()) }
    }

    /** Backward-compat overload (context ignored) */
    fun add(query: String, @Suppress("UNUSED_PARAMETER") context: Context) = add(query)

    fun remove(query: String) {
        scope.launch { db.searchHistoryDao().delete(query) }
    }

    fun remove(query: String, @Suppress("UNUSED_PARAMETER") context: Context) = remove(query)

    fun clearAll() {
        scope.launch { db.searchHistoryDao().clearAll() }
    }

    fun clearAll(@Suppress("UNUSED_PARAMETER") context: Context) = clearAll()
}
