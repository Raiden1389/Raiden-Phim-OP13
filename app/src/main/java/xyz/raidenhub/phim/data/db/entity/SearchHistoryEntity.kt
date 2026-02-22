package xyz.raidenhub.phim.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
    val count: Int = 1          // for Dynamic Trending (#S-5): rank by frequency
)
