package xyz.raidenhub.phim.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val thumbUrl: String,
    val posterUrl: String = "",
    val year: String = "",
    val source: String = "ophim",
    val addedAt: Long = System.currentTimeMillis()
)
