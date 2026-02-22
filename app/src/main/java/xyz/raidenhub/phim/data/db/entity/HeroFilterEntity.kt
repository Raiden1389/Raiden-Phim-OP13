package xyz.raidenhub.phim.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_heroes")
data class HeroFilterEntity(
    @PrimaryKey val slug: String,
    val hiddenAt: Long = System.currentTimeMillis()
)
