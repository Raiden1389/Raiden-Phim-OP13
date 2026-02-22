package xyz.raidenhub.phim.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.raidenhub.phim.data.db.entity.WatchlistEntity

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE slug = :slug)")
    fun isInWatchlist(slug: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE slug = :slug)")
    suspend fun isInWatchlistOnce(slug: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE slug = :slug")
    suspend fun delete(slug: String)

    @Query("DELETE FROM watchlist")
    suspend fun clearAll()
}
