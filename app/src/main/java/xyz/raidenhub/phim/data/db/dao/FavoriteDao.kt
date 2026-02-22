package xyz.raidenhub.phim.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.raidenhub.phim.data.db.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_movies ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorite_movies ORDER BY addedAt DESC")
    suspend fun getAllOnce(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE slug = :slug)")
    fun isFavorite(slug: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE slug = :slug)")
    suspend fun isFavoriteOnce(slug: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavoriteEntity)

    @Query("DELETE FROM favorite_movies WHERE slug = :slug")
    suspend fun delete(slug: String)

    @Query("DELETE FROM favorite_movies")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM favorite_movies")
    fun count(): Flow<Int>
}
