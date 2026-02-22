package xyz.raidenhub.phim.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "section_order")
data class SectionOrderEntity(
    @PrimaryKey val sectionId: String,   // e.g. "kdrama", "phim_le", "anime"
    val position: Int,
    val isVisible: Boolean = true
)
