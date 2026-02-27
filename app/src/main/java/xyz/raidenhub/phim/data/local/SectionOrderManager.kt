package xyz.raidenhub.phim.data.local

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.raidenhub.phim.data.db.AppDatabase
import xyz.raidenhub.phim.data.db.entity.SectionOrderEntity

/**
 * H-6 — Home Section Reorder (Room-backed).
 */
object SectionOrderManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    data class HomeSection(val id: String, val label: String, val emoji: String)

    val ALL_SECTIONS = listOf(
        HomeSection("new",           "Phim Mới",          "🔥"),
        HomeSection("korean",        "K-Drama",           "🇰🇷"),
        HomeSection("series",        "Phim Bộ",           "📺"),
        HomeSection("single",        "Phim Lẻ",           "🎬"),
        HomeSection("anime",         "Hoạt Hình",         "🎌"),
        HomeSection("tvshows",       "TV Shows",          "📺"),
        HomeSection("fshare_movies", "Fshare Phim Lẻ",    "💎"),
        HomeSection("fshare_series", "Fshare Phim Bộ",    "💎"),
    )

    fun init(db: AppDatabase) {
        this.db = db
        // Seed defaults nếu DB trống (fresh install)
        scope.launch {
            val existing = db.sectionOrderDao().getAllOnce()
            if (existing.isEmpty()) {
                val defaults = ALL_SECTIONS.mapIndexed { idx, s ->
                    SectionOrderEntity(sectionId = s.id, position = idx, isVisible = true)
                }
                db.sectionOrderDao().upsertAll(defaults)
            }
        }
    }

    /** Reactive ordered list of VISIBLE section IDs (for HomeScreen) */
    val visibleOrder: Flow<List<String>>
        get() = db.sectionOrderDao().getAll().map { list ->
            val visible = list.filter { it.isVisible }.map { it.sectionId }
            val allIds = ALL_SECTIONS.map { it.id }
            val missing = allIds.filter { id -> id !in list.map { it.sectionId } }
            visible + missing
        }

    /** Reactive ordered list of ALL section IDs with visibility (for Settings) */
    val order: Flow<List<String>>
        get() = db.sectionOrderDao().getAll().map { list ->
            val ids = list.map { it.sectionId }
            val missing = ALL_SECTIONS.map { it.id }.filter { it !in ids }
            ids + missing
        }

    /** Reactive map of sectionId → isVisible */
    val visibility: Flow<Map<String, Boolean>>
        get() = db.sectionOrderDao().getAll().map { list ->
            list.associate { it.sectionId to it.isVisible }
        }

    fun reorder(newOrder: List<String>) {
        scope.launch {
            val entities = newOrder.mapIndexed { idx, id ->
                SectionOrderEntity(sectionId = id, position = idx)
            }
            db.sectionOrderDao().upsertAll(entities)
        }
    }

    fun moveUp(id: String) {
        scope.launch {
            val current = db.sectionOrderDao().getAllOnce().map { it.sectionId }.toMutableList()
            val idx = current.indexOf(id)
            if (idx > 0) {
                current.removeAt(idx)
                current.add(idx - 1, id)
                reorder(current)
            }
        }
    }

    fun moveDown(id: String) {
        scope.launch {
            val current = db.sectionOrderDao().getAllOnce().map { it.sectionId }.toMutableList()
            val idx = current.indexOf(id)
            if (idx < current.size - 1) {
                current.removeAt(idx)
                current.add(idx + 1, id)
                reorder(current)
            }
        }
    }

    fun reset() {
        reorder(ALL_SECTIONS.map { it.id })
    }

    fun getSectionInfo(id: String) = ALL_SECTIONS.find { it.id == id }

    fun toggleVisibility(id: String) {
        scope.launch {
            val all = db.sectionOrderDao().getAllOnce()
            val entity = all.find { it.sectionId == id }
            if (entity != null) {
                db.sectionOrderDao().upsert(entity.copy(isVisible = !entity.isVisible))
            } else {
                // Not in DB yet — add with visibility=false (hiding)
                val pos = ALL_SECTIONS.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: all.size
                db.sectionOrderDao().upsert(SectionOrderEntity(sectionId = id, position = pos, isVisible = false))
            }
        }
    }

    fun isVisible(id: String): Boolean {
        return runBlocking {
            db.sectionOrderDao().getAllOnce().find { it.sectionId == id }?.isVisible ?: true
        }
    }
}
