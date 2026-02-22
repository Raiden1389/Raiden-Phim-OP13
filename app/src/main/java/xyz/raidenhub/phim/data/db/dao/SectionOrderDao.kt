package xyz.raidenhub.phim.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.raidenhub.phim.data.db.entity.SectionOrderEntity

@Dao
interface SectionOrderDao {
    @Query("SELECT * FROM section_order ORDER BY position ASC")
    fun getAll(): Flow<List<SectionOrderEntity>>

    @Query("SELECT * FROM section_order ORDER BY position ASC")
    suspend fun getAllOnce(): List<SectionOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SectionOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SectionOrderEntity>)

    @Query("DELETE FROM section_order")
    suspend fun clearAll()
}
