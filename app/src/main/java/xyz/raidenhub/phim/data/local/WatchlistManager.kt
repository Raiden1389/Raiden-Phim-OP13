package xyz.raidenhub.phim.data.local

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.db.AppDatabase
import xyz.raidenhub.phim.data.db.entity.PlaylistEntity
import xyz.raidenhub.phim.data.db.entity.PlaylistItemEntity
import xyz.raidenhub.phim.data.db.entity.WatchlistEntity
import xyz.raidenhub.phim.data.db.dao.PlaylistWithItems

// ═══ Model classes (UI-facing) — giữ nguyên để không phá UI ═══

@Immutable
data class WatchlistItem(
    val slug: String,
    val name: String,
    val thumbUrl: String,
    val source: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Immutable
data class Playlist(
    val id: String,
    val name: String,
    val items: List<WatchlistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

// ═══ WatchlistManager — "Xem Sau" ═══

object WatchlistManager {
    private val TAG = "WatchlistManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    fun init(db: AppDatabase) {
        this.db = db
    }

    val items: Flow<List<WatchlistItem>>
        get() = db.watchlistDao().getAll().map { list ->
            list.map { WatchlistItem(it.slug, it.name, it.thumbUrl, it.source, it.addedAt) }
        }

    fun isInWatchlistFlow(slug: String): Flow<Boolean> =
        db.watchlistDao().isInWatchlist(slug)

    fun toggle(slug: String, name: String, thumbUrl: String, source: String = "") {
        scope.launch {
            if (db.watchlistDao().isInWatchlistOnce(slug)) {
                db.watchlistDao().delete(slug)
                Log.d(TAG, "removed from watchlist: $slug")
            } else {
                db.watchlistDao().insert(WatchlistEntity(slug, name, thumbUrl, source = source))
                Log.d(TAG, "added to watchlist: $slug")
            }
        }
    }

    fun remove(slug: String) {
        scope.launch { db.watchlistDao().delete(slug) }
    }

    fun clearAll() {
        scope.launch { db.watchlistDao().clearAll() }
    }
}

// ═══ PlaylistManager — User custom playlists ═══

object PlaylistManager {
    private val TAG = "PlaylistManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    fun init(db: AppDatabase) {
        this.db = db
    }

    val playlists: Flow<List<Playlist>>
        get() = db.playlistDao().getAllPlaylistsWithItems().map { list ->
            list.map { it.toPlaylist() }
        }

    fun createPlaylist(name: String) {
        scope.launch {
            db.playlistDao().createPlaylist(PlaylistEntity(name = name))
            Log.d(TAG, "created playlist: $name")
        }
    }

    fun deletePlaylist(id: String) {
        scope.launch {
            db.playlistDao().deletePlaylist(id.toLongOrNull() ?: return@launch)
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        scope.launch {
            db.playlistDao().renamePlaylist(id.toLongOrNull() ?: return@launch, newName)
        }
    }

    fun addToPlaylist(playlistId: String, slug: String, name: String, thumbUrl: String, source: String = "") {
        scope.launch {
            db.playlistDao().addItem(PlaylistItemEntity(
                playlistId = playlistId.toLongOrNull() ?: return@launch,
                movieSlug = slug,
                movieName = name,
                thumbUrl = thumbUrl,
                source = source
            ))
        }
    }

    fun removeFromPlaylist(playlistId: String, slug: String) {
        scope.launch {
            db.playlistDao().removeItem(playlistId.toLongOrNull() ?: return@launch, slug)
        }
    }

    private fun PlaylistWithItems.toPlaylist() = Playlist(
        id = playlist.id.toString(),
        name = playlist.name,
        items = items.map { WatchlistItem(it.movieSlug, it.movieName, it.thumbUrl, it.source, it.addedAt) },
        createdAt = playlist.createdAt
    )
}
