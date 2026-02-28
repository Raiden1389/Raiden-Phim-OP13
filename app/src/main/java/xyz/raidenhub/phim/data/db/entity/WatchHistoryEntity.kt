package xyz.raidenhub.phim.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Continue Watching — phim đang xem dở (1 entry per slug, upsert on progress save)
 */
// P4: Index on lastWatched — ORDER BY lastWatched DESC được tối ưu hóa
@Entity(
    tableName = "continue_watching",
    indices = [Index(value = ["lastWatched"])]
)
data class ContinueWatchingEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val thumbUrl: String,
    val source: String = "ophim",
    val episodeIdx: Int = 0,
    val episodeName: String = "",
    val episodeSlug: String = "",       // Fshare episode URL for direct resume
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatched: Long = System.currentTimeMillis()
) {
    val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
}

/**
 * Watched Episodes — set of episode indices watched per slug
 * Composite PK: slug + episodeIdx
 */
// P4: Index on slug — query getWatchedEpisodes(slug) tối ưu hóa
@Entity(
    tableName = "watched_episodes",
    primaryKeys = ["slug", "episodeIdx"],
    indices = [Index(value = ["slug"])]
)
data class WatchedEpisodeEntity(
    val slug: String,
    val episodeIdx: Int,
    val watchedAt: Long = System.currentTimeMillis()
)
