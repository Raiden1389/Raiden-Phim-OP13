package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * H-6 — Home Section Reorder
 * Lưu thứ tự các section row trên Home theo preference của user.
 */
object SectionOrderManager {
    private const val PREF_NAME = "section_order"
    private const val KEY_ORDER = "order"

    // Định nghĩa các section có thể reorder
    data class HomeSection(val id: String, val label: String, val emoji: String)

    val ALL_SECTIONS = listOf(
        HomeSection("new",     "Phim Mới",    "🔥"),
        HomeSection("korean",  "K-Drama",     "🇰🇷"),
        HomeSection("series",  "Phim Bộ",     "📺"),
        HomeSection("single",  "Phim Lẻ",     "🎬"),
        HomeSection("anime",   "Hoạt Hình",   "🎌"),
        HomeSection("tvshows", "TV Shows",    "📺"),
    )

    private lateinit var prefs: SharedPreferences

    private val _order = MutableStateFlow(ALL_SECTIONS.map { it.id })
    val order = _order.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_ORDER, null)
        if (saved != null) {
            val ids = saved.split(",").filter { id -> ALL_SECTIONS.any { it.id == id } }
            // Thêm section mới nếu chưa có trong saved order
            val missing = ALL_SECTIONS.map { it.id }.filter { it !in ids }
            _order.value = ids + missing
        }
    }

    fun reorder(newOrder: List<String>) {
        _order.value = newOrder
        prefs.edit().putString(KEY_ORDER, newOrder.joinToString(",")).apply()
    }

    fun moveUp(id: String) {
        val current = _order.value.toMutableList()
        val idx = current.indexOf(id)
        if (idx > 0) {
            current.removeAt(idx)
            current.add(idx - 1, id)
            reorder(current)
        }
    }

    fun moveDown(id: String) {
        val current = _order.value.toMutableList()
        val idx = current.indexOf(id)
        if (idx < current.size - 1) {
            current.removeAt(idx)
            current.add(idx + 1, id)
            reorder(current)
        }
    }

    fun reset() {
        val defaultOrder = ALL_SECTIONS.map { it.id }
        reorder(defaultOrder)
    }

    fun getSectionInfo(id: String) = ALL_SECTIONS.find { it.id == id }
}
