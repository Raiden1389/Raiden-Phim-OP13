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
        HomeSection("new",     "Phim Mới",   "🔥"),
        HomeSection("korean",  "K-Drama",    "🇰🇷"),
        HomeSection("series",  "Phim Bộ",    "📺"),
        HomeSection("single",  "Phim Lẻ",    "🎬"),
        HomeSection("anime",   "Hoạt Hình",  "🎌"),
        HomeSection("tvshows", "TV Shows",   "📺"),
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

    /** Reactive ordered list of section IDs */
    val order: Flow<List<String>>
        get() = db.sectionOrderDao().getAll().map { list ->
            val ids = list.map { it.sectionId }
            // Merge new sections not in DB yet
            val missing = ALL_SECTIONS.map { it.id }.filter { it !in ids }
            ids + missing
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
}
