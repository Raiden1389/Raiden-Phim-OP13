package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages favorite movies using SharedPreferences.
 * Stores movie slugs + basic info for display.
 */
object FavoriteManager {
    private const val PREF_NAME = "favorites"
    private const val KEY_FAVORITES = "fav_list"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favorites = _favorites.asStateFlow()

    data class FavoriteItem(
        val slug: String,
        val name: String,
        val thumbUrl: String = "",
        val source: String = "ophim",
        val addedAt: Long = System.currentTimeMillis()
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _favorites.value = loadFromPrefs()
    }

    fun isFavorite(slug: String): Boolean {
        return _favorites.value.any { it.slug == slug }
    }

    fun toggle(slug: String, name: String, thumbUrl: String = "", source: String = "ophim"): Boolean {
        val current = _favorites.value.toMutableList()
        val existing = current.find { it.slug == slug }
        return if (existing != null) {
            current.remove(existing)
            _favorites.value = current
            saveToPrefs(current)
            false // removed
        } else {
            current.add(0, FavoriteItem(slug, name, thumbUrl, source))
            _favorites.value = current
            saveToPrefs(current)
            true // added
        }
    }

    private fun loadFromPrefs(): List<FavoriteItem> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<FavoriteItem>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToPrefs(items: List<FavoriteItem>) {
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(items)).apply()
    }

    fun clearAll() {
        _favorites.value = emptyList()
        prefs.edit().clear().apply()
    }
}
