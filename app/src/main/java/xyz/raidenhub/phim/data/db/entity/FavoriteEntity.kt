package xyz.raidenhub.phim.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_movies")
data class FavoriteEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val thumbUrl: String,
    val posterUrl: String = "",
    val year: String = "",
    val quality: String = "",
    val source: String = "ophim",
    val addedAt: Long = System.currentTimeMillis()
)
