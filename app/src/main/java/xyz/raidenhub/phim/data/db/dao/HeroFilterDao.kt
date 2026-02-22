package xyz.raidenhub.phim.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.raidenhub.phim.data.db.entity.HeroFilterEntity

@Dao
interface HeroFilterDao {
    @Query("SELECT slug FROM hidden_heroes")
    fun getHiddenSlugs(): Flow<List<String>>

    @Query("SELECT slug FROM hidden_heroes")
    suspend fun getHiddenSlugsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hide(entity: HeroFilterEntity)

    @Query("DELETE FROM hidden_heroes WHERE slug = :slug")
    suspend fun unhide(slug: String)

    @Query("SELECT COUNT(*) FROM hidden_heroes")
    fun count(): Flow<Int>

    @Query("DELETE FROM hidden_heroes")
    suspend fun clearAll()
}
