package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject

// ═══ Watchlist Item ═══
data class WatchlistItem(
    val slug: String,
    val name: String,
    val thumbUrl: String,
    val source: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

// ═══ Playlist ═══
data class Playlist(
    val id: String,
    val name: String,
    val items: List<WatchlistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

// ═══ WatchlistManager — "Xem Sau" ═══
object WatchlistManager {
    private lateinit var prefs: SharedPreferences
    private const val KEY_WATCHLIST = "watchlist_v1"

    private val _items = MutableStateFlow<List<WatchlistItem>>(emptyList())
    val items = _items.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("watchlist", Context.MODE_PRIVATE)
        _items.value = load()
    }

    fun isInWatchlist(slug: String) = _items.value.any { it.slug == slug }

    fun toggle(slug: String, name: String, thumbUrl: String, source: String = "") {
        val current = _items.value.toMutableList()
        val idx = current.indexOfFirst { it.slug == slug }
        if (idx >= 0) current.removeAt(idx)
        else current.add(0, WatchlistItem(slug, name, thumbUrl, source))
        _items.value = current
        save(current)
    }

    fun remove(slug: String) {
        val current = _items.value.filter { it.slug != slug }
        _items.value = current
        save(current)
    }

    fun clearAll() {
        _items.value = emptyList()
        prefs.edit().remove(KEY_WATCHLIST).apply()
    }

    private fun load(): List<WatchlistItem> {
        val json = prefs.getString(KEY_WATCHLIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                WatchlistItem(
                    slug = o.optString("slug"),
                    name = o.optString("name"),
                    thumbUrl = o.optString("thumbUrl"),
                    source = o.optString("source"),
                    addedAt = o.optLong("addedAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun save(items: List<WatchlistItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("slug", item.slug)
                put("name", item.name)
                put("thumbUrl", item.thumbUrl)
                put("source", item.source)
                put("addedAt", item.addedAt)
            })
        }
        prefs.edit().putString(KEY_WATCHLIST, arr.toString()).apply()
    }
}

// ═══ PlaylistManager — User custom playlists ═══
object PlaylistManager {
    private lateinit var prefs: SharedPreferences
    private const val KEY_PLAYLISTS = "playlists_v1"

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("playlists", Context.MODE_PRIVATE)
        _playlists.value = load()
    }

    fun createPlaylist(name: String): String {
        val id = System.currentTimeMillis().toString()
        val updated = _playlists.value + Playlist(id = id, name = name)
        _playlists.value = updated
        save(updated)
        return id
    }

    fun deletePlaylist(id: String) {
        val updated = _playlists.value.filter { it.id != id }
        _playlists.value = updated
        save(updated)
    }

    fun renamePlaylist(id: String, newName: String) {
        val updated = _playlists.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
        _playlists.value = updated
        save(updated)
    }

    fun addToPlaylist(playlistId: String, slug: String, name: String, thumbUrl: String, source: String = "") {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.items.any { it.slug == slug }) pl
                else pl.copy(items = pl.items + WatchlistItem(slug, name, thumbUrl, source))
            } else pl
        }
        _playlists.value = updated
        save(updated)
    }

    fun removeFromPlaylist(playlistId: String, slug: String) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) pl.copy(items = pl.items.filter { it.slug != slug })
            else pl
        }
        _playlists.value = updated
        save(updated)
    }

    fun isInPlaylist(playlistId: String, slug: String): Boolean =
        _playlists.value.find { it.id == playlistId }?.items?.any { it.slug == slug } ?: false

    private fun load(): List<Playlist> {
        val json = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val itemsArr = o.optJSONArray("items") ?: JSONArray()
                Playlist(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    items = (0 until itemsArr.length()).map { j ->
                        val item = itemsArr.getJSONObject(j)
                        WatchlistItem(
                            slug = item.optString("slug"),
                            name = item.optString("name"),
                            thumbUrl = item.optString("thumbUrl"),
                            source = item.optString("source"),
                            addedAt = item.optLong("addedAt", 0)
                        )
                    },
                    createdAt = o.optLong("createdAt", 0)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun save(playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val itemsArr = JSONArray()
            pl.items.forEach { item ->
                itemsArr.put(JSONObject().apply {
                    put("slug", item.slug)
                    put("name", item.name)
                    put("thumbUrl", item.thumbUrl)
                    put("source", item.source)
                    put("addedAt", item.addedAt)
                })
            }
            arr.put(JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("items", itemsArr)
                put("createdAt", pl.createdAt)
            })
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }
}
