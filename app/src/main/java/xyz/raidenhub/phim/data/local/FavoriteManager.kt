package xyz.raidenhub.phim.data.local

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.db.AppDatabase
import xyz.raidenhub.phim.data.db.entity.FavoriteEntity

/** UI-facing model — fields match old SharedPrefs Gson model */
@Immutable
data class FavoriteItem(
    val slug: String,
    val name: String,
    val thumbUrl: String,
    val posterUrl: String = "",
    val year: String = "",
    val quality: String = "",
    val source: String = "ophim",
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * FavoriteManager — migrated to Room DB (Phase 03).
 * API compatible với trước — UI không cần đổi.
 */
object FavoriteManager {
    private val TAG = "FavoriteManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    fun init(db: AppDatabase) {
        this.db = db
        Log.d(TAG, "init: Room-backed FavoriteManager ready")
    }

    /** Reactive Flow<List<FavoriteItem>> — dùng .collectAsState(initial=emptyList()) trong Compose */
    val favorites: Flow<List<FavoriteItem>>
        get() = db.favoriteDao().getAll().map { list ->
            list.map { it.toItem() }
        }

    /** Raw entity flow nếu cần truy cập trực tiếp */
    val favoritesRaw: Flow<List<FavoriteEntity>>
        get() = db.favoriteDao().getAll()

    /** Sync suspend — dùng trong Worker/background */
    suspend fun getFavoritesOnce(): List<FavoriteItem> =
        db.favoriteDao().getAllOnce().map { it.toItem() }

    fun isFavoriteFlow(slug: String): Flow<Boolean> =
        db.favoriteDao().isFavorite(slug)

    /** Toggle favorite — fire-and-forget từ UI */
    fun toggle(slug: String, name: String, thumbUrl: String = "", source: String = "ophim") {
        scope.launch {
            val exists = db.favoriteDao().isFavoriteOnce(slug)
            if (exists) {
                db.favoriteDao().delete(slug)
                Log.d(TAG, "removed favorite: $slug")
            } else {
                db.favoriteDao().insert(FavoriteEntity(
                    slug = slug,
                    name = name,
                    thumbUrl = thumbUrl,
                    source = source
                ))
                Log.d(TAG, "added favorite: $slug")
            }
        }
    }

    fun clearAll() {
        scope.launch { db.favoriteDao().clearAll() }
    }

    private fun FavoriteEntity.toItem() = FavoriteItem(
        slug = slug,
        name = name,
        thumbUrl = thumbUrl,
        posterUrl = posterUrl,
        year = year,
        quality = quality,
        source = source,
        addedAt = addedAt
    )
}
