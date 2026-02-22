package xyz.raidenhub.phim.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.raidenhub.phim.data.db.entity.SearchHistoryEntity

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 15): Flow<List<SearchHistoryEntity>>

    /** For #S-5 Dynamic Trending: top queries by frequency */
    @Query("SELECT * FROM search_history ORDER BY count DESC LIMIT :limit")
    suspend fun getTrending(limit: Int = 16): List<SearchHistoryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM search_history WHERE query = :query)")
    suspend fun exists(query: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(item: SearchHistoryEntity)

    @Query("UPDATE search_history SET count = count + 1, searchedAt = :now WHERE query = :query")
    suspend fun incrementCount(query: String, now: Long = System.currentTimeMillis())

    /** Upsert: insert new OR increment count if exists */
    @Transaction
    suspend fun addSearch(query: String) {
        if (exists(query)) {
            incrementCount(query)
        } else {
            insertNew(SearchHistoryEntity(query = query))
        }
    }

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
