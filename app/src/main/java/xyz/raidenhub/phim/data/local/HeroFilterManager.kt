package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * H-1 — Hero Carousel Filter
 * Lưu danh sách slug bị ẩn khỏi Hero Carousel (long press → "Bỏ qua phim này").
 */
object HeroFilterManager {
    private const val PREF_NAME = "hero_filter"
    private const val KEY_HIDDEN = "hidden_slugs"

    private lateinit var prefs: SharedPreferences

    private val _hiddenSlugs = MutableStateFlow<Set<String>>(emptySet())
    val hiddenSlugs = _hiddenSlugs.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _hiddenSlugs.value = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()
    }

    fun hide(slug: String) {
        val updated = _hiddenSlugs.value + slug
        _hiddenSlugs.value = updated
        prefs.edit().putStringSet(KEY_HIDDEN, updated).apply()
    }

    fun isHidden(slug: String) = _hiddenSlugs.value.contains(slug)

    fun clearAll() {
        _hiddenSlugs.value = emptySet()
        prefs.edit().remove(KEY_HIDDEN).apply()
    }

    val hiddenCount: Int get() = _hiddenSlugs.value.size
}
