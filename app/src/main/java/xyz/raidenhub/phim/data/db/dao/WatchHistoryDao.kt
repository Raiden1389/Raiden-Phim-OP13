package xyz.raidenhub.phim.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.raidenhub.phim.data.db.entity.ContinueWatchingEntity
import xyz.raidenhub.phim.data.db.entity.WatchedEpisodeEntity

@Dao
interface WatchHistoryDao {

    // ═══ Continue Watching ═══

    @Query("SELECT * FROM continue_watching ORDER BY lastWatched DESC")
    fun getContinueWatching(): Flow<List<ContinueWatchingEntity>>

    @Query("SELECT * FROM continue_watching WHERE slug = :slug")
    suspend fun getContinueItem(slug: String): ContinueWatchingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContinue(item: ContinueWatchingEntity)

    @Query("DELETE FROM continue_watching WHERE slug = :slug")
    suspend fun removeContinue(slug: String)

    @Query("DELETE FROM continue_watching")
    suspend fun clearAllContinue()

    @Query("SELECT COUNT(*) FROM continue_watching")
    fun continueCount(): Flow<Int>

    // ═══ Watched Episodes ═══

    @Query("SELECT episodeIdx FROM watched_episodes WHERE slug = :slug")
    fun getWatchedEpisodes(slug: String): Flow<List<Int>>

    @Query("SELECT episodeIdx FROM watched_episodes WHERE slug = :slug")
    suspend fun getWatchedEpisodesOnce(slug: String): List<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM watched_episodes WHERE slug = :slug AND episodeIdx = :epIdx)")
    suspend fun isWatched(slug: String, epIdx: Int): Boolean

    @Query("SELECT COUNT(*) FROM watched_episodes WHERE slug = :slug")
    suspend fun watchedCount(slug: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markWatched(ep: WatchedEpisodeEntity)

    @Query("DELETE FROM watched_episodes WHERE slug = :slug")
    suspend fun clearWatchedForSlug(slug: String)

    @Query("DELETE FROM watched_episodes")
    suspend fun clearAllWatched()
}
