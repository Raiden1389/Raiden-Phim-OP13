package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages favorite movies using SharedPreferences.
 */
object FavoriteManager {
    private const val PREF_NAME = "favorites"
    private const val KEY_FAVORITES = "fav_list"
    private const val TAG = "FavoriteManager"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favorites = _favorites.asStateFlow()

    data class FavoriteItem(
        @SerializedName("slug") val slug: String = "",
        @SerializedName("name") val name: String = "",
        @SerializedName("thumbUrl") val thumbUrl: String = "",
        @SerializedName("source") val source: String = "ophim",
        @SerializedName("addedAt") val addedAt: Long = System.currentTimeMillis()
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _favorites.value = loadFromPrefs()
        Log.d(TAG, "init: loaded ${_favorites.value.size} favorites")
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
            false
        } else {
            current.add(0, FavoriteItem(slug, name, thumbUrl, source))
            _favorites.value = current
            saveToPrefs(current)
            true
        }
    }

    private fun loadFromPrefs(): List<FavoriteItem> {
        val json = prefs.getString(KEY_FAVORITES, null)
        if (json.isNullOrBlank()) return emptyList()
        Log.d(TAG, "loadFromPrefs JSON: ${json.take(300)}")
        return try {
            // Dùng Array::class.java thay vì TypeToken — tránh R8 strip generic signature
            val arr = gson.fromJson(json, Array<FavoriteItem>::class.java)
            val result = arr?.toList() ?: emptyList()
            Log.d(TAG, "loadFromPrefs OK: ${result.size} items, first=${result.firstOrNull()?.slug}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "loadFromPrefs FAILED: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }

    private fun saveToPrefs(items: List<FavoriteItem>) {
        val json = gson.toJson(items)
        Log.d(TAG, "saveToPrefs: ${items.size} items, json=${json.take(300)}")
        prefs.edit().putString(KEY_FAVORITES, json).commit()
    }

    fun clearAll() {
        _favorites.value = emptyList()
        prefs.edit().clear().commit()
    }
}
